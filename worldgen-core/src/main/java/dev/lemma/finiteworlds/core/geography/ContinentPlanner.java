package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class ContinentPlanner {

    private ContinentPlanner() {
    }

    public static ContinentPlan generate(
            long worldSeed
    ) {

        SplittableRandom random =
                new SplittableRandom(
                        SeedUtil.derive(
                                worldSeed,
                                "continent-plan"
                        )
                );

        List<GeoShape> land =
                new ArrayList<>();

        List<GeoShape> cutouts =
                new ArrayList<>();

        /*
         * =====================================================
         * MAINLAND
         * =====================================================
         */

        int coastlinePointCount =
                64;

        double baseRadiusX =
                range(
                        random,
                        0.40,
                        0.46
                );

        double baseRadiusZ =
                range(
                        random,
                        0.51,
                        0.58
                );

        double phase2 =
                random.nextDouble() * Math.PI * 2.0;
        double phase3 =
                random.nextDouble() * Math.PI * 2.0;
        double phase5 =
                random.nextDouble() * Math.PI * 2.0;
        double phase7 =
                random.nextDouble() * Math.PI * 2.0;
        double phase11 =
                random.nextDouble() * Math.PI * 2.0;
        double phase13 =
                random.nextDouble() * Math.PI * 2.0;

        double[] jitter =
                new double[coastlinePointCount];

        for (
                int i = 0;
                i < coastlinePointCount;
                i++
        ) {
            jitter[i] =
                    range(
                            random,
                            -0.070,
                            0.070
                    );
        }

        /*
         * Smooth twice so the coastline has shape variation
         * without sharp ugly spikes.
         */
        for (
                int pass = 0;
                pass < 2;
                pass++
        ) {
            double[] smoothed =
                    new double[coastlinePointCount];

            for (
                    int i = 0;
                    i < coastlinePointCount;
                    i++
            ) {

                double previous =
                        jitter[
                                mod(
                                        i - 1,
                                        coastlinePointCount
                                )
                                ];

                double current =
                        jitter[i];

                double next =
                        jitter[
                                mod(
                                        i + 1,
                                        coastlinePointCount
                                )
                                ];

                smoothed[i] =
                        previous * 0.24
                                + current * 0.52
                                + next * 0.24;
            }

            jitter =
                    smoothed;
        }

        /*
         * Broad coastline features:
         * peninsulas + bays + smaller localized variation.
         */
        List<CoastFeature> coastFeatures =
                new ArrayList<>();

        int peninsulaCount =
                3 + random.nextInt(3);

        for (
                int i = 0;
                i < peninsulaCount;
                i++
        ) {
            coastFeatures.add(
                    new CoastFeature(
                            random.nextDouble() * Math.PI * 2.0,
                            range(
                                    random,
                                    0.09,
                                    0.18
                            ),
                            range(
                                    random,
                                    0.14,
                                    0.28
                            )
                    )
            );
        }

        int bayCount =
                4 + random.nextInt(3);

        for (
                int i = 0;
                i < bayCount;
                i++
        ) {
            coastFeatures.add(
                    new CoastFeature(
                            random.nextDouble() * Math.PI * 2.0,
                            -range(
                                    random,
                                    0.08,
                                    0.16
                            ),
                            range(
                                    random,
                                    0.10,
                                    0.22
                            )
                    )
            );
        }

        int microFeatureCount =
                6 + random.nextInt(4);

        for (
                int i = 0;
                i < microFeatureCount;
                i++
        ) {

            double amplitude =
                    range(
                            random,
                            0.025,
                            0.055
                    );

            if (random.nextBoolean()) {
                amplitude = -amplitude;
            }

            coastFeatures.add(
                    new CoastFeature(
                            random.nextDouble() * Math.PI * 2.0,
                            amplitude,
                            range(
                                    random,
                                    0.045,
                                    0.095
                            )
                    )
            );
        }

        double[] mainlandRadius =
                new double[coastlinePointCount];

        for (
                int i = 0;
                i < coastlinePointCount;
                i++
        ) {

            double angle =
                    (Math.PI * 2.0 * i)
                            / coastlinePointCount;

            double broadShape =
                    Math.sin(
                            angle * 2.0 + phase2
                    ) * 0.080

                            + Math.sin(
                            angle * 3.0 + phase3
                    ) * 0.050

                            + Math.sin(
                            angle * 5.0 + phase5
                    ) * 0.032

                            + Math.sin(
                            angle * 7.0 + phase7
                    ) * 0.020

                            + Math.sin(
                            angle * 11.0 + phase11
                    ) * 0.010

                            + Math.sin(
                            angle * 13.0 + phase13
                    ) * 0.006;

            double featureDisplacement =
                    0.0;

            for (
                    CoastFeature feature :
                    coastFeatures
            ) {

                double angularDistance =
                        circularAngleDistance(
                                angle,
                                feature.angle()
                        );

                double normalized =
                        angularDistance
                                / feature.width();

                featureDisplacement +=
                        feature.amplitude()
                                * Math.exp(
                                -normalized
                                        * normalized
                                        * 2.0
                        );
            }

            double radialScale =
                    1.0
                            + broadShape
                            + jitter[i]
                            + featureDisplacement;

            radialScale =
                    clamp(
                            radialScale,
                            0.66,
                            1.38
                    );

            mainlandRadius[i] =
                    radialScale;
        }

        land.add(
                new RadialSplineShape(
                        0.0,
                        0.0,
                        baseRadiusX,
                        baseRadiusZ,
                        0.0,
                        mainlandRadius
                )
        );

        /*
         * =====================================================
         * WESTERN SOUND SYSTEM ONLY
         * =====================================================
         */

        double primarySoundAngle =
                Math.PI
                        + range(
                        random,
                        -0.42,
                        0.42
                );

        List<GeoPoint> primarySound =
                createSoundPath(
                        random,
                        primarySoundAngle,
                        0.64,
                        0.19,
                        9,
                        0.048
                );

        cutouts.add(
                new TaperedSplineShape(
                        primarySound,
                        12,
                        0.080,
                        0.013
                )
        );

        /*
         * Smaller, subtler branches than the last iteration.
         */
        /*
         * Main sound gets 1-3 modest branches.
         */
        int primaryBranchCount =
                1 + random.nextInt(3);

        addSoundBranches(
                cutouts,
                random,
                primarySound,
                primarySoundAngle,

                primaryBranchCount,

                /*
                 * Slightly wider than previous version.
                 */
                0.020,
                0.0075,

                /*
                 * Slightly longer, but still clearly
                 * subordinate to the main sound.
                 */
                0.040,
                0.075
        );

        int secondaryWesternSoundCount =
                1;

        for (
                int sound = 0;
                sound < secondaryWesternSoundCount;
                sound++
        ) {

            double angle =
                    Math.PI
                            + range(
                            random,
                            -0.95,
                            0.95
                    );

            if (
                    circularAngleDistance(
                            angle,
                            primarySoundAngle
                    ) < 0.28
            ) {
                angle +=
                        angle < primarySoundAngle
                                ? -0.38
                                : 0.38;
            }

            List<GeoPoint> path =
                    createSoundPath(
                            random,
                            angle,
                            range(
                                    random,
                                    0.58,
                                    0.64
                            ),
                            range(
                                    random,
                                    0.30,
                                    0.40
                            ),
                            6 + random.nextInt(2),
                            range(
                                    random,
                                    0.020,
                                    0.036
                            )
                    );

            cutouts.add(
                    new TaperedSplineShape(
                            path,
                            10,
                            range(
                                    random,
                                    0.040,
                                    0.054
                            ),
                            range(
                                    random,
                                    0.008,
                                    0.012
                            )
                    )
            );

            /*
             * Only some secondary sounds get one very small branch.
             */
            /*
             * Secondary sound may have no branches at all,
             * or up to two.
             */
            int secondaryBranchCount =
                    random.nextInt(3);

            if (secondaryBranchCount > 0) {

                addSoundBranches(
                        cutouts,
                        random,
                        path,
                        angle,

                        secondaryBranchCount,

                        0.014,
                        0.0055,

                        0.030,
                        0.055
                );
            }
        }

        /*
         * =====================================================
         * WESTERN INLET / ARCHIPELAGO ISLANDS
         * =====================================================
         *
         * Larger than the latest iteration, closer in spirit
         * to the previous one.
         */

        double westernClusterRadius =
                0.66;

        double westernClusterCenterX =
                Math.cos(
                        primarySoundAngle
                ) * westernClusterRadius;

        double westernClusterCenterZ =
                Math.sin(
                        primarySoundAngle
                ) * westernClusterRadius;

        double soundDirectionX =
                Math.cos(
                        primarySoundAngle
                );

        double soundDirectionZ =
                Math.sin(
                        primarySoundAngle
                );

        double soundPerpendicularX =
                -soundDirectionZ;

        double soundPerpendicularZ =
                soundDirectionX;

        int westernClusterCount =
                10 + random.nextInt(7);

        for (
                int i = 0;
                i < westernClusterCount;
                i++
        ) {

            double scatterAngle =
                    random.nextDouble() * Math.PI * 2.0;

            double scatterRadius =
                    Math.sqrt(
                            random.nextDouble()
                    );

            double alongCoast =
                    Math.cos(
                            scatterAngle
                    ) * scatterRadius * 0.135;

            double crossCoast =
                    Math.sin(
                            scatterAngle
                    ) * scatterRadius * 0.082;

            double islandX =
                    westernClusterCenterX
                            + soundPerpendicularX * alongCoast
                            + soundDirectionX * crossCoast;

            double islandZ =
                    westernClusterCenterZ
                            + soundPerpendicularZ * alongCoast
                            + soundDirectionZ * crossCoast;

            double size;

            /*
             * Several proper anchor islands, then progressively
             * smaller islands around them.
             */
            if (i < 4) {

                size =
                        range(
                                random,
                                0.030,
                                0.048
                        );

            } else if (i < 8) {

                size =
                        range(
                                random,
                                0.018,
                                0.032
                        );

            } else {

                size =
                        range(
                                random,
                                0.010,
                                0.020
                        );
            }

            land.add(
                    createOrganicIsland(
                            random,
                            islandX,
                            islandZ,
                            size * range(
                                    random,
                                    1.15,
                                    1.90
                            ),

                            size * range(
                                    random,
                                    0.68,
                                    1.15
                            )
                    )
            );
        }

        /*
         * One or two larger western offshore islands.
         */
        int largeWesternIslandCount =
                1 + random.nextInt(2);

        for (
                int i = 0;
                i < largeWesternIslandCount;
                i++
        ) {

            double angle =
                    Math.PI
                            + range(
                            random,
                            -0.90,
                            0.90
                    );

            if (
                    circularAngleDistance(
                            angle,
                            primarySoundAngle
                    ) < 0.30
            ) {
                angle +=
                        angle < primarySoundAngle
                                ? -0.42
                                : 0.42;
            }

            double radius =
                    range(
                            random,
                            0.72,
                            0.82
                    );

            double centerX =
                    Math.cos(angle) * radius;

            double centerZ =
                    Math.sin(angle) * radius;

            double size =
                    range(
                            random,
                            0.045,
                            0.072
                    );

            land.add(
                    createOrganicIsland(
                            random,
                            centerX,
                            centerZ,
                            size * range(
                                    random,
                                    1.25,
                                    1.80
                            ),
                            size * range(
                                    random,
                                    0.80,
                                    1.10
                            )
                    )
            );
        }

        /*
         * =====================================================
         * EASTERN SMALL ISLANDS ONLY
         * =====================================================
         *
         * No eastern sounds anymore.
         * Just a sparse little island group.
         */

        if (random.nextDouble() < 0.85) {

            double easternClusterAngle =
                    range(
                            random,
                            -0.55,
                            0.55
                    );

            double easternClusterRadius =
                    range(
                            random,
                            0.72,
                            0.80
                    );

            double easternCenterX =
                    Math.cos(
                            easternClusterAngle
                    ) * easternClusterRadius;

            double easternCenterZ =
                    Math.sin(
                            easternClusterAngle
                    ) * easternClusterRadius;

            int easternIslandCount =
                    2 + random.nextInt(4);

            for (
                    int i = 0;
                    i < easternIslandCount;
                    i++
            ) {

                double scatterAngle =
                        random.nextDouble() * Math.PI * 2.0;

                double scatterRadius =
                        Math.sqrt(
                                random.nextDouble()
                        );

                double islandX =
                        easternCenterX
                                + Math.cos(
                                scatterAngle
                        ) * scatterRadius * 0.050;

                double islandZ =
                        easternCenterZ
                                + Math.sin(
                                scatterAngle
                        ) * scatterRadius * 0.040;

                double size =
                        i == 0
                                ? range(
                                random,
                                0.012,
                                0.022
                        )
                                : range(
                                random,
                                0.005,
                                0.014
                        );

                land.add(
                        createOrganicIsland(
                                random,
                                islandX,
                                islandZ,
                                size * range(
                                        random,
                                        1.05,
                                        1.50
                                ),
                                size * range(
                                        random,
                                        0.70,
                                        1.05
                                )
                        )
                );
            }
        }

        /*
         * =====================================================
         * CASCADE MOUNTAIN SYSTEM
         * =====================================================
         */

        List<GeoPoint> mountainControls =
                new ArrayList<>();

        int mountainControlCount =
                8;

        double mountainBaseX =
                range(
                        random,
                        0.08,
                        0.18
                );

        double mountainPhase =
                random.nextDouble()
                        * Math.PI
                        * 2.0;

        for (
                int i = 0;
                i < mountainControlCount;
                i++
        ) {

            double t =
                    i / (double) (
                            mountainControlCount - 1
                    );

            double z =
                    lerp(
                            -0.52,
                            0.52,
                            t
                    );

            double x =
                    mountainBaseX
                            + Math.sin(
                            t * Math.PI * 2.0
                                    + mountainPhase
                    ) * 0.045
                            + range(
                            random,
                            -0.035,
                            0.035
                    );

            mountainControls.add(
                    new GeoPoint(
                            x,
                            z
                    )
            );
        }

        MountainSpine mountainSpine =
                new MountainSpine(
                        mountainControls,
                        24
                );

        MountainSystem mountainSystem =
                new MountainSystem(
                        worldSeed,
                        mountainSpine,
                        0.045,
                        0.18,
                        190.0
                );

        return new ContinentPlan(
                land,
                cutouts,
                mountainSystem
        );
    }

    /*
     * =========================================================
     * SOUND PATH GENERATION
     * =========================================================
     */

    private static List<GeoPoint> createSoundPath(
            SplittableRandom random,
            double angle,
            double startRadius,
            double endRadius,
            int pointCount,
            double meander
    ) {

        List<GeoPoint> path =
                new ArrayList<>();

        double directionX =
                Math.cos(angle);
        double directionZ =
                Math.sin(angle);

        double perpendicularX =
                -directionZ;
        double perpendicularZ =
                directionX;

        double phase =
                random.nextDouble()
                        * Math.PI
                        * 2.0;

        double waveCount =
                range(
                        random,
                        1.10,
                        1.75
                );

        for (
                int i = 0;
                i < pointCount;
                i++
        ) {

            double t =
                    i / (double) (
                            pointCount - 1
                    );

            double radius =
                    lerp(
                            startRadius,
                            endRadius,
                            t
                    );

            double meanderEnvelope =
                    smoothstep(
                            0.08,
                            0.32,
                            t
                    );

            double lateral =
                    (
                            Math.sin(
                                    t * Math.PI * waveCount
                                            + phase
                            ) * meander
                                    + range(
                                    random,
                                    -meander * 0.15,
                                    meander * 0.15
                            )
                    ) * meanderEnvelope;

            path.add(
                    new GeoPoint(
                            directionX * radius
                                    + perpendicularX * lateral,
                            directionZ * radius
                                    + perpendicularZ * lateral
                    )
            );
        }

        return path;
    }

    /*
     * =========================================================
     * SMALL BRANCHES
     * =========================================================
     */

    private static void addSoundBranches(
            List<GeoShape> cutouts,
            SplittableRandom random,
            List<GeoPoint> mainPath,
            double soundAngle,
            int branchCount,
            double branchStartWidth,
            double branchEndWidth,
            double minimumLength,
            double maximumLength
    ) {

        double inwardX =
                -Math.cos(
                        soundAngle
                );

        double inwardZ =
                -Math.sin(
                        soundAngle
                );

        double perpendicularX =
                -Math.sin(
                        soundAngle
                );

        double perpendicularZ =
                Math.cos(
                        soundAngle
                );

        for (
                int branch = 0;
                branch < branchCount;
                branch++
        ) {

            double branchT =
                    range(
                            random,
                            0.48,
                            0.76
                    );

            int index =
                    Math.min(
                            mainPath.size() - 1,
                            Math.max(
                                    1,
                                    (int) Math.round(
                                            branchT
                                                    * (mainPath.size() - 1)
                                    )
                            )
                    );

            GeoPoint start =
                    mainPath.get(index);

            double side =
                    random.nextBoolean()
                            ? 1.0
                            : -1.0;

            double branchLength =
                    range(
                            random,
                            minimumLength,
                            maximumLength
                    );

            int pointCount =
                    4;

            List<GeoPoint> branchPath =
                    new ArrayList<>();

            for (
                    int i = 0;
                    i < pointCount;
                    i++
            ) {

                double t =
                        i / (double) (
                                pointCount - 1
                        );

                double sideways =
                        branchLength
                                * t
                                * side;

                double forward =
                        branchLength
                                * t
                                * 0.22;

                double curve =
                        Math.sin(
                                t * Math.PI
                        )
                                * branchLength
                                * 0.10
                                * side;

                branchPath.add(
                        new GeoPoint(
                                start.x()
                                        + perpendicularX * (sideways + curve)
                                        + inwardX * forward,
                                start.z()
                                        + perpendicularZ * (sideways + curve)
                                        + inwardZ * forward
                        )
                );
            }

            cutouts.add(
                    new TaperedSplineShape(
                            branchPath,
                            8,
                            branchStartWidth,
                            branchEndWidth
                    )
            );
        }
    }

    /*
     * =========================================================
     * ORGANIC ISLANDS
     * =========================================================
     */

    private static RadialSplineShape createOrganicIsland(
            SplittableRandom random,
            double centerX,
            double centerZ,
            double radiusX,
            double radiusZ
    ) {

        int pointCount =
                18 + random.nextInt(11);

        double[] radius =
                new double[pointCount];

        double phase2 =
                random.nextDouble() * Math.PI * 2.0;
        double phase3 =
                random.nextDouble() * Math.PI * 2.0;
        double phase5 =
                random.nextDouble() * Math.PI * 2.0;
        double phase7 =
                random.nextDouble() * Math.PI * 2.0;

        double[] jitter =
                new double[pointCount];

        for (
                int i = 0;
                i < pointCount;
                i++
        ) {
            jitter[i] =
                    range(
                            random,
                            -0.12,
                            0.12
                    );
        }

        double[] smoothed =
                new double[pointCount];

        for (
                int i = 0;
                i < pointCount;
                i++
        ) {
            smoothed[i] =
                    jitter[
                            mod(
                                    i - 1,
                                    pointCount
                            )
                            ] * 0.20
                            + jitter[i] * 0.60
                            + jitter[
                            mod(
                                    i + 1,
                                    pointCount
                            )
                            ] * 0.20;
        }

        for (
                int i = 0;
                i < pointCount;
                i++
        ) {

            double angle =
                    Math.PI * 2.0 * i
                            / pointCount;

            radius[i] =
                    1.0
                            + Math.sin(
                            angle * 2.0 + phase2
                    ) * 0.12
                            + Math.sin(
                            angle * 3.0 + phase3
                    ) * 0.075
                            + Math.sin(
                            angle * 5.0 + phase5
                    ) * 0.045
                            + Math.sin(
                            angle * 7.0 + phase7
                    ) * 0.022
                            + smoothed[i];

            radius[i] =
                    clamp(
                            radius[i],
                            0.62,
                            1.38
                    );
        }

        return new RadialSplineShape(
                centerX,
                centerZ,
                radiusX,
                radiusZ,
                random.nextDouble() * Math.PI * 2.0,
                radius
        );
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private static double circularAngleDistance(
            double a,
            double b
    ) {

        double difference =
                Math.abs(a - b);

        difference %=
                Math.PI * 2.0;

        return Math.min(
                difference,
                Math.PI * 2.0 - difference
        );
    }

    private static double range(
            SplittableRandom random,
            double min,
            double max
    ) {
        return min
                + random.nextDouble()
                * (max - min);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a + (b - a) * t;
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
                clamp(
                        t,
                        0.0,
                        1.0
                );

        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static int mod(
            int value,
            int modulus
    ) {

        int result =
                value % modulus;

        return result < 0
                ? result + modulus
                : result;
    }

    private record CoastFeature(
            double angle,
            double amplitude,
            double width
    ) {
    }
}