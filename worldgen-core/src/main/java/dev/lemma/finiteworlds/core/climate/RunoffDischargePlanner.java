package dev.lemma.finiteworlds.core.climate;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.hydrology.FlowDirection;
import dev.lemma.finiteworlds.core.hydrology.HydrologyGrid;

/**
 * Climate Pass 3E: converts the locked Phase-3 climate fields into a
 * climate-weighted runoff and effective river-discharge field.
 *
 * This pass is intentionally non-destructive. It does not alter stream
 * topology, river centerlines, valley geometry, channel width, or terrain.
 * Instead, every land cell receives a relative annual water-yield value based
 * on refined precipitation, temperature, and snow storage. Those local yields
 * are then accumulated through the already-validated routed hydrology graph.
 *
 * Later passes can use effective discharge to resize channels or refine stream
 * initiation without discarding the geometric work completed in Phase 2.
 */
public final class RunoffDischargePlanner {

    private static final double MINIMUM_RUNOFF_COEFFICIENT =
            0.12;

    private static final double MAXIMUM_RUNOFF_COEFFICIENT =
            0.90;

    private static final double MOISTURE_BASEFLOW_WEIGHT =
            0.010;

    private RunoffDischargePlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        ClimateGrid climate =
                world.climate();

        HydrologyGrid hydrology =
                world.hydrology();

        if (!climate.rainShadowRefinementPlanned()) {
            throw new IllegalStateException(
                    "Climate Pass 3E requires completed rain-shadow refinement"
            );
        }

        if (hydrology.flowAccumulationProcessedCellCount() <= 0) {
            throw new IllegalStateException(
                    "Climate Pass 3E requires hydrology flow accumulation"
            );
        }

        if (hydrology.routedSinkCount() != 0) {
            throw new IllegalStateException(
                    "Climate Pass 3E requires a sink-free routed graph"
            );
        }

        if (hydrology.routedCycleCount() != 0) {
            throw new IllegalStateException(
                    "Climate Pass 3E requires an acyclic routed graph"
            );
        }

        int size =
                world.resolution();

        int cellCount =
                size * size;

        double[] localRunoff =
                new double[cellCount];

        double[] snowStorage =
                new double[cellCount];

        double[] discharge =
                new double[cellCount];

        int[] indegree =
                new int[cellCount];

        int nonOceanCells =
                0;

        /*
         * Build the same DAG used by FlowAccumulator and initialize each cell
         * with its climate-derived water yield.
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

                nonOceanCells++;

                int cell =
                        index(
                                x,
                                z,
                                size
                        );

                double precipitation =
                        clamp01(
                                climate.refinedPrecipitation(
                                        x,
                                        z
                                )
                        );

                double moisture =
                        clamp01(
                                climate.refinedMoisture(
                                        x,
                                        z
                                )
                        );

                double temperature =
                        climate.temperatureCelsius(
                                x,
                                z
                        );

                /*
                 * TemperaturePlanner stores an annualized baseline rather than
                 * a winter minimum. Requiring an annual mean below freezing
                 * made almost all seasonal mountain snow disappear. 3E.1
                 * therefore separates persistent alpine snow from the broader
                 * seasonal snow belt.
                 */
                double persistentSnow =
                        1.0
                                - smoothstep(
                                -4.5,
                                1.5,
                                temperature
                        );

                double seasonalSnow =
                        1.0
                                - smoothstep(
                                1.0,
                                9.0,
                                temperature
                        );

                double snowFraction =
                        clamp01(
                                Math.max(
                                        persistentSnow,
                                        seasonalSnow * 0.72
                                )
                        );

                double wetness =
                        smoothstep(
                                0.025,
                                0.32,
                                precipitation
                        );

                double warmth =
                        smoothstep(
                                2.0,
                                16.0,
                                temperature
                        );

                /*
                 * Wet/cold catchments export a larger share of precipitation.
                 * Warm dry basins lose more water to evapotranspiration and
                 * infiltration. Snow slightly increases annual water yield but
                 * retains part of it seasonally before melt.
                 */
                double runoffCoefficient =
                        0.18
                                + 0.55 * wetness
                                + 0.08 * snowFraction
                                - 0.10 * warmth;

                runoffCoefficient =
                        clamp(
                                runoffCoefficient,
                                MINIMUM_RUNOFF_COEFFICIENT,
                                MAXIMUM_RUNOFF_COEFFICIENT
                        );

                double seasonalSnowRelease =
                        1.0
                                - 0.05 * snowFraction;

                double runoff =
                        precipitation
                                * runoffCoefficient
                                * seasonalSnowRelease
                                + moisture
                                * MOISTURE_BASEFLOW_WEIGHT;

                runoff =
                        Math.max(
                                0.0,
                                runoff
                        );

                localRunoff[cell] =
                        runoff;

                snowStorage[cell] =
                        snowFraction;

                discharge[cell] =
                        runoff;

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

                indegree[
                        index(
                                nx,
                                nz,
                                size
                        )
                ]++;
            }
        }

        int[] queue =
                new int[nonOceanCells];

        int head =
                0;

        int tail =
                0;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (
                        hydrology.routedFlowDirection(x, z)
                                == FlowDirection.OCEAN
                ) {
                    continue;
                }

                int cell =
                        index(
                                x,
                                z,
                                size
                        );

                if (indegree[cell] == 0) {
                    queue[tail++] =
                            cell;
                }
            }
        }

        int processed =
                0;

        double maximumDischarge =
                0.0;

        while (head < tail) {
            int cell =
                    queue[head++];

            int x =
                    cell % size;

            int z =
                    cell / size;

            processed++;

            maximumDischarge =
                    Math.max(
                            maximumDischarge,
                            discharge[cell]
                    );

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
                    index(
                            nx,
                            nz,
                            size
                    );

            discharge[target] +=
                    discharge[cell];

            indegree[target]--;

            if (indegree[target] == 0) {
                queue[tail++] =
                        target;
            }
        }

        if (processed != nonOceanCells) {
            throw new IllegalStateException(
                    "Climate runoff accumulation processed "
                            + processed
                            + " of "
                            + nonOceanCells
                            + " non-ocean cells"
            );
        }

        double logMaximum =
                Math.log1p(
                        Math.max(
                                1.0e-9,
                                maximumDischarge
                        )
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.routedFlowDirection(
                                x,
                                z
                        );

                if (direction == FlowDirection.OCEAN) {
                    climate.setRunoffDischargeCell(
                            x,
                            z,
                            0.0f,
                            0.0f,
                            0.0f,
                            0.0f,
                            0.0f
                    );

                    continue;
                }

                int cell =
                        index(
                                x,
                                z,
                                size
                        );

                long contributingCells =
                        Math.max(
                                1L,
                                hydrology.flowAccumulation(
                                        x,
                                        z
                                )
                        );

                double specific =
                        discharge[cell]
                                / contributingCells;

                double normalized =
                        logMaximum > 1.0e-12
                                ? Math.log1p(
                                discharge[cell]
                        )
                                / logMaximum
                                : 0.0;

                climate.setRunoffDischargeCell(
                        x,
                        z,
                        (float) localRunoff[cell],
                        (float) snowStorage[cell],
                        (float) discharge[cell],
                        (float) specific,
                        (float) clamp01(normalized)
                );
            }
        }

        climate.finishRunoffDischarge(
                maximumDischarge
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

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        if (edge1 <= edge0) {
            return value >= edge1
                    ? 1.0
                    : 0.0;
        }

        double t =
                clamp01(
                        (value - edge0)
                                / (edge1 - edge0)
                );

        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(
            double value
    ) {
        return clamp(
                value,
                0.0,
                1.0
        );
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
