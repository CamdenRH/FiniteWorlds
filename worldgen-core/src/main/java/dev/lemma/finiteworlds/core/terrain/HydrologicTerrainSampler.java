package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.hydrology.HydrologyGrid;
import dev.lemma.finiteworlds.core.hydrology.Lake;
import dev.lemma.finiteworlds.core.hydrology.RiverCrossSectionPoint;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentCrossSection;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentValleyCorridor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Materializes hydrology at block resolution, after local terrain detail.
 * Macro cells reserve valleys but cannot represent narrow physical channels.
 * An immutable spatial index finds continuous cross-section edges without
 * scanning the complete river network for every Minecraft column.
 */
final class HydrologicTerrainSampler {
    private static final double BUCKET_SIZE = 512.0;
    private static final double MAX_EDGE_LENGTH = 32.0;
    private final WorldBlueprint blueprint;
    private final Edge[] edges;
    private final Map<Long, int[]> buckets;

    HydrologicTerrainSampler(WorldBlueprint blueprint) {
        this.blueprint = blueprint;
        HydrologyGrid hydrology = blueprint.hydrology();
        Map<Integer, RiverSegmentValleyCorridor> corridors = new HashMap<>();
        for (RiverSegmentValleyCorridor corridor : hydrology.riverSegmentValleyCorridors()) {
            corridors.put(corridor.segmentId(), corridor);
        }
        List<Edge> plannedEdges = new ArrayList<>();
        Map<Long, List<Integer>> index = new HashMap<>();
        for (RiverSegmentCrossSection section : hydrology.riverSegmentCrossSections()) {
            if (section.lakePassage() || section.points().size() < 2) {
                continue;
            }
            List<RiverCrossSectionPoint> points = section.points();
            double[] water = waterSurfaces(points);
            RiverSegmentValleyCorridor corridor = corridors.get(section.segmentId());
            int from = 0;
            while (from < points.size() - 1) {
                int to = from + 1;
                // Retain tight bends while avoiding redundant eight-block edges
                // on the straight portions of the densely sampled plan.
                while (to < points.size() - 1
                        && points.get(to + 1).distanceBlocks() - points.get(from).distanceBlocks()
                        <= MAX_EDGE_LENGTH
                        && chordDeviation(points, from, to + 1) <= 0.35) {
                    to++;
                }
                RiverCrossSectionPoint a = points.get(from);
                RiverCrossSectionPoint b = points.get(to);
                if (Math.hypot(b.blockX() - a.blockX(), b.blockZ() - a.blockZ()) > 1.0e-6) {
                    double valleyA = valleyWidth(corridor, from, a);
                    double valleyB = valleyWidth(corridor, to, b);
                    Edge edge = new Edge(section.segmentId(), a, b, water[from], water[to], valleyA, valleyB);
                    int edgeIndex = plannedEdges.size();
                    plannedEdges.add(edge);
                    double radius = Math.max(valleyA, valleyB);
                    int minX = bucket(Math.min(a.blockX(), b.blockX()) - radius);
                    int maxX = bucket(Math.max(a.blockX(), b.blockX()) + radius);
                    int minZ = bucket(Math.min(a.blockZ(), b.blockZ()) - radius);
                    int maxZ = bucket(Math.max(a.blockZ(), b.blockZ()) + radius);
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int x = minX; x <= maxX; x++) {
                            index.computeIfAbsent(key(x, z), ignored -> new ArrayList<>()).add(edgeIndex);
                        }
                    }
                }
                from = to;
            }
        }
        this.edges = plannedEdges.toArray(Edge[]::new);
        Map<Long, int[]> frozen = new HashMap<>();
        index.forEach((key, value) -> frozen.put(key, value.stream().mapToInt(Integer::intValue).toArray()));
        this.buckets = Map.copyOf(frozen);
    }

    TerrainColumn sample(double x, double z, double terrainElevation) {
        double terrain = terrainElevation;
        double water = Double.NaN;
        boolean river = false;
        int[] candidates = buckets.get(key(bucket(x), bucket(z)));
        if (candidates != null) {
            // Bucket entries are grouped by source segment. Only its closest
            // edge shapes this column; upstream endpoint caps cannot cut a
            // staircase into the downstream valley.
            Edge nearest = null;
            double nearestDistance = Double.POSITIVE_INFINITY;
            double nearestT = 0.0;
            for (int i = 0; i <= candidates.length; i++) {
                Edge edge = i < candidates.length ? edges[candidates[i]] : null;
                if (nearest != null && (edge == null || edge.segmentId != nearest.segmentId)) {
                    Influence influence = influence(nearest, nearestT, nearestDistance, x, z, terrainElevation);
                    if (influence != null) {
                        terrain = Math.min(terrain, influence.elevation);
                        if (Double.isFinite(influence.water)) {
                            water = Double.isFinite(water) ? Math.min(water, influence.water) : influence.water;
                            river = true;
                        }
                    }
                    nearest = null;
                    nearestDistance = Double.POSITIVE_INFINITY;
                }
                if (edge == null) {
                    break;
                }
                double dx = edge.b.blockX() - edge.a.blockX();
                double dz = edge.b.blockZ() - edge.a.blockZ();
                double t = clamp01(((x - edge.a.blockX()) * dx + (z - edge.a.blockZ()) * dz)
                        / (dx * dx + dz * dz));
                double distance = Math.hypot(x - lerp(edge.a.blockX(), edge.b.blockX(), t),
                        z - lerp(edge.a.blockZ(), edge.b.blockZ(), t));
                if (distance < nearestDistance) {
                    nearest = edge;
                    nearestDistance = distance;
                    nearestT = t;
                }
            }
        }

        LakeInfluence lake = lakeAt(x, z);
        boolean lakeWater = false;
        if (lake != null) {
            double shoreline = lake.support < 0.45
                    ? lerp(terrainElevation, lake.water + 1.0, smoothstep(0.0, 0.45, lake.support))
                    : lerp(lake.water + 1.0, lake.water - lake.depth, smoothstep(0.45, 0.55, lake.support));
            terrain = Math.min(terrain, shoreline);
            lakeWater = lake.support >= 0.45 && Math.floor(shoreline) < Math.floor(lake.water);
            if (lakeWater) {
                water = lake.water;
                river = false;
            }
        }
        int seaLevel = blueprint.config().seaLevel();
        if (terrain < seaLevel) {
            water = Double.isFinite(water) ? Math.max(seaLevel, water) : seaLevel;
        }
        return new TerrainColumn(terrain, water, river, lakeWater);
    }

    private static Influence influence(Edge edge, double t, double distance, double x, double z, double original) {
        RiverCrossSectionPoint a = edge.a;
        RiverCrossSectionPoint b = edge.b;
        double valley = lerp(edge.valleyA, edge.valleyB, t);
        if (distance >= valley) {
            return null;
        }
        double lateral = (x - lerp(a.blockX(), b.blockX(), t)) * -(b.blockZ() - a.blockZ())
                + (z - lerp(a.blockZ(), b.blockZ(), t)) * (b.blockX() - a.blockX());
        double width = Math.max(1.5, lateral >= 0.0
                ? lerp(a.leftBankOffsetBlocks(), b.leftBankOffsetBlocks(), t)
                : lerp(a.rightBankOffsetBlocks(), b.rightBankOffsetBlocks(), t));
        double water = lerp(edge.waterA, edge.waterB, t);
        // Even a headwater must contain actual water blocks after rounding.
        double bed = Math.min(lerp(a.plannedBedElevation(), b.plannedBedElevation(), t), water - 2.0);
        double bank = Math.max(water + 0.8, bed + lerp(a.bankfullDepthBlocks(), b.bankfullDepthBlocks(), t));
        double inner = Math.max(width + 2.0, lerp(a.innerValleyHalfWidthBlocks(), b.innerValleyHalfWidthBlocks(), t));
        valley = Math.max(inner + 1.0, valley);
        double floodplain = Math.max(bank, lerp(a.floodplainElevation(), b.floodplainElevation(), t));
        if (distance <= width) {
            double normalized = distance / width;
            double target = bed + (bank - bed) * normalized * normalized * normalized;
            return new Influence(target, Math.floor(target) < Math.floor(water) ? water : Double.NaN);
        }
        if (distance <= inner) {
            double target = lerp(bank, floodplain, smoothstep(width, inner, distance));
            return new Influence(Math.min(original, target), Double.NaN);
        }
        double target = lerp(floodplain, Math.max(floodplain, original), smoothstep(inner, valley, distance));
        return new Influence(Math.min(original, target), Double.NaN);
    }

    private LakeInfluence lakeAt(double x, double z) {
        HydrologyGrid hydrology = blueprint.hydrology();
        if (hydrology.lakeCount() == 0) {
            return null;
        }
        double halfWorld = blueprint.config().worldSizeBlocks() / 2.0;
        double px = (x + halfWorld) / blueprint.config().worldSizeBlocks() * (blueprint.resolution() - 1);
        double pz = (z + halfWorld) / blueprint.config().worldSizeBlocks() * (blueprint.resolution() - 1);
        if (px < 0.0 || pz < 0.0 || px > blueprint.resolution() - 1 || pz > blueprint.resolution() - 1) {
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
        return new LakeInfluence(lake.waterSurfaceElevation(), depth, bestWeight);
    }

    private static double cornerWeight(int corner, double tx, double tz) {
        return ((corner & 1) == 0 ? 1.0 - tx : tx) * ((corner >> 1) == 0 ? 1.0 - tz : tz);
    }

    private static double[] waterSurfaces(List<RiverCrossSectionPoint> points) {
        double[] water = new double[points.size()];
        for (int i = 0; i < water.length; i++) {
            RiverCrossSectionPoint point = points.get(i);
            water[i] = point.plannedBedElevation() + Math.max(0.55, point.bankfullDepthBlocks() * 0.72);
        }
        for (int pass = 0; pass < 2; pass++) {
            double[] next = water.clone();
            for (int i = 1; i < water.length - 1; i++) {
                next[i] += 0.26 * (0.5 * (water[i - 1] + water[i + 1]) - water[i]);
            }
            water = next;
        }
        for (int i = 1; i < water.length; i++) {
            water[i] = Math.min(water[i], water[i - 1]);
        }
        return water;
    }

    private static double valleyWidth(RiverSegmentValleyCorridor corridor, int index, RiverCrossSectionPoint point) {
        return Math.max(point.innerValleyHalfWidthBlocks() + 8.0,
                corridor != null && index < corridor.points().size()
                        ? corridor.points().get(index).valleyHalfWidthBlocks()
                        : point.innerValleyHalfWidthBlocks() * 2.0);
    }

    private static double chordDeviation(List<RiverCrossSectionPoint> points, int from, int to) {
        RiverCrossSectionPoint a = points.get(from);
        RiverCrossSectionPoint b = points.get(to);
        double dx = b.blockX() - a.blockX();
        double dz = b.blockZ() - a.blockZ();
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared < 1.0e-6) {
            return Double.POSITIVE_INFINITY;
        }
        double maximum = 0.0;
        for (int i = from + 1; i < to; i++) {
            RiverCrossSectionPoint p = points.get(i);
            double t = clamp01(((p.blockX() - a.blockX()) * dx + (p.blockZ() - a.blockZ()) * dz) / lengthSquared);
            maximum = Math.max(maximum, Math.hypot(p.blockX() - lerp(a.blockX(), b.blockX(), t),
                    p.blockZ() - lerp(a.blockZ(), b.blockZ(), t)));
        }
        return maximum;
    }

    private static int bucket(double coordinate) {
        return (int) Math.floor(coordinate / BUCKET_SIZE);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double low, double high, double value) {
        double t = clamp01((value - low) / (high - low));
        return t * t * (3.0 - 2.0 * t);
    }

    private record Edge(int segmentId, RiverCrossSectionPoint a, RiverCrossSectionPoint b,
                        double waterA, double waterB, double valleyA, double valleyB) {
    }

    private record Influence(double elevation, double water) {
    }

    private record LakeInfluence(double water, double depth, double support) {
    }
}
