package dev.lemma.finiteworlds.core.climate;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * Climate Pass 3A: produces a stable annualized baseline temperature field.
 *
 * The model intentionally remains simple and interpretable:
 *
 *  - north/south position establishes the regional temperature gradient,
 *  - final authored elevation applies an environmental lapse rate,
 *  - nearby ocean moderates temperatures toward a maritime baseline,
 *  - inland continentality mildly amplifies the north/south thermal contrast.
 *
 * No precipitation, humidity, seasonality, runoff, or biome classification is
 * attempted in this pass.
 */
public final class TemperaturePlanner {

    private static final double SOUTHERN_SEA_LEVEL_C =
            14.0;

    private static final double NORTHERN_SEA_LEVEL_C =
            7.0;

    private static final double MARITIME_REFERENCE_C =
            10.5;

    /*
     * Effective lapse rate for the intentionally exaggerated vertical scale
     * used by FiniteWorlds. This is stronger than a literal real-atmosphere
     * lapse rate so the Cascade crest becomes properly alpine and the
     * landmark volcano reaches the coldest climate band.
     */
    private static final double LAPSE_RATE_C_PER_BLOCK =
            0.0140;

    private static final double REFERENCE_ELEVATION =
            64.0;

    private static final double MINIMUM_TEMPERATURE_C =
            -18.0;

    private static final double MAXIMUM_TEMPERATURE_C =
            18.0;

    private TemperaturePlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        ClimateGrid climate =
                world.climate();

        int size =
                world.resolution();

        double worldSizeBlocks =
                world.config()
                        .worldSizeBlocks();

        for (int z = 0; z < size; z++) {
            /*
             * Raster z increases southward: z=0 is the north edge and
             * z=size-1 is the south edge.
             */
            double southwardPosition =
                    size > 1
                            ? z / (double) (size - 1)
                            : 0.5;

            double latitudeTemperature =
                    lerp(
                            NORTHERN_SEA_LEVEL_C,
                            SOUTHERN_SEA_LEVEL_C,
                            southwardPosition
                    );

            for (int x = 0; x < size; x++) {
                double elevation =
                        world.elevation(x, z);

                double elevationAboveReference =
                        Math.max(
                                0.0,
                                elevation
                                        - REFERENCE_ELEVATION
                        );

                double elevationCooling =
                        elevationAboveReference
                                * LAPSE_RATE_C_PER_BLOCK;

                double coastDistance =
                        Math.max(
                                0.0,
                                world.coastDistance(x, z)
                        );

                double maritimeInfluence =
                        1.0
                                - smoothstep(
                                worldSizeBlocks * 0.025,
                                worldSizeBlocks * 0.18,
                                coastDistance
                        );

                /*
                 * Maritime air damps the regional north/south contrast instead
                 * of simply adding a constant coastal temperature bonus.
                 */
                double maritimeTemperature =
                        lerp(
                                latitudeTemperature,
                                MARITIME_REFERENCE_C,
                                maritimeInfluence * 0.42
                        );

                double inlandness =
                        smoothstep(
                                worldSizeBlocks * 0.07,
                                worldSizeBlocks * 0.32,
                                coastDistance
                        );

                /*
                 * Continentality mildly strengthens the regional gradient:
                 * southern interior cells run a little warmer than their
                 * maritime equivalent while northern interior cells run
                 * somewhat cooler. The magnitude remains intentionally small
                 * until a future seasonal model exists.
                 */
                double regionalContrast =
                        southwardPosition - 0.5;

                double continentalAdjustment =
                        inlandness
                                * regionalContrast
                                * 3.0;

                double temperature =
                        maritimeTemperature
                                + continentalAdjustment
                                - elevationCooling;

                /*
                 * Ocean cells still receive a useful climate value for later
                 * atmospheric moisture transport. Their temperature is kept
                 * close to the maritime baseline and ignores ocean depth.
                 */
                if (world.landMask(x, z) < 0.5f) {
                    temperature =
                            lerp(
                                    latitudeTemperature,
                                    MARITIME_REFERENCE_C,
                                    0.72
                            );

                    elevationCooling =
                            0.0;

                    continentalAdjustment =
                            0.0;
                }

                temperature =
                        clamp(
                                temperature,
                                MINIMUM_TEMPERATURE_C,
                                MAXIMUM_TEMPERATURE_C
                        );

                double normalized =
                        clamp01(
                                (temperature
                                        - MINIMUM_TEMPERATURE_C)
                                        / (
                                        MAXIMUM_TEMPERATURE_C
                                                - MINIMUM_TEMPERATURE_C
                                )
                        );

                climate.setTemperature(
                        x,
                        z,
                        (float) temperature,
                        (float) normalized,
                        (float) latitudeTemperature,
                        (float) elevationCooling,
                        (float) continentalAdjustment
                );
            }
        }
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
