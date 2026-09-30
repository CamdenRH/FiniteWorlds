package dev.lemma.finiteworlds.core.biome;

import dev.lemma.finiteworlds.core.WorldConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    @Test
    public void straightBlueprintEdgeBecomesCoherentCurveWithoutDither() {
        int size = 17;
        byte[] cells = new byte[size * size];
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                cells[z * size + x] = (byte) (x < 8
                        ? BiomeIntent.TEMPERATE_FOREST.ordinal() : BiomeIntent.SHRUB_STEPPE.ordinal());
            }
        }
        WorldConfig config = new WorldConfig(1024, size, 64);
        BiomeIntentPlan plan = plan(size, cells);
        BiomeIntentSampler sampler = new BiomeIntentSampler(plan, config, 9123L);
        BiomeIntentSampler repeated = new BiomeIntentSampler(plan, config, 9123L);
        BiomeIntentSampler different = new BiomeIntentSampler(plan, config, 291L);
        int minimumEdge = Integer.MAX_VALUE;
        int maximumEdge = Integer.MIN_VALUE;
        int seedDifferences = 0;
        for (int z = -400; z <= 400; z += 4) {
            int transitions = 0;
            int boundary = 0;
            BiomeIntent previous = sampler.intentAtBlock(-120, z);
            for (int x = -119; x <= 80; x++) {
                BiomeIntent intent = sampler.intentAtBlock(x, z);
                expect(intent, repeated.intentAtBlock(x, z));
                if (intent != different.intentAtBlock(x, z)) {
                    seedDifferences++;
                }
                if (intent != previous) {
                    transitions++;
                    boundary = x;
                    previous = intent;
                }
            }
            if (transitions != 1) {
                throw new AssertionError("Boundary produced stripes or speckles at z=" + z);
            }
            minimumEdge = Math.min(minimumEdge, boundary);
            maximumEdge = Math.max(maximumEdge, boundary);
        }
        if (maximumEdge - minimumEdge < 8) {
            throw new AssertionError("Biome edge retained a straight blueprint-cell seam");
        }
        if (seedDifferences < 100) {
            throw new AssertionError("Biome boundary must vary with the world seed");
        }
    }

    @Test
    public void preservesNarrowCoastalPocketsAndFillsOnlyCoarseRiverCells() {
        int size = 9;
        byte[] cells = new byte[size * size];
        Arrays.fill(cells, (byte) BiomeIntent.TEMPERATE_FOREST.ordinal());
        for (int z = 0; z < size; z++) {
            cells[z * size + 4] = (byte) BiomeIntent.RIVER.ordinal();
        }
        cells[4 * size + 2] = (byte) BiomeIntent.SANDY_BEACH.ordinal();
        cells[4 * size + 6] = (byte) BiomeIntent.ROCKY_COAST.ordinal();
        BiomeIntentSampler sampler = new BiomeIntentSampler(plan(size, cells),
                new WorldConfig(512, size, 64), 113L);
        expect(BiomeIntent.SANDY_BEACH, sampler.intentAtBlock(-128, 0));
        expect(BiomeIntent.ROCKY_COAST, sampler.intentAtBlock(128, 0));
        expect(BiomeIntent.RIVER, sampler.intentAtBlock(0, 0));
        expect(BiomeIntent.SANDY_BEACH, sampler.landIntentAtBlock(-128, 0));
        expect(BiomeIntent.ROCKY_COAST, sampler.landIntentAtBlock(128, 0));
        for (int z = -240; z <= 240; z += 4) {
            expect(BiomeIntent.TEMPERATE_FOREST, sampler.landIntentAtBlock(0, z));
        }
    }

    @Test
    public void isolatedCategoryPocketHasRoundedCornersInsteadOfSquareCellEdges() {
        int size = 7;
        byte[] cells = new byte[size * size];
        Arrays.fill(cells, (byte) BiomeIntent.TEMPERATE_FOREST.ordinal());
        cells[3 * size + 3] = (byte) BiomeIntent.SANDY_BEACH.ordinal();
        BiomeIntentSampler sampler = new BiomeIntentSampler(plan(size, cells),
                new WorldConfig(384, size, 64), 52L);
        expect(BiomeIntent.SANDY_BEACH, sampler.intentAtBlock(0, 0));
        // These four diagonals were all inside the old 64x64 nearest-cell box.
        // Their interpolated beach membership is weaker than surrounding land.
        for (int x : new int[]{-30, 30}) {
            for (int z : new int[]{-30, 30}) {
                expect(BiomeIntent.TEMPERATE_FOREST, sampler.intentAtBlock(x, z));
            }
        }
    }

    @Test
    public void snowBeltsAreHigherContinuousAndSeedStable() {
        byte[] cells = new byte[9];
        Arrays.fill(cells, (byte) BiomeIntent.PERMANENT_SNOWFIELD.ordinal());
        BiomeIntentPlan plan = plan(3, cells);
        WorldConfig config = new WorldConfig(2048, 3, 64);
        BiomeIntentSampler sampler = new BiomeIntentSampler(plan, config, 174L);
        BiomeIntentSampler repeated = new BiomeIntentSampler(plan, config, 174L);
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int z = -1024; z <= 1024; z += 64) {
            for (int x = -1024; x <= 1024; x += 64) {
                double snowline = sampler.snowlineAtBlock(x, z);
                if (snowline < 544.0 || snowline > 704.0) {
                    throw new AssertionError("Seasonal snowline escaped its raised height envelope");
                }
                if (snowline != repeated.snowlineAtBlock(x, z)
                        || sampler.permanentSnowlineAtBlock(x, z) != snowline + 375.0) {
                    throw new AssertionError("Snow bands are not stable or ordered");
                }
                if (Math.abs(sampler.snowlineAtBlock(x + 1, z) - snowline) > 1.0) {
                    throw new AssertionError("Snowline changes abruptly between adjacent columns");
                }
                minimum = Math.min(minimum, snowline);
                maximum = Math.max(maximum, snowline);
            }
        }
        if (maximum - minimum < 25.0) {
            throw new AssertionError("Snowline should follow coherent local climate variation");
        }
    }

    private static BiomeIntentPlan plan(int size, byte[] cells) {
        int[] counts = new int[BiomeIntent.values().length];
        for (byte cell : cells) {
            counts[Byte.toUnsignedInt(cell)]++;
        }
        return new BiomeIntentPlan(size, cells, counts);
    }

    private static void expect(BiomeIntent expected, BiomeIntent actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
