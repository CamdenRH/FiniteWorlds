package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Hydrology Pass 1J: converts the conditioned D8 field into a lake-aware,
 * compound-aware drainage graph without modifying elevation.
 *
 * Lake cells are routed across their conceptual water surface toward the
 * planned outlet. Compound depression groups that were not retained as lakes
 * receive a virtual escape tree toward the external spill measured in Pass
 * 1H. This graph is intended to be the input to later flow accumulation.
 */
public final class DrainageRouter {

    private DrainageRouter() {
    }

    public static void route(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        hydrology.resetRoutedFlow();

        if (hydrology.resolution() <= 0) {
            return;
        }

        routeLakes(
                hydrology
        );

        routeNonLakeCompoundBasins(
                hydrology
        );

        resolveRoutingCycles(
                hydrology
        );

        /*
         * Pass 1K completes the graph-only drainage topology by attaching
         * any residual sinks to an already-open, acyclic route. Terrain
         * elevation is intentionally left untouched.
         */
        ResidualSinkRouter.resolve(
                hydrology
        );

        /*
         * The residual router is constructed to avoid cycles, but run the
         * existing cycle resolver once more as a defensive invariant before
         * declaring the Phase-1 graph complete.
         */
        resolveRoutingCycles(
                hydrology
        );

        analyzeFinalGraph(
                hydrology
        );
    }

    private static void routeLakes(
            HydrologyGrid hydrology
    ) {
        int lakeCount =
                hydrology.lakeCount();

        if (lakeCount == 0) {
            return;
        }

        int size =
                hydrology.resolution();

        @SuppressWarnings("unchecked")
        List<Integer>[] cellsByLake =
                new List[lakeCount];

        for (int i = 0; i < lakeCount; i++) {
            cellsByLake[i] =
                    new ArrayList<>();
        }

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int lakeId =
                        hydrology.lakeId(
                                x,
                                z
                        );

                if (lakeId >= 0 && lakeId < lakeCount) {
                    cellsByLake[lakeId].add(
                            index(x, z, size)
                    );
                }
            }
        }

        int cellCount =
                size * size;

        int[] visitStamp =
                new int[cellCount];

        ArrayDeque<Integer> queue =
                new ArrayDeque<>();

        int stamp = 0;

        for (Lake lake : hydrology.lakes()) {
            List<Integer> cells =
                    cellsByLake[lake.id()];

            if (cells.isEmpty() || !lake.hasOutlet()) {
                hydrology.recordRoutingFailure();
                continue;
            }

            stamp++;
            queue.clear();

            int outletIndex =
                    validCell(
                            lake.outletX(),
                            lake.outletZ(),
                            size
                    )
                            ? index(
                            lake.outletX(),
                            lake.outletZ(),
                            size
                    )
                            : -1;

            boolean seeded =
                    false;

            if (
                    outletIndex >= 0
                            && hydrology.lakeId(
                            lake.outletX(),
                            lake.outletZ()
                    ) == lake.id()
            ) {
                FlowDirection outletDirection =
                        directionToAdjacent(
                                lake.outletX(),
                                lake.outletZ(),
                                lake.outletTargetX(),
                                lake.outletTargetZ()
                        );

                if (outletDirection != null) {
                    setRoutedDirection(
                            hydrology,
                            lake.outletX(),
                            lake.outletZ(),
                            outletDirection,
                            HydrologyRouteType.LAKE_OUTLET
                    );

                    visitStamp[outletIndex] =
                            stamp;

                    queue.addLast(
                            outletIndex
                    );

                    seeded = true;
                }
            } else if (outletIndex >= 0) {
                /*
                 * The analytical shoreline normally excludes the spill-side
                 * cell itself because its depth is approximately zero. Seed
                 * every inundated cell that touches that outlet, then route
                 * the rest of the lake toward those edge cells.
                 */
                for (FlowDirection direction : FlowDirection.values()) {
                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int x =
                            lake.outletX() + direction.dx();

                    int z =
                            lake.outletZ() + direction.dz();

                    if (
                            !validCell(x, z, size)
                                    || hydrology.lakeId(x, z) != lake.id()
                    ) {
                        continue;
                    }

                    int cell =
                            index(x, z, size);

                    if (visitStamp[cell] == stamp) {
                        continue;
                    }

                    FlowDirection towardOutlet =
                            directionToAdjacent(
                                    x,
                                    z,
                                    lake.outletX(),
                                    lake.outletZ()
                            );

                    if (towardOutlet == null) {
                        continue;
                    }

                    setRoutedDirection(
                            hydrology,
                            x,
                            z,
                            towardOutlet,
                            HydrologyRouteType.LAKE_INTERIOR
                    );

                    visitStamp[cell] =
                            stamp;

                    queue.addLast(
                            cell
                    );

                    seeded = true;
                }

                FlowDirection outletDirection =
                        directionToAdjacent(
                                lake.outletX(),
                                lake.outletZ(),
                                lake.outletTargetX(),
                                lake.outletTargetZ()
                        );

                if (outletDirection != null) {
                    setRoutedDirection(
                            hydrology,
                            lake.outletX(),
                            lake.outletZ(),
                            outletDirection,
                            HydrologyRouteType.LAKE_OUTLET
                    );
                }
            }

            if (!seeded) {
                hydrology.recordRoutingFailure();
                continue;
            }

            while (!queue.isEmpty()) {
                int current =
                        queue.removeFirst();

                int cx =
                        current % size;

                int cz =
                        current / size;

                for (FlowDirection direction : FlowDirection.values()) {
                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int nx =
                            cx + direction.dx();

                    int nz =
                            cz + direction.dz();

                    if (
                            !validCell(nx, nz, size)
                                    || hydrology.lakeId(nx, nz) != lake.id()
                    ) {
                        continue;
                    }

                    int neighbor =
                            index(nx, nz, size);

                    if (visitStamp[neighbor] == stamp) {
                        continue;
                    }

                    FlowDirection towardCurrent =
                            directionToAdjacent(
                                    nx,
                                    nz,
                                    cx,
                                    cz
                            );

                    setRoutedDirection(
                            hydrology,
                            nx,
                            nz,
                            towardCurrent,
                            HydrologyRouteType.LAKE_INTERIOR
                    );

                    visitStamp[neighbor] =
                            stamp;

                    queue.addLast(
                            neighbor
                    );
                }
            }

            int routed =
                    0;

            for (int cell : cells) {
                if (visitStamp[cell] == stamp) {
                    routed++;
                }
            }

            if (routed != cells.size()) {
                hydrology.recordRoutingFailure();
            }
        }
    }

    private static void routeNonLakeCompoundBasins(
            HydrologyGrid hydrology
    ) {
        List<CompoundBasin> compounds =
                hydrology.compoundBasins();

        if (compounds.isEmpty()) {
            return;
        }

        int size =
                hydrology.resolution();

        int depressionCount =
                hydrology.depressions().size();

        int[] groupByDepression =
                new int[depressionCount];

        Arrays.fill(
                groupByDepression,
                -1
        );

        for (DepressionClassification classification
                : hydrology.depressionClassifications()) {
            if (classification.belongsToCompoundGroup()) {
                groupByDepression[classification.depressionId()] =
                        classification.compoundGroupId();
            }
        }

        boolean[] compoundIsLake =
                new boolean[compounds.size()];

        for (Lake lake : hydrology.lakes()) {
            if (
                    lake.sourceType() == LakeSourceType.COMPOUND_BASIN
                            && lake.sourceId() >= 0
                            && lake.sourceId() < compoundIsLake.length
            ) {
                compoundIsLake[lake.sourceId()] =
                        true;
            }
        }

        @SuppressWarnings("unchecked")
        List<Integer>[] cellsByGroup =
                new List[compounds.size()];

        for (int i = 0; i < cellsByGroup.length; i++) {
            cellsByGroup[i] =
                    new ArrayList<>();
        }

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int depressionId =
                        hydrology.rawCatchmentDepressionId(
                                x,
                                z
                        );

                if (
                        depressionId < 0
                                || depressionId >= groupByDepression.length
                ) {
                    continue;
                }

                int groupId =
                        groupByDepression[depressionId];

                if (groupId >= 0 && groupId < cellsByGroup.length) {
                    cellsByGroup[groupId].add(
                            index(x, z, size)
                    );
                }
            }
        }

        int cellCount =
                size * size;

        int[] visitStamp =
                new int[cellCount];

        int[] nextTowardOutlet =
                new int[cellCount];

        Arrays.fill(
                nextTowardOutlet,
                -1
        );

        ArrayDeque<Integer> queue =
                new ArrayDeque<>();

        int stamp =
                0;

        List<Depression> depressions =
                hydrology.depressions();

        for (CompoundBasin basin : compounds) {
            int groupId =
                    basin.groupId();

            if (
                    groupId < 0
                            || groupId >= cellsByGroup.length
                            || compoundIsLake[groupId]
                            || !basin.hasExternalSpill()
            ) {
                continue;
            }

            List<Integer> groupCells =
                    cellsByGroup[groupId];

            if (groupCells.isEmpty()) {
                hydrology.recordRoutingFailure();
                continue;
            }

            stamp++;
            queue.clear();

            int spill =
                    index(
                            basin.spillX(),
                            basin.spillZ(),
                            size
                    );

            boolean seeded =
                    false;

            if (
                    belongsToCompoundGroup(
                            hydrology,
                            groupByDepression,
                            basin.spillX(),
                            basin.spillZ(),
                            groupId
                    )
            ) {
                visitStamp[spill] =
                        stamp;

                nextTowardOutlet[spill] =
                        -1;

                queue.addLast(
                        spill
                );

                seeded = true;
            } else {
                for (FlowDirection direction : FlowDirection.values()) {
                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int x =
                            basin.spillX() + direction.dx();

                    int z =
                            basin.spillZ() + direction.dz();

                    if (
                            !validCell(x, z, size)
                                    || !belongsToCompoundGroup(
                                    hydrology,
                                    groupByDepression,
                                    x,
                                    z,
                                    groupId
                            )
                    ) {
                        continue;
                    }

                    int cell =
                            index(x, z, size);

                    visitStamp[cell] =
                            stamp;

                    nextTowardOutlet[cell] =
                            spill;

                    queue.addLast(
                            cell
                    );

                    seeded = true;
                }
            }

            if (!seeded) {
                hydrology.recordRoutingFailure();
                continue;
            }

            while (!queue.isEmpty()) {
                int current =
                        queue.removeFirst();

                int cx =
                        current % size;

                int cz =
                        current / size;

                for (FlowDirection direction : FlowDirection.values()) {
                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int nx =
                            cx + direction.dx();

                    int nz =
                            cz + direction.dz();

                    if (
                            !validCell(nx, nz, size)
                                    || !belongsToCompoundGroup(
                                    hydrology,
                                    groupByDepression,
                                    nx,
                                    nz,
                                    groupId
                            )
                    ) {
                        continue;
                    }

                    int neighbor =
                            index(nx, nz, size);

                    if (visitStamp[neighbor] == stamp) {
                        continue;
                    }

                    visitStamp[neighbor] =
                            stamp;

                    nextTowardOutlet[neighbor] =
                            current;

                    queue.addLast(
                            neighbor
                    );
                }
            }

            boolean failed =
                    false;

            for (int depressionId : basin.memberDepressionIds()) {
                if (
                        depressionId < 0
                                || depressionId >= depressions.size()
                ) {
                    continue;
                }

                Depression depression =
                        depressions.get(depressionId);

                int current =
                        index(
                                depression.sinkX(),
                                depression.sinkZ(),
                                size
                        );

                if (visitStamp[current] != stamp) {
                    failed = true;
                    continue;
                }

                int guard =
                        0;

                while (current != spill) {
                    int next =
                            nextTowardOutlet[current];

                    if (next < 0) {
                        failed = true;
                        break;
                    }

                    int cx =
                            current % size;

                    int cz =
                            current / size;

                    int nx =
                            next % size;

                    int nz =
                            next / size;

                    FlowDirection direction =
                            directionToAdjacent(
                                    cx,
                                    cz,
                                    nx,
                                    nz
                            );

                    if (direction == null) {
                        failed = true;
                        break;
                    }

                    setRoutedDirection(
                            hydrology,
                            cx,
                            cz,
                            direction,
                            HydrologyRouteType.COMPOUND_ESCAPE
                    );

                    current =
                            next;

                    guard++;

                    if (guard > groupCells.size() + 8) {
                        failed = true;
                        break;
                    }
                }
            }

            FlowDirection spillDirection =
                    directionToAdjacent(
                            basin.spillX(),
                            basin.spillZ(),
                            basin.spillTargetX(),
                            basin.spillTargetZ()
                    );

            if (spillDirection != null) {
                setRoutedDirection(
                        hydrology,
                        basin.spillX(),
                        basin.spillZ(),
                        spillDirection,
                        HydrologyRouteType.COMPOUND_ESCAPE
                );
            } else {
                failed = true;
            }

            if (failed) {
                hydrology.recordRoutingFailure();
            }
        }
    }

    private static boolean belongsToCompoundGroup(
            HydrologyGrid hydrology,
            int[] groupByDepression,
            int x,
            int z,
            int groupId
    ) {
        int depressionId =
                hydrology.rawCatchmentDepressionId(
                        x,
                        z
                );

        return depressionId >= 0
                && depressionId < groupByDepression.length
                && groupByDepression[depressionId] == groupId;
    }

    private static void resolveRoutingCycles(
            HydrologyGrid hydrology
    ) {
        int size =
                hydrology.resolution();

        int cellCount =
                size * size;

        int[] cycleMarker =
                new int[cellCount];

        int[] allCycleMarker =
                new int[cellCount];

        int[] searchStamp =
                new int[cellCount];

        int[] parent =
                new int[cellCount];

        double[] bestCost =
                new double[cellCount];

        int cycleStamp =
                0;

        int searchId =
                0;

        for (int round = 0; round < 6; round++) {
            List<List<Integer>> cycles =
                    findRoutingCycles(
                            hydrology
                    );

            if (cycles.isEmpty()) {
                return;
            }

            int allStamp =
                    round + 1;

            for (List<Integer> cycle : cycles) {
                for (int cell : cycle) {
                    allCycleMarker[cell] =
                            allStamp;
                }
            }

            int resolved =
                    0;

            for (List<Integer> cycle : cycles) {
                cycleStamp++;
                searchId++;

                for (int cell : cycle) {
                    cycleMarker[cell] =
                            cycleStamp;
                }

                if (
                        routeCycleToEscape(
                                hydrology,
                                cycle,
                                cycleMarker,
                                cycleStamp,
                                allCycleMarker,
                                allStamp,
                                searchStamp,
                                searchId,
                                parent,
                                bestCost
                        )
                ) {
                    resolved++;
                } else {
                    hydrology.recordRoutingFailure();
                }
            }

            if (resolved == 0) {
                return;
            }
        }
    }

    private static boolean routeCycleToEscape(
            HydrologyGrid hydrology,
            List<Integer> cycle,
            int[] cycleMarker,
            int cycleStamp,
            int[] allCycleMarker,
            int allStamp,
            int[] searchStamp,
            int searchId,
            int[] parent,
            double[] bestCost
    ) {
        final int maximumVisited =
                25000;

        int size =
                hydrology.resolution();

        PriorityQueue<SearchNode> queue =
                new PriorityQueue<>();

        for (int cell : cycle) {
            searchStamp[cell] =
                    searchId;

            parent[cell] =
                    -1;

            bestCost[cell] =
                    0.0;

            queue.add(
                    new SearchNode(
                            cell,
                            0.0
                    )
            );
        }

        int visited =
                0;

        int target =
                -1;

        while (!queue.isEmpty() && visited < maximumVisited) {
            SearchNode node =
                    queue.poll();

            int current =
                    node.index();

            if (
                    searchStamp[current] != searchId
                            || node.cost() > bestCost[current] + 1.0e-9
            ) {
                continue;
            }

            visited++;

            if (
                    cycleMarker[current] != cycleStamp
                            && drainsAwayFromCycle(
                            hydrology,
                            current,
                            cycleMarker,
                            cycleStamp,
                            allCycleMarker,
                            allStamp
                    )
            ) {
                target =
                        current;
                break;
            }

            int x =
                    current % size;

            int z =
                    current / size;

            float sourceElevation =
                    hydrology.conditionedElevation(
                            x,
                            z
                    );

            for (FlowDirection direction : FlowDirection.values()) {
                if (!direction.routesToNeighbor()) {
                    continue;
                }

                int nx =
                        x + direction.dx();

                int nz =
                        z + direction.dz();

                if (!validCell(nx, nz, size)) {
                    continue;
                }

                int neighbor =
                        index(nx, nz, size);

                if (
                        allCycleMarker[neighbor] == allStamp
                                && cycleMarker[neighbor] != cycleStamp
                ) {
                    continue;
                }

                float neighborElevation =
                        hydrology.conditionedElevation(
                                nx,
                                nz
                        );

                double rise =
                        Math.max(
                                0.0,
                                neighborElevation - sourceElevation
                        );

                double stepCost =
                        direction.gridDistance()
                                + rise * 5.0;

                double newCost =
                        node.cost() + stepCost;

                if (
                        searchStamp[neighbor] != searchId
                                || newCost < bestCost[neighbor]
                ) {
                    searchStamp[neighbor] =
                            searchId;

                    bestCost[neighbor] =
                            newCost;

                    parent[neighbor] =
                            current;

                    queue.add(
                            new SearchNode(
                                    neighbor,
                                    newCost
                            )
                    );
                }
            }
        }

        if (target < 0) {
            return false;
        }

        List<Integer> reversed =
                new ArrayList<>();

        int current =
                target;

        while (current >= 0) {
            reversed.add(current);

            if (cycleMarker[current] == cycleStamp) {
                break;
            }

            current =
                    parent[current];
        }

        if (
                reversed.isEmpty()
                        || cycleMarker[
                        reversed.getLast()
                ] != cycleStamp
        ) {
            return false;
        }

        for (int i = reversed.size() - 1; i > 0; i--) {
            int from =
                    reversed.get(i);

            int to =
                    reversed.get(i - 1);

            int fromX =
                    from % size;

            int fromZ =
                    from / size;

            int toX =
                    to % size;

            int toZ =
                    to / size;

            FlowDirection direction =
                    directionToAdjacent(
                            fromX,
                            fromZ,
                            toX,
                            toZ
                    );

            if (direction == null) {
                return false;
            }

            setRoutedDirection(
                    hydrology,
                    fromX,
                    fromZ,
                    direction,
                    HydrologyRouteType.CYCLE_ESCAPE
            );
        }

        return true;
    }

    private static boolean drainsAwayFromCycle(
            HydrologyGrid hydrology,
            int start,
            int[] cycleMarker,
            int cycleStamp,
            int[] allCycleMarker,
            int allStamp
    ) {
        int size =
                hydrology.resolution();

        int current =
                start;

        for (int step = 0; step < 8192; step++) {
            if (cycleMarker[current] == cycleStamp) {
                return false;
            }

            if (
                    allCycleMarker[current] == allStamp
                            && cycleMarker[current] != cycleStamp
            ) {
                return false;
            }

            int x =
                    current % size;

            int z =
                    current / size;

            FlowDirection direction =
                    hydrology.routedFlowDirection(
                            x,
                            z
                    );

            if (!direction.routesToNeighbor()) {
                return true;
            }

            int nx =
                    x + direction.dx();

            int nz =
                    z + direction.dz();

            if (!validCell(nx, nz, size)) {
                return true;
            }

            current =
                    index(nx, nz, size);
        }

        return false;
    }

    private static List<List<Integer>> findRoutingCycles(
            HydrologyGrid hydrology
    ) {
        int size =
                hydrology.resolution();

        int cellCount =
                size * size;

        byte[] finished =
                new byte[cellCount];

        int[] walkStamp =
                new int[cellCount];

        int[] walkPosition =
                new int[cellCount];

        int[] path =
                new int[cellCount];

        int walkId =
                0;

        List<List<Integer>> cycles =
                new ArrayList<>();

        for (int start = 0; start < cellCount; start++) {
            if (finished[start] != 0) {
                continue;
            }

            walkId++;

            int pathLength =
                    0;

            int current =
                    start;

            while (true) {
                if (current < 0 || current >= cellCount) {
                    break;
                }

                if (finished[current] != 0) {
                    break;
                }

                if (walkStamp[current] == walkId) {
                    int cycleStart =
                            walkPosition[current];

                    List<Integer> cycle =
                            new ArrayList<>(
                                    pathLength - cycleStart
                            );

                    for (int i = cycleStart; i < pathLength; i++) {
                        cycle.add(
                                path[i]
                        );
                    }

                    cycles.add(
                            List.copyOf(cycle)
                    );
                    break;
                }

                walkStamp[current] =
                        walkId;

                walkPosition[current] =
                        pathLength;

                path[pathLength++] =
                        current;

                int x =
                        current % size;

                int z =
                        current / size;

                FlowDirection direction =
                        hydrology.routedFlowDirection(
                                x,
                                z
                        );

                if (!direction.routesToNeighbor()) {
                    break;
                }

                int nx =
                        x + direction.dx();

                int nz =
                        z + direction.dz();

                if (!validCell(nx, nz, size)) {
                    break;
                }

                current =
                        index(nx, nz, size);
            }

            for (int i = 0; i < pathLength; i++) {
                finished[path[i]] =
                        1;
            }
        }

        return List.copyOf(cycles);
    }

    private static void analyzeFinalGraph(
            HydrologyGrid hydrology
    ) {
        hydrology.clearRoutedCycles();

        List<List<Integer>> cycles =
                findRoutingCycles(
                        hydrology
                );

        int size =
                hydrology.resolution();

        int cellCount =
                0;

        for (List<Integer> cycle : cycles) {
            for (int cell : cycle) {
                hydrology.markRoutedCycle(
                        cell % size,
                        cell / size
                );

                cellCount++;
            }
        }

        hydrology.setRoutedCycleStats(
                cycles.size(),
                cellCount
        );
    }

    private record SearchNode(
            int index,
            double cost
    ) implements Comparable<SearchNode> {

        @Override
        public int compareTo(
                SearchNode other
        ) {
            return Double.compare(
                    cost,
                    other.cost
            );
        }
    }


    private static void setRoutedDirection(
            HydrologyGrid hydrology,
            int x,
            int z,
            FlowDirection direction,
            HydrologyRouteType routeType
    ) {
        hydrology.setRoutedFlowDirection(
                x,
                z,
                direction
        );

        hydrology.setRouteType(
                x,
                z,
                routeType
        );
    }

    private static FlowDirection directionToAdjacent(
            int fromX,
            int fromZ,
            int toX,
            int toZ
    ) {
        int dx =
                toX - fromX;

        int dz =
                toZ - fromZ;

        if (
                dx < -1
                        || dx > 1
                        || dz < -1
                        || dz > 1
                        || (dx == 0 && dz == 0)
        ) {
            return null;
        }

        for (FlowDirection direction : FlowDirection.values()) {
            if (
                    direction.routesToNeighbor()
                            && direction.dx() == dx
                            && direction.dz() == dz
            ) {
                return direction;
            }
        }

        return null;
    }

    private static boolean validCell(
            int x,
            int z,
            int size
    ) {
        return x >= 0
                && x < size
                && z >= 0
                && z < size;
    }

    private static int index(
            int x,
            int z,
            int size
    ) {
        return z * size + x;
    }
}
