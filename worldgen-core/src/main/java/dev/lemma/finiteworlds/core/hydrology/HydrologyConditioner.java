package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Hydrology Pass 1G: applies the two conservative conditioning actions that
 * are currently considered safe enough to test on the hydrology working
 * surface.
 *
 * FILL depressions are raised to their measured spill level with a minute
 * drainage gradient. BREACH depressions receive a narrow descending channel
 * from the raw sink through the measured spill saddle and into naturally
 * lower downstream terrain.
 *
 * PRESERVE_LAKE and MERGE_COMPOUND basins remain untouched. The authored
 * terrain elevation in WorldBlueprint is never modified by this pass.
 */
public final class HydrologyConditioner {

    private static final float FILL_GRADIENT_PER_CELL =
            0.0001f;

    private static final float BREACH_GRADIENT_PER_GRID_UNIT =
            0.0002f;

    /*
     * Pass 1G is intentionally conservative. A proposed breach that would
     * require a cut deeper than this remains unresolved for a later pass
     * instead of excavating a major trench through the macro terrain.
     */
    private static final float MAXIMUM_BREACH_CUT =
            6.0f;

    private static final int MAXIMUM_DOWNSTREAM_TRACE =
            384;

    private static final double VERTICAL_PATH_COST_WEIGHT =
            4.0;

    private HydrologyConditioner() {
    }

    public static void applyFillAndBreach(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        hydrology.resetConditionedSurface();

        List<Depression> depressions =
                hydrology.depressions();

        if (
                depressions.isEmpty()
                        || hydrology.depressionResolutionPlans().isEmpty()
        ) {
            HydrologyPlanner.populateConditionedFlow(
                    world
            );
            return;
        }

        boolean[] shouldFill =
                new boolean[depressions.size()];

        boolean[] shouldBreach =
                new boolean[depressions.size()];

        for (DepressionResolutionPlan plan : hydrology.depressionResolutionPlans()) {
            int id =
                    plan.depressionId();

            if (
                    id < 0
                            || id >= depressions.size()
            ) {
                continue;
            }

            switch (plan.action()) {
                case FILL -> shouldFill[id] = true;
                case BREACH -> shouldBreach[id] = true;
                case PRESERVE_LAKE,
                     MERGE_COMPOUND -> {
                    /* intentionally untouched */
                }
            }
        }

        applyFills(
                hydrology,
                depressions,
                shouldFill
        );

        /*
         * Breach planning uses the post-fill D8 field as its downstream
         * guide. It is intentionally computed once here rather than after
         * every individual breach.
         */
        HydrologyPlanner.populateConditionedFlow(
                world
        );

        applyBreaches(
                world,
                hydrology,
                depressions,
                shouldBreach
        );

        HydrologyPlanner.populateConditionedFlow(
                world
        );
    }

    private static void applyFills(
            HydrologyGrid hydrology,
            List<Depression> depressions,
            boolean[] shouldFill
    ) {
        List<List<Integer>> footprintCells =
                createFootprintLists(
                        hydrology,
                        shouldFill
                );

        int[] distance =
                new int[
                        hydrology.resolution()
                                * hydrology.resolution()
                ];

        Arrays.fill(
                distance,
                -1
        );

        for (Depression depression : depressions) {
            int id =
                    depression.id();

            if (
                    id < 0
                            || id >= shouldFill.length
                            || !shouldFill[id]
            ) {
                continue;
            }

            List<Integer> cells =
                    footprintCells.get(id);

            if (cells.isEmpty()) {
                continue;
            }

            applyFillGradient(
                    hydrology,
                    depression,
                    cells,
                    distance
            );
        }
    }

    private static void applyBreaches(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            List<Depression> depressions,
            boolean[] shouldBreach
    ) {
        List<Depression> ordered =
                new ArrayList<>();

        for (Depression depression : depressions) {
            int id =
                    depression.id();

            if (
                    id >= 0
                            && id < shouldBreach.length
                            && shouldBreach[id]
            ) {
                ordered.add(depression);
            }
        }

        /*
         * Resolve downstream basins before their upstream tributary basins.
         * Pass 1D chainDepth is 1 nearest a terminal outlet/group and grows
         * upstream.
         */
        ordered.sort(
                Comparator.comparingInt(
                        depression -> {
                            DepressionClassification classification =
                                    hydrology.depressionClassification(
                                            depression.id()
                                    );

                            return classification != null
                                    ? classification.chainDepth()
                                    : Integer.MAX_VALUE;
                        }
                )
        );

        BreachPathFinder pathFinder =
                new BreachPathFinder(
                        hydrology.resolution()
                );

        for (Depression depression : ordered) {
            BreachCandidate candidate =
                    buildBreachCandidate(
                            world,
                            hydrology,
                            depression,
                            pathFinder
                    );

            if (
                    candidate == null
                            || candidate.maximumCutDepth()
                            > MAXIMUM_BREACH_CUT
            ) {
                hydrology.recordSkippedBreach();
                continue;
            }

            applyBreachCandidate(
                    hydrology,
                    candidate
            );

            hydrology.recordAppliedBreach();
        }
    }

    private static BreachCandidate buildBreachCandidate(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            Depression depression,
            BreachPathFinder pathFinder
    ) {
        if (!depression.hasMeasuredSpill()) {
            return null;
        }

        int size =
                hydrology.resolution();

        List<Integer> interiorPath =
                pathFinder.findInteriorPath(
                        hydrology,
                        depression
                );

        if (interiorPath.isEmpty()) {
            return null;
        }

        List<PathStep> steps =
                new ArrayList<>(
                        interiorPath.size() + 16
                );

        double sourceElevation =
                hydrology.conditionedElevation(
                        depression.sinkX(),
                        depression.sinkZ()
                );

        double targetElevation =
                sourceElevation;

        int previous =
                interiorPath.getFirst();

        steps.add(
                new PathStep(
                        previous,
                        (float) targetElevation
                )
        );

        for (int i = 1; i < interiorPath.size(); i++) {
            int current =
                    interiorPath.get(i);

            targetElevation -=
                    BREACH_GRADIENT_PER_GRID_UNIT
                            * gridDistance(
                            previous,
                            current,
                            size
                    );

            steps.add(
                    new PathStep(
                            current,
                            (float) targetElevation
                    )
            );

            previous = current;
        }

        int spillTarget =
                index(
                        depression.spillTargetX(),
                        depression.spillTargetZ(),
                        size
                );

        targetElevation -=
                BREACH_GRADIENT_PER_GRID_UNIT
                        * gridDistance(
                        previous,
                        spillTarget,
                        size
                );

        DownstreamExtension extension =
                extendIntoDownstreamTerrain(
                        world,
                        hydrology,
                        depression,
                        spillTarget,
                        targetElevation
                );

        if (!extension.valid()) {
            return null;
        }

        steps.addAll(
                extension.steps()
        );

        float maximumCut =
                0.0f;

        for (PathStep step : steps) {
            int x =
                    step.index() % size;

            int z =
                    step.index() / size;

            float existing =
                    hydrology.conditionedElevation(
                            x,
                            z
                    );

            maximumCut =
                    Math.max(
                            maximumCut,
                            existing - step.targetElevation()
                    );
        }

        return new BreachCandidate(
                depression.id(),
                List.copyOf(steps),
                Math.max(
                        0.0f,
                        maximumCut
                )
        );
    }

    private static DownstreamExtension extendIntoDownstreamTerrain(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            Depression source,
            int startIndex,
            double initialTargetElevation
    ) {
        int size =
                hydrology.resolution();

        List<PathStep> steps =
                new ArrayList<>();

        boolean[] visited =
                new boolean[
                        size * size
                ];

        int current =
                startIndex;

        int previous =
                index(
                        source.spillX(),
                        source.spillZ(),
                        size
                );

        double targetElevation =
                initialTargetElevation;

        for (int trace = 0; trace < MAXIMUM_DOWNSTREAM_TRACE; trace++) {
            if (
                    current < 0
                            || current >= visited.length
                            || visited[current]
            ) {
                return DownstreamExtension.invalid();
            }

            visited[current] = true;

            int x =
                    current % size;

            int z =
                    current / size;

            if (!isLand(world, x, z)) {
                return new DownstreamExtension(
                        true,
                        List.copyOf(steps)
                );
            }

            float existing =
                    hydrology.conditionedElevation(
                            x,
                            z
                    );

            /*
             * Natural terrain is already lower than the breach channel.
             * Stop before touching it; the final recomputed D8 field will
             * connect the cut directly into this lower cell.
             */
            if (
                    existing
                            < targetElevation
                            - BREACH_GRADIENT_PER_GRID_UNIT * 0.5
            ) {
                return new DownstreamExtension(
                        true,
                        List.copyOf(steps)
                );
            }

            int depressionId =
                    hydrology.depressionId(
                            x,
                            z
                    );

            DepressionResolutionPlan targetPlan =
                    hydrology.depressionResolutionPlan(
                            depressionId
                    );

            if (
                    targetPlan != null
                            && (
                            targetPlan.action()
                                    == DepressionResolutionAction.PRESERVE_LAKE
                                    || targetPlan.action()
                                    == DepressionResolutionAction.MERGE_COMPOUND
                    )
            ) {
                /*
                 * Pass 1G promises not to excavate preserved lakes or
                 * compound systems. If they are not already low enough to
                 * accept the incoming breach naturally, defer this breach.
                 */
                return DownstreamExtension.invalid();
            }

            steps.add(
                    new PathStep(
                            current,
                            (float) targetElevation
                    )
            );

            FlowDirection direction =
                    hydrology.conditionedFlowDirection(
                            x,
                            z
                    );

            if (
                    direction == FlowDirection.OUTLET
                            || direction == FlowDirection.OCEAN
            ) {
                return new DownstreamExtension(
                        true,
                        List.copyOf(steps)
                );
            }

            if (direction == FlowDirection.SINK) {
                /*
                 * Reaching a lower downstream sink is sufficient: the source
                 * basin has been connected into that still-unresolved basin.
                 * If this sink had to be cut below the channel to get here,
                 * however, it would simply become a new artificial pit.
                 */
                return existing <= targetElevation
                        ? new DownstreamExtension(
                        true,
                        List.copyOf(steps)
                )
                        : DownstreamExtension.invalid();
            }

            int nextX =
                    x + direction.dx();

            int nextZ =
                    z + direction.dz();

            if (
                    nextX < 0
                            || nextX >= size
                            || nextZ < 0
                            || nextZ >= size
            ) {
                return new DownstreamExtension(
                        true,
                        List.copyOf(steps)
                );
            }

            int next =
                    index(
                            nextX,
                            nextZ,
                            size
                    );

            targetElevation -=
                    BREACH_GRADIENT_PER_GRID_UNIT
                            * gridDistance(
                            current,
                            next,
                            size
                    );

            previous = current;
            current = next;
        }

        return DownstreamExtension.invalid();
    }

    private static void applyBreachCandidate(
            HydrologyGrid hydrology,
            BreachCandidate candidate
    ) {
        int size =
                hydrology.resolution();

        for (PathStep step : candidate.steps()) {
            int x =
                    step.index() % size;

            int z =
                    step.index() / size;

            float before =
                    hydrology.conditionedElevation(
                            x,
                            z
                    );

            float after =
                    Math.min(
                            before,
                            step.targetElevation()
                    );

            hydrology.markBreachPath(
                    x,
                    z
            );

            if (after < before) {
                hydrology.setConditionedElevation(
                        x,
                        z,
                        after
                );

                hydrology.recordBreachCut(
                        x,
                        z,
                        before - after
                );
            }
        }
    }

    private static List<List<Integer>> createFootprintLists(
            HydrologyGrid hydrology,
            boolean[] selected
    ) {
        List<List<Integer>> result =
                new ArrayList<>(selected.length);

        for (int i = 0; i < selected.length; i++) {
            result.add(
                    selected[i]
                            ? new ArrayList<>()
                            : List.of()
            );
        }

        int size =
                hydrology.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int id =
                        hydrology.depressionId(
                                x,
                                z
                        );

                if (
                        id < 0
                                || id >= selected.length
                                || !selected[id]
                ) {
                    continue;
                }

                result.get(id)
                        .add(
                                index(x, z, size)
                        );
            }
        }

        return result;
    }

    private static void applyFillGradient(
            HydrologyGrid hydrology,
            Depression depression,
            List<Integer> cells,
            int[] distance
    ) {
        int size =
                hydrology.resolution();

        for (int cell : cells) {
            distance[cell] = -1;
        }

        int seed =
                findClosestFootprintCellToSpill(
                        depression,
                        cells,
                        size
                );

        ArrayDeque<Integer> queue =
                new ArrayDeque<>();

        distance[seed] = 0;
        queue.add(seed);

        while (!queue.isEmpty()) {
            int current =
                    queue.removeFirst();

            int x =
                    current % size;

            int z =
                    current / size;

            int nextDistance =
                    distance[current] + 1;

            for (FlowDirection direction : FlowDirection.values()) {
                if (!direction.routesToNeighbor()) {
                    continue;
                }

                int nx =
                        x + direction.dx();

                int nz =
                        z + direction.dz();

                if (
                        nx < 0
                                || nx >= size
                                || nz < 0
                                || nz >= size
                ) {
                    continue;
                }

                if (
                        hydrology.depressionId(nx, nz)
                                != depression.id()
                ) {
                    continue;
                }

                int neighbor =
                        index(nx, nz, size);

                if (distance[neighbor] >= 0) {
                    continue;
                }

                distance[neighbor] =
                        nextDistance;

                queue.addLast(neighbor);
            }
        }

        for (int cell : cells) {
            int x =
                    cell % size;

            int z =
                    cell / size;

            int cellDistance =
                    distance[cell];

            if (cellDistance < 0) {
                cellDistance =
                        Math.max(
                                1,
                                (int) Math.ceil(
                                        Math.hypot(
                                                x - depression.spillX(),
                                                z - depression.spillZ()
                                        )
                                )
                        );
            }

            float targetElevation =
                    depression.spillElevation()
                            + FILL_GRADIENT_PER_CELL
                            * (cellDistance + 1);

            hydrology.setConditionedElevation(
                    x,
                    z,
                    Math.max(
                            hydrology.surfaceElevation(x, z),
                            targetElevation
                    )
            );
        }
    }

    private static int findClosestFootprintCellToSpill(
            Depression depression,
            List<Integer> cells,
            int size
    ) {
        int bestCell =
                cells.getFirst();

        long bestDistanceSquared =
                Long.MAX_VALUE;

        for (int cell : cells) {
            int x =
                    cell % size;

            int z =
                    cell / size;

            long dx =
                    x - depression.spillX();

            long dz =
                    z - depression.spillZ();

            long distanceSquared =
                    dx * dx + dz * dz;

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestCell = cell;
            }
        }

        return bestCell;
    }

    private static boolean isLand(
            WorldBlueprint world,
            int x,
            int z
    ) {
        return world.landMask(x, z) >= 0.5f;
    }

    private static double gridDistance(
            int a,
            int b,
            int size
    ) {
        int ax =
                a % size;

        int az =
                a / size;

        int bx =
                b % size;

        int bz =
                b / size;

        int dx =
                Math.abs(ax - bx);

        int dz =
                Math.abs(az - bz);

        return dx != 0 && dz != 0
                ? Math.sqrt(2.0)
                : 1.0;
    }

    private static int index(
            int x,
            int z,
            int size
    ) {
        return z * size + x;
    }

    private record PathStep(
            int index,
            float targetElevation
    ) {
    }

    private record BreachCandidate(
            int depressionId,
            List<PathStep> steps,
            float maximumCutDepth
    ) {
    }

    private record DownstreamExtension(
            boolean valid,
            List<PathStep> steps
    ) {
        private static DownstreamExtension invalid() {
            return new DownstreamExtension(
                    false,
                    List.of()
            );
        }
    }

    private static final class BreachPathFinder {

        private final double[] bestCost;
        private final int[] parent;
        private final int[] visitStamp;
        private int stamp;

        private BreachPathFinder(
                int resolution
        ) {
            int cellCount =
                    resolution * resolution;

            this.bestCost =
                    new double[cellCount];

            this.parent =
                    new int[cellCount];

            this.visitStamp =
                    new int[cellCount];

            this.stamp =
                    1;
        }

        private List<Integer> findInteriorPath(
                HydrologyGrid hydrology,
                Depression depression
        ) {
            int size =
                    hydrology.resolution();

            int start =
                    index(
                            depression.sinkX(),
                            depression.sinkZ(),
                            size
                    );

            int goal =
                    index(
                            depression.spillX(),
                            depression.spillZ(),
                            size
                    );

            if (start == goal) {
                return List.of(start);
            }

            advanceStamp();

            PriorityQueue<SearchNode> open =
                    new PriorityQueue<>(
                            Comparator.comparingDouble(
                                    SearchNode::cost
                            )
                    );

            visitStamp[start] = stamp;
            bestCost[start] = 0.0;
            parent[start] = -1;

            open.add(
                    new SearchNode(
                            start,
                            0.0
                    )
            );

            while (!open.isEmpty()) {
                SearchNode node =
                        open.poll();

                int current =
                        node.index();

                if (
                        visitStamp[current] != stamp
                                || node.cost() > bestCost[current] + 1.0e-12
                ) {
                    continue;
                }

                if (current == goal) {
                    return reconstructPath(
                            start,
                            goal
                    );
                }

                int x =
                        current % size;

                int z =
                        current / size;

                for (FlowDirection direction : FlowDirection.values()) {
                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int nx =
                            x + direction.dx();

                    int nz =
                            z + direction.dz();

                    if (
                            nx < 0
                                    || nx >= size
                                    || nz < 0
                                    || nz >= size
                    ) {
                        continue;
                    }

                    int next =
                            index(nx, nz, size);

                    if (
                            next != goal
                                    && hydrology.depressionId(nx, nz)
                                    != depression.id()
                    ) {
                        continue;
                    }

                    double verticalPenalty =
                            Math.max(
                                    0.0,
                                    hydrology.conditionedElevation(nx, nz)
                                            - depression.minimumElevation()
                            );

                    double stepCost =
                            direction.gridDistance()
                                    + verticalPenalty
                                    * VERTICAL_PATH_COST_WEIGHT;

                    double candidateCost =
                            node.cost()
                                    + stepCost;

                    if (
                            visitStamp[next] != stamp
                                    || candidateCost < bestCost[next]
                    ) {
                        visitStamp[next] = stamp;
                        bestCost[next] = candidateCost;
                        parent[next] = current;

                        open.add(
                                new SearchNode(
                                        next,
                                        candidateCost
                                )
                        );
                    }
                }
            }

            return List.of();
        }

        private List<Integer> reconstructPath(
                int start,
                int goal
        ) {
            List<Integer> reversed =
                    new ArrayList<>();

            int current =
                    goal;

            while (current >= 0) {
                reversed.add(current);

                if (current == start) {
                    break;
                }

                current =
                        parent[current];
            }

            if (
                    reversed.isEmpty()
                            || reversed.getLast() != start
            ) {
                return List.of();
            }

            List<Integer> result =
                    new ArrayList<>(
                            reversed.size()
                    );

            for (int i = reversed.size() - 1; i >= 0; i--) {
                result.add(
                        reversed.get(i)
                );
            }

            return List.copyOf(result);
        }

        private void advanceStamp() {
            if (stamp == Integer.MAX_VALUE) {
                Arrays.fill(
                        visitStamp,
                        0
                );

                stamp = 1;
                return;
            }

            stamp++;
        }
    }

    private record SearchNode(
            int index,
            double cost
    ) {
    }
}
