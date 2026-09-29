package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * Hydrology Pass 2A: computes upstream contributing-cell accumulation on the
 * final sink-free, acyclic routed drainage graph.
 *
 * Every non-ocean hydrology cell contributes one unit. Those contributions are
 * propagated downstream in topological order. No stream thresholds, river
 * classification, erosion, or terrain modification occurs in this pass.
 */
public final class FlowAccumulator {

    private FlowAccumulator() {
    }

    public static void compute(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        if (hydrology.routedSinkCount() != 0) {
            throw new IllegalStateException(
                    "Flow accumulation requires a sink-free routed graph"
            );
        }

        if (hydrology.routedCycleCount() != 0) {
            throw new IllegalStateException(
                    "Flow accumulation requires an acyclic routed graph"
            );
        }

        int size =
                hydrology.resolution();

        int cellCount =
                size * size;

        int[] indegree =
                new int[cellCount];

        int nonOceanCellCount =
                0;

        hydrology.clearFlowAccumulation();

        /*
         * Initialize one unit of contributing area on every non-ocean cell and
         * count inbound routed edges. Edges that discharge directly into ocean
         * terminate at the final land cell and are not propagated into ocean
         * cells.
         */
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.routedFlowDirection(
                                x,
                                z
                        );

                if (direction == FlowDirection.OCEAN) {
                    continue;
                }

                nonOceanCellCount++;

                hydrology.setFlowAccumulation(
                        x,
                        z,
                        1L
                );

                if (!direction.routesToNeighbor()) {
                    continue;
                }

                int nx =
                        x + direction.dx();

                int nz =
                        z + direction.dz();

                if (!inside(nx, nz, size)) {
                    continue;
                }

                if (
                        hydrology.routedFlowDirection(nx, nz)
                                == FlowDirection.OCEAN
                ) {
                    continue;
                }

                indegree[index(nx, nz, size)]++;
            }
        }

        int[] queue =
                new int[nonOceanCellCount];

        int head = 0;
        int tail = 0;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (
                        hydrology.routedFlowDirection(x, z)
                                == FlowDirection.OCEAN
                ) {
                    continue;
                }

                int cell =
                        index(x, z, size);

                if (indegree[cell] == 0) {
                    queue[tail++] = cell;
                }
            }
        }

        int processed =
                0;

        long maximumAccumulation =
                0L;

        long terminalAccumulation =
                0L;

        while (head < tail) {
            int cell =
                    queue[head++];

            int x =
                    cell % size;

            int z =
                    cell / size;

            processed++;

            long accumulation =
                    hydrology.flowAccumulation(
                            x,
                            z
                    );

            maximumAccumulation =
                    Math.max(
                            maximumAccumulation,
                            accumulation
                    );

            FlowDirection direction =
                    hydrology.routedFlowDirection(
                            x,
                            z
                    );

            if (!direction.routesToNeighbor()) {
                terminalAccumulation +=
                        accumulation;
                continue;
            }

            int nx =
                    x + direction.dx();

            int nz =
                    z + direction.dz();

            if (!inside(nx, nz, size)) {
                terminalAccumulation +=
                        accumulation;
                continue;
            }

            if (
                    hydrology.routedFlowDirection(nx, nz)
                            == FlowDirection.OCEAN
            ) {
                terminalAccumulation +=
                        accumulation;
                continue;
            }

            hydrology.addFlowAccumulation(
                    nx,
                    nz,
                    accumulation
            );

            int target =
                    index(nx, nz, size);

            indegree[target]--;

            if (indegree[target] == 0) {
                queue[tail++] = target;
            }
        }

        if (processed != nonOceanCellCount) {
            throw new IllegalStateException(
                    "Flow accumulation processed "
                            + processed
                            + " of "
                            + nonOceanCellCount
                            + " non-ocean cells; routed graph is not acyclic"
            );
        }

        if (terminalAccumulation != nonOceanCellCount) {
            throw new IllegalStateException(
                    "Flow accumulation conservation failed: terminal catchments contain "
                            + terminalAccumulation
                            + " of "
                            + nonOceanCellCount
                            + " contributing cells"
            );
        }

        hydrology.setFlowAccumulationStats(
                processed,
                maximumAccumulation
        );
    }

    private static boolean inside(
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
