package dev.lemma.finiteworlds.core.climate;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * Climate Pass 3B: establishes the atmospheric moisture source and prevailing
 * wind transport field used by later precipitation passes.
 *
 * The Cascadia template receives a dominant west-southwesterly marine flow:
 * air moves from WSW toward ENE. Ocean cells continuously recharge atmospheric
 * moisture, while air moving over land slowly dries with travel distance.
 *
 * This pass intentionally performs no orographic rainout. Mountains do not
 * consume moisture yet; Pass 3C will use this transported moisture budget plus
 * terrain uplift to generate windward precipitation.
 */
public final class MoistureTransportPlanner {

    /*
     * Unit vector pointing downwind. Raster +x is east and raster +z is south,
     * so ENE transport has positive x and negative z.
     */
    private static final double PREVAILING_WIND_X =
            0.94;

    private static final double PREVAILING_WIND_Z =
            -0.34;

    private static final double OCEAN_SOURCE =
            1.0;

    private static final double COASTAL_RECHARGE =
            0.10;

    private static final double MINIMUM_LAND_MOISTURE =
            0.035;

    private static final int SMOOTHING_PASSES =
            2;

    private MoistureTransportPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        ClimateGrid climate =
                world.climate();

        int size =
                world.resolution();

        if (size <= 0) {
            climate.finishMoistureTransport();
            return;
        }

        double blocksPerCell =
                world.config()
                        .blocksPerCell();

        double worldSize =
                world.config()
                        .worldSizeBlocks();

        /*
         * In the absence of precipitation, marine air should cross a large
         * fraction of the continent before drying substantially. 3C will
         * become the dominant moisture sink once mountains begin raining out.
         */
        double dryoutLengthBlocks =
                worldSize * 0.78;

        double cardinalRetention =
                Math.exp(
                        -blocksPerCell
                                / dryoutLengthBlocks
                );

        double diagonalRetention =
                Math.exp(
                        -(blocksPerCell * Math.sqrt(2.0))
                                / dryoutLengthBlocks
                );

        double[] transported =
                new double[size * size];

        double[] sourceField =
                new double[size * size];

        /*
         * Wind travels toward +x/-z, therefore processing x west->east and z
         * south->north guarantees the principal upwind samples have already
         * been solved. Compared with 3B, 3B.1 deliberately widens the upwind
         * stencil so transport is less locked to a single raster diagonal.
         */
        for (int x = 0; x < size; x++) {
            for (int z = size - 1; z >= 0; z--) {
                int index =
                        z * size + x;

                boolean ocean =
                        world.landMask(x, z) < 0.5f;

                double source =
                        ocean
                                ? OCEAN_SOURCE
                                : coastalRecharge(
                                world,
                                x,
                                z,
                                worldSize
                        );

                double advected =
                        upwindMoisture(
                                transported,
                                size,
                                x,
                                z,
                                cardinalRetention,
                                diagonalRetention
                        );

                double moisture;

                if (ocean) {
                    /*
                     * Ocean acts as a persistent reservoir instead of merely
                     * one boundary source. This also lets air crossing sounds
                     * and large inlets recharge before moving inland again.
                     */
                    moisture =
                            Math.max(
                                    OCEAN_SOURCE,
                                    advected
                            );
                } else {
                    moisture =
                            Math.max(
                                    MINIMUM_LAND_MOISTURE,
                                    advected
                                            + source
                                            * (1.0 - advected)
                            );
                }

                moisture =
                        clamp01(moisture);

                transported[index] =
                        moisture;
                sourceField[index] =
                        source;
            }
        }

        smoothTransportField(
                world,
                transported,
                SMOOTHING_PASSES
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index =
                        z * size + x;

                climate.setMoistureTransport(
                        x,
                        z,
                        (float) sourceField[index],
                        (float) transported[index],
                        (float) PREVAILING_WIND_X,
                        (float) PREVAILING_WIND_Z
                );
            }
        }

        climate.finishMoistureTransport();
    }

    private static double upwindMoisture(
            double[] transported,
            int size,
            int x,
            int z,
            double cardinalRetention,
            double diagonalRetention
    ) {
        double total =
                0.0;

        double weight =
                0.0;

        if (x > 0) {
            total +=
                    transported[
                            z * size
                                    + (x - 1)
                    ]
                            * cardinalRetention
                            * 0.46;

            weight +=
                    0.46;
        }

        if (
                x > 0
                        && z + 1 < size
        ) {
            total +=
                    transported[
                            (z + 1) * size
                                    + (x - 1)
                    ]
                            * diagonalRetention
                            * 0.26;

            weight +=
                    0.26;
        }

        if (
                x > 0
                        && z > 0
        ) {
            total +=
                    transported[
                            (z - 1) * size
                                    + (x - 1)
                    ]
                            * diagonalRetention
                            * 0.14;

            weight +=
                    0.14;
        }

        if (x > 1) {
            total +=
                    transported[
                            z * size
                                    + (x - 2)
                    ]
                            * cardinalRetention
                            * cardinalRetention
                            * 0.14;

            weight +=
                    0.14;
        }

        if (weight <= 1.0e-9) {
            return 0.0;
        }

        return total / weight;
    }

    private static void smoothTransportField(
            WorldBlueprint world,
            double[] transported,
            int passes
    ) {
        int size =
                world.resolution();

        if (size < 2 || passes <= 0) {
            return;
        }

        double[] scratch =
                new double[transported.length];

        for (int pass = 0; pass < passes; pass++) {
            for (int z = 0; z < size; z++) {
                int north =
                        Math.max(0, z - 1);
                int south =
                        Math.min(size - 1, z + 1);

                for (int x = 0; x < size; x++) {
                    int west =
                            Math.max(0, x - 1);
                    int east =
                            Math.min(size - 1, x + 1);

                    int index =
                            z * size + x;

                    boolean ocean =
                            world.landMask(x, z) < 0.5f;

                    double cardinalAverage =
                            0.25 * (
                                    transported[z * size + west]
                                            + transported[z * size + east]
                                            + transported[north * size + x]
                                            + transported[south * size + x]
                            );

                    double diagonalAverage =
                            0.25 * (
                                    transported[north * size + west]
                                            + transported[north * size + east]
                                            + transported[south * size + west]
                                            + transported[south * size + east]
                            );

                    double neighborAverage =
                            0.62 * cardinalAverage
                                    + 0.38 * diagonalAverage;

                    double blend =
                            ocean
                                    ? 0.16
                                    : 0.24;

                    double smoothed =
                            lerp(
                                    transported[index],
                                    neighborAverage,
                                    blend
                            );

                    if (ocean) {
                        smoothed =
                                Math.max(
                                        OCEAN_SOURCE,
                                        smoothed
                                );
                    } else {
                        smoothed =
                                Math.max(
                                        MINIMUM_LAND_MOISTURE,
                                        smoothed
                                );
                    }

                    scratch[index] =
                            clamp01(smoothed);
                }
            }

            System.arraycopy(
                    scratch,
                    0,
                    transported,
                    0,
                    transported.length
            );
        }
    }

    private static double coastalRecharge(
            WorldBlueprint world,
            int x,
            int z,
            double worldSize
    ) {
        double coastDistance =
                Math.max(
                        0.0,
                        world.coastDistance(x, z)
                );

        double coastInfluence =
                1.0
                        - smoothstep(
                        worldSize * 0.004,
                        worldSize * 0.035,
                        coastDistance
                );

        /*
         * This is intentionally weak. It represents persistent marine boundary
         * layer influence near shore, not precipitation or soil evaporation.
         */
        return COASTAL_RECHARGE
                * coastInfluence;
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

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (b - a) * clamp01(t);
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
