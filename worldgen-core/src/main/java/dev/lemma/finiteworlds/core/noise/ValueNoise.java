package dev.lemma.finiteworlds.core.noise;

import dev.lemma.finiteworlds.core.SeedUtil;

public final class ValueNoise {

    private final long seed;

    public ValueNoise(long seed) {
        this.seed = seed;
    }

    public double sample(double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);

        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = x - x0;
        double tz = z - z0;

        tx = smooth(tx);
        tz = smooth(tz);

        double a = randomValue(x0, z0);
        double b = randomValue(x1, z0);
        double c = randomValue(x0, z1);
        double d = randomValue(x1, z1);

        double ab = lerp(a, b, tx);
        double cd = lerp(c, d, tx);

        return lerp(ab, cd, tz);
    }

    public double fbm(
            double x,
            double z,
            int octaves,
            double lacunarity,
            double persistence
    ) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxAmplitude = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += sample(
                    x * frequency,
                    z * frequency
            ) * amplitude;

            maxAmplitude += amplitude;

            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total / maxAmplitude;
    }

    private double randomValue(int x, int z) {
        long h = seed;

        h ^= (long) x * 0x632BE59BD9B4E019L;
        h ^= (long) z * 0x9E3779B97F4A7C15L;

        h = SeedUtil.mix64(h);

        long bits = h >>> 11;

        double normalized =
                bits * 0x1.0p-53;

        return normalized * 2.0 - 1.0;
    }

    private static int fastFloor(double value) {
        int i = (int) value;

        return value < i ? i - 1 : i;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a + (b - a) * t;
    }
}