package dev.lemma.finiteworlds.core.generator;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

public final class CascadiaGenerator {

    public WorldBlueprint generate(
            long seed,
            WorldConfig config
    ) {
        WorldBlueprint blueprint =
                new WorldBlueprint(config);

        int size =
                config.blueprintResolution();

        /*
         * Controls broad distortion of the
         * underlying continent shape.
         */
        ValueNoise warpNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "continent-warp"
                        )
                );

        /*
         * Controls irregularity along the coastline.
         */
        ValueNoise coastNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "coastline"
                        )
                );

        /*
         * Currently used primarily for ocean-floor
         * variation. This is deliberately separate
         * from inland elevation.
         */
        ValueNoise terrainNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "terrain"
                        )
                );

        /*
         * Controls satellite-island placement.
         */
        ValueNoise islandNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "islands"
                        )
                );

        /*
         * Broad inland elevation.
         *
         * IMPORTANT:
         * This is independent of the land/coast field.
         */
        ValueNoise elevationNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "macro-elevation"
                        )
                );

        /*
         * Medium-scale regional variation in inland
         * terrain elevation.
         */
        ValueNoise reliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "macro-relief"
                        )
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                /*
                 * Your current experimental range:
                 *
                 * -2 .. +2
                 *
                 * rather than the earlier -1 .. +1.
                 */
                double nx =
                        ((x + 0.5) / size)
                                * 4.0
                                - 2.0;

                double nz =
                        ((z + 0.5) / size)
                                * 4.0
                                - 2.0;

                generateCell(
                        blueprint,
                        x,
                        z,
                        nx,
                        nz,
                        warpNoise,
                        coastNoise,
                        terrainNoise,
                        islandNoise,
                        elevationNoise,
                        reliefNoise
                );
            }
        }

        return blueprint;
    }

    private void generateCell(
            WorldBlueprint blueprint,
            int x,
            int z,
            double nx,
            double nz,
            ValueNoise warpNoise,
            ValueNoise coastNoise,
            ValueNoise terrainNoise,
            ValueNoise islandNoise,
            ValueNoise elevationNoise,
            ValueNoise reliefNoise
    ) {

        /*
         * =====================================================
         * DOMAIN WARP
         * =====================================================
         *
         * Distorts coordinates before evaluating the
         * continent shape.
         *
         * This prevents the base landmass from looking
         * like a mathematically perfect ellipse.
         */

        double warpX =
                warpNoise.fbm(
                        nx * 1.8,
                        nz * 1.8,
                        4,
                        2.0,
                        0.5
                ) * 0.12;

        double warpZ =
                warpNoise.fbm(
                        nx * 1.8 + 91.7,
                        nz * 1.8 - 47.3,
                        4,
                        2.0,
                        0.5
                ) * 0.12;

        double wx =
                nx + warpX;

        double wz =
                nz + warpZ;


        /*
         * =====================================================
         * MAIN CONTINENT
         * =====================================================
         *
         * Slightly north/south elongated ellipse.
         *
         * These retain your current experimental values.
         */

        double continentX =
                wx / 0.6;

        double continentZ =
                wz / 0.7;

        double radius =
                Math.sqrt(
                        continentX * continentX
                                + continentZ * continentZ
                );

        double continent =
                1.0 - radius;


        /*
         * =====================================================
         * COASTLINE DISTORTION
         * =====================================================
         */

        double coast =
                coastNoise.fbm(
                        wx * 3.2,
                        wz * 3.2,
                        5,
                        2.05,
                        0.53
                );

        /*
         * Your current stronger coastline distortion.
         */
        continent +=
                coast * 0.6;


        /*
         * =====================================================
         * SATELLITE ISLAND REGION
         * =====================================================
         *
         * Concentrates additional islands around the
         * outside of the primary continent.
         */

        double ringDistance =
                Math.abs(
                        radius - 1.1
                );

        double islandEnvelope =
                Math.max(
                        0.0,
                        1.0
                                - ringDistance / 3.0
                );

        double islands =
                islandNoise.fbm(
                        nx * 8.0,
                        nz * 8.0,
                        4,
                        2.1,
                        0.55
                );

        double islandField =
                islands
                        * islandEnvelope
                        - 0.33;


        /*
         * Whichever land field is stronger wins:
         *
         * main continent
         * or
         * satellite island.
         */

        double landValue =
                Math.max(
                        continent,
                        islandField
                );


        /*
         * =====================================================
         * LAND MASK
         * =====================================================
         *
         * This field should answer:
         *
         * "Is this land or ocean?"
         *
         * It should NOT be responsible for inland
         * elevation anymore.
         */

        float landMask =
                (float) smoothstep(
                        -0.025,
                        0.06,
                        landValue
                );

        blueprint.setLandMask(
                x,
                z,
                landMask
        );


        /*
         * =====================================================
         * ELEVATION
         * =====================================================
         *
         * IMPORTANT CHANGE:
         *
         * landValue controls only the coastal transition.
         *
         * Inland elevation now comes from independent
         * elevation fields.
         *
         * This prevents elevation contours from simply
         * following the outline of the coast.
         */

        double elevation;

        if (landValue >= 0.0) {

            /*
             * ---------------------------------------------
             * BROAD CONTINENTAL UNDULATION
             * ---------------------------------------------
             *
             * Very large regional changes in elevation.
             *
             * This is intentionally LOW frequency.
             */

            double broadElevation =
                    elevationNoise.fbm(
                            nx * 1.35,
                            nz * 1.35,
                            4,
                            2.0,
                            0.5
                    );


            /*
             * ---------------------------------------------
             * REGIONAL RELIEF
             * ---------------------------------------------
             *
             * Adds smaller regional variation without
             * turning this into our eventual mountain
             * generator.
             */

            double regionalRelief =
                    reliefNoise.fbm(
                            nx * 4.0,
                            nz * 4.0,
                            4,
                            2.05,
                            0.52
                    );


            /*
             * ---------------------------------------------
             * INTERIOR ELEVATION
             * ---------------------------------------------
             *
             * Keep this deliberately moderate.
             *
             * Large mountain systems will eventually be
             * explicit geographic objects instead of
             * merely amplified noise.
             */

            double interiorElevation =
                    105.0
                            + broadElevation
                            * 35.0
                            + regionalRelief
                            * 16.0;


            /*
             * Avoid random inland depressions dropping
             * below sea level for now.
             *
             * Later, explicit basins and lakes can be
             * allowed to do that intentionally.
             */

            interiorElevation =
                    Math.max(
                            72.0,
                            interiorElevation
                    );


            /*
             * ---------------------------------------------
             * COASTAL TRANSITION
             * ---------------------------------------------
             *
             * landValue is still useful here because it
             * tells us how newly-emerged the terrain is.
             *
             * But its influence disappears quickly once
             * we move inland.
             */

            double coastBlend =
                    smoothstep(
                            0.0,
                            0.12,
                            landValue
                    );


            /*
             * Immediate coastal terrain starts just over
             * sea level and climbs gently.
             */

            double coastalElevation =
                    65.0
                            + smoothstep(
                            0.0,
                            0.10,
                            landValue
                    ) * 18.0;


            /*
             * Blend:
             *
             * coast-controlled elevation
             *        ↓
             * independent inland elevation
             */

            elevation =
                    lerp(
                            coastalElevation,
                            interiorElevation,
                            coastBlend
                    );

        } else {

            /*
             * =================================================
             * OCEAN BATHYMETRY
             * =================================================
             *
             * The ocean may continue using landValue for now.
             *
             * Eventually this will also become independent:
             *
             * continental shelf
             * continental slope
             * abyssal plain
             * ocean ridges
             * seamounts
             * trenches
             */

            double oceanDetail =
                    terrainNoise.fbm(
                            nx * 6.0,
                            nz * 6.0,
                            5,
                            2.0,
                            0.5
                    );

            elevation =
                    64.0
                            + landValue
                            * 360.0
                            + oceanDetail
                            * 15.0;

            elevation =
                    Math.max(
                            -180.0,
                            elevation
                    );
        }


        /*
         * Store final macro elevation in blueprint.
         */

        blueprint.setElevation(
                x,
                z,
                (float) elevation
        );
    }


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private static double smoothstep(
            double edge0,
            double edge1,
            double x
    ) {

        double t =
                (x - edge0)
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
                + (b - a)
                * t;
    }
}