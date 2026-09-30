package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LandmarkVolcanoHeightPlannerChecks {
    @Test
    public void raisesOnlyTheVolcanoAndPreservesItsContinuousSummit() {
        int size = 16;
        WorldBlueprint blueprint = new WorldBlueprint(new WorldConfig(1024, size, 64));
        float[] uplift = new float[size * size];
        float[] original = new float[size * size];
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double radius = Math.hypot((x - 6.35) / 3.8, (z - 7.45) / 4.3);
                uplift[z * size + x] = (float) (420.0 * Math.pow(Math.max(0.0, 1.0 - radius), 1.4));
                original[z * size + x] = 140.0f + x + uplift[z * size + x];
                blueprint.setElevation(x, z, original[z * size + x]);
                blueprint.setCascadeUplift(x, z, 35.0f + uplift[z * size + x]);
                blueprint.setBaseElevation(x, z, 105.0f + x);
            }
        }

        LandmarkVolcanoHeightPlanner.apply(blueprint, uplift);

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                assertEquals(105.0f + x, blueprint.baseElevation(x, z));
                if (uplift[z * size + x] == 0.0f) {
                    assertEquals(original[z * size + x], blueprint.elevation(x, z),
                            "Ordinary terrain outside the edifice changed");
                } else {
                    assertEquals(blueprint.elevation(x, z) - 140.0f - x,
                            blueprint.cascadeUplift(x, z) - 35.0f, 0.001,
                            "Volcanic uplift metadata and elevation diverged");
                }
            }
        }
        double maximum = Double.NEGATIVE_INFINITY;
        for (double z = 5.0; z <= 10.0; z += 1.0 / 64.0) {
            for (double x = 4.0; x <= 9.0; x += 1.0 / 64.0) {
                double height = blueprint.smoothElevationAtBlock(
                        (x / (size - 1.0) - 0.5) * 1024.0,
                        (z / (size - 1.0) - 0.5) * 1024.0);
                maximum = Math.max(maximum, height);
            }
        }
        assertTrue(maximum >= 1431.0 && maximum <= 1433.0,
                "Continuous summit missed height budget: " + maximum);
    }

    @Test
    public void aWorldWithoutAVolcanoIsUnchanged() {
        WorldBlueprint blueprint = new WorldBlueprint(new WorldConfig(1024, 16, 64));
        blueprint.setElevation(6, 7, 264.0f);
        LandmarkVolcanoHeightPlanner.apply(blueprint, new float[256]);
        assertEquals(264.0f, blueprint.elevation(6, 7));
    }
}
