package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class ContinentPlanner {

    /*
     * Shared mountain-system geometry constants.
     *
     * Keeping these in one place prevents the placement solver from
     * using different influence widths than the actual MountainSystem.
     */
    private static final double CASCADE_OUTER_WIDTH =
            0.18;

    private static final double WESTERN_INNER_WIDTH =
            0.034;

    private static final double WESTERN_OUTER_WIDTH =
            0.090;

    private static final double WESTERN_LOWLAND_GAP =
            0.040;

    private static final double WESTERN_SOUND_CLEARANCE =
            0.22;

    /*
     * Most western ranges remain detached from the shoreline, but a
     * minority are allowed to run into the coast like a subdued
     * Olympic / Coast Mountains analogue.
     */
    private static final double WESTERN_COASTAL_CONTACT_CHANCE =
            0.32;

    /*
     * The mountain spine itself should remain on land.  A coastal
     * range "meets the ocean" because its outer mountain corridor
     * reaches the shoreline, not because the spine is placed offshore.
     */
    private static final double WESTERN_MINIMUM_LAND_MARGIN =
            0.010;

    private static final double WESTERN_INLAND_COAST_GAP =
            0.020;

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
         * Keep an exact planner-side representation of the same
         * generated mainland boundary.  The western-range solver uses
         * this instead of the old approximate circular coastal radius.
         */
        CoastlineProfile mainlandCoastline =
                createCoastlineProfile(
                        baseRadiusX,
                        baseRadiusZ,
                        mainlandRadius
                );

        /*
         * =====================================================
         * WESTERN SOUND SYSTEM ONLY
         * =====================================================
         */

        List<List<GeoPoint>> soundClearancePaths =
                new ArrayList<>();

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

        soundClearancePaths.add(
                primarySound
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
                soundClearancePaths,
                random,
                primarySound,
                primarySoundAngle,

                primaryBranchCount,

                0.020,
                0.0075,

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

            angle =
                    normalizeAngle(
                            angle
                    );

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

            soundClearancePaths.add(
                    path
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
                        soundClearancePaths,
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

        MountainSpine cascadeSpine =
                new MountainSpine(
                        mountainControls,
                        24
                );

        MountainSystem cascadeMountainSystem =
                new MountainSystem(
                        worldSeed,
                        "cascades",
                        cascadeSpine,
                        0.045,
                        CASCADE_OUTER_WIDTH,
                        190.0
                );

        /*
         * =====================================================
         * LOCALIZED WESTERN MOUNTAIN SYSTEM
         * =====================================================
         *
         * Short, compact and seed-random.
         * It occupies only part of the western region, leaving
         * other western lowlands open all the way to the ocean.
         */

        WesternRangePlacement westernPlacement =
                createWesternRangeControls(
                        random,
                        soundClearancePaths,
                        mountainControls,
                        mainlandCoastline
                );

        MountainSpine westernSpine =
                new MountainSpine(
                        westernPlacement.controls(),
                        18
                );

        /*
         * If a seed has very little free western lowland, the placement
         * solver is allowed to make the Olympic-like range narrower.
         * This preserves a real lowland gap instead of forcing the range
         * into the Cascades or the sound network.
         */
        double westernWidthScale =
                westernPlacement.widthScale();

        MountainSystem westernMountainSystem =
                new MountainSystem(
                        worldSeed,
                        "western-range",
                        westernSpine,
                        WESTERN_INNER_WIDTH * westernWidthScale,
                        WESTERN_OUTER_WIDTH * westernWidthScale,
                        range(
                                random,
                                95.0,
                                125.0
                        )
                );

        return new ContinentPlan(
                land,
                cutouts,
                cascadeMountainSystem,
                westernMountainSystem
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
            List<List<GeoPoint>> soundClearancePaths,
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
                                        + perpendicularX
                                        * (sideways + curve)
                                        + inwardX
                                        * forward,

                                start.z()
                                        + perpendicularZ
                                        * (sideways + curve)
                                        + inwardZ
                                        * forward
                        )
                );
            }

            /*
             * Keep the branch for western-range exclusion.
             */
            soundClearancePaths.add(
                    branchPath
            );

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
     * LOCALIZED WESTERN RANGE
     * =========================================================
     *
     * Olympic-inspired in layout, but deliberately less
     * pronounced.  This creates a short curved mountain spine
     * near one portion of the western coast instead of a range
     * extending along the entire coastline.
     */
    private static WesternRangePlacement createWesternRangeControls(
            SplittableRandom random,
            List<List<GeoPoint>> soundPaths,
            List<GeoPoint> cascadeControls,
            CoastlineProfile coastline
    ) {

        /*
         * Actual system influence radii.
         */
        final double minimumCascadeClearance =
                CASCADE_OUTER_WIDTH
                        + WESTERN_OUTER_WIDTH
                        + WESTERN_LOWLAND_GAP;

        /*
         * Keep the western system well away from the entire sound
         * network, including branches.
         */
        final double minimumSoundClearance =
                WESTERN_SOUND_CLEARANCE;

        /*
         * This choice is seed-stable because it is made once before
         * the candidate search starts.
         *
         * Coastal mode means the OUTER mountain corridor is allowed to
         * touch the shoreline.  The mountain spine still has to remain
         * on the mainland.
         */
        boolean preferCoastalContact =
                random.nextDouble()
                        < WESTERN_COASTAL_CONTACT_CHANCE;

        /*
         * Planner-level search only.  This does not run per terrain
         * sample, so a few hundred candidates are inexpensive and are
         * much safer than forcing a range into a bad pocket.
         */
        final int maxAttempts =
                160;

        List<GeoPoint> bestCandidate =
                null;

        double bestClearance =
                Double.NEGATIVE_INFINITY;

        for (
                int attempt = 0;
                attempt < maxAttempts;
                attempt++
        ) {

            /*
             * pi = due west.
             *
             * Keep this on the western half, but allow substantial
             * north/south movement so it can escape sounds and the
             * Cascade corridor.
             */
            double rangeAngle =
                    Math.PI
                            + range(
                            random,
                            -1.18,
                            1.18
                    );

            double inlandOffset;

            if (preferCoastalContact) {

                /*
                 * Close enough that the scaled outer corridor reaches
                 * the real generated coastline.
                 */
                inlandOffset =
                        range(
                                random,
                                0.050,
                                0.080
                        );

            } else {

                /*
                 * Detached massif with a genuine strip of lowland
                 * between the range and ocean.
                 */
                inlandOffset =
                        range(
                                random,
                                0.125,
                                0.190
                        );
            }

            List<GeoPoint> candidate =
                    createWesternRangeCandidate(
                            random,
                            coastline,
                            rangeAngle,
                            inlandOffset,
                            1.0
                    );

            if (
                    !isWesternRangeCandidateValid(
                            candidate,
                            coastline,
                            soundPaths,
                            cascadeControls,
                            1.0,
                            preferCoastalContact,
                            minimumSoundClearance,
                            minimumCascadeClearance
                    )
            ) {
                continue;
            }

            double soundDistance =
                    distanceToSoundNetwork(
                            candidate,
                            soundPaths
                    );

            double cascadeDistance =
                    distanceBetweenPolylines(
                            candidate,
                            cascadeControls
                    );

            /*
             * Prefer the center of the safest available lowland pocket.
             * Coast relation has already been handled as a hard
             * mode-specific rule above.
             */
            double minimumClearance =
                    Math.min(
                            soundDistance,
                            cascadeDistance
                    );

            if (
                    minimumClearance
                            > bestClearance
            ) {

                bestClearance =
                        minimumClearance;

                bestCandidate =
                        candidate;
            }
        }

        if (bestCandidate != null) {
            return new WesternRangePlacement(
                    bestCandidate,
                    1.0
            );
        }

        /*
         * The seeded random pass could not satisfy every hard
         * requirement.  Search the full western sector densely.
         *
         * We try the seed-selected coastal/inland style first, then
         * permit the opposite style rather than violating sound or
         * Cascade separation.
         */
        return createWesternRangeGuaranteed(
                random,
                soundPaths,
                cascadeControls,
                coastline,
                minimumSoundClearance,
                minimumCascadeClearance,
                preferCoastalContact
        );
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

    private static double normalizeAngle(
            double angle
    ) {

        double twoPi =
                Math.PI * 2.0;

        angle %=
                twoPi;

        if (angle < 0.0) {
            angle += twoPi;
        }

        return angle;
    }

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

    private record WesternRangePlacement(
            List<GeoPoint> controls,
            double widthScale
    ) {
    }

    private record CoastFeature(
            double angle,
            double amplitude,
            double width
    ) {
    }

    /*
     * Exact planner-side representation of the generated mainland
     * outline.  boundaryPoints contains the closing point as well, so
     * it can be treated as an ordinary polyline by the distance helpers.
     */
    private record CoastlineProfile(
            double radiusX,
            double radiusZ,
            double[] radialScale,
            List<GeoPoint> boundaryPoints
    ) {
    }

    private record CoastFrame(
            GeoPoint point,
            double tangentX,
            double tangentZ,
            double inwardX,
            double inwardZ
    ) {
    }

    private static CoastlineProfile createCoastlineProfile(
            double radiusX,
            double radiusZ,
            double[] radialScale
    ) {

        double[] copiedScale =
                radialScale.clone();

        List<GeoPoint> boundary =
                new ArrayList<>();

        /*
         * Use more samples than the source radial array so distance
         * checks see the same smooth Catmull-Rom coastline that the
         * actual RadialSplineShape produces.
         */
        int sampleCount =
                Math.max(
                        128,
                        copiedScale.length * 2
                );

        for (
                int i = 0;
                i < sampleCount;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / sampleCount;

            double scale =
                    sampleCircularCatmullRom(
                            copiedScale,
                            angle
                    );

            boundary.add(
                    new GeoPoint(
                            Math.cos(angle)
                                    * radiusX
                                    * scale,
                            Math.sin(angle)
                                    * radiusZ
                                    * scale
                    )
            );
        }

        /*
         * Explicitly close the polyline.
         */
        boundary.add(
                boundary.getFirst()
        );

        return new CoastlineProfile(
                radiusX,
                radiusZ,
                copiedScale,
                List.copyOf(
                        boundary
                )
        );
    }

    private static double sampleCircularCatmullRom(
            double[] samples,
            double angle
    ) {

        angle =
                normalizeAngle(
                        angle
                );

        double position =
                angle
                        / (Math.PI * 2.0)
                        * samples.length;

        int i1 =
                (int) Math.floor(
                        position
                );

        double t =
                position - i1;

        double p0 =
                samples[
                        mod(
                                i1 - 1,
                                samples.length
                        )
                        ];

        double p1 =
                samples[
                        mod(
                                i1,
                                samples.length
                        )
                        ];

        double p2 =
                samples[
                        mod(
                                i1 + 1,
                                samples.length
                        )
                        ];

        double p3 =
                samples[
                        mod(
                                i1 + 2,
                                samples.length
                        )
                        ];

        double t2 =
                t * t;

        double t3 =
                t2 * t;

        return 0.5
                * (
                2.0 * p1

                        + (-p0 + p2)
                        * t

                        + (
                        2.0 * p0
                                - 5.0 * p1
                                + 4.0 * p2
                                - p3
                ) * t2

                        + (
                        -p0
                                + 3.0 * p1
                                - 3.0 * p2
                                + p3
                ) * t3
        );
    }

    private static GeoPoint coastlinePointAt(
            CoastlineProfile coastline,
            double angle
    ) {

        double scale =
                sampleCircularCatmullRom(
                        coastline.radialScale(),
                        angle
                );

        return new GeoPoint(
                Math.cos(angle)
                        * coastline.radiusX()
                        * scale,
                Math.sin(angle)
                        * coastline.radiusZ()
                        * scale
        );
    }

    private static CoastFrame coastlineFrameAt(
            CoastlineProfile coastline,
            double angle
    ) {

        GeoPoint point =
                coastlinePointAt(
                        coastline,
                        angle
                );

        final double delta =
                0.012;

        GeoPoint before =
                coastlinePointAt(
                        coastline,
                        angle - delta
                );

        GeoPoint after =
                coastlinePointAt(
                        coastline,
                        angle + delta
                );

        double tangentX =
                after.x() - before.x();

        double tangentZ =
                after.z() - before.z();

        double tangentLength =
                Math.sqrt(
                        tangentX * tangentX
                                + tangentZ * tangentZ
                );

        if (tangentLength < 1.0e-9) {

            tangentX =
                    -Math.sin(angle);

            tangentZ =
                    Math.cos(angle);

            tangentLength =
                    1.0;
        }

        tangentX /=
                tangentLength;

        tangentZ /=
                tangentLength;

        /*
         * One normal points inland and the other points to sea.
         * Pick the one whose dot product points toward the origin.
         */
        double inwardX =
                -tangentZ;

        double inwardZ =
                tangentX;

        double towardCenterX =
                -point.x();

        double towardCenterZ =
                -point.z();

        if (
                inwardX * towardCenterX
                        + inwardZ * towardCenterZ
                        < 0.0
        ) {

            inwardX =
                    -inwardX;

            inwardZ =
                    -inwardZ;
        }

        return new CoastFrame(
                point,
                tangentX,
                tangentZ,
                inwardX,
                inwardZ
        );
    }

    /*
     * Same normalized signed field used by the mainland
     * RadialSplineShape: positive values are inside land.
     */
    private static double mainlandMargin(
            CoastlineProfile coastline,
            double x,
            double z
    ) {

        double nx =
                x / coastline.radiusX();

        double nz =
                z / coastline.radiusZ();

        double actualRadius =
                Math.sqrt(
                        nx * nx
                                + nz * nz
                );

        double angle =
                Math.atan2(
                        nz,
                        nx
                );

        if (angle < 0.0) {
            angle +=
                    Math.PI * 2.0;
        }

        double targetRadius =
                sampleCircularCatmullRom(
                        coastline.radialScale(),
                        angle
                );

        return targetRadius
                - actualRadius;
    }

    private static boolean westernRangeSpineIsOnMainland(
            List<GeoPoint> candidate,
            CoastlineProfile coastline
    ) {

        if (candidate.isEmpty()) {
            return false;
        }

        /*
         * Check controls and several points between controls.  This
         * catches a short spline segment trying to cut across a bay or
         * leave the coast between two otherwise valid control points.
         */
        final int samplesPerSegment =
                4;

        for (
                int i = 0;
                i < candidate.size() - 1;
                i++
        ) {

            GeoPoint a =
                    candidate.get(i);

            GeoPoint b =
                    candidate.get(i + 1);

            for (
                    int sample = 0;
                    sample <= samplesPerSegment;
                    sample++
            ) {

                double t =
                        sample
                                / (double) samplesPerSegment;

                double x =
                        lerp(
                                a.x(),
                                b.x(),
                                t
                        );

                double z =
                        lerp(
                                a.z(),
                                b.z(),
                                t
                        );

                if (
                        mainlandMargin(
                                coastline,
                                x,
                                z
                        ) < WESTERN_MINIMUM_LAND_MARGIN
                ) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isWesternRangeCandidateValid(
            List<GeoPoint> candidate,
            CoastlineProfile coastline,
            List<List<GeoPoint>> soundPaths,
            List<GeoPoint> cascadeControls,
            double scale,
            boolean coastalContact,
            double minimumSoundClearance,
            double minimumCascadeClearance
    ) {

        if (
                !westernRangeSpineIsOnMainland(
                        candidate,
                        coastline
                )
        ) {
            return false;
        }

        double soundDistance =
                distanceToSoundNetwork(
                        candidate,
                        soundPaths
                );

        /*
         * The clearance inputs are measured spine-to-spine.  If the
         * western system has been deliberately scaled down, reduce its
         * required centerline separation by exactly the amount its
         * outer influence radius shrank.  This preserves the same
         * lowland gap instead of over-penalizing emergency compact
         * ranges.
         */
        double scaledSoundClearance =
                minimumSoundClearance
                        - WESTERN_OUTER_WIDTH
                        * (1.0 - scale);

        if (
                soundDistance
                        < scaledSoundClearance
        ) {
            return false;
        }

        double cascadeDistance =
                distanceBetweenPolylines(
                        candidate,
                        cascadeControls
                );

        double scaledCascadeClearance =
                minimumCascadeClearance
                        - WESTERN_OUTER_WIDTH
                        * (1.0 - scale);

        if (
                cascadeDistance
                        < scaledCascadeClearance
        ) {
            return false;
        }

        double coastDistance =
                distanceBetweenPolylines(
                        candidate,
                        coastline.boundaryPoints()
                );

        double scaledOuterWidth =
                WESTERN_OUTER_WIDTH
                        * scale;

        if (coastalContact) {

            /*
             * The corridor must actually be capable of reaching the
             * shoreline.  A small tolerance accounts for the later
             * MountainSystem coordinate warp.
             */
            double maximumContactDistance =
                    scaledOuterWidth
                            + 0.010;

            if (
                    coastDistance
                            > maximumContactDistance
            ) {
                return false;
            }

        } else {

            /*
             * Detached variant: preserve visible lowlands between the
             * coast and the western massif.
             */
            double minimumCoastDistance =
                    scaledOuterWidth
                            + WESTERN_INLAND_COAST_GAP;

            if (
                    coastDistance
                            < minimumCoastDistance
            ) {
                return false;
            }
        }

        return true;
    }

    private static double distanceToPolyline(
            double x,
            double z,
            List<GeoPoint> points
    ) {

        if (points.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        if (points.size() == 1) {

            double dx =
                    x - points.get(0).x();

            double dz =
                    z - points.get(0).z();

            return Math.sqrt(
                    dx * dx
                            + dz * dz
            );
        }

        double minimumDistance =
                Double.POSITIVE_INFINITY;

        for (
                int i = 0;
                i < points.size() - 1;
                i++
        ) {

            GeoPoint a =
                    points.get(i);

            GeoPoint b =
                    points.get(i + 1);

            double distance =
                    distanceToSegment(
                            x,
                            z,
                            a.x(),
                            a.z(),
                            b.x(),
                            b.z()
                    );

            minimumDistance =
                    Math.min(
                            minimumDistance,
                            distance
                    );
        }

        return minimumDistance;
    }

    private static double distanceToSegment(
            double px,
            double pz,
            double ax,
            double az,
            double bx,
            double bz
    ) {

        double abX =
                bx - ax;

        double abZ =
                bz - az;

        double apX =
                px - ax;

        double apZ =
                pz - az;

        double lengthSquared =
                abX * abX
                        + abZ * abZ;

        if (lengthSquared < 1.0e-12) {

            double dx =
                    px - ax;

            double dz =
                    pz - az;

            return Math.sqrt(
                    dx * dx
                            + dz * dz
            );
        }

        double t =
                (
                        apX * abX
                                + apZ * abZ
                ) / lengthSquared;

        t =
                clamp(
                        t,
                        0.0,
                        1.0
                );

        double closestX =
                ax + abX * t;

        double closestZ =
                az + abZ * t;

        double dx =
                px - closestX;

        double dz =
                pz - closestZ;

        return Math.sqrt(
                dx * dx
                        + dz * dz
        );
    }

    private static List<GeoPoint> createWesternRangeCandidate(
            SplittableRandom random,
            CoastlineProfile coastline,
            double rangeAngle,
            double inlandOffset,
            double scale
    ) {

        CoastFrame frame =
                coastlineFrameAt(
                        coastline,
                        rangeAngle
                );

        double coastX =
                frame.point().x();

        double coastZ =
                frame.point().z();

        double inwardX =
                frame.inwardX();

        double inwardZ =
                frame.inwardZ();

        double alongCoastX =
                frame.tangentX();

        double alongCoastZ =
                frame.tangentZ();

        /*
         * Keep only a small along-shore displacement.  The selected
         * coastline angle should control the macro location rather
         * than letting random translation drift the massif toward a
         * sound or the Cascades after validation.
         */
        double alongCoastOffset =
                range(
                        random,
                        -0.022,
                        0.022
                ) * scale;

        double centerX =
                coastX
                        + inwardX * inlandOffset
                        + alongCoastX * alongCoastOffset;

        double centerZ =
                coastZ
                        + inwardZ * inlandOffset
                        + alongCoastZ * alongCoastOffset;

        /*
         * Mostly parallel to the local coastline, with just enough
         * rotation to avoid a stamped / mechanically tangent look.
         */
        double rotation =
                range(
                        random,
                        -0.30,
                        0.30
                );

        double axisX =
                alongCoastX
                        * Math.cos(rotation)
                        + inwardX
                        * Math.sin(rotation)
                        * 0.30;

        double axisZ =
                alongCoastZ
                        * Math.cos(rotation)
                        + inwardZ
                        * Math.sin(rotation)
                        * 0.30;

        double axisLength =
                Math.sqrt(
                        axisX * axisX
                                + axisZ * axisZ
                );

        if (axisLength < 1.0e-9) {

            axisX =
                    alongCoastX;

            axisZ =
                    alongCoastZ;

            axisLength =
                    1.0;
        }

        axisX /=
                axisLength;

        axisZ /=
                axisLength;

        /*
         * Perpendicular to the spine.  Flip it so the Olympic-like
         * arc always bows inland instead of bulging out to sea.
         */
        double crossX =
                -axisZ;

        double crossZ =
                axisX;

        if (
                crossX * inwardX
                        + crossZ * inwardZ
                        < 0.0
        ) {

            crossX =
                    -crossX;

            crossZ =
                    -crossZ;
        }

        double halfLength =
                range(
                        random,
                        0.075,
                        0.105
                ) * scale;

        double bulge =
                range(
                        random,
                        0.030,
                        0.052
                ) * scale;

        double middleBiasStrength =
                range(
                        random,
                        0.000,
                        0.014
                ) * scale;

        List<GeoPoint> controls =
                new ArrayList<>();

        int controlCount =
                6;

        for (
                int i = 0;
                i < controlCount;
                i++
        ) {

            double t =
                    i / (double) (
                            controlCount - 1
                    );

            double signed =
                    lerp(
                            -1.0,
                            1.0,
                            t
                    );

            double along =
                    signed
                            * halfLength;

            double arc =
                    (
                            1.0
                                    - signed * signed
                    ) * bulge;

            double middleEnvelope =
                    Math.max(
                            0.0,
                            1.0
                                    - Math.abs(signed)
                                    * 1.2
                    );

            double middleBias =
                    middleEnvelope
                            * middleBiasStrength;

            double jitterX =
                    range(
                            random,
                            -0.0065,
                            0.0065
                    ) * scale;

            double jitterZ =
                    range(
                            random,
                            -0.0065,
                            0.0065
                    ) * scale;

            controls.add(
                    new GeoPoint(
                            centerX
                                    + axisX * along
                                    + crossX * arc
                                    + inwardX * middleBias
                                    + jitterX,

                            centerZ
                                    + axisZ * along
                                    + crossZ * arc
                                    + inwardZ * middleBias
                                    + jitterZ
                    )
            );
        }

        return controls;
    }

    private static double distanceToSoundNetwork(
            List<GeoPoint> westernRange,
            List<List<GeoPoint>> soundPaths
    ) {

        double minimum =
                Double.POSITIVE_INFINITY;

        for (
                List<GeoPoint> soundPath :
                soundPaths
        ) {

            minimum =
                    Math.min(
                            minimum,
                            distanceBetweenPolylines(
                                    westernRange,
                                    soundPath
                            )
                    );
        }

        return minimum;
    }

    private static double distanceBetweenPolylines(
            List<GeoPoint> first,
            List<GeoPoint> second
    ) {

        if (
                first.size() < 2
                        || second.size() < 2
        ) {
            return Double.POSITIVE_INFINITY;
        }

        double minimum =
                Double.POSITIVE_INFINITY;

        for (
                int a = 0;
                a < first.size() - 1;
                a++
        ) {

            GeoPoint a0 =
                    first.get(a);

            GeoPoint a1 =
                    first.get(a + 1);

            /*
             * Sampling along the first segment is sufficient here
             * because these are short, densely controlled planner
             * splines.
             */
            final int samples =
                    8;

            for (
                    int sample = 0;
                    sample <= samples;
                    sample++
            ) {

                double t =
                        sample / (double) samples;

                double px =
                        lerp(
                                a0.x(),
                                a1.x(),
                                t
                        );

                double pz =
                        lerp(
                                a0.z(),
                                a1.z(),
                                t
                        );

                double distance =
                        distanceToPolyline(
                                px,
                                pz,
                                second
                        );

                minimum =
                        Math.min(
                                minimum,
                                distance
                        );
            }
        }

        /*
         * Also test in the opposite direction so short segments
         * cannot slip through each other's sampling gaps.
         */
        for (
                int b = 0;
                b < second.size() - 1;
                b++
        ) {

            GeoPoint b0 =
                    second.get(b);

            GeoPoint b1 =
                    second.get(b + 1);

            final int samples =
                    8;

            for (
                    int sample = 0;
                    sample <= samples;
                    sample++
            ) {

                double t =
                        sample / (double) samples;

                double px =
                        lerp(
                                b0.x(),
                                b1.x(),
                                t
                        );

                double pz =
                        lerp(
                                b0.z(),
                                b1.z(),
                                t
                        );

                double distance =
                        distanceToPolyline(
                                px,
                                pz,
                                first
                        );

                minimum =
                        Math.min(
                                minimum,
                                distance
                        );
            }
        }

        return minimum;
    }

    private static WesternRangePlacement createWesternRangeGuaranteed(
            SplittableRandom random,
            List<List<GeoPoint>> soundPaths,
            List<GeoPoint> cascadeControls,
            CoastlineProfile coastline,
            double minimumSoundClearance,
            double minimumCascadeClearance,
            boolean preferCoastalContact
    ) {

        /*
         * Preserve a full-size western massif whenever possible.
         * Only shrink it if the seed genuinely has too little clean
         * western lowland between sounds, coast and Cascades.
         */
        double[] scales = {
                1.00,
                0.92,
                0.84,
                0.76
        };

        double[] coastalOffsets = {
                0.050,
                0.062,
                0.074,
                0.086
        };

        double[] inlandOffsets = {
                0.120,
                0.145,
                0.170,
                0.195
        };

        /*
         * Try the seed-selected style first.
         */
        WesternRangePlacement preferred =
                searchWesternRangeSweep(
                        random,
                        soundPaths,
                        cascadeControls,
                        coastline,
                        minimumSoundClearance,
                        minimumCascadeClearance,
                        preferCoastalContact,
                        scales,
                        preferCoastalContact
                                ? coastalOffsets
                                : inlandOffsets,
                        Math.PI - 1.38,
                        Math.PI + 1.38,
                        181
                );

        if (preferred != null) {
            return preferred;
        }

        /*
         * Do not break the hard separation rules merely to preserve
         * the cosmetic coastal/inland style choice.  If the preferred
         * style cannot fit, try the other style at the same scales.
         */
        WesternRangePlacement alternate =
                searchWesternRangeSweep(
                        random,
                        soundPaths,
                        cascadeControls,
                        coastline,
                        minimumSoundClearance,
                        minimumCascadeClearance,
                        !preferCoastalContact,
                        scales,
                        !preferCoastalContact
                                ? coastalOffsets
                                : inlandOffsets,
                        Math.PI - 1.38,
                        Math.PI + 1.38,
                        181
                );

        if (alternate != null) {
            return alternate;
        }

        return createEmergencyWesternRange(
                random,
                soundPaths,
                cascadeControls,
                coastline,
                minimumSoundClearance,
                minimumCascadeClearance,
                preferCoastalContact
        );
    }

    private static WesternRangePlacement searchWesternRangeSweep(
            SplittableRandom random,
            List<List<GeoPoint>> soundPaths,
            List<GeoPoint> cascadeControls,
            CoastlineProfile coastline,
            double minimumSoundClearance,
            double minimumCascadeClearance,
            boolean coastalContact,
            double[] scales,
            double[] inlandOffsets,
            double minimumAngle,
            double maximumAngle,
            int angleSamples
    ) {

        /*
         * A small phase prevents every fallback sweep from sampling
         * exactly the same angular lattice relative to the radial
         * coastline controls.
         */
        double phase =
                random.nextDouble();

        for (double scale : scales) {

            List<GeoPoint> bestCandidate =
                    null;

            double bestScore =
                    Double.NEGATIVE_INFINITY;

            for (
                    int i = 0;
                    i < angleSamples;
                    i++
            ) {

                double t =
                        (
                                i + phase
                        ) / angleSamples;

                double angle =
                        lerp(
                                minimumAngle,
                                maximumAngle,
                                t
                        );

                for (
                        double inlandOffset :
                        inlandOffsets
                ) {

                    List<GeoPoint> candidate =
                            createWesternRangeCandidate(
                                    random,
                                    coastline,
                                    angle,
                                    inlandOffset,
                                    scale
                            );

                    if (
                            !isWesternRangeCandidateValid(
                                    candidate,
                                    coastline,
                                    soundPaths,
                                    cascadeControls,
                                    scale,
                                    coastalContact,
                                    minimumSoundClearance,
                                    minimumCascadeClearance
                            )
                    ) {
                        continue;
                    }

                    double soundDistance =
                            distanceToSoundNetwork(
                                    candidate,
                                    soundPaths
                            );

                    double cascadeDistance =
                            distanceBetweenPolylines(
                                    candidate,
                                    cascadeControls
                            );

                    /*
                     * Maximize the weaker of the two critical
                     * separations.  We evaluate every valid candidate
                     * at this scale instead of returning the first one,
                     * which avoids an artificial north/south edge bias.
                     */
                    double score =
                            Math.min(
                                    soundDistance,
                                    cascadeDistance
                            );

                    if (score > bestScore) {

                        bestScore =
                                score;

                        bestCandidate =
                                candidate;
                    }
                }
            }

            /*
             * Prefer the largest scale that has any valid placement.
             */
            if (bestCandidate != null) {
                return new WesternRangePlacement(
                        bestCandidate,
                        scale
                );
            }
        }

        return null;
    }

    private static WesternRangePlacement createEmergencyWesternRange(
            SplittableRandom random,
            List<List<GeoPoint>> soundPaths,
            List<GeoPoint> cascadeControls,
            CoastlineProfile coastline,
            double minimumSoundClearance,
            double minimumCascadeClearance,
            boolean preferCoastalContact
    ) {

        /*
         * Last-resort search: substantially smaller massif and nearly
         * the whole western half of the mainland, but still NO sound
         * overlap and NO Cascade/foothill overlap.
         */
        double[] emergencyScales = {
                0.68,
                0.58,
                0.48,
                0.40,
                0.32,
                0.24
        };

        double[] emergencyCoastalOffsets = {
                0.038,
                0.050,
                0.062,
                0.074
        };

        double[] emergencyInlandOffsets = {
                0.095,
                0.120,
                0.150,
                0.180,
                0.215
        };

        WesternRangePlacement preferred =
                searchWesternRangeSweep(
                        random,
                        soundPaths,
                        cascadeControls,
                        coastline,
                        minimumSoundClearance,
                        minimumCascadeClearance,
                        preferCoastalContact,
                        emergencyScales,
                        preferCoastalContact
                                ? emergencyCoastalOffsets
                                : emergencyInlandOffsets,
                        Math.PI - 1.50,
                        Math.PI + 1.50,
                        241
                );

        if (preferred != null) {
            return preferred;
        }

        WesternRangePlacement alternate =
                searchWesternRangeSweep(
                        random,
                        soundPaths,
                        cascadeControls,
                        coastline,
                        minimumSoundClearance,
                        minimumCascadeClearance,
                        !preferCoastalContact,
                        emergencyScales,
                        !preferCoastalContact
                                ? emergencyCoastalOffsets
                                : emergencyInlandOffsets,
                        Math.PI - 1.50,
                        Math.PI + 1.50,
                        241
                );

        if (alternate != null) {
            return alternate;
        }

        throw new IllegalStateException(
                "Unable to place western mountain range while preserving "
                        + "mainland containment, sound clearance, Cascade "
                        + "clearance, and coastline rules."
        );
    }

}
