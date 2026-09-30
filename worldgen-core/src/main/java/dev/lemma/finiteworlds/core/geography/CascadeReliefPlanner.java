package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

/** Raises dissected Cascade ridges and foothills without turning the range into a capped slab. */
public final class CascadeReliefPlanner {
    private CascadeReliefPlanner() {}

    public static void apply(long seed, WorldBlueprint world, float[] volcanic) {
        ValueNoise ridge = new ValueNoise(SeedUtil.derive(seed, "cascades-alpine-ridges"));
        ValueNoise warp = new ValueNoise(SeedUtil.derive(seed, "cascades-valley-warp"));
        ValueNoise hills = new ValueNoise(SeedUtil.derive(seed, "regional-drainage-relief"));
        int n = world.resolution();
        double spacing = world.config().worldSizeBlocks() / (n - 1.0);
        for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
            double inland = smooth(180, 1000, world.coastDistance(x, z));
            if (inland == 0) continue;
            double bx = x * spacing, bz = z * spacing;
            double wx = bx + 420 * warp.fbm(bx / 2300, bz / 2300, 3, 2, .5);
            double wz = bz + 420 * warp.fbm(bx / 2300 + 41, bz / 2300 - 29, 3, 2, .5);
            double r = Math.pow(Math.max(0, 1 - Math.abs(ridge.fbm(wx / 1100, wz / 1100, 4, 2.05, .52)) * 1.65), 3.2);
            double original = Math.max(0, world.cascadeUplift(x,z) - volcanic[z*n+x]);
            double support = smooth(4, 125, original);
            double mountain = original * 2.55 + support * (95 + 430 * r);
            double coastal = world.coastRangeUplift(x,z);
            double coastSupport = smooth(5, 90, coastal);
            double coastalAdded = coastal * .55 + coastSupport * 95 * r;
            // Broad variation breaks the parallel D8 drainage on the old near-planar lowlands.
            double lowland = (hills.fbm(wx / 1900, wz / 1900, 4, 2, .5) * 48
                    + hills.fbm(wx / 620, wz / 620, 3, 2, .5) * 16) * inland;
            double added = (mountain-original + coastalAdded) * inland + lowland;
            world.setElevation(x,z, (float)(world.elevation(x,z)+added));
            world.setCascadeUplift(x,z, (float)(world.cascadeUplift(x,z)+(mountain-original)*inland));
            world.setCoastRangeUplift(x,z, (float)(coastal+coastalAdded*inland));
        }
    }
    private static double smooth(double a,double b,double v) {
        double t=Math.max(0,Math.min(1,(v-a)/(b-a))); return t*t*(3-2*t);
    }
}
