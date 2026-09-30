package dev.lemma.finiteworlds.core.biome;

/**
 * Semantic biome intents produced by Climate Pass 3G.
 *
 * These are deliberately registry-agnostic. The terrain/climate model decides
 * the ecological + geomorphic intent first; a resolver then chooses the best
 * available Minecraft / Terralith biome ID.
 */
public enum BiomeIntent {
    OCEAN,

    RIVER,
    ESTUARY,

    SANDY_BEACH,
    GRAVEL_BEACH,
    COLD_BEACH,
    ROCKY_COAST,

    PERMANENT_SNOWFIELD,
    FROZEN_CLIFFS,
    ALPINE_HIGHLANDS,
    ROCKY_ALPINE,

    SUBALPINE_GROVE,
    MONTANE_FOREST,
    WET_HIGHLAND_FOREST,

    TEMPERATE_RAINFOREST,
    TEMPERATE_FOREST,
    DRY_FOREST,

    SHRUB_STEPPE,
    COLD_STEPPE;

    public static BiomeIntent fromOrdinal(
            int ordinal
    ) {
        BiomeIntent[] values =
                values();

        if (
                ordinal < 0
                        || ordinal >= values.length
        ) {
            return OCEAN;
        }

        return values[ordinal];
    }
}
