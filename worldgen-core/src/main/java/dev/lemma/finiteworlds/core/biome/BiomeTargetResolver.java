package dev.lemma.finiteworlds.core.biome;

/**
 * Pass 3G registry mapping.
 *
 * The semantic layer never depends on a mod. Terralith is treated as a richer
 * preferred palette when available, with a complete vanilla fallback for every
 * intent.
 *
 * Terralith IDs used here are from the 2.5.8 biome palette. The resolver is
 * intentionally centralized so later optional biome-mod adapters or
 * FiniteWorlds custom biomes can be inserted without rewriting terrain logic.
 */
public final class BiomeTargetResolver {

    private BiomeTargetResolver() {
    }

    public static BiomeTarget target(
            BiomeIntent intent
    ) {
        return switch (intent) {
            case OCEAN ->
                    vanilla(
                            "minecraft:ocean"
                    );

            case RIVER ->
                    vanilla(
                            "minecraft:river"
                    );

            case ESTUARY ->
                    vanilla(
                            "minecraft:river"
                    );

            case SANDY_BEACH ->
                    vanilla(
                            "minecraft:beach"
                    );

            case GRAVEL_BEACH ->
                    terralith(
                            "terralith:gravel_beach",
                            "minecraft:stony_shore"
                    );

            case COLD_BEACH ->
                    vanilla(
                            "minecraft:snowy_beach"
                    );

            case ROCKY_COAST ->
                    terralith(
                            "terralith:granite_cliffs",
                            "minecraft:stony_shore"
                    );

            case PERMANENT_SNOWFIELD ->
                    terralith(
                            "terralith:glacial_chasm",
                            "minecraft:frozen_peaks"
                    );

            case FROZEN_CLIFFS ->
                    terralith(
                            "terralith:frozen_cliffs",
                            "minecraft:jagged_peaks"
                    );

            case ALPINE_HIGHLANDS ->
                    terralith(
                            "terralith:alpine_highlands",
                            "minecraft:snowy_slopes"
                    );

            case ROCKY_ALPINE ->
                    terralith(
                            "terralith:rocky_mountains",
                            "minecraft:jagged_peaks"
                    );

            case SUBALPINE_GROVE ->
                    terralith(
                            "terralith:alpine_grove",
                            "minecraft:snowy_taiga"
                    );

            case MONTANE_FOREST ->
                    terralith(
                            "terralith:forested_highlands",
                            "minecraft:old_growth_pine_taiga"
                    );

            case WET_HIGHLAND_FOREST ->
                    terralith(
                            "terralith:haze_mountain",
                            "minecraft:old_growth_spruce_taiga"
                    );

            case TEMPERATE_RAINFOREST ->
                    terralith(
                            "terralith:cloud_forest",
                            "minecraft:old_growth_spruce_taiga"
                    );

            case TEMPERATE_FOREST ->
                    vanilla(
                            "minecraft:forest"
                    );

            case DRY_FOREST ->
                    vanilla(
                            "minecraft:taiga"
                    );

            case SHRUB_STEPPE ->
                    terralith(
                            "terralith:steppe",
                            "minecraft:plains"
                    );

            case COLD_STEPPE ->
                    terralith(
                            "terralith:cold_shrubland",
                            "minecraft:snowy_plains"
                    );
        };
    }

    /** Natural regional variants; fantasy and tropical palettes are intentionally excluded. */
    public static java.util.List<String> variants(BiomeIntent intent) {
        return switch(intent) {
            case TEMPERATE_FOREST -> java.util.List.of("terralith:temperate_highlands", "terralith:valley_clearing",
                "terralith:highlands", "minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest");
            case MONTANE_FOREST -> java.util.List.of("terralith:forested_highlands", "terralith:shield",
                "terralith:birch_taiga", "terralith:yosemite_lowlands");
            case WET_HIGHLAND_FOREST, TEMPERATE_RAINFOREST -> java.util.List.of("terralith:cloud_forest",
                "terralith:haze_mountain", "terralith:lush_valley");
            case DRY_FOREST -> java.util.List.of("terralith:highlands", "terralith:brushland", "terralith:shrubland");
            case SHRUB_STEPPE -> java.util.List.of("terralith:steppe", "terralith:rocky_shrubland",
                "minecraft:plains", "minecraft:meadow");
            case COLD_STEPPE -> java.util.List.of("terralith:cold_shrubland", "terralith:wintry_lowlands");
            case ALPINE_HIGHLANDS -> java.util.List.of("terralith:alpine_highlands", "terralith:temperate_highlands");
            case ROCKY_ALPINE -> java.util.List.of("terralith:rocky_mountains", "terralith:yosemite_cliffs",
                "terralith:stony_spires");
            case SUBALPINE_GROVE -> java.util.List.of("terralith:alpine_grove", "terralith:wintry_forest",
                "terralith:snowy_shield");
            default -> java.util.List.of(target(intent).preferredId());
        };
    }

    private static BiomeTarget vanilla(
            String id
    ) {
        return new BiomeTarget(
                id,
                id
        );
    }

    private static BiomeTarget terralith(
            String preferred,
            String fallback
    ) {
        return new BiomeTarget(
                preferred,
                fallback
        );
    }
}
