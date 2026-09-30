package dev.lemma.finiteworlds.core.biome;

import dev.lemma.finiteworlds.core.WorldConfig;
import org.junit.jupiter.api.Test;

public final class BiomeIntentSamplerChecks {
    @Test
    public void samplesAllIntentsAndFiniteWorldEdges() {
        BiomeIntent[] intents = BiomeIntent.values();
        int resolution = 5;
        byte[] cells = new byte[resolution * resolution];
        int[] counts = new int[intents.length];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = (byte) (i % intents.length);
            counts[i % intents.length]++;
        }
        BiomeIntentPlan plan = new BiomeIntentPlan(resolution, cells, counts);
        BiomeIntentSampler sampler = new BiomeIntentSampler(plan, new WorldConfig(400, resolution, 64));
        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                expect(plan.intent(x, z), sampler.intentAtBlock(-200 + x * 100, -200 + z * 100));
            }
        }
        expect(plan.intent(0, 0), sampler.intentAtBlock(-151, -151));
        expect(plan.intent(1, 1), sampler.intentAtBlock(-149, -149));
        expect(BiomeIntent.OCEAN, sampler.intentAtBlock(-201, 0));
        expect(BiomeIntent.OCEAN, sampler.intentAtBlock(201, 0));
        expect(BiomeIntent.OCEAN, sampler.intentAtBlock(0, -201));
        expect(BiomeIntent.OCEAN, sampler.intentAtBlock(0, 201));
        try {
            new BiomeIntentSampler(plan, new WorldConfig(400, 6, 64));
            throw new AssertionError("Mismatched resolutions accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        System.out.println("Biome coordinate checks passed for all intents and finite-world edges.");
    }

    private static void expect(BiomeIntent expected, BiomeIntent actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
