package dev.lemma.finiteworlds.core.biome;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.climate.BioclimaticRegion;
import dev.lemma.finiteworlds.core.geography.CoastalMorphologyPlanner;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;

/**
 * Climate Pass 3G: converts the locked 3F bioclimatic field into semantic
 * Minecraft biome intents.
 *
 * 3G is deliberately terrain-aware:
 *
 *  - COAST2 morphology becomes sandy/gravel/rocky/cold shoreline intent,
 *  - active macro river channels become river/estuary intent,
 *  - mountain bioclimates split into forested, alpine, rocky, and frozen forms,
 *  - lowland forest/steppe classes preserve the locked 3F climate pattern.
 *
 * Actual registry IDs are selected separately by BiomeTargetResolver.
 *
 * 3G.1 consumes the exact COAST2 authored morphology field so beaches are
 * pocketed rather than inferred from coast distance alone. It also makes
 * alpine/snow ruggedness depend primarily on measured local grade instead of
 * automatically treating every Cascade-core cell as a cliff.
 *
 * 3G.2 tightens beach intent to the stronger cores of those authored pockets
 * and raises the frozen-cliff grade threshold so some gentler permanent-snow
 * terrain resolves as snowfield rather than nearly all becoming cliff.
 *
 * 3G.3 performs the final shoreline/snow split polish: explicit beach intent
 * is limited to the strongest COAST2 pocket cores, while frozen-cliff intent
 * is reserved for genuinely steep permanent-snow terrain.
 */
public final class BiomeIntentPlanner {

    private static final double SEA_LEVEL =
            64.0;

    private BiomeIntentPlanner() {
    }

    public static BiomeIntentPlan plan(
            WorldBlueprint world
    ) {
        if (
                !world.climate()
                        .bioclimaticRegionsPlanned()
        ) {
            throw new IllegalStateException(
                    "Climate Pass 3G requires completed 3F bioclimatic regions"
            );
        }

        int size =
                world.resolution();

        byte[] intents =
                new byte[
                        size * size
                        ];

        int[] counts =
                new int[
                        BiomeIntent.values().length
                        ];

        double worldSize =
                world.config()
                        .worldSizeBlocks();

        double blocksPerCell =
                world.config()
                        .blocksPerCell();

        double coastalBiomeWidth =
                worldSize * 0.0100;

        double estuaryWidth =
                worldSize * 0.0120;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                BiomeIntent intent =
                        classify(
                                world,
                                x,
                                z,
                                size,
                                blocksPerCell,
                                coastalBiomeWidth,
                                estuaryWidth
                        );

                intents[
                        index(
                                x,
                                z,
                                size
                        )
                        ] =
                        (byte) intent.ordinal();

                counts[
                        intent.ordinal()
                        ]++;
            }
        }

        return new BiomeIntentPlan(
                size,
                intents,
                counts
        );
    }

    private static BiomeIntent classify(
            WorldBlueprint world,
            int x,
            int z,
            int size,
            double blocksPerCell,
            double coastalBiomeWidth,
            double estuaryWidth
    ) {
        if (
                world.landMask(x, z)
                        < 0.5f
        ) {
            return BiomeIntent.OCEAN;
        }

        double elevation =
                world.elevation(
                        x,
                        z
                );

        double coastDistance =
                Math.max(
                        0.0,
                        world.coastDistance(
                                x,
                                z
                        )
                );

        double temperature =
                world.climate()
                        .temperatureCelsius(
                                x,
                                z
                        );

        double discharge =
                clamp01(
                        world.climate()
                                .normalizedEffectiveDischarge(
                                        x,
                                        z
                                )
                );

        double channel =
                clamp01(
                        world.hydrology()
                                .riverChannelStrength(
                                        x,
                                        z
                                )
                );

        double grade =
                localGrade(
                        world,
                        x,
                        z,
                        size,
                        blocksPerCell
                );

        TerrainProvince province =
                world.terrainProvince(
                        x,
                        z
                );

        BioclimaticRegion region =
                world.climate()
                        .bioclimaticRegion(
                                x,
                                z
                        );

        /*
         * =============================================================
         * HYDROLOGY OVERRIDES
         * =============================================================
         *
         * Strong authored channels get first right of refusal. Near the coast
         * the same channel becomes estuarine intent, which later 3G passes can
         * refine into mudflat/delta/custom biome treatment if desired.
         */
        boolean activeRiver =
                channel >= 0.22
                        || (
                        channel >= 0.10
                                && discharge >= 0.58
                );

        if (activeRiver) {
            if (
                    coastDistance <= estuaryWidth
                            && elevation <= SEA_LEVEL + 24.0
            ) {
                return BiomeIntent.ESTUARY;
            }

            return BiomeIntent.RIVER;
        }

        /*
         * =============================================================
         * COASTAL MORPHOLOGY
         * =============================================================
         *
         * COAST2 already decided where a coherent pocket beach was actually
         * authored. Consume that exact strength field rather than converting
         * every low cell near the ocean into a beach biome.
         */
        double beachStrength =
                CoastalMorphologyPlanner.beachStrength(
                        world,
                        x,
                        z
                );

        double headlandStrength =
                CoastalMorphologyPlanner.headlandStrength(
                        world,
                        x,
                        z
                );

        boolean rockyProvince =
                province == TerrainProvince.COAST_RANGE
                        || province == TerrainProvince.CASCADE_FOOTHILLS
                        || province == TerrainProvince.CASCADE_CORE;

        boolean authoredBeach =
                beachStrength >= 0.44
                        && coastDistance <= coastalBiomeWidth
                        && elevation <= SEA_LEVEL + 28.0;

        if (authoredBeach) {
            if (temperature <= 2.5) {
                return BiomeIntent.COLD_BEACH;
            }

            /*
             * Strong, very gentle pocket cores become sandy beaches. Weaker
             * margins and more mineral/rock-influenced coasts become gravel.
             */
            if (
                    beachStrength >= 0.48
                            && grade < 0.024
                            && !rockyProvince
            ) {
                return BiomeIntent.SANDY_BEACH;
            }

            return BiomeIntent.GRAVEL_BEACH;
        }

        boolean protectedRockyCoast =
                coastDistance <= coastalBiomeWidth
                        && elevation <= SEA_LEVEL + 64.0
                        && (
                        headlandStrength >= 0.44
                                || grade >= 0.095
                                || (
                                rockyProvince
                                        && headlandStrength >= 0.26
                        )
                );

        if (protectedRockyCoast) {
            return BiomeIntent.ROCKY_COAST;
        }

        /*
         * =============================================================
         * BIOCLIMATIC + TERRAIN RESOLUTION
         * =============================================================
         */
        return switch (region) {
            case OCEAN ->
                    BiomeIntent.OCEAN;

            case PERMANENT_SNOW -> {
                double frozenCliffThreshold =
                        province == TerrainProvince.CASCADE_CORE
                                ? 0.190
                                : 0.215;

                if (grade >= frozenCliffThreshold) {
                    yield BiomeIntent.FROZEN_CLIFFS;
                }

                yield BiomeIntent.PERMANENT_SNOWFIELD;
            }

            case ALPINE_TUNDRA -> {
                double rockyAlpineThreshold =
                        province == TerrainProvince.CASCADE_CORE
                                ? 0.095
                                : province == TerrainProvince.COAST_RANGE
                                ? 0.105
                                : 0.115;

                if (grade >= rockyAlpineThreshold) {
                    yield BiomeIntent.ROCKY_ALPINE;
                }

                yield BiomeIntent.ALPINE_HIGHLANDS;
            }

            case SUBALPINE_FOREST ->
                    BiomeIntent.SUBALPINE_GROVE;

            case MONTANE_FOREST -> {
                if (
                        grade >= 0.060
                                || province == TerrainProvince.CASCADE_CORE
                                || province == TerrainProvince.COAST_RANGE
                ) {
                    yield BiomeIntent.WET_HIGHLAND_FOREST;
                }

                yield BiomeIntent.MONTANE_FOREST;
            }

            case TEMPERATE_RAINFOREST -> {
                if (
                        grade >= 0.055
                                || province == TerrainProvince.COAST_RANGE
                ) {
                    yield BiomeIntent.WET_HIGHLAND_FOREST;
                }

                yield BiomeIntent.TEMPERATE_RAINFOREST;
            }

            case TEMPERATE_FOREST ->
                    BiomeIntent.TEMPERATE_FOREST;

            case DRY_FOREST ->
                    BiomeIntent.DRY_FOREST;

            case SHRUB_STEPPE ->
                    BiomeIntent.SHRUB_STEPPE;

            case COLD_STEPPE ->
                    BiomeIntent.COLD_STEPPE;
        };
    }

    private static double localGrade(
            WorldBlueprint world,
            int x,
            int z,
            int size,
            double blocksPerCell
    ) {
        int west =
                Math.max(
                        0,
                        x - 1
                );

        int east =
                Math.min(
                        size - 1,
                        x + 1
                );

        int north =
                Math.max(
                        0,
                        z - 1
                );

        int south =
                Math.min(
                        size - 1,
                        z + 1
                );

        double dx =
                (
                        world.elevation(east, z)
                                - world.elevation(west, z)
                )
                        / Math.max(
                        1.0,
                        (east - west)
                                * blocksPerCell
                );

        double dz =
                (
                        world.elevation(x, south)
                                - world.elevation(x, north)
                )
                        / Math.max(
                        1.0,
                        (south - north)
                                * blocksPerCell
                );

        return Math.hypot(
                dx,
                dz
        );
    }

    private static int index(
            int x,
            int z,
            int size
    ) {
        return z * size + x;
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
