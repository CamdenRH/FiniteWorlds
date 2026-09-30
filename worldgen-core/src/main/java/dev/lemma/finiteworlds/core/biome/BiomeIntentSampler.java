package dev.lemma.finiteworlds.core.biome;

import dev.lemma.finiteworlds.core.WorldConfig;

/** Samples the categorical blueprint without interpolating enum ordinals. */
public final class BiomeIntentSampler {
    private final BiomeIntentPlan plan;
    private final WorldConfig config;

    public BiomeIntentSampler(BiomeIntentPlan plan, WorldConfig config) {
        if (plan.resolution() != config.blueprintResolution()) {
            throw new IllegalArgumentException("Biome plan and world resolution must match");
        }
        this.plan = plan;
        this.config = config;
    }

    public BiomeIntent intentAtBlock(double blockX, double blockZ) {
        double halfWorld = config.worldSizeBlocks() / 2.0;
        double u = (blockX + halfWorld) / config.worldSizeBlocks();
        double v = (blockZ + halfWorld) / config.worldSizeBlocks();
        if (u < 0.0 || u > 1.0 || v < 0.0 || v > 1.0) {
            return BiomeIntent.OCEAN;
        }
        // Blueprint samples span both endpoints, matching WorldBlueprint's
        // elevation lookup. Nearest-cell sampling preserves the 3G classes.
        int x = (int) Math.round(u * (plan.resolution() - 1));
        int z = (int) Math.round(v * (plan.resolution() - 1));
        return plan.intent(x, z);
    }
}
