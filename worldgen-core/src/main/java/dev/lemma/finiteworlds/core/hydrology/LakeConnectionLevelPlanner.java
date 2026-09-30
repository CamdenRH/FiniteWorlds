package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.terrain.LakeFootprintSampler;
import java.util.*;

/** Breach incompatible downstream lake levels to the water ceiling of their feeding catchments. */
public final class LakeConnectionLevelPlanner {
  private LakeConnectionLevelPlanner() {}

  public static void plan(WorldBlueprint world, long seed) {
    var h = world.hydrology();
    if (h.riverSegments().isEmpty()) return;
    var footprints = new LakeFootprintSampler(world, seed);
    int[] nodeLakes = new int[h.riverNodes().size()];
    Arrays.fill(nodeLakes, -1);
    double spacing = world.config().worldSizeBlocks() / (world.resolution() - 1.0);
    for (var node : h.riverNodes()) {
      var lake =
          footprints.sample(
              node.x() * spacing - world.config().worldSizeBlocks() / 2.0,
              node.z() * spacing - world.config().worldSizeBlocks() / 2.0);
      if (lake != null && lake.support() > .35) nodeLakes[node.id()] = lake.id();
    }
    double[] levels = h.lakes().stream().mapToDouble(Lake::waterSurfaceElevation).toArray();
    var ordered = new ArrayList<>(h.riverSegments());
    ordered.sort(
        Comparator.comparingDouble(
            s -> h.riverSegmentHierarchy().get(s.id()).longestSourceDistanceBlocks()));
    double[] physicalCeilings = new double[h.riverNodes().size()];
    for (int i = 0; i < physicalCeilings.length; i++)
      physicalCeilings[i] = h.riverNodeWaterCeiling(i);
    double[] finalCeiling = new double[h.riverNodes().size()];
    // Lower levels only. Repeating topological sweeps also resolves neighboring
    // lake masks that physically touch a shared inlet or confluence.
    for (int pass = 0; pass <= levels.length; pass++) {
      boolean changed = false;
      double[] ceiling = new double[h.riverNodes().size()];
      System.arraycopy(physicalCeilings, 0, ceiling, 0, ceiling.length);
      for (var segment : ordered) {
        var start = h.riverNodes().get(segment.startNodeId());
        var end = h.riverNodes().get(segment.endNodeId());
        double water = ceiling[start.id()];
        if (nodeLakes[start.id()] >= 0) water = Math.min(water, levels[nodeLakes[start.id()]]);
        if (start.outletLakeId() >= 0) water = Math.min(water, levels[start.outletLakeId()]);
        if (segment.type() == RiverSegmentType.CHANNEL && start.outletLakeId() < 0) {
          var startGrade = h.riverSegmentGradePlans().get(segment.id());
          var startProfile = h.riverSegmentProfiles().get(segment.id());
          if (startGrade.sampleCount() > 0)
            water =
                Math.min(
                    water,
                    startGrade.plannedBedElevationAt(0)
                        + Math.max(.55, startProfile.smoothedStartDepthBlocks() * .64));
        }
        water = Math.max(world.config().seaLevel(), water);
        ceiling[start.id()] = Math.min(ceiling[start.id()], water);
        if (start.outletLakeId() >= 0 && levels[start.outletLakeId()] > water + .001) {
          levels[start.outletLakeId()] = Math.max(world.config().seaLevel(), water);
          changed = true;
        }
        if (segment.type() == RiverSegmentType.LAKE_PASSAGE) {
          if (segment.lakeId() >= 0) water = levels[segment.lakeId()];
        } else {
          var grade = h.riverSegmentGradePlans().get(segment.id());
          var profile = h.riverSegmentProfiles().get(segment.id());
          for (int i = 0; i < grade.sampleCount(); i++) {
            double t = grade.sampleCount() <= 1 ? 0 : i / (double) (grade.sampleCount() - 1);
            double depth =
                profile.smoothedStartDepthBlocks()
                    + (profile.smoothedEndDepthBlocks() - profile.smoothedStartDepthBlocks()) * t;
            water = Math.min(water, grade.plannedBedElevationAt(i) + Math.max(.55, depth * .64));
          }
        }
        water = Math.max(world.config().seaLevel(), water);
        ceiling[end.id()] = Math.min(ceiling[end.id()], water);
        for (int id : new int[] {end.inletLakeId(), end.outletLakeId(), nodeLakes[end.id()]}) {
          if (id < 0) continue;
          double target = Math.max(world.config().seaLevel(), Math.min(levels[id], water));
          if (target < levels[id] - .001) {
            levels[id] = target;
            changed = true;
          }
        }
      }
      for (var lake : h.lakes())
        if (lake.downstreamLakeId() >= 0) {
          int downstream = lake.downstreamLakeId();
          if (levels[downstream] > levels[lake.id()] + .001) {
            levels[downstream] = levels[lake.id()];
            changed = true;
          }
        }
      finalCeiling = ceiling;
      if (!changed) break;
    }
    // Preserve each depression's depth while moving the whole basin down.
    // This intentionally favors coherent visible water over a literal erosion simulation.
    var result = new ArrayList<Lake>();
    for (var lake : h.lakes()) result.add(lake.lowerWaterSurfaceTo(levels[lake.id()]));
    h.setLakes(result);
    h.setRiverNodeWaterCeilings(finalCeiling);
  }
}
