package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

/** Continuous local relief beneath the macro terrain and physical river corridors. */
public final class LocalTerrainSampler {
    private final int seaLevel;
    private final WorldBlueprint blueprint;
    private final ValueNoise broadNoise;
    private final ValueNoise hillNoise;
    private final ValueNoise ridgeNoise;
    private final ValueNoise valleyNoise;
    private final ValueNoise warpNoise;
    private final ValueNoise fineNoise;

    public LocalTerrainSampler(long worldSeed, int seaLevel) {
        this(worldSeed, seaLevel, null);
    }

    public LocalTerrainSampler(long worldSeed, WorldBlueprint blueprint) {
        this(worldSeed, blueprint.config().seaLevel(), blueprint);
    }

    private LocalTerrainSampler(long worldSeed, int seaLevel, WorldBlueprint blueprint) {
        this.seaLevel = seaLevel;
        this.blueprint = blueprint;
        this.broadNoise = new ValueNoise(SeedUtil.derive(worldSeed, "local-broad"));
        this.hillNoise = new ValueNoise(SeedUtil.derive(worldSeed, "local-hills"));
        this.ridgeNoise = new ValueNoise(SeedUtil.derive(worldSeed, "local-ridges"));
        this.valleyNoise = new ValueNoise(SeedUtil.derive(worldSeed, "local-erosional-valleys"));
        this.warpNoise = new ValueNoise(SeedUtil.derive(worldSeed, "local-relief-warp"));
        this.fineNoise = new ValueNoise(SeedUtil.derive(worldSeed, "local-fine"));
    }

    public double sample(double x, double z, double macroElevation) {
        if (macroElevation >= 1350.0) {
            return 0.0;
        }
        double mountainStrength = mountainStrength(x, z, macroElevation);
        // Keep the calibrated volcanic summit inside the height envelope. Its
        // upper cone and crater already have their own explicit morphology.
        mountainStrength *= 1.0 - smoothstep(1200.0, 1350.0, macroElevation);

        double broad = broadNoise.fbm(x / 520.0, z / 520.0, 4, 2.0, 0.5) * 10.0;
        double hills = hillNoise.fbm(x / 180.0, z / 180.0, 4, 2.0, 0.5) * 6.0;
        double fine = fineNoise.fbm(x / 70.0, z / 70.0, 3, 2.0, 0.5) * 2.5;

        double mountainRelief = 0.0;
        if (mountainStrength > 0.0) {
            // Coherent domain warping bends crest lines and gullies over hundreds
            // of blocks, rather than adding unrelated spikes to every column.
            double warpedX = x + warpNoise.fbm(x / 850.0, z / 850.0, 2, 2.0, 0.5) * 105.0;
            double warpedZ = z + warpNoise.fbm(x / 850.0 + 43.0, z / 850.0 - 27.0, 2, 2.0, 0.5) * 105.0;
            double ridges = ridgedRelief(warpedX / 340.0, warpedZ / 340.0);
            double valleySignal = valleyNoise.fbm(warpedX / 620.0, warpedZ / 620.0, 3, 2.0, 0.5);
            double gully = 1.0 - smoothstep(0.025, 0.19, Math.abs(valleySignal));
            double rockDetail = fineNoise.fbm(warpedX / 45.0, warpedZ / 45.0, 3, 2.0, 0.5) * 8.0;

            // Valleys remove material between ridges. Amplitude grows through
            // foothills into high mountains; it never affects flat plateaus.
            double reliefScale = 44.0 + 36.0 * mountainStrength;
            mountainRelief = ((ridges - 0.40) * reliefScale
                    - gully * (20.0 + 28.0 * mountainStrength)
                    + rockDetail) * mountainStrength;
        }

        double coastProtection = smoothstep(3.0, 28.0, Math.abs(macroElevation - seaLevel));
        double terrainStrength = macroElevation >= seaLevel ? 1.0 : 0.35;
        double summitProtection = 1.0 - smoothstep(1250.0, 1350.0, macroElevation);
        return (broad + hills + fine + mountainRelief) * terrainStrength
                * (0.20 + 0.80 * coastProtection) * summitProtection;
    }

    private double ridgedRelief(double x, double z) {
        double result = 0.0;
        double amplitude = 1.0;
        double normalization = 0.0;
        for (int octave = 0; octave < 4; octave++) {
            double ridge = 1.0 - Math.abs(ridgeNoise.sample(x, z));
            result += ridge * ridge * ridge * amplitude;
            normalization += amplitude;
            x = x * 2.0 + 17.0;
            z = z * 2.0 - 11.0;
            amplitude *= 0.48;
        }
        return result / normalization;
    }

    private double mountainStrength(double x, double z, double macroElevation) {
        if (blueprint == null) {
            return smoothstep(seaLevel + 50.0, seaLevel + 200.0, macroElevation);
        }
        double size = blueprint.resolution();
        double px = (x / blueprint.config().worldSizeBlocks() + 0.5) * (size - 1.0);
        double pz = (z / blueprint.config().worldSizeBlocks() + 0.5) * (size - 1.0);
        if (px < 0.0 || pz < 0.0 || px > size - 1.0 || pz > size - 1.0) {
            return 0.0;
        }
        int x0 = (int) Math.floor(px);
        int z0 = (int) Math.floor(pz);
        int x1 = Math.min(x0 + 1, blueprint.resolution() - 1);
        int z1 = Math.min(z0 + 1, blueprint.resolution() - 1);
        double tx = px - x0;
        double tz = pz - z0;
        double a = mountainUplift(x0, z0);
        double b = mountainUplift(x1, z0);
        double c = mountainUplift(x0, z1);
        double d = mountainUplift(x1, z1);
        double uplift = (a + (b - a) * tx) * (1.0 - tz) + (c + (d - c) * tx) * tz;
        return smoothstep(12.0, 145.0, uplift);
    }

    private double mountainUplift(int x, int z) {
        return blueprint.cascadeUplift(x, z) + blueprint.coastRangeUplift(x, z);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }
}
