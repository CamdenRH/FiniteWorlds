package dev.lemma.finiteworlds.core.climate;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * Climate Pass 3C: converts the transported marine moisture field into
 * first-order orographic precipitation.
 *
 * Positive terrain rise along the prevailing WSW -> ENE flow forces air
 * upward. A fraction of the available atmospheric moisture rains out on those
 * windward slopes and the remaining moisture continues downwind. This pass is
 * intentionally conservative on explicit lee-side drying; Pass 3D will refine
 * the rain shadow after the first-order precipitation field is verified.
 */
public final class OrographicPrecipitationPlanner {

    private static final double OCEAN_MOISTURE_FLOOR =
            0.98;

    private static final double MINIMUM_LAND_MOISTURE =
            0.020;

    private static final double BASE_LAND_RAINOUT =
            0.010;

    private static final double MAX_OROGRAPHIC_RAINOUT =
            0.62;

    private static final int LIFT_SMOOTHING_PASSES =
            2;

    private static final int PRECIPITATION_SMOOTHING_PASSES =
            2;

    private OrographicPrecipitationPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        ClimateGrid climate =
                world.climate();

        int size =
                world.resolution();

        if (size <= 0) {
            climate.finishOrographicPrecipitation();
            return;
        }

        double[] remaining =
                new double[size * size];

        double[] liftField =
                new double[size * size];

        double[] precipitationField =
                new double[size * size];

        /*
         * Build a broad windward-ascent field before moisture is consumed.
         * 3C used only the immediate local rise, which made the result behave
         * like a ridge detector. 3C.1 samples terrain rise over several
         * upwind distances and then lightly diffuses it across neighboring
         * windward cells so whole mountain faces participate.
         */
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    continue;
                }

                liftField[z * size + x] =
                        orographicLift(
                                world,
                                x,
                                z
                        );
            }
        }

        smoothLiftField(
                world,
                liftField,
                LIFT_SMOOTHING_PASSES
        );

        /*
         * Same sweep direction used by Pass 3B. With WSW -> ENE flow, west
         * and southwest cells are the dominant upwind contributors.
         */
        for (int x = 0; x < size; x++) {
            for (int z = size - 1; z >= 0; z--) {
                int index =
                        z * size + x;

                boolean ocean =
                        world.landMask(x, z) < 0.5f;

                double transportCeiling =
                        clamp01(
                                climate.transportedMoisture(x, z)
                        );

                if (ocean) {
                    remaining[index] =
                            Math.max(
                                    OCEAN_MOISTURE_FLOOR,
                                    transportCeiling
                            );

                    continue;
                }

                double advected =
                        upwindRemainingMoisture(
                                remaining,
                                size,
                                x,
                                z
                        );

                double incoming;

                if (advected <= 1.0e-9) {
                    incoming =
                            transportCeiling;
                } else {
                    /*
                     * 3B is the no-rainout moisture envelope. Once 3C removes
                     * water upstream, that deficit is allowed to propagate,
                     * but the local 3B field remains an upper bound.
                     */
                    incoming =
                            Math.min(
                                    transportCeiling,
                                    advected
                                            + 0.012
                            );
                }

                incoming =
                        Math.max(
                                MINIMUM_LAND_MOISTURE,
                                incoming
                        );

                double lift =
                        liftField[index];

                double rainoutFraction =
                        BASE_LAND_RAINOUT
                                + MAX_OROGRAPHIC_RAINOUT
                                * Math.pow(
                                lift,
                                0.68
                        );

                rainoutFraction =
                        Math.min(
                                0.66,
                                rainoutFraction
                        );

                double removedMoisture =
                        incoming
                                * rainoutFraction;

                double postRainout =
                        Math.max(
                                MINIMUM_LAND_MOISTURE,
                                incoming
                                        - removedMoisture
                        );

                /*
                 * Store precipitation on a fixed relative 0..1 scale. The
                 * removed atmospheric fraction remains the physical driver;
                 * this multiplier simply makes useful PNW precipitation
                 * contrast occupy the diagnostic/climate range.
                 */
                double precipitation =
                        clamp01(
                                removedMoisture
                                        / 0.22
                        );

                remaining[index] =
                        postRainout;

                precipitationField[index] =
                        precipitation;
            }
        }

        smoothPrecipitation(
                world,
                precipitationField,
                PRECIPITATION_SMOOTHING_PASSES
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index =
                        z * size + x;

                climate.setOrographicPrecipitation(
                        x,
                        z,
                        (float) liftField[index],
                        (float) precipitationField[index],
                        (float) remaining[index]
                );
            }
        }

        climate.finishOrographicPrecipitation();
    }

    private static double upwindRemainingMoisture(
            double[] remaining,
            int size,
            int x,
            int z
    ) {
        double total =
                0.0;

        double weight =
                0.0;

        if (x > 0) {
            total +=
                    remaining[z * size + (x - 1)]
                            * 0.50;
            weight +=
                    0.50;
        }

        if (x > 0 && z + 1 < size) {
            total +=
                    remaining[(z + 1) * size + (x - 1)]
                            * 0.28;
            weight +=
                    0.28;
        }

        if (x > 0 && z > 0) {
            total +=
                    remaining[(z - 1) * size + (x - 1)]
                            * 0.12;
            weight +=
                    0.12;
        }

        if (x > 1) {
            total +=
                    remaining[z * size + (x - 2)]
                            * 0.10;
            weight +=
                    0.10;
        }

        return weight > 1.0e-9
                ? total / weight
                : 0.0;
    }

    private static double orographicLift(
            WorldBlueprint world,
            int x,
            int z
    ) {
        double current =
                world.elevation(x, z);

        /*
         * WSW -> ENE means the upwind direction points west-southwest:
         * negative x and positive z. Sampling multiple distances captures
         * sustained ascent across a broad mountain face rather than only the
         * final one-cell jump at the crest.
         */
        double nearRise =
                terrainRiseFromUpwind(
                        world,
                        x,
                        z,
                        2
                );

        double middleRise =
                terrainRiseFromUpwind(
                        world,
                        x,
                        z,
                        5
                );

        double broadRise =
                terrainRiseFromUpwind(
                        world,
                        x,
                        z,
                        9
                );

        double nearLift =
                smoothstep(
                        1.5,
                        42.0,
                        nearRise
                );

        double middleLift =
                smoothstep(
                        5.0,
                        105.0,
                        middleRise
                );

        double broadLift =
                smoothstep(
                        12.0,
                        210.0,
                        broadRise
                );

        /*
         * Broad ascent carries the most weight so precipitation begins on the
         * western flanks. Near-field slope still sharpens ridge/volcano
         * maxima, but no longer controls the whole precipitation footprint.
         */
        double ascent =
                nearLift * 0.22
                        + middleLift * 0.38
                        + broadLift * 0.40;

        double alpineBoost =
                smoothstep(
                        140.0,
                        680.0,
                        current
                );

        return clamp01(
                ascent
                        * (
                        0.88
                                + 0.24 * alpineBoost
                )
        );
    }

    private static double terrainRiseFromUpwind(
            WorldBlueprint world,
            int x,
            int z,
            int distance
    ) {
        int size =
                world.resolution();

        /*
         * Approximate the -wind vector (-0.94,+0.34). A small lateral sample
         * on either side prevents one raster ray from imprinting on the field.
         */
        int upwindX =
                Math.max(
                        0,
                        x - distance
                );

        int upwindZ =
                Math.min(
                        size - 1,
                        z
                                + (int) Math.round(
                                distance * 0.36
                        )
                );

        int lateral =
                Math.max(
                        1,
                        distance / 3
                );

        int zNorth =
                Math.max(
                        0,
                        upwindZ - lateral
                );

        int zSouth =
                Math.min(
                        size - 1,
                        upwindZ + lateral
                );

        double upwindElevation =
                world.elevation(upwindX, upwindZ)
                        * 0.56
                        + world.elevation(upwindX, zNorth)
                        * 0.22
                        + world.elevation(upwindX, zSouth)
                        * 0.22;

        return Math.max(
                0.0,
                world.elevation(x, z)
                        - upwindElevation
        );
    }

    private static void smoothLiftField(
            WorldBlueprint world,
            double[] lift,
            int passes
    ) {
        int size =
                world.resolution();

        if (size < 2 || passes <= 0) {
            return;
        }

        double[] scratch =
                new double[lift.length];

        for (int pass = 0; pass < passes; pass++) {
            for (int z = 0; z < size; z++) {
                int north =
                        Math.max(0, z - 1);
                int south =
                        Math.min(size - 1, z + 1);

                for (int x = 0; x < size; x++) {
                    int index =
                            z * size + x;

                    if (world.landMask(x, z) < 0.5f) {
                        scratch[index] =
                                0.0;
                        continue;
                    }

                    int west =
                            Math.max(0, x - 1);
                    int east =
                            Math.min(size - 1, x + 1);

                    double cardinal =
                            (
                                    lift[z * size + west]
                                            + lift[z * size + east]
                                            + lift[north * size + x]
                                            + lift[south * size + x]
                            ) * 0.25;

                    double diagonal =
                            (
                                    lift[north * size + west]
                                            + lift[north * size + east]
                                            + lift[south * size + west]
                                            + lift[south * size + east]
                            ) * 0.25;

                    double neighborhood =
                            cardinal * 0.64
                                    + diagonal * 0.36;

                    /*
                     * Bias toward retaining the local maximum while spreading
                     * enough lift laterally to turn thin ridge traces into a
                     * contiguous windward precipitation belt.
                     */
                    double spread =
                            lerp(
                                    lift[index],
                                    neighborhood,
                                    0.34
                            );

                    scratch[index] =
                            clamp01(
                                    Math.max(
                                            lift[index] * 0.92,
                                            spread
                                    )
                            );
                }
            }

            System.arraycopy(
                    scratch,
                    0,
                    lift,
                    0,
                    lift.length
            );
        }
    }

    private static void smoothPrecipitation(
            WorldBlueprint world,
            double[] precipitation,
            int passes
    ) {
        int size =
                world.resolution();

        if (size < 2 || passes <= 0) {
            return;
        }

        double[] scratch =
                new double[precipitation.length];

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

                    if (world.landMask(x, z) < 0.5f) {
                        scratch[index] =
                                0.0;
                        continue;
                    }

                    double cardinal =
                            0.25 * (
                                    precipitation[z * size + west]
                                            + precipitation[z * size + east]
                                            + precipitation[north * size + x]
                                            + precipitation[south * size + x]
                            );

                    double diagonal =
                            0.25 * (
                                    precipitation[north * size + west]
                                            + precipitation[north * size + east]
                                            + precipitation[south * size + west]
                                            + precipitation[south * size + east]
                            );

                    double neighborhood =
                            cardinal * 0.66
                                    + diagonal * 0.34;

                    scratch[index] =
                            clamp01(
                                    lerp(
                                            precipitation[index],
                                            neighborhood,
                                            0.30
                                    )
                            );
                }
            }

            System.arraycopy(
                    scratch,
                    0,
                    precipitation,
                    0,
                    precipitation.length
            );
        }
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0 : 0.0;
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
