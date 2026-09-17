package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

public final class MountainSystem {

    private final MountainSpine spine;

    private final double innerWidth;
    private final double outerWidth;

    private final double maximumUplift;

    private final ValueNoise warpNoise;
    private final ValueNoise ridgeNoise;
    private final ValueNoise reliefNoise;
    private final ValueNoise passNoise;


    public MountainSystem(
            long worldSeed,
            MountainSpine spine,
            double innerWidth,
            double outerWidth,
            double maximumUplift
    ) {

        this.spine =
                spine;

        this.innerWidth =
                innerWidth;

        this.outerWidth =
                outerWidth;

        this.maximumUplift =
                maximumUplift;


        this.warpNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "mountain-warp"
                        )
                );

        this.ridgeNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "mountain-ridges"
                        )
                );

        this.reliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "mountain-relief"
                        )
                );

        this.passNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "mountain-passes"
                        )
                );
    }


    public MountainSpine spine() {
        return spine;
    }


    /*
     * 0 outside the mountain corridor.
     * 1 near the core of the range.
     */
    public double corridorMask(
            double x,
            double z
    ) {

        double warpedX =
                x
                        + warpNoise.fbm(
                        x * 2.3,
                        z * 2.3,
                        3,
                        2.0,
                        0.5
                ) * 0.018;

        double warpedZ =
                z
                        + warpNoise.fbm(
                        x * 2.3 + 71.3,
                        z * 2.3 - 39.2,
                        3,
                        2.0,
                        0.5
                ) * 0.018;


        double distance =
                spine.distanceTo(
                        warpedX,
                        warpedZ
                );


        /*
         * Full strength inside innerWidth.
         *
         * Smoothly fades out by outerWidth.
         */

        return 1.0
                - smoothstep(
                innerWidth,
                outerWidth,
                distance
        );
    }


    /*
     * Actual vertical contribution in Minecraft
     * blocks.
     */
    public double upliftAt(
            double x,
            double z
    ) {

        double mask =
                corridorMask(
                        x,
                        z
                );

        if (mask <= 0.0) {
            return 0.0;
        }


        /*
         * Broad regional mountain variation.
         */
        double regional =
                reliefNoise.fbm(
                        x * 7.0,
                        z * 7.0,
                        4,
                        2.0,
                        0.5
                );

        regional =
                0.75
                        + regional * 0.25;


        /*
         * Ridged component.
         *
         * This is now geographically restricted
         * to the mountain corridor.
         */
        double rawRidge =
                ridgeNoise.fbm(
                        x * 15.0,
                        z * 10.0,
                        5,
                        2.05,
                        0.52
                );

        double ridge =
                1.0
                        - Math.abs(
                        rawRidge
                );

        ridge =
                ridge * ridge;

        ridge =
                0.35
                        + ridge * 0.65;


        /*
         * Passes / low saddles along the range.
         *
         * This prevents the spine from becoming
         * an uninterrupted wall.
         */
        double pass =
                passNoise.fbm(
                        x * 2.0,
                        z * 5.0,
                        3,
                        2.0,
                        0.5
                );

        double passStrength =
                smoothstep(
                        -0.25,
                        0.35,
                        pass
                );

        passStrength =
                0.55
                        + passStrength * 0.45;


        /*
         * Sharpen mountain concentration toward
         * the central part of the corridor.
         */
        double core =
                Math.pow(
                        mask,
                        1.55
                );


        return maximumUplift
                * core
                * regional
                * ridge
                * passStrength;
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
}