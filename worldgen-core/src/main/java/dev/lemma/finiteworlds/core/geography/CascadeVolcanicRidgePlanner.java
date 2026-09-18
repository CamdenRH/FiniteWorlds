package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Plans the handful of large buttress ridges radiating from the landmark
 * Cascade volcano.
 *
 * These are intentionally different from the fine radial morphology stored on
 * the volcano itself.  The fine field is drainage guidance; these paths are
 * real macro terrain structures that can leave the edifice and merge into the
 * surrounding mountain range.
 */
public final class CascadeVolcanicRidgePlanner {

    private CascadeVolcanicRidgePlanner() {
    }

    public static List<CascadeVolcanicRidge> generate(
            long worldSeed,
            CascadeVolcano volcano
    ) {
        SplittableRandom random =
                new SplittableRandom(
                        SeedUtil.derive(
                                worldSeed,
                                "cascade-landmark-volcano-structural-ridges"
                        )
                );

        int majorCount =
                6 + random.nextInt(4);

        /*
         * Unequal angular gaps avoid the artificial wagon-wheel look. One
         * deliberately broad gap creates a side of the volcano where valleys
         * and the pre-existing Cascade terrain can dominate instead.
         */
        double[] gapWeights =
                new double[majorCount];

        double totalGapWeight =
                0.0;

        int broadGapIndex =
                random.nextInt(majorCount);

        for (int i = 0; i < majorCount; i++) {
            double weight =
                    range(
                            random,
                            0.65,
                            1.45
                    );

            if (i == broadGapIndex) {
                weight *=
                        range(
                                random,
                                1.55,
                                2.05
                        );
            }

            gapWeights[i] = weight;
            totalGapWeight += weight;
        }

        double angle =
                range(
                        random,
                        -Math.PI,
                        Math.PI
                );

        List<CascadeVolcanicRidge> result =
                new ArrayList<>();

        for (int i = 0; i < majorCount; i++) {
            double gap =
                    Math.PI * 2.0
                            * gapWeights[i]
                            / totalGapWeight;

            angle += gap;

            double baseAngle =
                    angle
                            + range(
                            random,
                            -0.12,
                            0.12
                    );

            double rootRadius =
                    range(
                            random,
                            0.28,
                            0.43
                    );

            double endRadius =
                    range(
                            random,
                            1.30,
                            1.78
                    );

            /*
             * Guarantee a couple of true long buttresses that push well into
             * the surrounding mountain complex rather than ending at the
             * volcano's nominal base.
             */
            if (i == 0 || i == majorCount / 2) {
                endRadius =
                        range(
                                random,
                                1.72,
                                2.02
                        );
            }

            double curvature =
                    range(
                            random,
                            -0.34,
                            0.34
                    );

            double waviness =
                    range(
                            random,
                            0.035,
                            0.105
                    );

            double wavePhase =
                    range(
                            random,
                            -Math.PI,
                            Math.PI
                    );

            double scale =
                    (volcano.radiusAcross()
                            + volcano.radiusAlong())
                            * 0.5;

            CascadeVolcanicRidge major =
                    buildRidge(
                            volcano,
                            random,
                            baseAngle,
                            rootRadius,
                            endRadius,
                            curvature,
                            waviness,
                            wavePhase,
                            scale * range(random, 0.115, 0.175),
                            scale * range(random, 0.055, 0.095),
                            range(random, 0.64, 0.96),
                            false
                    );

            result.add(major);

            /*
             * Some buttresses bifurcate after leaving the upper cone. These
             * daughters are fewer, narrower, and weaker than their parent.
             */
            if (random.nextDouble() < 0.42) {
                double branchStart =
                        range(
                                random,
                                0.58,
                                0.92
                        );

                double branchDirection =
                        random.nextBoolean()
                                ? 1.0
                                : -1.0;

                double branchAngle =
                        baseAngle
                                + curvature * 0.45
                                + branchDirection
                                * range(
                                random,
                                0.24,
                                0.52
                        );

                CascadeVolcanicRidge branch =
                        buildRidge(
                                volcano,
                                random,
                                branchAngle,
                                branchStart,
                                range(
                                        random,
                                        Math.max(branchStart + 0.34, 1.08),
                                        Math.max(branchStart + 0.58, 1.52)
                                ),
                                curvature * 0.45
                                        + range(random, -0.18, 0.18),
                                range(random, 0.025, 0.075),
                                range(random, -Math.PI, Math.PI),
                                scale * range(random, 0.070, 0.115),
                                scale * range(random, 0.040, 0.070),
                                range(random, 0.38, 0.62),
                                true
                        );

                result.add(branch);
            }
        }

        return List.copyOf(result);
    }

    private static CascadeVolcanicRidge buildRidge(
            CascadeVolcano volcano,
            SplittableRandom random,
            double baseAngle,
            double startRadius,
            double endRadius,
            double curvature,
            double waviness,
            double wavePhase,
            double startWidth,
            double endWidth,
            double strength,
            boolean secondary
    ) {
        int nodeCount =
                secondary
                        ? 6 + random.nextInt(3)
                        : 8 + random.nextInt(4);

        List<CascadeVolcanicRidge.Node> nodes =
                new ArrayList<>(nodeCount);

        double rotation =
                volcano.rotationRadians();

        double cosine =
                Math.cos(rotation);

        double sine =
                Math.sin(rotation);

        double minSigned =
                Double.POSITIVE_INFINITY;
        double maxSigned =
                Double.NEGATIVE_INFINITY;
        double minArc =
                Double.POSITIVE_INFINITY;
        double maxArc =
                Double.NEGATIVE_INFINITY;

        for (int i = 0; i < nodeCount; i++) {
            double t =
                    i / (double) (nodeCount - 1);

            double easedT =
                    Math.pow(
                            t,
                            0.94
                    );

            double radius =
                    lerp(
                            startRadius,
                            endRadius,
                            easedT
                    );

            double angle =
                    baseAngle
                            + curvature
                            * smoothstep(
                            0.0,
                            1.0,
                            t
                    )
                            + Math.sin(
                            t * Math.PI
                                    + wavePhase
                    )
                            * waviness
                            * Math.sin(t * Math.PI);

            double localX =
                    Math.cos(angle)
                            * radius;

            double localY =
                    Math.sin(angle)
                            * radius;

            /*
             * Reverse the same volcano-local rotation used by the morphology
             * sampler so path nodes land directly in Cascade-local space.
             */
            double normalizedAcross =
                    localX * cosine
                            - localY * sine;

            double normalizedAlong =
                    localX * sine
                            + localY * cosine;

            double signedDistance =
                    volcano.signedDistance()
                            + normalizedAcross
                            * volcano.radiusAcross();

            double arcPosition =
                    volcano.arcPosition()
                            + normalizedAlong
                            * volcano.radiusAlong();

            nodes.add(
                    new CascadeVolcanicRidge.Node(
                            signedDistance,
                            arcPosition,
                            t
                    )
            );

            minSigned =
                    Math.min(
                            minSigned,
                            signedDistance
                    );
            maxSigned =
                    Math.max(
                            maxSigned,
                            signedDistance
                    );
            minArc =
                    Math.min(
                            minArc,
                            arcPosition
                    );
            maxArc =
                    Math.max(
                            maxArc,
                            arcPosition
                    );
        }

        return new CascadeVolcanicRidge(
                nodes,
                startWidth,
                endWidth,
                strength,
                secondary,
                minSigned,
                maxSigned,
                minArc,
                maxArc,
                Math.max(startWidth, endWidth)
        );
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
        return a + (b - a) * t;
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
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
