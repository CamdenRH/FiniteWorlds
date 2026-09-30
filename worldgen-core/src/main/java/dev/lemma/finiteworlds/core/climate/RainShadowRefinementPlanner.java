package dev.lemma.finiteworlds.core.climate;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * Climate Pass 3D: refines the first-order 3C.1 precipitation field into a
 * persistent Cascadia-style rain shadow.
 *
 * Pass 3D.3 treats the Cascades as a continuous atmospheric barrier instead
 * of allowing individual peaks to create independent shadow plumes. It derives
 * a continuous barrier corridor from the 3C.1 orographic-lift field, injects a
 * lee-side dry-air source immediately downwind of that corridor, then advects
 * and diffuses the resulting regional rain shadow toward the east-northeast.
 * Pass 3D.4 softens the crest onset, introduces gradual far-lee recovery, and
 * smooths coastal mountain/ocean transitions on the windward side.
 * Pass 3D.5 replaces the remaining crest seam with a finite-width atmospheric
 * transition zone that widens where the Cascade barrier reaches the ocean.
 * Pass 3D.6 derives that transition from a two-dimensional signed-distance
 * field around the actual barrier corridor, including curved coastal termini.
 */
public final class RainShadowRefinementPlanner {

    private static final double MINIMUM_REFINED_MOISTURE = 0.012;
    private static final double SHADOW_RETENTION = 0.994;
    private static final int BARRIER_SMOOTHING_PASSES = 6;
    private static final int BARRIER_PATH_SMOOTHING_PASSES = 8;
    private static final int SHADOW_SMOOTHING_PASSES = 3;
    private static final int CLIMATE_SMOOTHING_PASSES = 2;

    private RainShadowRefinementPlanner() {
    }

    public static void plan(WorldBlueprint world) {
        ClimateGrid climate = world.climate();
        int size = world.resolution();
        double worldSizeBlocks = world.config().worldSizeBlocks();

        if (size <= 0) {
            climate.finishRainShadowRefinement();
            return;
        }

        double[] barrierPotential = buildBarrierPotential(world, climate);
        int globalBarrierX = findGlobalBarrierX(world, barrierPotential);
        double[] barrierX = buildBarrierPath(world, barrierPotential, globalBarrierX);
        double[] barrierStrength = buildBarrierStrength(world, barrierPotential, barrierX);

        smoothSeries(barrierX, BARRIER_PATH_SMOOTHING_PASSES, 0.42);
        smoothSeries(barrierStrength, BARRIER_PATH_SMOOTHING_PASSES, 0.36);

        double[] signedBarrierDistance =
                buildSignedBarrierDistance(
                        world,
                        barrierX
                );

        double[] shadow = new double[size * size];
        double[] refinedPrecipitation = new double[size * size];
        double[] refinedMoisture = new double[size * size];

        for (int x = 0; x < size; x++) {
            for (int z = size - 1; z >= 0; z--) {
                int index = z * size + x;

                if (world.landMask(x, z) < 0.5f) {
                    shadow[index] = 0.0;
                    continue;
                }

                double carried = barrierCrossedShadow(shadow, size, x, z) * SHADOW_RETENTION;
                double localSource =
                        barrierLeeSource(
                                world,
                                x,
                                z,
                                signedBarrierDistance,
                                barrierX,
                                barrierStrength,
                                worldSizeBlocks
                        );
                double localLift = clamp01(climate.orographicLift(x, z));

                double ascentRecovery =
                        1.0 - 0.10 * Math.pow(localLift, 0.85);

                shadow[index] = clamp01(Math.max(localSource, carried * ascentRecovery));
            }
        }

        smoothField(world, shadow, SHADOW_SMOOTHING_PASSES, 0.28, true);

        applyFarLeeRecovery(
                world,
                shadow,
                signedBarrierDistance
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;

                if (world.landMask(x, z) < 0.5f) {
                    refinedPrecipitation[index] = 0.0;
                    refinedMoisture[index] = clamp01(climate.postOrographicMoisture(x, z));
                    continue;
                }

                double shadowStrength = shadow[index];
                double firstOrderPrecipitation = clamp01(climate.orographicPrecipitation(x, z));
                double postOrographicMoisture = clamp01(climate.postOrographicMoisture(x, z));
                double transportedMoisture = clamp01(climate.transportedMoisture(x, z));

                double marineBackground =
                        (0.030 + 0.105 * Math.pow(transportedMoisture, 1.35))
                                * (1.0 - 0.76 * shadowStrength);

                double coastalBlend =
                        coastalOrographicBlend(
                                world,
                                x,
                                z,
                                signedBarrierDistance,
                                worldSizeBlocks
                        );

                double coastalAdjustedFirstOrder =
                        firstOrderPrecipitation
                                * (
                                0.34
                                        + 0.66 * coastalBlend
                        );

                double mountainPrecipitation =
                        coastalAdjustedFirstOrder
                                * (
                                1.0
                                        - 0.34 * shadowStrength
                        );

                refinedPrecipitation[index] =
                        clamp01(marineBackground + mountainPrecipitation);

                double moisture =
                        postOrographicMoisture * (1.0 - 0.50 * shadowStrength);

                refinedMoisture[index] =
                        Math.max(MINIMUM_REFINED_MOISTURE, clamp01(moisture));
            }
        }

        smoothField(world, refinedPrecipitation, CLIMATE_SMOOTHING_PASSES, 0.24, false);
        smoothField(world, refinedMoisture, CLIMATE_SMOOTHING_PASSES, 0.18, false);

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                climate.setRainShadowRefinement(
                        x,
                        z,
                        (float) shadow[index],
                        (float) refinedPrecipitation[index],
                        (float) refinedMoisture[index]
                );
            }
        }

        climate.finishRainShadowRefinement();
    }

    private static double[] buildBarrierPotential(WorldBlueprint world, ClimateGrid climate) {
        int size = world.resolution();
        double[] potential = new double[size * size];

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                if (world.landMask(x, z) < 0.5f) {
                    potential[index] = 0.0;
                    continue;
                }

                double lift = clamp01(climate.orographicLift(x, z));
                potential[index] = Math.pow(lift, 0.48);
            }
        }

        smoothField(world, potential, BARRIER_SMOOTHING_PASSES, 0.44, true);
        return potential;
    }

    private static int findGlobalBarrierX(WorldBlueprint world, double[] potential) {
        int size = world.resolution();
        int minimumX = Math.max(1, (int) Math.round(size * 0.34));
        int maximumX = Math.min(size - 2, (int) Math.round(size * 0.82));
        int bestX = (minimumX + maximumX) / 2;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int x = minimumX; x <= maximumX; x++) {
            double score = 0.0;
            double weight = 0.0;

            for (int z = 0; z < size; z++) {
                if (world.landMask(x, z) < 0.5f) {
                    continue;
                }

                double value = potential[z * size + x];
                score += value * value;
                weight += 1.0;
            }

            if (weight > 0.0) {
                score /= weight;
            }

            if (score > bestScore) {
                bestScore = score;
                bestX = x;
            }
        }

        return bestX;
    }

    private static double[] buildBarrierPath(
            WorldBlueprint world,
            double[] potential,
            int globalBarrierX
    ) {
        int size = world.resolution();
        double[] path = new double[size];
        int searchRadius = Math.max(8, (int) Math.round(size * 0.12));
        int previousX = globalBarrierX;

        for (int z = 0; z < size; z++) {
            int startX = Math.max(1, globalBarrierX - searchRadius);
            int endX = Math.min(size - 2, globalBarrierX + searchRadius);
            int bestX = previousX;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int x = startX; x <= endX; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    continue;
                }

                double local = potential[z * size + x];
                double continuity = Math.abs(x - previousX) / (double) Math.max(1, searchRadius);
                double globalOffset = Math.abs(x - globalBarrierX) / (double) Math.max(1, searchRadius);
                double score = local - 0.16 * continuity - 0.05 * globalOffset;

                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                }
            }

            path[z] = bestX;
            previousX = bestX;
        }

        return path;
    }

    private static double[] buildBarrierStrength(
            WorldBlueprint world,
            double[] potential,
            double[] barrierX
    ) {
        int size = world.resolution();
        double[] strength = new double[size];
        double globalMaximum = 1.0e-9;

        for (int z = 0; z < size; z++) {
            int centerX = clampInt((int) Math.round(barrierX[z]), 0, size - 1);
            double rowMaximum = 0.0;

            for (int dx = -5; dx <= 5; dx++) {
                int sampleX = centerX + dx;
                if (sampleX < 0 || sampleX >= size) {
                    continue;
                }
                rowMaximum = Math.max(rowMaximum, potential[z * size + sampleX]);
            }

            strength[z] = rowMaximum;
            globalMaximum = Math.max(globalMaximum, rowMaximum);
        }

        for (int z = 0; z < size; z++) {
            double normalized = clamp01(strength[z] / globalMaximum);
            strength[z] = 0.42 + 0.46 * Math.pow(normalized, 0.70);
        }

        return strength;
    }

    private static double[] buildSignedBarrierDistance(
            WorldBlueprint world,
            double[] barrierX
    ) {
        int size = world.resolution();
        int cellCount = size * size;

        double infinity =
                size * 4.0;

        double[] distance =
                new double[cellCount];

        boolean[] barrier =
                new boolean[cellCount];

        for (int i = 0; i < cellCount; i++) {
            distance[i] =
                    infinity;
        }

        /*
         * Rasterize only barrier cells that are actually on land. When the
         * mountain chain terminates at the ocean, the mask ends there rather
         * than continuing as an artificial vertical line through marine rows.
         */
        for (int z = 0; z < size; z++) {
            int centerX =
                    clampInt(
                            (int) Math.round(
                                    barrierX[z]
                            ),
                            0,
                            size - 1
                    );

            for (int dx = -1; dx <= 1; dx++) {
                int x =
                        centerX + dx;

                if (x < 0 || x >= size) {
                    continue;
                }

                if (world.landMask(x, z) < 0.5f) {
                    continue;
                }

                int index =
                        z * size + x;

                barrier[index] =
                        true;

                distance[index] =
                        0.0;
            }
        }

        /*
         * Two-pass chamfer distance transform. Cardinal and diagonal costs
         * approximate Euclidean distance while remaining O(n^2).
         */
        final double diagonalCost =
                Math.sqrt(2.0);

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index =
                        z * size + x;

                double best =
                        distance[index];

                if (x > 0) {
                    best =
                            Math.min(
                                    best,
                                    distance[index - 1] + 1.0
                            );
                }

                if (z > 0) {
                    best =
                            Math.min(
                                    best,
                                    distance[index - size] + 1.0
                            );

                    if (x > 0) {
                        best =
                                Math.min(
                                        best,
                                        distance[index - size - 1]
                                                + diagonalCost
                                );
                    }

                    if (x + 1 < size) {
                        best =
                                Math.min(
                                        best,
                                        distance[index - size + 1]
                                                + diagonalCost
                                );
                    }
                }

                distance[index] =
                        best;
            }
        }

        for (int z = size - 1; z >= 0; z--) {
            for (int x = size - 1; x >= 0; x--) {
                int index =
                        z * size + x;

                double best =
                        distance[index];

                if (x + 1 < size) {
                    best =
                            Math.min(
                                    best,
                                    distance[index + 1] + 1.0
                            );
                }

                if (z + 1 < size) {
                    best =
                            Math.min(
                                    best,
                                    distance[index + size] + 1.0
                            );

                    if (x + 1 < size) {
                        best =
                                Math.min(
                                        best,
                                        distance[index + size + 1]
                                                + diagonalCost
                                );
                    }

                    if (x > 0) {
                        best =
                                Math.min(
                                        best,
                                        distance[index + size - 1]
                                                + diagonalCost
                                );
                    }
                }

                distance[index] =
                        best;
            }
        }

        double[] signed =
                new double[cellCount];

        for (int z = 0; z < size; z++) {
            /*
             * Use the local path only to determine which side of the range a
             * point occupies. The magnitude comes entirely from the 2-D
             * distance transform and therefore follows bends and termini.
             */
            double localBarrierX =
                    barrierX[z];

            for (int x = 0; x < size; x++) {
                int index =
                        z * size + x;

                double sign =
                        x >= localBarrierX
                                ? 1.0
                                : -1.0;

                signed[index] =
                        distance[index]
                                * sign;
            }
        }

        return signed;
    }

    private static double barrierLeeSource(
            WorldBlueprint world,
            int x,
            int z,
            double[] signedBarrierDistance,
            double[] barrierX,
            double[] barrierStrength,
            double worldSizeBlocks
    ) {
        int size = world.resolution();
        int index = z * size + x;

        if (world.landMask(x, z) < 0.5f) {
            return 0.0;
        }

        double signedDistance =
                signedBarrierDistance[index];

        /*
         * Estimate how coastal this section of the barrier is. This controls
         * only the width of the atmospheric transition, not the final shadow
         * strength, so coastal mountain termini blend without becoming wetter
         * or drier merely because they touch the ocean.
         */
        int barrierCellX =
                clampInt(
                        (int) Math.round(
                                barrierX[z]
                        ),
                        0,
                        size - 1
                );

        double barrierCoastDistance =
                Math.max(
                        0.0,
                        world.coastDistance(
                                barrierCellX,
                                z
                        )
                );

        double coastalBarrier =
                1.0
                        - smoothstep(
                        worldSizeBlocks * 0.010,
                        worldSizeBlocks * 0.075,
                        barrierCoastDistance
                );

        /*
         * Distance is measured in blueprint cells. Negative values are
         * windward; positive values are leeward. Because magnitude comes from
         * a 2-D distance transform, contours wrap naturally around curved range
         * ends instead of forming a vertical x = barrierX seam.
         */
        double windwardWidth =
                size
                        * (
                        0.010
                                + 0.024 * coastalBarrier
                );

        double leewardWidth =
                size
                        * (
                        0.025
                                + 0.040 * coastalBarrier
                );

        double onset =
                smoothstep(
                        -windwardWidth,
                        leewardWidth,
                        signedDistance
                );

        /*
         * The source is strongest near the newly crossed barrier and relaxes
         * into the advected regional shadow farther east.
         */
        double sourceDecay =
                1.0
                        - smoothstep(
                        size * 0.035,
                        size * 0.125,
                        Math.max(
                                0.0,
                                signedDistance
                        )
                );

        double source =
                barrierStrength[z]
                        * onset
                        * (
                        0.62
                                + 0.38 * sourceDecay
                );

        return clamp01(source);
    }

    private static double barrierCrossedShadow(
            double[] shadow,
            int size,
            int x,
            int z
    ) {
        if (x <= 0) {
            return 0.0;
        }

        double total = 0.0;
        double weight = 0.0;

        int[] offsets = {-7, -5, -3, -2, -1, 0, 1, 2, 3, 5, 7};
        double[] weights = {0.020, 0.035, 0.055, 0.075, 0.100, 0.150, 0.135, 0.105, 0.075, 0.040, 0.025};

        for (int i = 0; i < offsets.length; i++) {
            int sampleZ = z + offsets[i];
            if (sampleZ < 0 || sampleZ >= size) {
                continue;
            }

            total += shadow[sampleZ * size + (x - 1)] * weights[i];
            weight += weights[i];
        }

        if (x > 1) {
            total += shadow[z * size + (x - 2)] * 0.11;
            weight += 0.11;
        }

        if (x > 3) {
            total += shadow[z * size + (x - 4)] * 0.055;
            weight += 0.055;
        }

        return weight > 1.0e-9 ? total / weight : 0.0;
    }

    private static void applyFarLeeRecovery(
            WorldBlueprint world,
            double[] shadow,
            double[] signedBarrierDistance
    ) {
        int size =
                world.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index =
                        z * size + x;

                if (world.landMask(x, z) < 0.5f) {
                    shadow[index] =
                            0.0;
                    continue;
                }

                double leeDistance =
                        Math.max(
                                0.0,
                                signedBarrierDistance[index]
                        );

                /*
                 * Preserve the strong near-lee shadow, then allow roughly 30%
                 * recovery across the far eastern interior. Signed distance
                 * keeps this recovery aligned to the actual curved barrier.
                 */
                double recovery =
                        smoothstep(
                                size * 0.18,
                                size * 0.48,
                                leeDistance
                        );

                shadow[index] =
                        clamp01(
                                shadow[index]
                                        * (
                                        1.0
                                                - 0.30 * recovery
                                )
                        );
            }
        }
    }

    private static double coastalOrographicBlend(
            WorldBlueprint world,
            int x,
            int z,
            double[] signedBarrierDistance,
            double worldSizeBlocks
    ) {
        int size =
                world.resolution();

        int index =
                z * size + x;

        /*
         * This blend is only for windward/coastal orographic spikes. Once the
         * air is clearly leeward, the rain-shadow model owns the transition.
         */
        if (signedBarrierDistance[index] > size * 0.020) {
            return 1.0;
        }

        double coastDistance =
                Math.max(
                        0.0,
                        world.coastDistance(x, z)
                );

        return smoothstep(
                worldSizeBlocks * 0.003,
                worldSizeBlocks * 0.040,
                coastDistance
        );
    }

    private static void smoothSeries(double[] values, int passes, double blend) {
        if (values.length < 2 || passes <= 0) {
            return;
        }

        double[] scratch = new double[values.length];

        for (int pass = 0; pass < passes; pass++) {
            for (int i = 0; i < values.length; i++) {
                int previous = Math.max(0, i - 1);
                int next = Math.min(values.length - 1, i + 1);
                double neighborhood = (values[previous] + values[i] + values[next]) / 3.0;
                scratch[i] = lerp(values[i], neighborhood, blend);
            }

            System.arraycopy(scratch, 0, values, 0, values.length);
        }
    }

    private static void smoothField(
            WorldBlueprint world,
            double[] field,
            int passes,
            double blend,
            boolean oceanZero
    ) {
        int size = world.resolution();
        if (size < 2 || passes <= 0) {
            return;
        }

        double[] scratch = new double[field.length];

        for (int pass = 0; pass < passes; pass++) {
            for (int z = 0; z < size; z++) {
                int north = Math.max(0, z - 1);
                int south = Math.min(size - 1, z + 1);

                for (int x = 0; x < size; x++) {
                    int index = z * size + x;

                    if (oceanZero && world.landMask(x, z) < 0.5f) {
                        scratch[index] = 0.0;
                        continue;
                    }

                    int west = Math.max(0, x - 1);
                    int east = Math.min(size - 1, x + 1);

                    double cardinal = 0.25 * (
                            field[z * size + west]
                                    + field[z * size + east]
                                    + field[north * size + x]
                                    + field[south * size + x]
                    );

                    double diagonal = 0.25 * (
                            field[north * size + west]
                                    + field[north * size + east]
                                    + field[south * size + west]
                                    + field[south * size + east]
                    );

                    double neighborhood = cardinal * 0.64 + diagonal * 0.36;
                    scratch[index] = clamp01(lerp(field[index], neighborhood, blend));
                }
            }

            System.arraycopy(scratch, 0, field, 0, field.length);
        }
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0 : 0.0;
        }

        double t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp01(t);
    }

    private static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
