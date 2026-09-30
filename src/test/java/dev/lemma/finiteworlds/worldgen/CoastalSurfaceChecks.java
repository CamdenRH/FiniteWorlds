package dev.lemma.finiteworlds.worldgen;

import com.mojang.serialization.Lifecycle;
import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import dev.lemma.finiteworlds.core.biome.BiomeIntentPlanner;
import dev.lemma.finiteworlds.core.biome.BiomeTargetResolver;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.Blender;

import java.util.EnumMap;
import java.util.Map;

/** Exercises the coastal renderer with real Minecraft chunks, without another bootstrap. */
final class CoastalSurfaceChecks {
    private static final HeightLimitView LIMITS = HeightLimitView.create(
            FiniteChunkGenerator.MIN_Y, FiniteChunkGenerator.WORLD_HEIGHT);

    static void run(FiniteChunkGenerator generator, FiniteBiomeSource source,
                    RegistryWrapper.Impl<Biome> vanilla) {
        var biomes = new SimpleRegistry<Biome>(RegistryKeys.BIOME, Lifecycle.stable());
        vanilla.streamEntries().forEach(entry -> biomes.add(
                entry.registryKey(), entry.value(), RegistryEntryInfo.DEFAULT));
        biomes.freeze();

        Samples representatives = findRepresentatives(generator, source);
        for (BiomeIntent intent : new BiomeIntent[]{BiomeIntent.SANDY_BEACH,
                BiomeIntent.GRAVEL_BEACH, BiomeIntent.ROCKY_COAST, BiomeIntent.TEMPERATE_FOREST}) {
            Point point = representatives.dry().get(intent);
            require(point != null, "Production seed must provide a dry " + intent + " test location");
            var chunk = populate(generator, source, biomes, point.x() >> 4, point.z() >> 4);
            checkColumn(generator, source, chunk, point.x(), point.z());
            checkLayers(chunk, point.x(), point.z(), point.surface(), intent);
            require(point.surface() >= FiniteChunkGenerator.SEA_LEVEL,
                    "Client test coordinates must have an exposed surface for " + intent);
            String expectedId = BiomeTargetResolver.target(intent).vanillaFallbackId();
            var expectedKey = RegistryKey.of(RegistryKeys.BIOME, Identifier.of(expectedId));
            require(source.getBiome(point.x() >> 2, point.surface() >> 2, point.z() >> 2, null)
                            .matchesKey(expectedKey),
                    "Coastal surface intent and the F3 biome must agree for " + intent);
            System.out.println("Coastal seed-1 client check: " + intent + " (" + expectedId + ") /tp "
                    + point.x() + " " + (point.surface() + 2) + " " + point.z());

            // Probe both sides of an actual chunk boundary using independently generated chunks.
            int eastX = chunk.getPos().getStartX() + 16;
            int boundaryZ = point.z();
            var neighbor = populate(generator, source, biomes, (eastX >> 4), boundaryZ >> 4);
            checkColumn(generator, source, chunk, eastX - 1, boundaryZ);
            checkColumn(generator, source, neighbor, eastX, boundaryZ);
        }

        Point submerged = representatives.submergedSand();
        require(submerged != null, "Production seed must provide a shallow submerged sandy beach");
        var sandyChunk = populate(generator, source, biomes, submerged.x() >> 4, submerged.z() >> 4);
        checkColumn(generator, source, sandyChunk, submerged.x(), submerged.z());
        checkLayers(sandyChunk, submerged.x(), submerged.z(), submerged.surface(), BiomeIntent.SANDY_BEACH);
        for (int y = submerged.surface() + 1; y <= FiniteChunkGenerator.SEA_LEVEL; y++) {
            require(sandyChunk.getBlockState(new BlockPos(submerged.x(), y, submerged.z())).isOf(Blocks.WATER),
                    "Sandy beach substrate must retain the prototype sea water above it");
        }

        // Production seed 1 does not necessarily contain cold beach. Exercise the same material contract.
        for (int depth = 0; depth <= 8; depth++) {
            BlockState expected = depth < 4 ? Blocks.SAND.getDefaultState()
                    : depth < 8 ? Blocks.SANDSTONE.getDefaultState() : Blocks.STONE.getDefaultState();
            require(CoastalSurfaceMaterials.stateAt(80 - depth, 80, BiomeIntent.COLD_BEACH).equals(expected),
                    "Cold beaches must share the sand and sandstone layers at depth " + depth);
        }
        System.out.println("Coastal surface materials, physical chunks, column samples, and chunk borders passed.");
    }

    private static Samples findRepresentatives(FiniteChunkGenerator generator, FiniteBiomeSource source) {
        var blueprint = source.blueprint();
        var plan = BiomeIntentPlanner.plan(blueprint);
        Map<BiomeIntent, Point> points = new EnumMap<>(BiomeIntent.class);
        Point submergedSand = null;
        double size = blueprint.config().worldSizeBlocks();
        double step = size / (plan.resolution() - 1);
        for (int z = 0; z < plan.resolution(); z++) {
            for (int x = 0; x < plan.resolution(); x++) {
                BiomeIntent intent = plan.intent(x, z);
                if (intent != BiomeIntent.SANDY_BEACH && intent != BiomeIntent.GRAVEL_BEACH
                        && intent != BiomeIntent.ROCKY_COAST && intent != BiomeIntent.TEMPERATE_FOREST) {
                    continue;
                }
                if (points.containsKey(intent)
                        && (intent != BiomeIntent.SANDY_BEACH || submergedSand != null)) {
                    continue;
                }
                // Use quart-aligned coordinates so Minecraft's stored biome and semantic surface sample agree.
                int blockX = (int) Math.round((-size / 2.0 + x * step) / 4.0) * 4;
                int blockZ = (int) Math.round((-size / 2.0 + z * step) / 4.0) * 4;
                if (source.intentAtBlock(blockX, blockZ) != intent) {
                    continue;
                }
                int surface = generator.getHeight(blockX, blockZ,
                        Heightmap.Type.WORLD_SURFACE_WG, LIMITS, null) - 1;
                var point = new Point(blockX, blockZ, surface);
                if (surface >= FiniteChunkGenerator.SEA_LEVEL) {
                    points.putIfAbsent(intent, point);
                } else if (intent == BiomeIntent.SANDY_BEACH && surface >= 60) {
                    if (submergedSand == null) {
                        submergedSand = point;
                    }
                }
                if (intent == BiomeIntent.SANDY_BEACH && submergedSand == null) {
                    // Grid centers sit above sea level; the continuous terrain can cross the
                    // waterline near a cell's edge while retaining sandy beach intent.
                    submergedSand = findSubmergedSand(generator, source, blockX, blockZ);
                }
            }
            if (points.size() == 4 && submergedSand != null) {
                break;
            }
        }
        return new Samples(points, submergedSand);
    }

    private static Point findSubmergedSand(FiniteChunkGenerator generator, FiniteBiomeSource source,
                                           int centerX, int centerZ) {
        for (int offsetZ = -28; offsetZ <= 28; offsetZ += 8) {
            for (int offsetX = -28; offsetX <= 28; offsetX += 8) {
                int x = centerX + offsetX;
                int z = centerZ + offsetZ;
                if (source.intentAtBlock(x, z) != BiomeIntent.SANDY_BEACH) {
                    continue;
                }
                int surface = generator.getHeight(x, z,
                        Heightmap.Type.WORLD_SURFACE_WG, LIMITS, null) - 1;
                if (surface >= 60 && surface < FiniteChunkGenerator.SEA_LEVEL) {
                    return new Point(x, z, surface);
                }
            }
        }
        return null;
    }

    private static ProtoChunk populate(FiniteChunkGenerator generator, FiniteBiomeSource source,
                                       SimpleRegistry<Biome> biomes, int chunkX, int chunkZ) {
        var chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.NO_UPGRADE_DATA,
                LIMITS, biomes, null);
        generator.populateNoise(Blender.getNoBlending(), null, null, chunk).join();
        chunk.populateBiomes(source, null);
        chunk.setStatus(ChunkStatus.BIOMES);
        return chunk;
    }

    private static void checkColumn(FiniteChunkGenerator generator, FiniteBiomeSource source,
                                    ProtoChunk chunk, int x, int z) {
        var sample = generator.getColumnSample(x, z, LIMITS, null);
        var pos = new BlockPos.Mutable();
        for (int y = FiniteChunkGenerator.MIN_Y; y <= FiniteChunkGenerator.MAX_Y; y++) {
            require(chunk.getBlockState(pos.set(x, y, z)).equals(sample.getState(y)),
                    "Actual chunk and column query disagree at " + x + "," + y + "," + z);
        }
        int surface = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, LIMITS, null) - 1;
        require(chunk.getBiomeForNoiseGen(x >> 2, surface >> 2, z >> 2)
                        .equals(source.getBiome(x >> 2, surface >> 2, z >> 2, null)),
                "Stored chunk biome differs from the source at " + x + "," + z);
        checkLayers(chunk, x, z, surface, source.intentAtBlock(x, z));
    }

    private static void checkLayers(ProtoChunk chunk, int x, int z, int surface, BiomeIntent intent) {
        for (int depth = 0; depth <= 8; depth++) {
            if (surface - depth < FiniteChunkGenerator.DEVELOPMENT_TERRAIN_MIN_Y) {
                require(chunk.getBlockState(new BlockPos(x, surface - depth, z)).isAir(),
                        "The temporary deep-world floor must remain air");
                continue;
            }
            var block = switch (intent) {
                case SANDY_BEACH, COLD_BEACH -> depth < 4 ? Blocks.SAND
                        : depth < 8 ? Blocks.SANDSTONE : Blocks.STONE;
                case GRAVEL_BEACH -> depth < 4 ? Blocks.GRAVEL : Blocks.STONE;
                case ROCKY_COAST -> Blocks.STONE;
                default -> depth == 0 ? Blocks.GRASS_BLOCK : depth < 4 ? Blocks.DIRT : Blocks.STONE;
            };
            require(chunk.getBlockState(new BlockPos(x, surface - depth, z)).isOf(block),
                    "Wrong " + intent + " substrate at " + x + "," + (surface - depth) + "," + z
                            + "; expected " + block);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Point(int x, int z, int surface) {
    }

    private record Samples(Map<BiomeIntent, Point> dry, Point submergedSand) {
    }
}
