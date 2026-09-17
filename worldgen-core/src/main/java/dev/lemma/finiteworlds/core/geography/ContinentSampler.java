package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

public final class ContinentSampler {

    private final ContinentPlan plan;


    /*
     * Domain deformation.
     */
    private final ValueNoise macroWarp;
    private final ValueNoise secondaryWarp;


    /*
     * Coastline displacement at several scales.
     */
    private final ValueNoise macroCoast;
    private final ValueNoise mediumCoast;
    private final ValueNoise fineCoast;
    private final ValueNoise microCoast;


    public ContinentSampler(
            long worldSeed,
            ContinentPlan plan
    ) {

        this.plan =
                plan;


        this.macroWarp =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "geography-macro-warp"
                        )
                );


        this.secondaryWarp =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "geography-secondary-warp"
                        )
                );


        this.macroCoast =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "coast-macro"
                        )
                );


        this.mediumCoast =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "coast-medium"
                        )
                );


        this.fineCoast =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "coast-fine"
                        )
                );

        this.microCoast =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "coast-micro"
                        )
                );
    }


    public double sample(
            double x,
            double z
    ) {

        /*
         * =====================================================
         * LARGE-SCALE DOMAIN WARP
         * =====================================================
         *
         * This bends the planned shapes themselves.
         *
         * A geometric peninsula therefore stops looking
         * like an ellipse before we even apply coastline
         * noise.
         */

        double macroWarpX =
                macroWarp.fbm(
                        x * 1.15,
                        z * 1.15,
                        4,
                        2.0,
                        0.5
                ) * 0.055;


        double macroWarpZ =
                macroWarp.fbm(
                        x * 1.15 + 83.1,
                        z * 1.15 - 37.4,
                        4,
                        2.0,
                        0.5
                ) * 0.035;


        double wx =
                x + macroWarpX;

        double wz =
                z + macroWarpZ;


        /*
         * =====================================================
         * SECONDARY DOMAIN WARP
         * =====================================================
         *
         * Adds medium-scale bends that the first warp
         * cannot produce.
         */

        double secondaryWarpX =
                secondaryWarp.fbm(
                        wx * 4.0,
                        wz * 4.0,
                        3,
                        2.0,
                        0.5
                ) * 0.018;


        double secondaryWarpZ =
                secondaryWarp.fbm(
                        wx * 4.0 + 124.5,
                        wz * 4.0 - 18.7,
                        3,
                        2.0,
                        0.5
                ) * 0.018;


        wx +=
                secondaryWarpX;

        wz +=
                secondaryWarpZ;


        /*
         * =====================================================
         * DESIGNED GEOGRAPHIC BASE
         * =====================================================
         */

        double base =
                plan.sampleBase(
                        wx,
                        wz
                );


        /*
         * =====================================================
         * COASTLINE INFLUENCE MASK
         * =====================================================
         *
         * Only distort land boundaries.
         *
         * Deep inland geography remains stable.
         */

        double distanceFromBoundary =
                Math.abs(
                        base
                );


        double coastWeight =
                1.0
                        - smoothstep(
                        0.035,
                        0.30,
                        distanceFromBoundary
                );


        /*
         * =====================================================
         * MACRO COASTLINE
         * =====================================================
         *
         * Produces major headlands, embayments and broad
         * irregularity.
         */

        double macro =
                macroCoast.fbm(
                        wx * 4.0,
                        wz * 4.0,
                        4,
                        2.0,
                        0.52
                ) * 0.050;


        /*
         * =====================================================
         * MEDIUM COASTLINE
         * =====================================================
         *
         * Produces bays, peninsulas and irregular coastal
         * segments at the next scale down.
         */

        double medium =
                mediumCoast.fbm(
                        wx * 13.0,
                        wz * 13.0,
                        4,
                        2.05,
                        0.53
                ) * 0.026;


        /*
         * =====================================================
         * FINE COASTLINE
         * =====================================================
         *
         * Small irregularities.
         *
         * Keep this weak or we'll create confetti islands.
         */

        double fine =
                fineCoast.fbm(
                        wx * 40.0,
                        wz * 40.0,
                        3,
                        2.1,
                        0.5
                ) * 0.009;

        /*
         * =====================================================
         * MICRO COASTLINE
         * =====================================================
         *
         * Detailed irregularities.
         *
         * Keep this weak or we'll create confetti islands.
         */

        double micro =
                microCoast.fbm(
                        wx * 90.0,
                        wz * 90.0,
                        2,
                        2.0,
                        0.5
                ) * 0.004;


        /*
         * Macro effects should extend slightly farther from
         * the exact zero-crossing than fine effects.
         */

        double macroWeight =
                1.0
                        - smoothstep(
                        0.06,
                        0.40,
                        distanceFromBoundary
                );


        double fineWeight =
                1.0
                        - smoothstep(
                        0.015,
                        0.12,
                        distanceFromBoundary
                );

        double microWeight =
                1.0
                        - smoothstep(
                        0.005,
                        0.055,
                        distanceFromBoundary
                );

        return base
                + macro
                * macroWeight
                + medium
                * coastWeight
                + fine
                * fineWeight
                + micro
                + microWeight;
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
}