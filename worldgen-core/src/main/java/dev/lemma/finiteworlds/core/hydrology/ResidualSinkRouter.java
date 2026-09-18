package dev.lemma.finiteworlds.core.hydrology;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Hydrology Pass 1K: completes the routed drainage graph by connecting the
 * handful of residual sinks left after lake/compound routing to an already
 * open, acyclic drainage path.
 *
 * This is intentionally graph-only. No terrain elevation is raised or cut.
 * The search is allowed to cross unresolved ordinary terrain, but it never
 * overwrites lake, compound, or previous semantic routing cells. Those cells
 * may only be used as an attachment target when they already drain to an open
 * outlet.
 */
final class ResidualSinkRouter {

    private static final int MAXIMUM_VISITED_PER_SINK =
            100_000;

    private static final double RISE_COST =
            6.0;

    private ResidualSinkRouter() {
    }

    static void resolve(
            HydrologyGrid hydrology
    ) {
        int size =
                hydrology.resolution();

        if (size <= 0) {
            return;
        }

        int cellCount =
                size * size;

        int[] firstUpstream =
                new int[cellCount];

        int[] nextUpstream =
                new int[cellCount];

        Arrays.fill(
                firstUpstream,
                -1
        );

        Arrays.fill(
                nextUpstream,
                -1
        );

        buildReverseGraph(
                hydrology,
                firstUpstream,
                nextUpstream
        );

        byte[] openDrainage =
                findOpenDrainageCells(
                        hydrology,
                        firstUpstream,
                        nextUpstream
                );

        List<Integer> sinks =
                collectResidualSinks(
                        hydrology
                );

        sinks.sort(
                Comparator.comparingDouble(
                        cell -> hydrology.conditionedElevation(
                                cell % size,
                                cell / size
                        )
                )
        );

        int[] searchStamp =
                new int[cellCount];

        int[] parent =
                new int[cellCount];

        double[] bestCost =
                new double[cellCount];

        int searchId =
                0;

        ArrayDeque<Integer> propagationQueue =
                new ArrayDeque<>();

        for (int sink : sinks) {
            int sinkX =
                    sink % size;

            int sinkZ =
                    sink / size;

            if (
                    hydrology.routedFlowDirection(
                            sinkX,
                            sinkZ
                    ) != FlowDirection.SINK
            ) {
                continue;
            }

            searchId++;

            List<Integer> path =
                    findEscapePath(
                            hydrology,
                            sink,
                            openDrainage,
                            searchStamp,
                            searchId,
                            parent,
                            bestCost
                    );

            if (path == null || path.size() < 2) {
                hydrology.recordRoutingFailure();
                continue;
            }

            boolean applied =
                    applyEscapePath(
                            hydrology,
                            path
                    );

            if (!applied) {
                hydrology.recordRoutingFailure();
                continue;
            }

            /*
             * The redirected path is now part of the open graph. Anything
             * that already flowed into those cells also becomes open, so
             * propagate that status backward through the pre-cleanup graph.
             */
            propagationQueue.clear();

            for (int i = 0; i < path.size() - 1; i++) {
                int cell =
                        path.get(i);

                if (openDrainage[cell] == 0) {
                    openDrainage[cell] = 1;
                    propagationQueue.addLast(cell);
                }
            }

            propagateOpenUpstream(
                    openDrainage,
                    firstUpstream,
                    nextUpstream,
                    propagationQueue
            );
        }
    }

    private static void buildReverseGraph(
            HydrologyGrid hydrology,
            int[] firstUpstream,
            int[] nextUpstream
    ) {
        int size =
                hydrology.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.routedFlowDirection(
                                x,
                                z
                        );

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

                int source =
                        index(x, z, size);

                int target =
                        index(nx, nz, size);

                nextUpstream[source] =
                        firstUpstream[target];

                firstUpstream[target] =
                        source;
            }
        }
    }

    private static byte[] findOpenDrainageCells(
            HydrologyGrid hydrology,
            int[] firstUpstream,
            int[] nextUpstream
    ) {
        int size =
                hydrology.resolution();

        int cellCount =
                size * size;

        byte[] open =
                new byte[cellCount];

        ArrayDeque<Integer> queue =
                new ArrayDeque<>();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.routedFlowDirection(
                                x,
                                z
                        );

                if (
                        direction == FlowDirection.OCEAN
                                || direction == FlowDirection.OUTLET
                ) {
                    int cell =
                            index(x, z, size);

                    open[cell] = 1;
                    queue.addLast(cell);
                }
            }
        }

        propagateOpenUpstream(
                open,
                firstUpstream,
                nextUpstream,
                queue
        );

        return open;
    }

    private static void propagateOpenUpstream(
            byte[] open,
            int[] firstUpstream,
            int[] nextUpstream,
            ArrayDeque<Integer> queue
    ) {
        while (!queue.isEmpty()) {
            int cell =
                    queue.removeFirst();

            for (
                    int upstream = firstUpstream[cell];
                    upstream >= 0;
                    upstream = nextUpstream[upstream]
            ) {
                if (open[upstream] != 0) {
                    continue;
                }

                open[upstream] = 1;
                queue.addLast(upstream);
            }
        }
    }

    private static List<Integer> collectResidualSinks(
            HydrologyGrid hydrology
    ) {
        int size =
                hydrology.resolution();

        List<Integer> result =
                new ArrayList<>();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (
                        hydrology.routedFlowDirection(
                                x,
                                z
                        ) == FlowDirection.SINK
                ) {
                    result.add(
                            index(x, z, size)
                    );
                }
            }
        }

        return result;
    }

    private static List<Integer> findEscapePath(
            HydrologyGrid hydrology,
            int sink,
            byte[] openDrainage,
            int[] searchStamp,
            int searchId,
            int[] parent,
            double[] bestCost
    ) {
        int size =
                hydrology.resolution();

        PriorityQueue<SearchNode> queue =
                new PriorityQueue<>();

        searchStamp[sink] =
                searchId;

        parent[sink] =
                -1;

        bestCost[sink] =
                0.0;

        queue.add(
                new SearchNode(
                        sink,
                        0.0
                )
        );

        int visited =
                0;

        int target =
                -1;

        while (
                !queue.isEmpty()
                        && visited < MAXIMUM_VISITED_PER_SINK
        ) {
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
                    current != sink
                            && openDrainage[current] != 0
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

                FlowDirection neighborDirection =
                        hydrology.routedFlowDirection(
                                nx,
                                nz
                        );

                if (neighborDirection == FlowDirection.OCEAN) {
                    continue;
                }

                int neighbor =
                        index(nx, nz, size);

                if (
                        neighbor != sink
                                && neighborDirection == FlowDirection.SINK
                                && openDrainage[neighbor] == 0
                ) {
                    continue;
                }

                boolean isOpenTarget =
                        openDrainage[neighbor] != 0;

                if (
                        !isOpenTarget
                                && hydrology.routeType(nx, nz)
                                != HydrologyRouteType.ORIGINAL
                ) {
                    continue;
                }

                if (
                        !isOpenTarget
                                && hydrology.lakeId(nx, nz) >= 0
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

                double newCost =
                        node.cost()
                                + direction.gridDistance()
                                + rise * RISE_COST;

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
            return null;
        }

        List<Integer> reversed =
                new ArrayList<>();

        int current =
                target;

        while (current >= 0) {
            reversed.add(current);

            if (current == sink) {
                break;
            }

            current =
                    parent[current];
        }

        if (
                reversed.isEmpty()
                        || reversed.getLast() != sink
        ) {
            return null;
        }

        List<Integer> path =
                new ArrayList<>(
                        reversed.size()
                );

        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(
                    reversed.get(i)
            );
        }

        return List.copyOf(path);
    }

    private static boolean applyEscapePath(
            HydrologyGrid hydrology,
            List<Integer> path
    ) {
        int size =
                hydrology.resolution();

        for (int i = 0; i < path.size() - 1; i++) {
            int from =
                    path.get(i);

            int to =
                    path.get(i + 1);

            int fromX =
                    from % size;

            int fromZ =
                    from / size;

            int toX =
                    to % size;

            int toZ =
                    to / size;

            if (
                    i > 0
                            && hydrology.routeType(
                            fromX,
                            fromZ
                    ) != HydrologyRouteType.ORIGINAL
            ) {
                return false;
            }

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
        }

        for (int i = 0; i < path.size() - 1; i++) {
            int from =
                    path.get(i);

            int to =
                    path.get(i + 1);

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

            hydrology.setRoutedFlowDirection(
                    fromX,
                    fromZ,
                    direction
            );

            hydrology.setRouteType(
                    fromX,
                    fromZ,
                    HydrologyRouteType.RESIDUAL_ESCAPE
            );
        }

        return true;
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
}
