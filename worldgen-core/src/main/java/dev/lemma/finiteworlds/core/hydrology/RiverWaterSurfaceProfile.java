package dev.lemma.finiteworlds.core.hydrology;

import java.util.Arrays;

/** Immutable water ceilings along a materialized river cross-section. */
public final class RiverWaterSurfaceProfile {
  private final int segmentId;
  private final double[] water;

  public RiverWaterSurfaceProfile(int segmentId, double[] water) {
    this.segmentId = segmentId;
    this.water = Arrays.copyOf(water, water.length);
  }

  public int segmentId() {
    return segmentId;
  }

  public int size() {
    return water.length;
  }

  public double waterAt(int index) {
    return water[index];
  }
}
