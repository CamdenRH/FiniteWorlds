package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.generator.CascadiaGenerator;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;
import java.util.*;

/** Production-scale audit of actual water columns, slopes and cross-section junctions. */
public final class TerrainAuditMain {
  public static void main(String[] args) {
    for (String arg : args) audit(Long.parseLong(arg));
  }

  private static void audit(long seed) {
    long started = System.nanoTime();
    var world = new CascadiaGenerator().generate(seed, WorldConfig.production());
    var terrain = new TerrainSampler(world, seed);
    double maximum = -1;
    int peakX = 0, peakZ = 0;
    for (int z = 0; z < world.resolution(); z++)
      for (int x = 0; x < world.resolution(); x++)
        if (world.elevation(x, z) > maximum) {
          maximum = world.elevation(x, z);
          peakX = x;
          peakZ = z;
        }
    double spacing = world.config().worldSizeBlocks() / (world.resolution() - 1.0);
    double cx = peakX * spacing - world.config().worldSizeBlocks() / 2.0;
    double cz = peakZ * spacing - world.config().worldSizeBlocks() / 2.0;
    double peak = -1;
    for (int z = -128; z <= 128; z += 2)
      for (int x = -128; x <= 128; x += 2)
        peak = Math.max(peak, terrain.surfaceElevationAt(cx + x, cz + z));
    long samples = 0, wet = 0, dryBanks = 0, lowBanks = 0, uphillWater = 0;
    double maxBankDrop = 0, junctionSpread = 0, maxWaterRise = 0;
    Map<Integer, List<Double>> junctions = new HashMap<>();
    for (var section : world.hydrology().riverSegmentCrossSections()) {
      if (section.lakePassage()) continue;
      var segment = world.hydrology().riverSegments().get(section.segmentId());
      double previousWater = Double.POSITIVE_INFINITY;
      for (int i = 0; i < section.points().size(); i++) {
        var p = section.points().get(i);
        var center = terrain.sampleColumn(p.blockX(), p.blockZ());
        samples++;
        if (center.hasWater()) {
          wet++;
          double rise = center.waterSurfaceElevation() - previousWater;
          if (rise > 1.0) {
            uphillWater++;
            maxWaterRise = Math.max(maxWaterRise, rise);
            if (rise > 1)
              System.out.printf(
                  Locale.ROOT,
                  "water rise segment=%d index=%d/%d pos=(%.1f,%.1f) previous=%.2f current=%.2f"
                      + " lake=%s bed=%.2f start=%s end=%s%n",
                  section.segmentId(),
                  i,
                  section.points().size(),
                  p.blockX(),
                  p.blockZ(),
                  previousWater,
                  center.waterSurfaceElevation(),
                  center.lake(),
                  p.plannedBedElevation(),
                  world.hydrology().riverNodes().get(segment.startNodeId()).primaryType(),
                  world.hydrology().riverNodes().get(segment.endNodeId()).primaryType());
          }
          previousWater = center.waterSurfaceElevation();
        }
        if (i == 0 || i == section.points().size() - 1) {
          int node = i == 0 ? segment.startNodeId() : segment.endNodeId();
          junctions
              .computeIfAbsent(node, k -> new ArrayList<>())
              .add(p.plannedBedElevation() + Math.max(.55, p.bankfullDepthBlocks() * .72));
        }
        if (!center.hasWater() || i % 8 != 0) continue;
        var next = section.points().get(Math.min(i + 1, section.points().size() - 1));
        var prev = section.points().get(Math.max(0, i - 1));
        double dx = next.blockX() - prev.blockX(), dz = next.blockZ() - prev.blockZ();
        double length = Math.max(1, Math.hypot(dx, dz));
        for (int side : new int[] {-1, 1}) {
          double width = (side > 0 ? p.leftBankOffsetBlocks() : p.rightBankOffsetBlocks()) + 2;
          var bank =
              terrain.sampleColumn(
                  p.blockX() - dz / length * width * side, p.blockZ() + dx / length * width * side);
          if (!bank.hasWater()) {
            dryBanks++;
            double drop = center.waterSurfaceElevation() - bank.terrainElevation();
            if (drop > 2) {
              lowBanks++;
              maxBankDrop = Math.max(maxBankDrop, drop);
            }
          }
        }
      }
    }
    int uphillLakes = 0;
    double maxLakeRise = 0;
    for (var segment : world.hydrology().riverSegments()) {
      if (segment.type() != dev.lemma.finiteworlds.core.hydrology.RiverSegmentType.CHANNEL)
        continue;
      var start = world.hydrology().riverNodes().get(segment.startNodeId());
      var end = world.hydrology().riverNodes().get(segment.endNodeId());
      if (start.outletLakeId() < 0 || end.inletLakeId() < 0) continue;
      double rise =
          world.hydrology().lake(end.inletLakeId()).waterSurfaceElevation()
              - world.hydrology().lake(start.outletLakeId()).waterSurfaceElevation();
      if (rise > 1) {
        uphillLakes++;
        maxLakeRise = Math.max(maxLakeRise, rise);
      }
    }
    System.out.printf(
        Locale.ROOT, "lake graph uphill edges=%d maxRise=%.2f%n", uphillLakes, maxLakeRise);
    for (var levels : junctions.values())
      if (levels.size() > 1)
        junctionSpread =
            Math.max(junctionSpread, Collections.max(levels) - Collections.min(levels));
    System.out.printf(
        Locale.ROOT,
        "seed=%d peakY=%.3f wet=%d/%d lowDryBanks=%d/%d maxBankDrop=%.2f plannedJunctionSpread=%.3f"
            + " uphillWater=%d maxWaterRise=%.2f headwaters=%d lakes=%d seconds=%.2f%n",
        seed,
        peak,
        wet,
        samples,
        lowBanks,
        dryBanks,
        maxBankDrop,
        junctionSpread,
        uphillWater,
        maxWaterRise,
        world.hydrology().streamHeadwaterSourceCount(),
        world.hydrology().lakeCount(),
        (System.nanoTime() - started) / 1e9);
    if (peak < 1400 || peak > 1435) throw new AssertionError("Peak height outside reserve budget");
    if (world.hydrology().routedCycleCount() != 0 || world.hydrology().routedSinkCount() != 0)
      throw new AssertionError("Drainage is not an acyclic connected graph");
    if (uphillLakes != 0 || maxWaterRise > 2)
      throw new AssertionError("Physical drainage rises downstream");
    if (wet != samples) throw new AssertionError("Physical river water continuity failed");
  }
}
