package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;

/**
 * Carry actual touching channel levels through the graph before the final terrain is materialized.
 */
public final class RiverJunctionWaterPlanner {
  private RiverJunctionWaterPlanner() {}

  public static boolean condition(WorldBlueprint world, long seed) {
    var h = world.hydrology();
    double[] ceilings = new double[h.riverNodes().size()];
    for (int i = 0; i < ceilings.length; i++) ceilings[i] = h.riverNodeWaterCeiling(i);
    var terrain = new TerrainSampler(world, seed);
    boolean changed = false;
    for (var section : h.riverSegmentCrossSections()) {
      if (section.lakePassage() || section.points().isEmpty()) continue;
      var segment = h.riverSegments().get(section.segmentId());
      int[] nodes = {segment.startNodeId(), segment.endNodeId()};
      for (int endpoint = 0; endpoint < 2; endpoint++) {
        var point = endpoint == 0 ? section.points().getFirst() : section.points().getLast();
        var column = terrain.sampleColumn(point.blockX(), point.blockZ());
        if (column.hasWater()
            && column.waterSurfaceElevation() < ceilings[nodes[endpoint]] - .001) {
          ceilings[nodes[endpoint]] = column.waterSurfaceElevation();
          changed = true;
        }
      }
    }
    if (changed) {
      h.setRiverNodeWaterCeilings(ceilings);
      LakeConnectionLevelPlanner.plan(world, seed);
    }
    return changed;
  }
}
