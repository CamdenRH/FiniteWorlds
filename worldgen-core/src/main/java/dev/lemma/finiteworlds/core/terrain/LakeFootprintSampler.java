package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.hydrology.HydrologyGrid;
import dev.lemma.finiteworlds.core.hydrology.Lake;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

/** Shared shoreline geometry for physical water and safe river meander planning. */
public final class LakeFootprintSampler {
  private final WorldBlueprint blueprint;
  private final ValueNoise shorelineNoise;

  public LakeFootprintSampler(WorldBlueprint blueprint, long seed) {
    this.blueprint = blueprint;
    this.shorelineNoise = new ValueNoise(SeedUtil.derive(seed, "lake-shoreline"));
  }

  public Surface sample(double x, double z) {
    HydrologyGrid hydrology = blueprint.hydrology();
    if (hydrology.lakeCount() == 0) {
      return null;
    }
    // Smooth coordinate distortion removes the underlying 64-block lattice
    // from lake shores while preserving one coherent lake level and ID.
    double unwarpedX = x, unwarpedZ = z;
    x +=
        18 * shorelineNoise.fbm(unwarpedX / 90, unwarpedZ / 90, 2, 2, .5)
            + 40 * shorelineNoise.fbm(unwarpedX / 170, unwarpedZ / 170, 3, 2, .5)
            + 80 * shorelineNoise.fbm(unwarpedX / 780, unwarpedZ / 780, 3, 2, .5);
    z +=
        18 * shorelineNoise.fbm(unwarpedX / 90 + 37, unwarpedZ / 90 - 51, 2, 2, .5)
            + 40 * shorelineNoise.fbm(unwarpedX / 170 + 37, unwarpedZ / 170 - 51, 3, 2, .5)
            + 80 * shorelineNoise.fbm(unwarpedX / 780 + 37, unwarpedZ / 780 - 51, 3, 2, .5);
    double halfWorld = blueprint.config().worldSizeBlocks() / 2.0;
    double px =
        (x + halfWorld) / blueprint.config().worldSizeBlocks() * (blueprint.resolution() - 1);
    double pz =
        (z + halfWorld) / blueprint.config().worldSizeBlocks() * (blueprint.resolution() - 1);
    if (px < 0.0
        || pz < 0.0
        || px > blueprint.resolution() - 1
        || pz > blueprint.resolution() - 1) {
      return null;
    }
    int x0 = (int) Math.floor(px);
    int z0 = (int) Math.floor(pz);
    double tx = px - x0;
    double tz = pz - z0;
    int bestId = -1;
    double bestWeight = 0.0;
    // Four corners, with repeated edge cells harmlessly summed.
    for (int corner = 0; corner < 4; corner++) {
      int cx = Math.min(x0 + (corner & 1), blueprint.resolution() - 1);
      int cz = Math.min(z0 + (corner >> 1), blueprint.resolution() - 1);
      int id = hydrology.lakeId(cx, cz);
      if (id < 0) {
        continue;
      }
      double support = 0.0;
      for (int other = 0; other < 4; other++) {
        int ox = Math.min(x0 + (other & 1), blueprint.resolution() - 1);
        int oz = Math.min(z0 + (other >> 1), blueprint.resolution() - 1);
        if (hydrology.lakeId(ox, oz) == id) {
          support += cornerWeight(other, tx, tz);
        }
      }
      if (support > bestWeight) {
        bestWeight = support;
        bestId = id;
      }
    }
    Lake lake = hydrology.lake(bestId);
    if (lake == null || bestWeight <= 0.0) {
      return null;
    }
    double depth = 0.0;
    for (int corner = 0; corner < 4; corner++) {
      int cx = Math.min(x0 + (corner & 1), blueprint.resolution() - 1);
      int cz = Math.min(z0 + (corner >> 1), blueprint.resolution() - 1);
      if (hydrology.lakeId(cx, cz) == bestId) {
        depth += cornerWeight(corner, tx, tz) * hydrology.lakeDepth(cx, cz);
      }
    }
    // Interpolate inundated depth and taper the edge into a beach rather
    // than painting the lake footprint as a set of square macro cells.
    depth *= smoothstep(0.45, 0.90, bestWeight);
    return new Surface(bestId, lake.waterSurfaceElevation(), depth, bestWeight);
  }

  private static double cornerWeight(int corner, double tx, double tz) {
    return ((corner & 1) == 0 ? 1 - tx : tx) * ((corner >> 1) == 0 ? 1 - tz : tz);
  }

  private static double smoothstep(double low, double high, double value) {
    double t = Math.max(0, Math.min(1, (value - low) / (high - low)));
    return t * t * (3 - 2 * t);
  }

  public record Surface(int id, double water, double depth, double support) {}
}
