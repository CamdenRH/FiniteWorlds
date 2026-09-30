package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;

/**
 * Hydrology Pass 2B: extracts a provisional macro stream network from the
 * completed flow-accumulation field.
 *
 * Channel initiation is terrain-aware: steep mountain terrain can begin a
 * channel with a smaller contributing area than flat lowlands. Once a channel
 * begins it remains active downstream, even if it crosses a province with a
 * higher initiation threshold. Lake-interior routing is deliberately hidden
 * from the channel mask; accumulated flow reappears at the lake outlet.
 *
 * This pass remains diagnostic-only. It does not carve terrain, assign final
 * river widths, place water, or model precipitation/discharge.
 */
public final class StreamNetworkExtractor {

    private static final long TRIBUTARY_ACCUMULATION =
            192L;

    private static final long RIVER_ACCUMULATION =
            768L;

    private static final long MAJOR_RIVER_ACCUMULATION =
            3_072L;

    private static final long TRUNK_RIVER_ACCUMULATION =
            12_288L;

    private StreamNetworkExtractor() {
    }

    public static void extract(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        if (hydrology.flowAccumulationProcessedCellCount() <= 0) {
            throw new IllegalStateException(
                    "Stream extraction requires flow accumulation"
            );
        }

        int size =
                hydrology.resolution();

        int cellCount =
                size * size;

        int[] indegree =
                new int[cellCount];

        boolean[] upstreamChannel =
                new boolean[cellCount];

        int[] queue =
                new int[cellCount];

        hydrology.clearStreamNetwork();

        /*
         * Build indegrees for the same final routed graph used by 2A. The
         * graph is already guaranteed sink-free and acyclic by Phase 1.
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

                int initiationThreshold =
                        initiationThreshold(
                                world,
                                hydrology,
                                x,
                                z
                        );

                hydrology.setStreamInitiationThreshold(
                        x,
                        z,
                        initiationThreshold
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

        int channelCells =
                0;

        int headwaterSources =
                0;

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

            boolean initiatesHere =
                    accumulation
                            >= hydrology.streamInitiationThreshold(
                            x,
                            z
                    );

            boolean channelActive =
                    upstreamChannel[cell]
                            || initiatesHere;

            boolean visibleChannel =
                    channelActive
                            && hydrology.lakeId(x, z) < 0;

            StreamClass streamClass =
                    visibleChannel
                            ? classForAccumulation(accumulation)
                            : StreamClass.NONE;

            hydrology.setStreamClass(
                    x,
                    z,
                    streamClass
            );

            if (streamClass.isChannel()) {
                channelCells++;

                if (!upstreamChannel[cell]) {
                    headwaterSources++;
                }
            }

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

            if (!inside(nx, nz, size)) {
                continue;
            }

            if (
                    hydrology.routedFlowDirection(nx, nz)
                            == FlowDirection.OCEAN
            ) {
                continue;
            }

            int target =
                    index(nx, nz, size);

            if (channelActive) {
                upstreamChannel[target] = true;
            }

            indegree[target]--;

            if (indegree[target] == 0) {
                queue[tail++] = target;
            }
        }

        if (
                processed
                        != hydrology.flowAccumulationProcessedCellCount()
        ) {
            throw new IllegalStateException(
                    "Stream extraction processed "
                            + processed
                            + " cells but accumulation processed "
                            + hydrology.flowAccumulationProcessedCellCount()
            );
        }

        hydrology.setStreamNetworkStats(
                channelCells,
                headwaterSources
        );
    }

    private static int initiationThreshold(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            int x,
            int z
    ) {
        TerrainProvince province =
                world.terrainProvince(
                        x,
                        z
                );

        double base =
                switch (province) {
                    case CASCADE_CORE -> 140.0;
                    case COAST_RANGE -> 200.0;
                    case CASCADE_FOOTHILLS -> 190.0;
                    case EASTERN_SLOPES -> 260.0;
                    case COASTAL -> 280.0;
                    case WESTERN_LOWLAND -> 320.0;
                    case INTERIOR_PLATEAU -> 420.0;
                    case OCEAN -> Double.POSITIVE_INFINITY;
                };

        if (!Double.isFinite(base)) {
            return Short.MAX_VALUE;
        }

        double slope =
                localSlope(
                        hydrology,
                        x,
                        z
                );

        /*
         * Steep terrain concentrates runoff into recognizable channels with
         * less contributing area; broad flat terrain needs a larger drainage
         * area before becoming a persistent macro channel. Climate will
         * eventually replace part of this province proxy with actual runoff.
         */
        double slopeFactor =
                clamp(
                        1.18 - slope * 2.50,
                        0.62,
                        1.18
                );

        return (int) Math.round(
                clamp(
                        base * slopeFactor / (0.65 + 0.85 * world.climate().refinedPrecipitation(x, z)),
                        80.0,
                        768.0
                )
        );
    }

    private static double localSlope(
            HydrologyGrid hydrology,
            int x,
            int z
    ) {
        int size =
                hydrology.resolution();

        int west =
                Math.max(
                        0,
                        x - 1
                );

        int east =
                Math.min(
                        size - 1,
                        x + 1
                );

        int north =
                Math.max(
                        0,
                        z - 1
                );

        int south =
                Math.min(
                        size - 1,
                        z + 1
                );

        double xDistance =
                Math.max(
                        1.0,
                        (east - west)
                                * hydrology.blocksPerCell()
                );

        double zDistance =
                Math.max(
                        1.0,
                        (south - north)
                                * hydrology.blocksPerCell()
                );

        double dx =
                (
                        hydrology.conditionedElevation(east, z)
                                - hydrology.conditionedElevation(west, z)
                )
                        / xDistance;

        double dz =
                (
                        hydrology.conditionedElevation(x, south)
                                - hydrology.conditionedElevation(x, north)
                )
                        / zDistance;

        return Math.hypot(
                dx,
                dz
        );
    }

    private static StreamClass classForAccumulation(
            long accumulation
    ) {
        if (accumulation >= TRUNK_RIVER_ACCUMULATION) {
            return StreamClass.TRUNK_RIVER;
        }

        if (accumulation >= MAJOR_RIVER_ACCUMULATION) {
            return StreamClass.MAJOR_RIVER;
        }

        if (accumulation >= RIVER_ACCUMULATION) {
            return StreamClass.RIVER;
        }

        if (accumulation >= TRIBUTARY_ACCUMULATION) {
            return StreamClass.TRIBUTARY;
        }

        return StreamClass.HEADWATER;
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

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}
