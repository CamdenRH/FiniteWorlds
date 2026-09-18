package dev.lemma.finiteworlds.core.generator;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;

import dev.lemma.finiteworlds.core.geography.CascadeMorphologySample;
import dev.lemma.finiteworlds.core.geography.CascadeMorphologySampler;
import dev.lemma.finiteworlds.core.geography.CoastDistanceField;
import dev.lemma.finiteworlds.core.geography.ContinentPlan;
import dev.lemma.finiteworlds.core.geography.ContinentPlanner;
import dev.lemma.finiteworlds.core.geography.ContinentSampler;
import dev.lemma.finiteworlds.core.geography.PhysiographySample;
import dev.lemma.finiteworlds.core.geography.PhysiographySampler;

import dev.lemma.finiteworlds.core.noise.ValueNoise;


public final class CascadiaGenerator {

    public ContinentPlan createPlan(
            long seed
    ) {

        return ContinentPlanner.generate(
                seed
        );
    }


    public WorldBlueprint generate(
            long seed,
            WorldConfig config
    ) {

        WorldBlueprint blueprint =
                new WorldBlueprint(
                        config
                );

        int size =
                config.blueprintResolution();


        /*
         * =====================================================
         * PHASE 1:
         * BUILD GLOBAL CONTINENT PLAN
         * =====================================================
         */

        ContinentPlan plan =
                createPlan(
                        seed
                );

        ContinentSampler continentSampler =
                new ContinentSampler(
                        seed,
                        plan
                );


        /*
         * =====================================================
         * PHASE 2:
         * GENERATE LAND MASK
         * =====================================================
         */

        for (
                int z = 0;
                z < size;
                z++
        ) {

            for (
                    int x = 0;
                    x < size;
                    x++
            ) {

                double nx =
                        normalizedCoordinate(
                                x,
                                size
                        );

                double nz =
                        normalizedCoordinate(
                                z,
                                size
                        );

                double landScore =
                        continentSampler.sample(
                                nx,
                                nz
                        );

                float landMask =
                        (float) smoothstep(
                                -0.018,
                                0.025,
                                landScore
                        );

                blueprint.setLandMask(
                        x,
                        z,
                        landMask
                );
            }
        }


        /*
         * =====================================================
         * PHASE 3:
         * SIGNED COAST DISTANCE
         * =====================================================
         */

        CoastDistanceField.populate(
                blueprint
        );


        /*
         * =====================================================
         * PHASE 4:
         * PHYSIOGRAPHY + MACRO ELEVATION
         * =====================================================
         */

        PhysiographySampler physiographySampler =
                new PhysiographySampler(
                        seed,
                        blueprint,
                        plan
                );

        CascadeMorphologySampler cascadeMorphologySampler =
                new CascadeMorphologySampler(
                        seed,
                        plan.cascadeMountainSystem(),
                        blueprint
                );


        ValueNoise baseElevationNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "physiography-base-elevation"
                        )
                );

        ValueNoise baseReliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "physiography-base-relief"
                        )
                );

        ValueNoise coastRangeReliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "coast-range-uplift"
                        )
                );

        ValueNoise coastRangeRidgeNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "coast-range-ridges"
                        )
                );

        ValueNoise plateauReliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "plateau-uplift"
                        )
                );

        ValueNoise oceanNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "ocean-relief"
                        )
                );


        for (
                int z = 0;
                z < size;
                z++
        ) {

            for (
                    int x = 0;
                    x < size;
                    x++
            ) {

                double nx =
                        normalizedCoordinate(
                                x,
                                size
                        );

                double nz =
                        normalizedCoordinate(
                                z,
                                size
                        );


                PhysiographySample physiography =
                        physiographySampler.sample(
                                x,
                                z,
                                nx,
                                nz
                        );


                blueprint.setTerrainProvince(
                        x,
                        z,
                        physiography.province()
                );


                generateElevation(
                        blueprint,
                        config,
                        plan,
                        physiography,
                        x,
                        z,
                        nx,
                        nz,
                        baseElevationNoise,
                        baseReliefNoise,
                        coastRangeReliefNoise,
                        coastRangeRidgeNoise,
                        plateauReliefNoise,
                        oceanNoise,
                        cascadeMorphologySampler
                );
            }
        }


        return blueprint;
    }


    private void generateElevation(
            WorldBlueprint blueprint,
            WorldConfig config,
            ContinentPlan plan,
            PhysiographySample physiography,
            int x,
            int z,
            double nx,
            double nz,
            ValueNoise baseElevationNoise,
            ValueNoise baseReliefNoise,
            ValueNoise coastRangeReliefNoise,
            ValueNoise coastRangeRidgeNoise,
            ValueNoise plateauReliefNoise,
            ValueNoise oceanNoise,
            CascadeMorphologySampler cascadeMorphologySampler
    ) {

        double coastDistance =
                blueprint.coastDistance(
                        x,
                        z
                );

        double seaLevel =
                config.seaLevel();

        double worldSize =
                config.worldSizeBlocks();

        double elevation;


        /*
         * =====================================================
         * LAND
         * =====================================================
         */

        if (coastDistance >= 0.0) {

            double baseElevation =
                    baseLandElevation(
                            config,
                            physiography,
                            coastDistance,
                            nx,
                            nz,
                            baseElevationNoise,
                            baseReliefNoise
                    );


            double coastRangeUplift =
                    coastRangeUplift(
                            physiography,
                            nx,
                            nz,
                            coastRangeReliefNoise,
                            coastRangeRidgeNoise
                    );


            double plateauUplift =
                    plateauUplift(
                            physiography,
                            nx,
                            nz,
                            plateauReliefNoise
                    );


            /*
             * Still suppress mountains immediately
             * against the shoreline.
             */

            double inlandMountainFade =
                    smoothstep(
                            150.0,
                            700.0,
                            coastDistance
                    );


            CascadeMorphologySample cascadeMorphology =
                    cascadeMorphologySampler.sample(
                            nx,
                            nz,
                            physiography.signedCascadeDistance(),
                            physiography.cascadeProgress(),
                            physiography.cascadeMask()
                    );

            double cascadeUplift =
                    cascadeMorphology.uplift()
                            * inlandMountainFade;


            blueprint.setBaseElevation(
                    x,
                    z,
                    (float) baseElevation
            );

            blueprint.setCoastRangeUplift(
                    x,
                    z,
                    (float) coastRangeUplift
            );

            blueprint.setCascadeUplift(
                    x,
                    z,
                    (float) cascadeUplift
            );

            blueprint.setPlateauUplift(
                    x,
                    z,
                    (float) plateauUplift
            );


            elevation =
                    baseElevation
                            + coastRangeUplift
                            + plateauUplift
                            + cascadeUplift;
        }


        /*
         * =====================================================
         * OCEAN
         * =====================================================
         */

        else {

            double offshoreDistance =
                    -coastDistance;


            double shelfWidth =
                    worldSize
                            * 0.025;


            double abyssStart =
                    worldSize
                            * 0.12;


            double depth;


            if (
                    offshoreDistance
                            <= shelfWidth
            ) {

                double t =
                        smoothstep(
                                0.0,
                                shelfWidth,
                                offshoreDistance
                        );

                depth =
                        lerp(
                                4.0,
                                34.0,
                                t
                        );

            } else {

                double t =
                        smoothstep(
                                shelfWidth,
                                abyssStart,
                                offshoreDistance
                        );

                depth =
                        lerp(
                                34.0,
                                215.0,
                                t
                        );
            }


            double oceanRelief =
                    oceanNoise.fbm(
                            nx * 5.0,
                            nz * 5.0,
                            5,
                            2.05,
                            0.52
                    );


            double reliefStrength =
                    lerp(
                            2.0,
                            12.0,
                            smoothstep(
                                    0.0,
                                    abyssStart,
                                    offshoreDistance
                            )
                    );


            elevation =
                    seaLevel
                            - depth
                            + oceanRelief
                            * reliefStrength;


            elevation =
                    Math.max(
                            -220.0,
                            elevation
                    );


            blueprint.setBaseElevation(
                    x,
                    z,
                    (float) elevation
            );

            blueprint.setCoastRangeUplift(
                    x,
                    z,
                    0.0f
            );

            blueprint.setCascadeUplift(
                    x,
                    z,
                    0.0f
            );

            blueprint.setPlateauUplift(
                    x,
                    z,
                    0.0f
            );
        }


        blueprint.setElevation(
                x,
                z,
                (float) elevation
        );
    }


    /*
     * =========================================================
     * BASE LAND ELEVATION
     * =========================================================
     */

    private double baseLandElevation(
            WorldConfig config,
            PhysiographySample physiography,
            double coastDistance,
            double nx,
            double nz,
            ValueNoise baseElevationNoise,
            ValueNoise baseReliefNoise
    ) {

        double seaLevel =
                config.seaLevel();

        double worldSize =
                config.worldSizeBlocks();


        double broadElevation =
                baseElevationNoise.fbm(
                        nx * 1.20,
                        nz * 1.20,
                        4,
                        2.0,
                        0.5
                );


        double regionalRelief =
                baseReliefNoise.fbm(
                        nx * 3.6,
                        nz * 3.6,
                        4,
                        2.05,
                        0.52
                );


        double inlandBase =
                seaLevel
                        + 36.0
                        + broadElevation
                        * 20.0
                        + regionalRelief
                        * 10.0;


        /*
         * Western lowlands stay comparatively low.
         */

        inlandBase +=
                physiography
                        .westernLowlandMask()
                        * 5.0;


        /*
         * Gentle regional ramps toward the Cascades.
         */

        inlandBase +=
                physiography
                        .cascadeFoothillMask()
                        * 22.0;

        inlandBase +=
                physiography
                        .easternSlopeMask()
                        * 28.0;


        /*
         * Preserve low, usable coastal terrain.
         */

        double coastalTransition =
                worldSize
                        * 0.032;


        double coastBlend =
                smoothstep(
                        0.0,
                        coastalTransition,
                        coastDistance
                );


        double coastalElevation =
                seaLevel
                        + 1.0
                        + smoothstep(
                        0.0,
                        coastalTransition
                                * 0.50,
                        coastDistance
                ) * 20.0;


        double result =
                lerp(
                        coastalElevation,
                        inlandBase,
                        coastBlend
                );


        return Math.max(
                seaLevel + 1.0,
                result
        );
    }


    /*
     * =========================================================
     * COAST RANGE
     * =========================================================
     */

    private double coastRangeUplift(
            PhysiographySample physiography,
            double nx,
            double nz,
            ValueNoise reliefNoise,
            ValueNoise ridgeNoise
    ) {

        double mask =
                physiography
                        .coastRangeMask();


        if (mask <= 0.0) {
            return 0.0;
        }


        double regional =
                reliefNoise.fbm(
                        nx * 5.0,
                        nz * 5.0,
                        4,
                        2.0,
                        0.5
                );


        double ridgeBase =
                ridgeNoise.fbm(
                        nx * 11.0,
                        nz * 8.0,
                        4,
                        2.05,
                        0.52
                );


        double ridge =
                1.0
                        - Math.abs(
                        ridgeBase
                );

        ridge *=
                ridge;


        double uplift =
                48.0
                        + ridge
                        * 62.0
                        + regional
                        * 14.0;


        return Math.max(
                0.0,
                uplift
                        * mask
        );
    }


    /*
     * =========================================================
     * EASTERN PLATEAU
     * =========================================================
     */

    private double plateauUplift(
            PhysiographySample physiography,
            double nx,
            double nz,
            ValueNoise plateauNoise
    ) {

        double mask =
                physiography
                        .plateauMask();


        if (mask <= 0.0) {
            return 0.0;
        }


        double regional =
                plateauNoise.fbm(
                        nx * 3.0,
                        nz * 3.0,
                        4,
                        2.0,
                        0.5
                );


        double uplift =
                52.0
                        + regional
                        * 20.0;


        return Math.max(
                0.0,
                uplift
                        * mask
        );
    }


    private static double normalizedCoordinate(
            int cell,
            int size
    ) {

        return (
                (cell + 0.5)
                        / size
        ) * 2.0
                - 1.0;
    }


    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {

        double t =
                (
                        value - edge0
                )
                        / (
                        edge1 - edge0
                );

        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );

        return t
                * t
                * (
                3.0 - 2.0 * t
        );
    }


    private static double lerp(
            double a,
            double b,
            double t
    ) {

        return a
                + (
                b - a
        ) * t;
    }
}