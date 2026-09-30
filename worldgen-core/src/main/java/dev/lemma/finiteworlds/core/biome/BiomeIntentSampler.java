package dev.lemma.finiteworlds.core.biome;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

/** Samples categorical membership fields with coherent, seed-stable boundaries. */
public final class BiomeIntentSampler {
    private final BiomeIntentPlan plan;
    private final BiomeIntentPlan landPlan;
    private final WorldConfig config;
    private final ValueNoise boundaryX;
    private final ValueNoise boundaryZ;
    private final ValueNoise snowlineNoise;

    public BiomeIntentSampler(BiomeIntentPlan plan, WorldConfig config) {
        this(plan, config, 0L);
    }

    public BiomeIntentSampler(BiomeIntentPlan plan, WorldConfig config, long seed) {
        if (plan.resolution() != config.blueprintResolution()) {
            throw new IllegalArgumentException("Biome plan and world resolution must match");
        }
        this.plan = plan;
        this.landPlan = withoutRiverStamps(plan);
        this.config = config;
        this.boundaryX = new ValueNoise(SeedUtil.derive(seed, "biome_boundary_x"));
        this.boundaryZ = new ValueNoise(SeedUtil.derive(seed, "biome_boundary_z"));
        this.snowlineNoise = new ValueNoise(SeedUtil.derive(seed, "biome_snowline"));
    }

    public BiomeIntent intentAtBlock(double blockX, double blockZ) {
        return sample(plan, blockX, blockZ);
    }

    /** Physical river footprints replace the coarse hydrology cells at runtime. */
    public BiomeIntent landIntentAtBlock(double blockX, double blockZ) {
        return sample(landPlan, blockX, blockZ);
    }

    private BiomeIntent sample(BiomeIntentPlan sampledPlan, double blockX, double blockZ) {
        double halfWorld = config.worldSizeBlocks() / 2.0;
        double u = (blockX + halfWorld) / config.worldSizeBlocks();
        double v = (blockZ + halfWorld) / config.worldSizeBlocks();
        if (u < 0.0 || u > 1.0 || v < 0.0 || v > 1.0) {
            return BiomeIntent.OCEAN;
        }
        double px = u * (plan.resolution() - 1);
        double pz = v * (plan.resolution() - 1);
        // Smooth displacement breaks the blueprint's axis-aligned cell seams.
        // Below 0.29 cells, each original sample still has majority membership,
        // preserving even narrow river/coastal pockets. There is no per-block
        // randomness and no interpolation of enum ordinals.
        double warpedX = clampGrid(px + 0.26 * boundaryX.fbm(
                px / 2.4, pz / 2.4, 2, 2.0, 0.35));
        double warpedZ = clampGrid(pz + 0.26 * boundaryZ.fbm(
                px / 2.4, pz / 2.4, 2, 2.0, 0.35));
        int x0 = (int) Math.floor(warpedX);
        int z0 = (int) Math.floor(warpedZ);
        int x1 = Math.min(x0 + 1, plan.resolution() - 1);
        int z1 = Math.min(z0 + 1, plan.resolution() - 1);
        double tx = warpedX - x0;
        double tz = warpedZ - z0;
        BiomeIntent a = sampledPlan.intent(x0, z0);
        BiomeIntent b = sampledPlan.intent(x1, z0);
        BiomeIntent c = sampledPlan.intent(x0, z1);
        BiomeIntent d = sampledPlan.intent(x1, z1);
        double wa = (1.0 - tx) * (1.0 - tz);
        double wb = tx * (1.0 - tz);
        double wc = (1.0 - tx) * tz;
        double wd = tx * tz;

        // Interpolate a membership field per represented class. Shared classes
        // combine across corners, rounding single-cell pockets into contours.
        BiomeIntent best = a;
        double bestWeight = weight(a, a, b, c, d, wa, wb, wc, wd);
        double candidate = weight(b, a, b, c, d, wa, wb, wc, wd);
        if (candidate > bestWeight) {
            best = b;
            bestWeight = candidate;
        }
        candidate = weight(c, a, b, c, d, wa, wb, wc, wd);
        if (candidate > bestWeight) {
            best = c;
            bestWeight = candidate;
        }
        candidate = weight(d, a, b, c, d, wa, wb, wc, wd);
        return candidate > bestWeight ? d : best;
    }

    /** Seasonal snow begins near Y=624, varying with latitude and broad local weather. */
    public double snowlineAtBlock(double blockX, double blockZ) {
        double latitude = Math.max(-1.0, Math.min(1.0,
                blockZ / (config.worldSizeBlocks() / 2.0)));
        return config.seaLevel() + 560.0 + 30.0 * latitude + 50.0 * snowlineNoise.fbm(
                blockX / 480.0, blockZ / 480.0, 2, 2.0, 0.35);
    }

    public double permanentSnowlineAtBlock(double blockX, double blockZ) {
        return snowlineAtBlock(blockX, blockZ) + 375.0;
    }

    private double clampGrid(double value) {
        return Math.max(0.0, Math.min(plan.resolution() - 1, value));
    }

    private static BiomeIntentPlan withoutRiverStamps(BiomeIntentPlan source) {
        int size = source.resolution();
        int length = size * size;
        byte[] land = new byte[length];
        int[] queue = new int[length];
        int tail = 0;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                BiomeIntent intent = source.intent(x, z);
                land[z * size + x] = river(intent) ? -1 : (byte) intent.ordinal();
            }
        }
        // Only shoreline donors around river cells need to enter the flood fill.
        // Existing climate classes remain untouched, and each river cell copies
        // the closest surrounding class in a deterministic cardinal traversal.
        for (int cell = 0; cell < length; cell++) {
            if (land[cell] == -1) {
                continue;
            }
            int x = cell % size;
            if ((x > 0 && land[cell - 1] == -1)
                    || (x + 1 < size && land[cell + 1] == -1)
                    || (cell >= size && land[cell - size] == -1)
                    || (cell + size < length && land[cell + size] == -1)) {
                queue[tail++] = cell;
            }
        }
        int head = 0;
        while (head < tail) {
            int cell = queue[head++];
            int x = cell % size;
            if (x > 0 && land[cell - 1] == -1) {
                land[cell - 1] = land[cell];
                queue[tail++] = cell - 1;
            }
            if (x + 1 < size && land[cell + 1] == -1) {
                land[cell + 1] = land[cell];
                queue[tail++] = cell + 1;
            }
            if (cell >= size && land[cell - size] == -1) {
                land[cell - size] = land[cell];
                queue[tail++] = cell - size;
            }
            if (cell + size < length && land[cell + size] == -1) {
                land[cell + size] = land[cell];
                queue[tail++] = cell + size;
            }
        }
        int[] counts = new int[BiomeIntent.values().length];
        for (int cell = 0; cell < length; cell++) {
            // A synthetic all-river plan has no surrounding donor terrain.
            if (land[cell] == -1) {
                land[cell] = (byte) BiomeIntent.TEMPERATE_FOREST.ordinal();
            }
            counts[Byte.toUnsignedInt(land[cell])]++;
        }
        return new BiomeIntentPlan(size, land, counts);
    }

    private static boolean river(BiomeIntent intent) {
        return intent == BiomeIntent.RIVER || intent == BiomeIntent.ESTUARY;
    }

    private static double weight(BiomeIntent intent, BiomeIntent a, BiomeIntent b,
                                 BiomeIntent c, BiomeIntent d,
                                 double wa, double wb, double wc, double wd) {
        return (a == intent ? wa : 0.0) + (b == intent ? wb : 0.0)
                + (c == intent ? wc : 0.0) + (d == intent ? wd : 0.0);
    }
}
