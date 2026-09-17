package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Cascade-specific macro morphology.
 *
 * MountainSystem owns where the Cascade corridor exists.  This sampler owns
 * what the terrain looks like inside that corridor:
 *
 *  - a broad asymmetric mountain envelope,
 *  - clustered high crest sections separated by lower saddles,
 *  - secondary ridge spurs projecting away from the crest.
 *
 * Volcanoes and explicit glacial / drainage erosion are intentionally left
 * for later passes so the basic mountain body can be evaluated first.
 */
public final class CascadeMorphologySampler {

    private final MountainSystem cascadeSystem;
    private final double spineLength;

    private final ValueNoise clusterNoise;
    private final ValueNoise crestNoise;
    private final ValueNoise ridgeTextureNoise;
    private final ValueNoise passNoise;

    private final List<RidgeSpur> ridgeSpurs;

    public CascadeMorphologySampler(
            long worldSeed,
            MountainSystem cascadeSystem
    ) {
        this.cascadeSystem = cascadeSystem;
        this.spineLength =
                cascadeSystem.spine()
                        .totalLength();

        long morphologySeed =
                SeedUtil.derive(
                        worldSeed,
                        "cascade-morphology"
                );

        this.clusterNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "clusters"
                        )
                );

        this.crestNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "crest"
                        )
                );

        this.ridgeTextureNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "ridge-texture"
                        )
                );

        this.passNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "passes"
                        )
                );

        this.ridgeSpurs =
                createRidgeSpurs(
                        SeedUtil.derive(
                                morphologySeed,
                                "ridge-spurs"
                        )
                );
    }

    /**
     * Convenience path used by preview tooling.
     */
    public CascadeMorphologySample sample(
            double nx,
            double nz
    ) {
        MountainProjection projection =
                cascadeSystem.projectToSpine(
                        nx,
                        nz
                );

        double corridorMask =
                cascadeSystem.corridorMaskFromDistance(
                        projection.signedDistance()
                );

        return sample(
                nx,
                nz,
                projection.signedDistance(),
                projection.progress(),
                corridorMask
        );
    }

    /**
     * Fast path used by the generator.  PhysiographySampler already knows
     * the closest spine projection, so the expensive polyline scan is not
     * repeated for every blueprint cell.
     */
    public CascadeMorphologySample sample(
            double nx,
            double nz,
            double signedCascadeDistance,
            double cascadeProgress,
            double cascadeMask
    ) {
        if (cascadeMask <= 0.0) {
            return CascadeMorphologySample.empty();
        }

        double absoluteDistance =
                Math.abs(
                        signedCascadeDistance
                );

        double arcPosition =
                clamp01(
                        cascadeProgress
                ) * spineLength;

        /*
         * =========================================================
         * BROAD ASYMMETRIC ENVELOPE
         * =========================================================
         *
         * West side: broader and more gradual.
         * East side: somewhat narrower and steeper.
         */
        double envelope;

        if (signedCascadeDistance >= 0.0) {
            envelope =
                    1.0
                            - smoothstep(
                            0.035,
                            0.180,
                            absoluteDistance
                    );
        } else {
            envelope =
                    1.0
                            - smoothstep(
                            0.032,
                            0.150,
                            absoluteDistance
                    );
        }

        envelope =
                clamp01(
                        envelope
                );

        /*
         * =========================================================
         * BROKEN CREST / MOUNTAIN CLUSTERS
         * =========================================================
         *
         * The low-frequency field runs along the actual sampled spine,
         * producing alternating high mountain groups and lower saddles.
         */
        double clusterSignal =
                clusterNoise.fbm(
                        arcPosition * 3.4 + 17.0,
                        11.0,
                        3,
                        2.0,
                        0.5
                );

        double clusterStrength =
                0.50
                        + 0.50
                        * smoothstep(
                        -0.50,
                        0.48,
                        clusterSignal
                );

        double crestEnvelope =
                1.0
                        - smoothstep(
                        0.018,
                        0.074,
                        absoluteDistance
                );

        double crestDetailSignal =
                crestNoise.fbm(
                        nx * 8.0,
                        nz * 11.0,
                        4,
                        2.0,
                        0.5
                );

        double crestDetail =
                0.84
                        + 0.16
                        * clamp01(
                        crestDetailSignal * 0.5 + 0.5
                );

        double passSignal =
                passNoise.fbm(
                        arcPosition * 5.8 + 43.0,
                        -7.0,
                        3,
                        2.0,
                        0.5
                );

        double passSuppression =
                smoothstep(
                        0.14,
                        0.50,
                        passSignal
                );

        double passMultiplier =
                1.0
                        - passSuppression
                        * 0.44;

        double crestStructure =
                clamp01(
                        crestEnvelope
                                * clusterStrength
                                * crestDetail
                                * passMultiplier
                );

        /*
         * =========================================================
         * SECONDARY RIDGE SPURS
         * =========================================================
         *
         * These are explicit elongated lobes tied to positions along the
         * Cascade spine.  Each ridge extends to only one side, bends as it
         * leaves the crest, and fades before the outer edge.  This avoids
         * the horizontal banding produced by anisotropic noise alone.
         */
        double ridgeRelief =
                ridgeSpurField(
                        signedCascadeDistance,
                        arcPosition
                );

        double ridgeTexture =
                ridgeTextureNoise.fbm(
                        nx * 8.0,
                        nz * 8.0,
                        3,
                        2.0,
                        0.5
                );

        ridgeTexture =
                0.82
                        + 0.18
                        * clamp01(
                        ridgeTexture * 0.5 + 0.5
                );

        ridgeRelief =
                clamp01(
                        ridgeRelief
                                * envelope
                                * ridgeTexture
                                * (0.74 + clusterStrength * 0.26)
                );

        /*
         * =========================================================
         * FINAL UPLIFT
         * =========================================================
         *
         * Keep the existing 190-ish macro vertical budget.  The broad
         * body carries the range continuously, the crest provides the main
         * alpine height, and the ridge spurs add structured relief.
         */
        double maximumUplift =
                cascadeSystem.maximumUplift();

        double broadUplift =
                maximumUplift
                        * 0.23
                        * envelope;

        double crestUplift =
                maximumUplift
                        * 0.63
                        * crestStructure;

        double ridgeUplift =
                maximumUplift
                        * 0.14
                        * ridgeRelief;

        double uplift =
                Math.min(
                        maximumUplift,
                        broadUplift
                                + crestUplift
                                + ridgeUplift
                );

        return new CascadeMorphologySample(
                envelope,
                crestStructure,
                ridgeRelief,
                passSuppression,
                uplift
        );
    }

    private double ridgeSpurField(
            double signedDistance,
            double arcPosition
    ) {
        double result =
                0.0;

        for (RidgeSpur spur : ridgeSpurs) {

            double outwardDistance =
                    signedDistance
                            * spur.side();

            if (
                    outwardDistance < -0.020
                            || outwardDistance > spur.length()
            ) {
                continue;
            }

            double outwardT =
                    clamp01(
                            Math.max(
                                    0.0,
                                    outwardDistance
                            )
                                    / spur.length()
                    );

            double bentCenter =
                    spur.centerArc()
                            + spur.bend()
                            * outwardDistance
                            + spur.curve()
                            * outwardDistance
                            * outwardDistance
                            * spur.side();

            double width =
                    spur.width()
                            * lerp(
                            0.78,
                            1.30,
                            outwardT
                    );

            double alongDistance =
                    Math.abs(
                            arcPosition
                                    - bentCenter
                    );

            double alongEnvelope =
                    1.0
                            - smoothstep(
                            width * 0.28,
                            width,
                            alongDistance
                    );

            double innerConnection =
                    smoothstep(
                            -0.020,
                            0.008,
                            outwardDistance
                    );

            double outerFade =
                    1.0
                            - smoothstep(
                            spur.length() * 0.70,
                            spur.length(),
                            Math.max(
                                    0.0,
                                    outwardDistance
                            )
                    );

            double contribution =
                    spur.strength()
                            * alongEnvelope
                            * innerConnection
                            * outerFade;

            result =
                    Math.max(
                            result,
                            contribution
                    );
        }

        return clamp01(
                result
        );
    }

    private List<RidgeSpur> createRidgeSpurs(
            long seed
    ) {
        SplittableRandom random =
                new SplittableRandom(
                        seed
                );

        List<RidgeSpur> result =
                new ArrayList<>();

        /*
         * Generate both west- and east-projecting ridges so neither flank
         * becomes featureless.  Quasi-even placement prevents accidental
         * giant empty sections while jitter keeps the pattern non-periodic.
         */
        int ridgesPerSide =
                13;

        for (int sideIndex = 0; sideIndex < 2; sideIndex++) {

            double side =
                    sideIndex == 0
                            ? 1.0
                            : -1.0;

            for (int i = 0; i < ridgesPerSide; i++) {

                double progress =
                        (
                                i
                                        + 0.20
                                        + random.nextDouble() * 0.60
                        ) / ridgesPerSide;

                double centerArc =
                        progress
                                * spineLength;

                boolean major =
                        random.nextDouble() < 0.24;

                double length;

                if (side > 0.0) {
                    length =
                            major
                                    ? range(
                                    random,
                                    0.125,
                                    0.170
                            )
                                    : range(
                                    random,
                                    0.080,
                                    0.135
                            );
                } else {
                    length =
                            major
                                    ? range(
                                    random,
                                    0.100,
                                    0.140
                            )
                                    : range(
                                    random,
                                    0.065,
                                    0.112
                            );
                }

                double width =
                        major
                                ? range(
                                random,
                                0.020,
                                0.034
                        )
                                : range(
                                random,
                                0.012,
                                0.026
                        );

                double strength =
                        major
                                ? range(
                                random,
                                0.72,
                                1.00
                        )
                                : range(
                                random,
                                0.46,
                                0.82
                        );

                result.add(
                        new RidgeSpur(
                                centerArc,
                                side,
                                length,
                                width,
                                strength,
                                range(
                                        random,
                                        -0.32,
                                        0.32
                                ),
                                range(
                                        random,
                                        -0.90,
                                        0.90
                                )
                        )
                );
            }
        }

        return List.copyOf(
                result
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
        return a
                + (b - a) * t;
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double t =
                (value - edge0)
                        / (edge1 - edge0);

        t = clamp01(t);

        return t
                * t
                * (3.0 - 2.0 * t);
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

    private record RidgeSpur(
            double centerArc,
            double side,
            double length,
            double width,
            double strength,
            double bend,
            double curve
    ) {
    }
}
