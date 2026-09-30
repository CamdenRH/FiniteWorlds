package dev.lemma.finiteworlds.core.hydrology;

import static org.junit.jupiter.api.Assertions.*;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LakeConnectionLevelChecks {
  @Test
  void aReceivingLakeCannotSitAboveItsFeedingChannel() {
    var world = new WorldBlueprint(new WorldConfig(2048, 9, 64));
    var h = world.hydrology();
    h.setLakes(List.of(lake(0, 101), lake(1, 180)));
    h.setLakeCell(2, 4, 0, 20);
    h.setLakeCell(6, 4, 1, 20);
    h.setRiverGraph(
        List.of(node(0, 2, -1, 0), node(1, 6, 1, -1)),
        List.of(
            new RiverSegment(
                0,
                RiverSegmentType.CHANNEL,
                0,
                1,
                List.of(38, 39, 40, 41, 42),
                -1,
                StreamClass.RIVER,
                1000,
                2000,
                1024,
                0)));
    h.setRiverSegmentHierarchy(
        List.of(new RiverSegmentHierarchy(0, 1, List.of(), -1, 0, 0, 1024, 0)));
    h.setRiverSegmentProfiles(
        List.of(
            new RiverSegmentProfile(0, 0, 1024, 1024, 0, 101, 180, 10, 2, 10, 10, 2, 2, 1, 0, 0)));
    float[] bed = {99, 88};
    h.setRiverSegmentGradePlans(
        List.of(new RiverSegmentGradePlan(0, false, bed, bed, bed, 0, 0, 0, 0, .01, .01)));
    LakeConnectionLevelPlanner.plan(world, 17);
    assertTrue(h.lake(1).waterSurfaceElevation() <= 89.3, "Receiving lake requires uphill water");
    assertTrue(h.lake(1).waterSurfaceElevation() <= h.lake(0).waterSurfaceElevation());
    assertEquals(20, h.lake(1).maximumDepth());
    assertEquals(
        20,
        h.lake(1).waterSurfaceElevation() - h.lake(1).minimumBedElevation(),
        .001,
        "Adjusting the connection drained its lake instead of preserving the basin");
    assertTrue(h.riverNodeWaterCeiling(1) <= h.riverNodeWaterCeiling(0));
  }

  private static Lake lake(int id, float level) {
    return new Lake(
        id,
        LakeSourceType.SIMPLE_DEPRESSION,
        id,
        List.of(id),
        1,
        4,
        level,
        level - 20,
        20,
        20,
        65536,
        262144,
        1310720,
        1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1);
  }

  private static RiverNode node(int id, int x, int inlet, int outlet) {
    return new RiverNode(
        id,
        x,
        4,
        false,
        false,
        inlet >= 0,
        outlet >= 0,
        false,
        inlet,
        outlet,
        StreamClass.RIVER,
        1000,
        200);
  }
}
