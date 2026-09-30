package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.terrain.LakeFootprintSampler;
import java.util.*;

/** Prevent high tributary bends from crossing a lower channel before their common junction. */
public final class RiverCollisionConstraintPlanner {
  private static final double BUCKET = 256;

  private RiverCollisionConstraintPlanner() {}

  public static List<RiverSegmentCenterline> constrain(
      WorldBlueprint world, long seed, List<RiverSegmentCenterline> input) {
    var lakes = new LakeFootprintSampler(world, seed);
    List<RiverSegmentCenterline> current = input;
    for (int pass = 0; pass < 6; pass++) {
      Map<Long, List<Edge>> buckets = new HashMap<>();
      for (var line : current)
        for (int i = 1; i < line.points().size(); i++) {
          var a = line.points().get(i - 1);
          var b = line.points().get(i);
          Edge edge = new Edge(line.segmentId(), a, b);
          double radius = Math.max(a.widthBlocks(), b.widthBlocks()) + 12;
          int x0 = bucket(Math.min(a.blockX(), b.blockX()) - radius),
              x1 = bucket(Math.max(a.blockX(), b.blockX()) + radius);
          int z0 = bucket(Math.min(a.blockZ(), b.blockZ()) - radius),
              z1 = bucket(Math.max(a.blockZ(), b.blockZ()) + radius);
          for (int z = z0; z <= z1; z++)
            for (int x = x0; x <= x1; x++)
              buckets.computeIfAbsent(key(x, z), k -> new ArrayList<>()).add(edge);
        }
      var result = new ArrayList<RiverSegmentCenterline>();
      for (var line : current) {
        if (line.lakePassage()) {
          result.add(line);
          continue;
        }
        var points = new ArrayList<RiverCenterlinePoint>(line.points());
        var startNode =
            world
                .hydrology()
                .riverNodes()
                .get(world.hydrology().riverSegments().get(line.segmentId()).startNodeId());
        int first =
            startNode.source()
                    && !startNode.confluence()
                    && !startNode.lakeInlet()
                    && !startNode.lakeOutlet()
                ? 0
                : 1;
        for (int i = first; i < points.size() - 1; i++) {
          var p = points.get(i);
          double x = p.blockX(), z = p.blockZ();
          double ownWater = p.plannedBedElevation() + Math.max(.55, p.depthBlocks() * .72);
          ownWater =
              Math.min(
                  ownWater,
                  world
                      .hydrology()
                      .riverNodeWaterCeiling(
                          world.hydrology().riverSegments().get(line.segmentId()).startNodeId()));
          var candidates = buckets.get(key(bucket(x), bucket(z)));
          if (candidates == null) continue;
          for (int iteration = 0; iteration < 6; iteration++) {
            double dxMove = 0, dzMove = 0;
            int overlaps = 0;
            for (var edge : candidates) {
              if (edge.id == line.segmentId()) continue;
              double dx = edge.b.blockX() - edge.a.blockX(), dz = edge.b.blockZ() - edge.a.blockZ();
              double denominator = dx * dx + dz * dz;
              if (denominator < 1e-6) continue;
              double t =
                  Math.max(
                      0,
                      Math.min(
                          1,
                          ((x - edge.a.blockX()) * dx + (z - edge.a.blockZ()) * dz) / denominator));
              double water =
                  lerp(edge.a.plannedBedElevation(), edge.b.plannedBedElevation(), t)
                      + Math.max(.55, lerp(edge.a.depthBlocks(), edge.b.depthBlocks(), t) * .72);
              water =
                  Math.min(
                      water,
                      world
                          .hydrology()
                          .riverNodeWaterCeiling(
                              world.hydrology().riverSegments().get(edge.id).startNodeId()));
              if (ownWater - water < 1) continue;
              double ex = x - lerp(edge.a.blockX(), edge.b.blockX(), t),
                  ez = z - lerp(edge.a.blockZ(), edge.b.blockZ(), t);
              double distance = Math.hypot(ex, ez);
              double separation =
                  (p.widthBlocks() + lerp(edge.a.widthBlocks(), edge.b.widthBlocks(), t)) * .75 + 8;
              if (distance >= separation) continue;
              if (distance < 1e-4) {
                ex = -dz;
                ez = dx;
                distance = Math.hypot(ex, ez);
              }
              dxMove +=
                  ex
                      / distance
                      * (separation
                          - Math.hypot(
                              x - lerp(edge.a.blockX(), edge.b.blockX(), t),
                              z - lerp(edge.a.blockZ(), edge.b.blockZ(), t)));
              dzMove +=
                  ez
                      / distance
                      * (separation
                          - Math.hypot(
                              x - lerp(edge.a.blockX(), edge.b.blockX(), t),
                              z - lerp(edge.a.blockZ(), edge.b.blockZ(), t)));
              overlaps++;
            }
            if (overlaps == 0) break;
            double nx = x + dxMove / overlaps, nz = z + dzMove / overlaps;
            var lake = lakes.sample(nx, nz);
            if (lake != null && lake.support() > .20 && Math.abs(lake.water() - ownWater) > .75) {
              boolean safe = false;
              for (double sign : new double[] {1, -1}) {
                double tx = x - dzMove / overlaps * sign, tz = z + dxMove / overlaps * sign;
                var alternative = lakes.sample(tx, tz);
                if (alternative == null
                    || alternative.support() < .20
                    || Math.abs(alternative.water() - ownWater) < .75) {
                  nx = tx;
                  nz = tz;
                  safe = true;
                  break;
                }
              }
              if (!safe) break;
            }
            x = nx;
            z = nz;
          }
          points.set(
              i,
              new RiverCenterlinePoint(
                  x,
                  z,
                  p.distanceBlocks(),
                  p.plannedBedElevation(),
                  p.widthBlocks(),
                  p.depthBlocks(),
                  p.lateralOffsetBlocks()));
        }
        double distance = 0;
        for (int i = 0; i < points.size(); i++) {
          var p = points.get(i);
          if (i > 0) {
            var previous = points.get(i - 1);
            distance += Math.hypot(p.blockX() - previous.blockX(), p.blockZ() - previous.blockZ());
          }
          points.set(
              i,
              new RiverCenterlinePoint(
                  p.blockX(),
                  p.blockZ(),
                  distance,
                  p.plannedBedElevation(),
                  p.widthBlocks(),
                  p.depthBlocks(),
                  p.lateralOffsetBlocks()));
        }
        result.add(
            new RiverSegmentCenterline(
                line.segmentId(),
                false,
                points,
                distance,
                line.referenceLengthBlocks(),
                distance / Math.max(1, line.referenceLengthBlocks()),
                line.maximumLateralOffsetBlocks(),
                line.lateralFreedom()));
      }
      current = List.copyOf(result);
    }
    return current;
  }

  private static int bucket(double value) {
    return (int) Math.floor(value / BUCKET);
  }

  private static long key(int x, int z) {
    return ((long) x << 32) ^ (z & 0xffffffffL);
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }

  private record Edge(int id, RiverCenterlinePoint a, RiverCenterlinePoint b) {}
}
