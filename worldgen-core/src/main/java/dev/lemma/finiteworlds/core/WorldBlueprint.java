package dev.lemma.finiteworlds.core;

public final class WorldBlueprint {

    private final WorldConfig config;

    private final float[] landMask;
    private final float[] elevation;
    private final float[] coastDistance;

    public WorldBlueprint(WorldConfig config) {
        this.config = config;

        int count = config.blueprintResolution() * config.blueprintResolution();

        this.landMask = new float[count];
        this.elevation = new float[count];
        this.coastDistance = new float[count];
    }

    public WorldConfig config() {
        return config;
    }

    public int resolution() {
        return config.blueprintResolution();
    }

    private int index(int x, int z) {
        return z * resolution() + x;
    }

    public float landMask(int x, int z) {
        return landMask[index(x, z)];
    }

    public void setLandMask(int x, int z, float value) {
        landMask[index(x, z)] = value;
    }

    public float elevation(int x, int z) {
        return elevation[index(x, z)];
    }

    public void setElevation(int x, int z, float value) {
        elevation[index(x, z)] = value;
    }

    public float coastDistance(
            int x,
            int z
    ) {
        return coastDistance[
                index(x, z)
                ];
    }

    public void setCoastDistance(
            int x,
            int z,
            float value
    ) {
        coastDistance[
                index(x, z)
                ] = value;
    }

    public float elevationAtBlock(
            double blockX,
            double blockZ
    ) {
        double halfWorld =
                config.worldSizeBlocks() / 2.0;

        double u =
                (blockX + halfWorld)
                        / config.worldSizeBlocks();

        double v =
                (blockZ + halfWorld)
                        / config.worldSizeBlocks();

        /*
         * Outside our finite planned world:
         * deep ocean.
         */

        if (
                u < 0.0
                        || u > 1.0
                        || v < 0.0
                        || v > 1.0
        ) {
            return -180;
        }

        double px =
                u * (resolution() - 1);

        double pz =
                v * (resolution() - 1);

        int x0 =
                (int) Math.floor(px);

        int z0 =
                (int) Math.floor(pz);

        int x1 =
                Math.min(
                        x0 + 1,
                        resolution() - 1
                );

        int z1 =
                Math.min(
                        z0 + 1,
                        resolution() - 1
                );

        double tx = px - x0;
        double tz = pz - z0;

        double a =
                elevation(x0, z0);

        double b =
                elevation(x1, z0);

        double c =
                elevation(x0, z1);

        double d =
                elevation(x1, z1);

        double top =
                a + (b - a) * tx;

        double bottom =
                c + (d - c) * tx;

        return (float) (
                top
                        + (bottom - top)
                        * tz
        );
    }

    public float smoothElevationAtBlock(
            double blockX,
            double blockZ
    ) {

        double halfWorld =
                config.worldSizeBlocks() / 2.0;

        double u =
                (blockX + halfWorld)
                        / config.worldSizeBlocks();

        double v =
                (blockZ + halfWorld)
                        / config.worldSizeBlocks();

        if (
                u < 0.0
                        || u > 1.0
                        || v < 0.0
                        || v > 1.0
        ) {
            return -180.0f;
        }

        double px =
                u * (resolution() - 1);

        double pz =
                v * (resolution() - 1);

        int baseX =
                (int) Math.floor(px);

        int baseZ =
                (int) Math.floor(pz);

        double tx =
                px - baseX;

        double tz =
                pz - baseZ;

        double[] rows =
                new double[4];

        for (int dz = -1; dz <= 2; dz++) {

            double p0 =
                    elevationClamped(
                            baseX - 1,
                            baseZ + dz
                    );

            double p1 =
                    elevationClamped(
                            baseX,
                            baseZ + dz
                    );

            double p2 =
                    elevationClamped(
                            baseX + 1,
                            baseZ + dz
                    );

            double p3 =
                    elevationClamped(
                            baseX + 2,
                            baseZ + dz
                    );

            rows[dz + 1] =
                    catmullRom(
                            p0,
                            p1,
                            p2,
                            p3,
                            tx
                    );
        }

        double result =
                catmullRom(
                        rows[0],
                        rows[1],
                        rows[2],
                        rows[3],
                        tz
                );

        /*
         * Catmull-Rom can overshoot slightly.
         * Clamp to a safe world range for now.
         */
        result =
                Math.max(
                        -256.0,
                        Math.min(
                                512.0,
                                result
                        )
                );

        return (float) result;
    }

    private float elevationClamped(
            int x,
            int z
    ) {

        x =
                Math.max(
                        0,
                        Math.min(
                                resolution() - 1,
                                x
                        )
                );

        z =
                Math.max(
                        0,
                        Math.min(
                                resolution() - 1,
                                z
                        )
                );

        return elevation(x, z);
    }

    private static double catmullRom(
            double p0,
            double p1,
            double p2,
            double p3,
            double t
    ) {

        double t2 =
                t * t;

        double t3 =
                t2 * t;

        return 0.5 * (
                (2.0 * p1)
                        + (-p0 + p2) * t
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
}