package dev.lemma.finiteworlds.core.climate;

/**
 * Broad climate/ecology classes produced by Climate Pass 3F.
 *
 * These are intentionally biome-agnostic. Pass 3G can map a single
 * bioclimatic class into multiple Minecraft biomes using terrain province,
 * elevation, slope, hydrology, geology, coast distance, and other local
 * context without duplicating the climate model.
 */
public enum BioclimaticRegion {
    OCEAN,
    PERMANENT_SNOW,
    ALPINE_TUNDRA,
    SUBALPINE_FOREST,
    MONTANE_FOREST,
    TEMPERATE_RAINFOREST,
    TEMPERATE_FOREST,
    DRY_FOREST,
    SHRUB_STEPPE,
    COLD_STEPPE;

    public static BioclimaticRegion fromOrdinal(
            int ordinal
    ) {
        BioclimaticRegion[] values =
                values();

        if (ordinal < 0 || ordinal >= values.length) {
            return OCEAN;
        }

        return values[ordinal];
    }
}
