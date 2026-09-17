package dev.lemma.finiteworlds.core.generator;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;

import dev.lemma.finiteworlds.core.geography.CoastDistanceField;
import dev.lemma.finiteworlds.core.geography.ContinentPlan;
import dev.lemma.finiteworlds.core.geography.ContinentPlanner;
import dev.lemma.finiteworlds.core.geography.ContinentSampler;

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
         * CALCULATE SIGNED COAST DISTANCE
         * =====================================================
         */

        CoastDistanceField.populate(
                blueprint
        );


        /*
         * =====================================================
         * PHASE 4:
         * GENERATE LAND / OCEAN ELEVATION
         * =====================================================
         */

        ValueNoise elevationNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "macro-elevation"
                        )
                );

        ValueNoise reliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "macro-relief"
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

                generateElevation(
                        blueprint,
                        config,
                        plan,
                        x,
                        z,
                        nx,
                        nz,
                        elevationNoise,
                        reliefNoise,
                        oceanNoise
                );
            }
        }

        return blueprint;
    }


    private void generateElevation(
            WorldBlueprint blueprint,
            WorldConfig config,
            ContinentPlan plan,
            int x,
            int z,
            double nx,
            double nz,
            ValueNoise elevationNoise,
            ValueNoise reliefNoise,
            ValueNoise oceanNoise
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

            double broadElevation =
                    elevationNoise.fbm(
                            nx * 1.30,
                            nz * 1.30,
                            4,
                            2.0,
                            0.5
                    );

            double regionalRelief =
                    reliefNoise.fbm(
                            nx * 4.0,
                            nz * 4.0,
                            4,
                            2.05,
                            0.52
                    );

            /*
             * Still deliberately modest.
             *
             * Explicit mountain systems come next.
             */
            double interiorElevation =
                    105.0
                            + broadElevation
                            * 34.0
                            + regionalRelief
                            * 15.0;

            interiorElevation =
                    Math.max(
                            seaLevel + 8.0,
                            interiorElevation
                    );


            /*
             * Width of coastal lowland.
             *
             * Scales with total world size so quick-test
             * worlds retain similar proportions.
             */
            double coastalTransition =
                    worldSize
                            * 0.035;

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
                                    * 0.45,
                            coastDistance
                    ) * 17.0;

            elevation =
                    lerp(
                            coastalElevation,
                            interiorElevation,
                            coastBlend
                    );

            /*
             * =================================================
             * CASCADE UPLIFT
             * =================================================
             */

            double mountainUplift =
                    plan.mountainSystem()
                            .upliftAt(
                                    nx,
                                    nz
                            );


            /*
             * Don't allow the mountain corridor to continue
             * straight into the ocean.
             *
             * Mountains fade in over the first ~500 blocks
             * inland.
             */

            double inlandMountainFade =
                    smoothstep(
                            150.0,
                            700.0,
                            coastDistance
                    );


            elevation +=
                    mountainUplift
                            * inlandMountainFade;
        }


        /*
         * =====================================================
         * OCEAN
         * =====================================================
         */

        else {

            double offshoreDistance =
                    -coastDistance;


            /*
             * Continental shelf width.
             */

            double shelfWidth =
                    worldSize
                            * 0.025;


            /*
             * End of continental slope and beginning
             * of deep-ocean basin.
             */

            double abyssStart =
                    worldSize
                            * 0.12;


            double depth;

            if (
                    offshoreDistance
                            <= shelfWidth
            ) {

                /*
                 * Shallow continental shelf.
                 */

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

                /*
                 * Continental slope into abyss.
                 */

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


            /*
             * Ocean floor variation.
             *
             * Kept deliberately small compared with
             * the bathymetric profile.
             */

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
        }


        blueprint.setElevation(
                x,
                z,
                (float) elevation
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
                (value - edge0)
                        / (edge1 - edge0);

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
                * (3.0 - 2.0 * t);
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