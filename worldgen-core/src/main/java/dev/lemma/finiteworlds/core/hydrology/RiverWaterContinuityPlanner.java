package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.terrain.LakeFootprintSampler;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;
import java.util.ArrayList;

/** Resolve early physical merges and lake contacts that lie between macro graph nodes. */
public final class RiverWaterContinuityPlanner {
  private RiverWaterContinuityPlanner() {}

  public static void condition(WorldBlueprint world, long seed) {
    var h = world.hydrology();
    var footprints = new LakeFootprintSampler(world, seed);
    for (int pass = 0; pass < 8; pass++) {
      var sampler = new TerrainSampler(world, seed);
      double[] lakeLevels = h.lakes().stream().mapToDouble(Lake::waterSurfaceElevation).toArray();
      var profiles = new ArrayList<RiverWaterSurfaceProfile>();
      boolean changed = false;
      for (var section : h.riverSegmentCrossSections()) {
        double[] water = new double[section.points().size()];
        double previous = Double.POSITIVE_INFINITY;
        for (int i = 0; i < water.length; i++) {
          var p = section.points().get(i);
          var column = sampler.sampleColumn(p.blockX(), p.blockZ());
          double actual =
              column.hasWater() ? column.waterSurfaceElevation() : Double.POSITIVE_INFINITY;
          water[i] = Math.min(previous, actual);
          if (actual > water[i] + .001) changed = true;
          // A channel may join a lake before its designated graph endpoint.
          // Keep the whole lake level coherent with that feeding water.
          if (column.lake()) {
            var lake = footprints.sample(p.blockX(), p.blockZ());
            if (lake != null) lakeLevels[lake.id()] = Math.min(lakeLevels[lake.id()], water[i]);
          }
          previous = water[i];
        }
        profiles.add(new RiverWaterSurfaceProfile(section.segmentId(), water));
      }
      h.setRiverWaterSurfaceProfiles(profiles);
      var lakes = new ArrayList<Lake>();
      boolean lakeChanged = false;
      for (var lake : h.lakes()) {
        var adjusted = lake.lowerWaterSurfaceTo(lakeLevels[lake.id()]);
        lakes.add(adjusted);
        lakeChanged |= adjusted != lake;
      }
      h.setLakes(lakes);
      if (lakeChanged) LakeConnectionLevelPlanner.plan(world, seed);
      changed |= lakeChanged;
      if (!changed) break;
    }
  }
}
