package dev.lemma.finiteworlds.core.climate;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * Climate Pass 3F: collapses continuous climate fields into broad Cascadia
 * bioclimatic regions.
 *
 * This is deliberately not Minecraft biome mapping. 3F describes climate and
 * vegetation potential only; Pass 3G can combine these classes with terrain
 * province, slope, coast distance, hydrology, geology, and local relief to
 * choose actual Minecraft biomes.
 *
 * Pass 3F.2 adds terrain elevation as a secondary control on ecological
 * zonation. Elevation is blended with temperature and snow storage rather than
 * used as a hard cutoff, preventing flat horizontal biome bands.
 * Pass 3F.3 strengthens the vertical ecological signal so the Cascades form a
 * broad montane -> subalpine -> alpine -> permanent-snow sequence while
 * preserving the horizontal west/east climate pattern established in 3F.2.
 * Pass 3F.4 makes the upper mountain belts more elevation-dominant after the
 * 3F.3 diagnostics showed that alpine and permanent-snow classes were still
 * confined to only the very highest individual cells.
 * Pass 3F.5 contains subalpine forest to actual cool, moisture-supported
 * mountain terrain so dry interior plateaus fall back to cold steppe instead
 * of becoming broad subalpine forest.
 */
public final class BioclimaticRegionPlanner {

    private BioclimaticRegionPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        ClimateGrid climate =
                world.climate();

        if (!climate.rainShadowRefinementPlanned()) {
            throw new IllegalStateException(
                    "Climate Pass 3F requires completed rain-shadow refinement"
            );
        }

        if (!climate.runoffDischargePlanned()) {
            throw new IllegalStateException(
                    "Climate Pass 3F requires completed runoff/discharge planning"
            );
        }

        int size =
                world.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    climate.setBioclimaticRegion(
                            x,
                            z,
                            BioclimaticRegion.OCEAN
                    );

                    continue;
                }

                double temperature =
                        climate.temperatureCelsius(
                                x,
                                z
                        );

                double elevation =
                        world.elevation(
                                x,
                                z
                        );

                double precipitation =
                        clamp01(
                                climate.refinedPrecipitation(
                                        x,
                                        z
                                )
                        );

                double moisture =
                        clamp01(
                                climate.refinedMoisture(
                                        x,
                                        z
                                )
                        );

                double snow =
                        clamp01(
                                climate.snowStorageFraction(
                                        x,
                                        z
                                )
                        );

                double rainShadow =
                        clamp01(
                                climate.rainShadowStrength(
                                        x,
                                        z
                                )
                        );

                double runoff =
                        clamp01(
                                climate.runoffPotential(
                                        x,
                                        z
                                )
                                / 0.30
                        );

                /*
                 * Moisture and precipitation describe atmospheric supply.
                 * Runoff supplies a weak hydrologic/ecologic correction so
                 * very wet mountain catchments do not classify like dry
                 * terrain merely because precipitation was locally depleted
                 * immediately after orographic rainout.
                 */
                double humidityIndex =
                        clamp01(
                                moisture * 0.46
                                        + precipitation * 0.42
                                        + runoff * 0.12
                        );

                double aridityIndex =
                        clamp01(
                                rainShadow * 0.58
                                        + (1.0 - humidityIndex) * 0.42
                        );

                BioclimaticRegion region =
                        classify(
                                elevation,
                                temperature,
                                precipitation,
                                humidityIndex,
                                snow,
                                aridityIndex
                        );

                climate.setBioclimaticRegion(
                        x,
                        z,
                        region
                );
            }
        }

        climate.finishBioclimaticRegions();
    }

    private static BioclimaticRegion classify(
            double elevation,
            double temperature,
            double precipitation,
            double humidity,
            double snow,
            double aridity
    ) {
        /*
         * 3F.2 treats vertical ecology as overlapping climate potentials rather
         * than hard elevation bands. Elevation helps identify where alpine
         * conditions are physically plausible, while temperature and snow
         * decide whether those conditions are actually realized.
         */
        double subalpineElevation =
                smoothstep(
                        95.0,
                        235.0,
                        elevation
                );

        double alpineElevation =
                smoothstep(
                        185.0,
                        355.0,
                        elevation
                );

        double permanentSnowElevation =
                smoothstep(
                        330.0,
                        520.0,
                        elevation
                );

        double coolness =
                1.0
                        - smoothstep(
                        4.0,
                        11.0,
                        temperature
                );

        double alpineCold =
                1.0
                        - smoothstep(
                        0.0,
                        6.0,
                        temperature
                );

        double severeCold =
                1.0
                        - smoothstep(
                        -4.0,
                        3.0,
                        temperature
                );

        double permanentSnowScore =
                clamp01(
                        snow * 0.30
                                + permanentSnowElevation * 0.50
                                + severeCold * 0.20
                );

        if (
                permanentSnowScore >= 0.49
                        || (
                        elevation >= 500.0
                                && temperature <= 5.5
                                && snow >= 0.10
                )
                        || (
                        elevation >= 620.0
                                && temperature <= 7.0
                )
                        || elevation >= 820.0
        ) {
            return BioclimaticRegion.PERMANENT_SNOW;
        }

        double alpineScore =
                clamp01(
                        snow * 0.20
                                + alpineElevation * 0.55
                                + alpineCold * 0.25
                );

        if (
                alpineScore >= 0.40
                        || (
                        elevation >= 290.0
                                && temperature <= 8.0
                )
                        || (
                        elevation >= 390.0
                                && temperature <= 9.5
                )
                        || elevation >= 540.0
        ) {
            return BioclimaticRegion.ALPINE_TUNDRA;
        }

        double subalpineScore =
                clamp01(
                        snow * 0.14
                                + subalpineElevation * 0.54
                                + coolness * 0.32
                );

        boolean subalpineCandidate =
                subalpineScore >= 0.40
                        || (
                        elevation >= 190.0
                                && temperature <= 9.0
                                && snow >= 0.06
                )
                        || (
                        elevation >= 260.0
                                && temperature <= 10.0
                                && humidity >= 0.24
                );

        if (subalpineCandidate) {
            /*
             * Elevation and coolness alone are not enough for subalpine
             * forest. The dry eastern plateaus should remain cold steppe unless
             * snow storage or atmospheric moisture supplies a credible forest
             * regime. Very high terrain gets a small allowance because local
             * snow persistence becomes increasingly important there.
             */
            boolean forestSupported =
                    humidity >= 0.28
                            || snow >= 0.16
                            || (
                            elevation >= 340.0
                                    && humidity >= 0.22
                                    && aridity <= 0.70
                    );

            return forestSupported
                    ? BioclimaticRegion.SUBALPINE_FOREST
                    : BioclimaticRegion.COLD_STEPPE;
        }

        /*
         * Montane forest occupies cool mid-elevation terrain beneath the
         * subalpine belt. Elevation is supportive rather than mandatory, so
         * cool maritime hills can still classify naturally.
         */
        double montanePotential =
                clamp01(
                        smoothstep(
                                70.0,
                                220.0,
                                elevation
                        )
                                * 0.52
                                + coolness * 0.48
                );

        if (
                montanePotential >= 0.39
                        && temperature <= 10.5
                        && humidity >= 0.24
                        && aridity < 0.74
        ) {
            return BioclimaticRegion.MONTANE_FOREST;
        }

        /*
         * Rainforest remains a low/mid-elevation hyper-humid maritime class.
         * 3F.2 relaxes the moisture thresholds enough for the west-side belt to
         * become continuous while still requiring weak rain-shadow influence.
         */
        if (
                humidity >= 0.50
                        && precipitation >= 0.12
                        && aridity <= 0.55
                        && elevation <= 360.0
        ) {
            return BioclimaticRegion.TEMPERATE_RAINFOREST;
        }

        if (
                humidity >= 0.42
                        && aridity <= 0.64
        ) {
            return BioclimaticRegion.TEMPERATE_FOREST;
        }

        if (
                humidity >= 0.27
                        && aridity <= 0.76
        ) {
            return BioclimaticRegion.DRY_FOREST;
        }

        if (
                temperature <= 8.0
                        || (
                        elevation >= 220.0
                                && aridity >= 0.68
                )
        ) {
            return BioclimaticRegion.COLD_STEPPE;
        }

        return BioclimaticRegion.SHRUB_STEPPE;
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
