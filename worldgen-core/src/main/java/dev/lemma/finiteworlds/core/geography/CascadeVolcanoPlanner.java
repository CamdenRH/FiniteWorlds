package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.List;
import java.util.SplittableRandom;

/**
 * Plans the single globally dominant Cascade volcano.
 *
 * Pass 1 established placement, footprint, and vertical authority. Pass 2
 * adds stable edifice controls for asymmetric flanks, summit displacement,
 * radial buttresses, and a modest summit crater. Hydrology, glacial carving,
 * lava-flow geology, and true erosional valleys remain later systems.
 */
public final class CascadeVolcanoPlanner {

    private static final int CANDIDATE_COUNT =
            192;

    private CascadeVolcanoPlanner() {
    }

    public static CascadeVolcano generate(
            long worldSeed,
            MountainSystem cascadeSystem,
            List<CascadeMassif> massifs,
            List<CascadePeak> peaks,
            WorldBlueprint blueprint
    ) {
        double spineLength =
                cascadeSystem.spine()
                        .totalLength();
        SplittableRandom random =
                new SplittableRandom(
                        SeedUtil.derive(
                                worldSeed,
                                "cascade-landmark-volcano"
                        )
                );

        Candidate best =
                null;

        for (int i = 0; i < CANDIDATE_COUNT; i++) {
            /*
             * Stratify progress so candidate evaluation covers the full
             * interior of the range rather than trusting pure RNG to do so.
             */
            double progress =
                    lerp(
                            0.14,
                            0.86,
                            (i + random.nextDouble())
                                    / CANDIDATE_COUNT
                    );

            double arcPosition =
                    progress * spineLength;

            /*
             * Mild west-side preference, analogous to the dominant volcanic
             * crest sitting on / just west of the broader Cascade axis.
             * The range remains wide enough for east-side candidates too.
             */
            double signedDistance =
                    clamp(
                            range(random, -0.018, 0.042)
                                    + triangular(random) * 0.010,
                            -0.030,
                            0.050
                    );

            double radiusAcross =
                    range(
                            random,
                            0.060,
                            0.082
                    );

            double radiusAlong =
                    range(
                            random,
                            0.070,
                            0.104
                    );

            double localMassifStrength =
                    massifStrengthAt(
                            arcPosition,
                            massifs
                    );

            /*
             * T2 moves the landmark volcano deeper into a genuinely
             * mountainous Cascade context. Pass 1 deliberately preferred a
             * weak shoulder (~0.48 massif strength), which kept the edifice
             * clear of major summits but could leave it visually isolated.
             * Favor strong highland terrain while still avoiding the exact
             * center of the most dominant massif.
             */
            double shoulderScore =
                    1.0
                            - Math.abs(
                            localMassifStrength - 0.72
                    ) / 0.40;

            shoulderScore =
                    clamp01(
                            shoulderScore
                    );

            double majorPeakContext =
                    majorPeakContextScore(
                            signedDistance,
                            arcPosition,
                            radiusAcross,
                            radiusAlong,
                            peaks
                    );

            double edgeScore =
                    smoothstep(
                            0.14,
                            0.25,
                            progress
                    )
                            * (
                            1.0
                                    - smoothstep(
                                    0.75,
                                    0.86,
                                    progress
                            )
                    );

            double westShoulderScore =
                    1.0
                            - clamp01(
                            Math.abs(
                                    signedDistance - 0.014
                            ) / 0.060
                    );

            double landRoomScore =
                    landRoomScore(
                            cascadeSystem,
                            blueprint,
                            signedDistance,
                            arcPosition,
                            radiusAcross,
                            radiusAlong
                    );

            double score =
                    shoulderScore * 0.33
                            + majorPeakContext * 0.20
                            + landRoomScore * 0.30
                            + edgeScore * 0.07
                            + westShoulderScore * 0.05
                            + random.nextDouble() * 0.05;

            /*
             * Reject shoreline-clipped candidates outright when the exact
             * populated coast-distance field is available.
             */
            if (blueprint != null && landRoomScore < 0.58) {
                continue;
            }

            Candidate candidate =
                    new Candidate(
                            signedDistance,
                            arcPosition,
                            radiusAcross,
                            radiusAlong,
                            score
                    );

            if (best == null || candidate.score() > best.score()) {
                best =
                        candidate;
            }
        }

        if (best == null) {
            throw new IllegalStateException(
                    "Unable to plan Cascade landmark volcano"
            );
        }

        /*
         * Deliberately dramatic for Pass 1.  Ordinary Cascade uplift tops out
         * at 1.0 * maximumUplift; this volcano alone may contribute more than
         * twice that amount at the summit.  We can scale it back after visual
         * inspection, but the planning invariant is that it must dominate.
         */
        double heightMultiplier =
                range(
                        random,
                        2.05,
                        2.30
                );

        /*
         * Pass-2 morphology controls. These are deliberately conservative
         * enough to preserve the singular stratovolcano silhouette while
         * ensuring the edifice is not a mathematically perfect cone.
         */
        double rotationRadians =
                range(
                        random,
                        -Math.PI,
                        Math.PI
                );

        double asymmetry =
                range(
                        random,
                        0.075,
                        0.145
                );

        double asymmetryPhase =
                range(
                        random,
                        -Math.PI,
                        Math.PI
                );

        double summitOffsetAcross =
                triangular(random)
                        * best.radiusAcross()
                        * range(random, 0.035, 0.085);

        double summitOffsetAlong =
                triangular(random)
                        * best.radiusAlong()
                        * range(random, 0.035, 0.085);

        int buttressCount =
                7 + random.nextInt(5);

        double buttressPhase =
                range(
                        random,
                        -Math.PI,
                        Math.PI
                );

        double buttressStrength =
                range(
                        random,
                        0.035,
                        0.070
                );

        double craterRadiusFraction =
                range(
                        random,
                        0.060,
                        0.092
                );

        double craterDepthFraction =
                range(
                        random,
                        0.045,
                        0.075
                );

        double flankRoughness =
                range(
                        random,
                        0.025,
                        0.050
                );

        return new CascadeVolcano(
                best.signedDistance(),
                best.arcPosition(),
                best.radiusAcross(),
                best.radiusAlong(),
                heightMultiplier,
                rotationRadians,
                asymmetry,
                asymmetryPhase,
                summitOffsetAcross,
                summitOffsetAlong,
                buttressCount,
                buttressPhase,
                buttressStrength,
                craterRadiusFraction,
                craterDepthFraction,
                flankRoughness
        );
    }

    private static double massifStrengthAt(
            double arcPosition,
            List<CascadeMassif> massifs
    ) {
        double result =
                0.0;

        for (CascadeMassif massif : massifs) {
            double normalizedDistance =
                    Math.abs(
                            arcPosition - massif.centerArc()
                    ) / massif.halfWidth();

            if (normalizedDistance >= 1.0) {
                continue;
            }

            double influence =
                    0.5
                            + 0.5
                            * Math.cos(
                            normalizedDistance * Math.PI
                    );

            result =
                    Math.max(
                            result,
                            Math.pow(influence, 0.82)
                                    * massif.strength()
                    );
        }

        return clamp01(result);
    }

    private static double majorPeakContextScore(
            double signedDistance,
            double arcPosition,
            double radiusAcross,
            double radiusAlong,
            List<CascadePeak> peaks
    ) {
        double closestNormalizedDistance =
                Double.POSITIVE_INFINITY;

        for (CascadePeak peak : peaks) {
            if (peak.peakClass() == CascadePeakClass.BACKGROUND_ALPINE
                    || peak.peakClass() == CascadePeakClass.ALPINE) {
                continue;
            }

            double cross =
                    (signedDistance - peak.signedDistance())
                            / radiusAcross;

            double along =
                    (arcPosition - peak.arcPosition())
                            / radiusAlong;

            double normalizedDistance =
                    Math.hypot(
                            cross,
                            along
                    );

            closestNormalizedDistance =
                    Math.min(
                            closestNormalizedDistance,
                            normalizedDistance
                    );
        }

        if (!Double.isFinite(closestNormalizedDistance)) {
            return 0.15;
        }

        /*
         * Too close would bury an existing major summit; too far recreates
         * the isolated-volcano problem. Prefer roughly one edifice radius
         * from an existing major/regional summit.
         */
        double minimumClearance =
                smoothstep(
                        0.50,
                        0.92,
                        closestNormalizedDistance
                );

        double neighborhoodContext =
                1.0
                        - smoothstep(
                        1.65,
                        2.65,
                        closestNormalizedDistance
                );

        return clamp01(
                minimumClearance
                        * neighborhoodContext
        );
    }

    private static double landRoomScore(
            MountainSystem cascadeSystem,
            WorldBlueprint blueprint,
            double signedDistance,
            double arcPosition,
            double radiusAcross,
            double radiusAlong
    ) {
        if (blueprint == null) {
            return 1.0;
        }

        SpineFrame frame =
                frameAtArc(
                        cascadeSystem.spine(),
                        arcPosition
                );

        double minimumCoastDistance =
                Double.POSITIVE_INFINITY;

        /*
         * Test the center and a ring covering the T2 volcanic foothill
         * transition, not just the formal cone. This prevents the guaranteed
         * landmark mountain complex from being clipped by a bay, sound, or
         * nearby coastline.
         */
        minimumCoastDistance =
                Math.min(
                        minimumCoastDistance,
                        coastDistanceAt(
                                blueprint,
                                frame,
                                signedDistance,
                                0.0
                        )
                );

        for (int i = 0; i < 16; i++) {
            double angle =
                    i / 16.0
                            * Math.PI
                            * 2.0;

            double crossOffset =
                    signedDistance
                            + Math.cos(angle)
                            * radiusAcross
                            * 1.62;

            double alongOffset =
                    Math.sin(angle)
                            * radiusAlong
                            * 1.62;

            minimumCoastDistance =
                    Math.min(
                            minimumCoastDistance,
                            coastDistanceAt(
                                    blueprint,
                                    frame,
                                    crossOffset,
                                    alongOffset
                            )
                    );
        }

        /*
         * 700 blocks is where CascadiaGenerator finishes its shoreline fade.
         * Prefer materially more clearance so the broad volcanic apron is not
         * attenuated at its edges.
         */
        return smoothstep(
                650.0,
                1350.0,
                minimumCoastDistance
        );
    }

    private static double coastDistanceAt(
            WorldBlueprint blueprint,
            SpineFrame frame,
            double crossOffset,
            double alongOffset
    ) {
        double nx =
                frame.x()
                        - frame.tangentZ() * crossOffset
                        + frame.tangentX() * alongOffset;

        double nz =
                frame.z()
                        + frame.tangentX() * crossOffset
                        + frame.tangentZ() * alongOffset;

        int size =
                blueprint.resolution();

        int cellX =
                (int) Math.floor(
                        (nx + 1.0)
                                * 0.5
                                * size
                );

        int cellZ =
                (int) Math.floor(
                        (nz + 1.0)
                                * 0.5
                                * size
                );

        if (cellX < 0 || cellX >= size || cellZ < 0 || cellZ >= size) {
            return -Double.MAX_VALUE;
        }

        if (blueprint.landMask(cellX, cellZ) < 0.5f) {
            return -Double.MAX_VALUE;
        }

        return blueprint.coastDistance(
                cellX,
                cellZ
        );
    }

    private static SpineFrame frameAtArc(
            MountainSpine spine,
            double arcPosition
    ) {
        List<GeoPoint> points =
                spine.points();

        double target =
                clamp(
                        arcPosition,
                        0.0,
                        spine.totalLength()
                );

        double running =
                0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            GeoPoint a =
                    points.get(i);

            GeoPoint b =
                    points.get(i + 1);

            double dx =
                    b.x() - a.x();

            double dz =
                    b.z() - a.z();

            double length =
                    Math.hypot(dx, dz);

            if (length <= 1.0e-12) {
                continue;
            }

            if (running + length >= target || i == points.size() - 2) {
                double t =
                        clamp01(
                                (target - running)
                                        / length
                        );

                return new SpineFrame(
                        lerp(a.x(), b.x(), t),
                        lerp(a.z(), b.z(), t),
                        dx / length,
                        dz / length
                );
            }

            running +=
                    length;
        }

        GeoPoint last =
                points.getLast();

        return new SpineFrame(
                last.x(),
                last.z(),
                0.0,
                1.0
        );
    }

    private static double triangular(
            SplittableRandom random
    ) {
        return random.nextDouble()
                - random.nextDouble();
    }

    private static double range(
            SplittableRandom random,
            double minimum,
            double maximum
    ) {
        return minimum
                + random.nextDouble()
                * (maximum - minimum);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (b - a)
                * clamp01(t);
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
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
        return clamp(
                value,
                0.0,
                1.0
        );
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    private record SpineFrame(
            double x,
            double z,
            double tangentX,
            double tangentZ
    ) {
    }

    private record Candidate(
            double signedDistance,
            double arcPosition,
            double radiusAcross,
            double radiusAlong,
            double score
    ) {
    }
}
