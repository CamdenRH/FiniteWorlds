package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LocalTerrainSamplerChecks {
    @Test
    public void mountainsHaveRidgesAndGulliesWhilePlateausRemainGentle() {
        WorldBlueprint mountains = flatBlueprint(180.0f);
        WorldBlueprint plateau = flatBlueprint(0.0f);
        LocalTerrainSampler mountain = new LocalTerrainSampler(1L, mountains);
        LocalTerrainSampler repeat = new LocalTerrainSampler(1L, mountains);
        LocalTerrainSampler plain = new LocalTerrainSampler(1L, plateau);
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        double plainMin = Double.POSITIVE_INFINITY;
        double plainMax = Double.NEGATIVE_INFINITY;
        double largestStep = 0.0;
        for (int z = -2048; z <= 2048; z += 32) {
            for (int x = -2048; x <= 2048; x += 32) {
                double value = mountain.sample(x, z, 250.0);
                assertEquals(value, repeat.sample(x, z, 250.0));
                double ordinary = plain.sample(x, z, 250.0);
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
                plainMin = Math.min(plainMin, ordinary);
                plainMax = Math.max(plainMax, ordinary);
                largestStep = Math.max(largestStep, Math.abs(value - mountain.sample(x + 1, z, 250.0)));
                assertTrue(Math.abs(mountain.sample(x, z, 64.0)) < 23.0,
                        "Mountain relief overwhelmed shoreline protection");
            }
        }
        assertTrue(maximum > 25.0, "No substantial mountain ridges: " + maximum);
        assertTrue(minimum < -35.0, "No substantial erosional gullies: " + minimum);
        assertTrue(maximum - minimum > 75.0, "Mountain relief remained smooth");
        assertTrue(plainMax - plainMin < 32.0, "Plateaus acquired mountain relief");
        assertTrue(largestStep < 8.0, "Local terrain produced isolated discontinuous spikes: " + largestStep);
    }

    @Test
    public void localReliefPreservesTheCalibratedVolcanicSummit() {
        LocalTerrainSampler sampler = new LocalTerrainSampler(1L, flatBlueprint(1300.0f));
        for (int z = -100; z <= 100; z += 10) {
            for (int x = -100; x <= 100; x += 10) {
                assertEquals(0.0, sampler.sample(x, z, 1520.0));
            }
        }
    }

    private static WorldBlueprint flatBlueprint(float mountainUplift) {
        WorldBlueprint blueprint = new WorldBlueprint(new WorldConfig(16384, 16, 64));
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                blueprint.setElevation(x, z, 250.0f);
                blueprint.setCascadeUplift(x, z, mountainUplift);
            }
        }
        return blueprint;
    }
}
