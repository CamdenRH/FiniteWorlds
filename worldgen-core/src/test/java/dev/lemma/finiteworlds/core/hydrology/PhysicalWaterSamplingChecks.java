package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.terrain.TerrainColumn;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class PhysicalWaterSamplingChecks {
    @Test
    public void riversHaveContinuousWaterAboveSeaLevelAcrossSpatialBuckets() {
        WorldBlueprint world = flatWorld(350);
        addRiver(world);
        TerrainSampler sampler = new TerrainSampler(world, 17);
        double previousWater = Double.POSITIVE_INFINITY;
        for (int x = -690; x <= 690; x++) {
            TerrainColumn column = sampler.sampleColumn(x, 0);
            assertTrue(column.river(), "Missing river footprint at " + x);
            assertTrue(column.hasWater(), "Missing physical water depth at " + x);
            assertTrue(column.waterSurfaceElevation() > 64);
            assertTrue(column.waterSurfaceElevation() <= previousWater + 1.0e-8,
                    "Water rises downstream at " + x);
            assertTrue(column.terrainElevation() <= column.waterSurfaceElevation() - 1.9,
                    "Local detail obstructs channel at " + x);
            assertEquals(column.terrainElevation(), sampler.surfaceElevationAt(x, 0));
            previousWater = column.waterSurfaceElevation();
        }
        assertFalse(sampler.sampleColumn(0, 180).hasWater());
        assertEquals(350, world.elevation(4, 4), "Sampling modified macro terrain");
    }

    @Test
    public void plannedAsymmetricBanksTransitionSmoothlyIntoValleyShoulders() {
        WorldBlueprint world = flatWorld(350);
        addRiver(world);
        TerrainSampler sampler = new TerrainSampler(world, 17);
        assertTrue(sampler.sampleColumn(0, 5).hasWater(), "Wider left bank lost its channel");
        assertFalse(sampler.sampleColumn(0, -5).hasWater(), "Narrower right bank was widened");
        double bed = sampler.surfaceElevationAt(0, 0);
        double bank = sampler.surfaceElevationAt(0, 10);
        double inner = sampler.surfaceElevationAt(0, 20);
        double shoulder = sampler.surfaceElevationAt(0, 65);
        double exterior = sampler.surfaceElevationAt(0, 120);
        assertTrue(bed < bank && bank <= inner && inner < shoulder && shoulder < exterior,
                "River cross-section did not form a valley");
        assertTrue(Math.abs(sampler.surfaceElevationAt(0, 99) - sampler.surfaceElevationAt(0, 101)) < 5,
                "Valley corridor ends at a cliff");
    }

    @Test
    public void lakesUseTheirOwnLevelAndInterpolateShoreDepth() {
        WorldBlueprint world = flatWorld(350);
        HydrologyGrid hydrology = world.hydrology();
        hydrology.setLakes(List.of(new Lake(0, LakeSourceType.SIMPLE_DEPRESSION, 0, List.of(0), 4, 4,
                180, 160, 20, 20, 10000, 10000, 200000, 1,
                -1, -1, -1, -1, -1, -1, -1)));
        for (int z = 4; z <= 5; z++) {
            for (int x = 4; x <= 5; x++) {
                hydrology.setLakeCell(x, z, 0, 20);
            }
        }
        TerrainSampler sampler = new TerrainSampler(world, 23);
        TerrainColumn center = sampler.sampleColumn(128, 128);
        assertTrue(center.lake());
        assertTrue(center.hasWater());
        assertEquals(180, center.waterSurfaceElevation());
        assertEquals(160, center.terrainElevation());
        TerrainColumn shallow = sampler.sampleColumn(-100, 128);
        assertTrue(shallow.lake());
        assertEquals(180, shallow.waterSurfaceElevation());
        assertTrue(shallow.terrainElevation() > center.terrainElevation());
        assertFalse(sampler.sampleColumn(-200, 128).lake());
        double previous = sampler.surfaceElevationAt(-257, 128);
        for (int x = -256; x <= 0; x++) {
            double elevation = sampler.surfaceElevationAt(x, 128);
            assertTrue(Math.abs(elevation - previous) < 5, "Lake shoreline ends in a cliff at " + x);
            previous = elevation;
        }
        assertEquals(350, world.elevation(4, 4));
        assertEquals(20, hydrology.lakeDepth(4, 4));
    }

    @Test
    public void oceansAndDryLandShareTheSameColumnContract() {
        TerrainSampler land = new TerrainSampler(flatWorld(100), 3);
        TerrainColumn dry = land.sampleColumn(0, 0);
        assertFalse(dry.hasWater());
        assertTrue(Double.isNaN(dry.waterSurfaceElevation()));
        TerrainColumn outside = land.sampleColumn(2049, 0);
        assertTrue(outside.hasWater());
        assertEquals(64, outside.waterSurfaceElevation());
        assertFalse(outside.river());
        assertFalse(outside.lake());
    }

    private static WorldBlueprint flatWorld(float elevation) {
        WorldBlueprint world = new WorldBlueprint(new WorldConfig(2048, 9, 64));
        for (int z = 0; z < world.resolution(); z++) {
            for (int x = 0; x < world.resolution(); x++) {
                world.setElevation(x, z, elevation);
                world.setLandMask(x, z, 1);
            }
        }
        return world;
    }

    private static void addRiver(WorldBlueprint world) {
        List<RiverCrossSectionPoint> sections = List.of(section(-700, 160, 0),
                section(0, 150, 700), section(700, 140, 1400));
        world.hydrology().setRiverSegmentValleyCorridors(List.of(new RiverSegmentValleyCorridor(0, false,
                List.of(corridor(-700, 0), corridor(0, 700), corridor(700, 1400)), 10, 20, 100, 0.3)));
        world.hydrology().setRiverSegmentCrossSections(List.of(new RiverSegmentCrossSection(0, false,
                sections, 8, 6, 0.1)));
    }

    private static RiverCrossSectionPoint section(double x, double bed, double distance) {
        return new RiverCrossSectionPoint(x, 0, distance, RiverReachType.FOOTHILL,
                bed, 6, 6, 8, 4, 0.8, 0.8, bed + 7, 20, 0.1, 0.3, 0.014);
    }

    private static RiverValleyCorridorPoint corridor(double x, double distance) {
        return new RiverValleyCorridorPoint(x, 0, distance, 10, 20, 100, 0.3, 0.014);
    }
}
