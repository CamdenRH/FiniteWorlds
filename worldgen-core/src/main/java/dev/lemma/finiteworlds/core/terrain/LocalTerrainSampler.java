package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

public final class LocalTerrainSampler {

    private final int seaLevel;

    private final ValueNoise broadNoise;
    private final ValueNoise hillNoise;
    private final ValueNoise ridgeNoise;
    private final ValueNoise fineNoise;

    public LocalTerrainSampler(
            long worldSeed,
            int seaLevel
    ) {
        this.seaLevel =
                seaLevel;

        this.broadNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "local-broad"
                        )
                );

        this.hillNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "local-hills"
                        )
                );

        this.ridgeNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "local-ridges"
                        )
                );

        this.fineNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "local-fine"
                        )
                );
    }

    public double sample(
            double x,
            double z,
            double macroElevation
    ) {

        /*
         * Large local undulation.
         *
         * Wavelength: hundreds of blocks.
         */

        double broad =
                broadNoise.fbm(
                        x / 520.0,
                        z / 520.0,
                        4,
                        2.0,
                        0.5
                ) * 10.0;


        /*
         * Ordinary rolling terrain.
         */

        double hills =
                hillNoise.fbm(
                        x / 180.0,
                        z / 180.0,
                        4,
                        2.0,
                        0.5
                ) * 6.0;


        /*
         * Ridge-shaped noise.
         */

        double ridgeBase =
                ridgeNoise.fbm(
                        x / 280.0,
                        z / 280.0,
                        4,
                        2.05,
                        0.52
                );

        double ridge =
                1.0
                        - Math.abs(
                        ridgeBase
                );

        ridge *= ridge;

        ridge =
                (ridge - 0.45)
                        * 0.0;


        /*
         * Small detail.
         */

        double fine =
                fineNoise.fbm(
                        x / 70.0,
                        z / 70.0,
                        3,
                        2.0,
                        0.5
                ) * 2.5;


        double combined =
                broad
                        + hills
                        + ridge
                        + fine;


        /*
         * Protect the shoreline.
         *
         * We don't yet want local noise
         * completely redesigning the global
         * coastline.
         */

        double distanceFromSea =
                Math.abs(
                        macroElevation
                                - seaLevel
                );

        double coastProtection =
                smoothstep(
                        3.0,
                        28.0,
                        distanceFromSea
                );


        /*
         * Ocean floor gets less vertical
         * detail than land for now.
         */

        double terrainStrength =
                macroElevation >= seaLevel
                        ? 1.0
                        : 0.35;


        /*
         * Never completely eliminate detail
         * near the coast, just suppress it.
         */

        double coastStrength =
                0.20
                        + 0.80
                        * coastProtection;


        return combined
                * terrainStrength
                * coastStrength;
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