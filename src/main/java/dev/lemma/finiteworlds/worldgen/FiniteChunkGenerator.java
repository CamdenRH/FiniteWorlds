package dev.lemma.finiteworlds.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import dev.lemma.finiteworlds.core.generator.CascadiaGenerator;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;

import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;

import net.minecraft.world.chunk.Chunk;

import net.minecraft.world.gen.StructureAccessor;

import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;

import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.structure.StructureSet;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class FiniteChunkGenerator
        extends ChunkGenerator {

    public static final int SEA_LEVEL = 64;

    /*
     * Final planned Finite Worlds vertical envelope.
     *
     * Keep the current prototype terrain itself on the familiar -64+ scale
     * while development continues, but allocate the eventual dimension height
     * now so exceptional terrain such as the landmark volcano is no longer
     * clipped at vanilla Overworld Y=319.
     */
    public static final int MIN_Y = -512;
    public static final int WORLD_HEIGHT = 2048;
    public static final int MAX_Y = MIN_Y + WORLD_HEIGHT - 1;

    /*
     * Temporary development terrain floor. Blocks below this level remain air
     * for now, avoiding the cost of filling an extra 448 vertical blocks per
     * column before the deep-world geology/cave pass exists.
     */
    public static final int DEVELOPMENT_TERRAIN_MIN_Y = -64;

    private volatile TerrainSampler terrainSampler;

    public static final MapCodec<FiniteChunkGenerator>
            CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(

                            BiomeSource.CODEC
                                    .fieldOf("biome_source")
                                    .forGetter(
                                            FiniteChunkGenerator::getBiomeSource
                                    ),

                            Codec.LONG
                                    .optionalFieldOf("seed")
                                    .forGetter(
                                            FiniteChunkGenerator::serializedSeed
                                    )

                    ).apply(
                            instance,
                            FiniteChunkGenerator::new
                    )
            );

    private long seed;
    private volatile boolean seedBound;
    private final Optional<Long> configuredSeed;

    public FiniteChunkGenerator(
            BiomeSource biomeSource,
            long seed
    ) {
        this(biomeSource, Optional.of(seed));
    }

    private FiniteChunkGenerator(BiomeSource biomeSource, Optional<Long> configuredSeed) {
        super(configuredSeed.isPresent() && biomeSource instanceof FiniteBiomeSource finite
                ? finite.withSeed(configuredSeed.get()) : biomeSource);
        this.seed = configuredSeed.orElse(0L);
        this.seedBound = configuredSeed.isPresent();
        this.configuredSeed = configuredSeed;
    }

    private Optional<Long> serializedSeed() {
        // New worlds use Minecraft's saved GeneratorOptions seed. Keep their
        // generator seed absent so Re-create World can select a different seed.
        return configuredSeed;
    }

    /** Minecraft calls this with ServerWorld.getSeed() before creating its generation context. */
    @Override
    public StructurePlacementCalculator createStructurePlacementCalculator(
            RegistryWrapper<StructureSet> structures, NoiseConfig noiseConfig, long worldSeed) {
        bindWorldSeed(worldSeed);
        return super.createStructurePlacementCalculator(structures, noiseConfig, worldSeed);
    }

    private synchronized void bindWorldSeed(long worldSeed) {
        if (seedBound) {
            // A serialized generator seed preserves already-created worlds.
            return;
        }
        if (getBiomeSource() instanceof FiniteBiomeSource finite) {
            finite.bindSeed(worldSeed);
        }
        seed = worldSeed;
        seedBound = true;
    }

    private TerrainSampler terrainSampler() {
        if (!seedBound) {
            throw new IllegalStateException("Finite Worlds has not received the saved world seed yet");
        }
        TerrainSampler result = terrainSampler;
        if (result == null) {
            synchronized (this) {
                result = terrainSampler;
                if (result == null) {
                    if (getBiomeSource() instanceof FiniteBiomeSource finite) {
                        result = finite.terrainSampler();
                    } else {
                        WorldBlueprint blueprint = new CascadiaGenerator().generate(
                                seed, WorldConfig.production());
                        result = new TerrainSampler(blueprint, seed);
                    }
                    terrainSampler = result;
                }
            }
        }
        return result;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator>
    getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(
            Blender blender,
            NoiseConfig noiseConfig,
            StructureAccessor structures,
            Chunk chunk
    ) {
        BlockPos.Mutable pos =
                new BlockPos.Mutable();

        int startX =
                chunk.getPos().getStartX();

        int startZ =
                chunk.getPos().getStartZ();

        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {

                int worldX =
                        startX + localX;

                int worldZ =
                        startZ + localZ;

                BlockColumn column =
                        blockColumn(
                                worldX,
                                worldZ
                        );

                BiomeIntent intent = surfaceIntent(worldX, worldZ);

                for (
                        int y = DEVELOPMENT_TERRAIN_MIN_Y;
                        y <= column.top();
                        y++
                ) {

                    BlockState state = columnState(y, column);

                    pos.set(
                            worldX,
                            y,
                            worldZ
                    );

                    chunk.setBlockState(
                            pos,
                            state,
                            0
                    );
                }

            }
        }

        return CompletableFuture.completedFuture(
                chunk
        );
    }

    @Override
    public int getHeight(
            int x,
            int z,
            Heightmap.Type heightmap,
            HeightLimitView world,
            NoiseConfig noiseConfig
    ) {
        BlockColumn column = blockColumn(x, z);
        return (heightmap == Heightmap.Type.OCEAN_FLOOR
                || heightmap == Heightmap.Type.OCEAN_FLOOR_WG
                ? column.terrainHeight() : column.top()) + 1;
    }

    @Override
    public VerticalBlockSample getColumnSample(
            int x,
            int z,
            HeightLimitView world,
            NoiseConfig noiseConfig
    ) {
        BlockState[] states =
                new BlockState[WORLD_HEIGHT];

        Arrays.fill(
                states,
                Blocks.AIR.getDefaultState()
        );

        BlockColumn column = blockColumn(x, z);

        BiomeIntent intent = surfaceIntent(x, z);

        for (
                int y = DEVELOPMENT_TERRAIN_MIN_Y;
                y <= column.top();
                y++
        ) {
            states[y - MIN_Y] =
                    columnState(y, column);
        }

        return new VerticalBlockSample(
                MIN_Y,
                states
        );
    }

    private BlockColumn blockColumn(
            int worldX,
            int worldZ
    ) {

        var sampled = terrainSampler().sampleColumn(worldX, worldZ);
        // Match TerrainColumn's wet-footprint contract at shallow banks.
        int height = clampHeight((int) Math.floor(sampled.terrainElevation()));
        int waterHeight = Double.isFinite(sampled.waterSurfaceElevation())
                ? clampHeight((int) Math.floor(sampled.waterSurfaceElevation()))
                : MIN_Y - 1;
        BiomeIntent intent = getBiomeSource() instanceof FiniteBiomeSource finite
                ? finite.intentAtBlock(worldX, worldZ) : BiomeIntent.TEMPERATE_FOREST;
        return new BlockColumn(height, waterHeight, intent);
    }

    private static int clampHeight(int height) {
        return Math.max(DEVELOPMENT_TERRAIN_MIN_Y, Math.min(MAX_Y, height));
    }

    private static BlockState columnState(int y, BlockColumn column) {
        if (y > column.terrainHeight()) {
            return y <= column.waterHeight()
                    ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
        }
        boolean submerged = column.waterHeight() > column.terrainHeight();
        boolean coastal = switch (column.intent()) {
            case SANDY_BEACH, COLD_BEACH, GRAVEL_BEACH, ROCKY_COAST -> true;
            default -> false;
        };
        if (submerged && !coastal) {
            return (y >= column.terrainHeight() - 3 ? Blocks.GRAVEL : Blocks.STONE)
                    .getDefaultState();
        }
        return CoastalSurfaceMaterials.stateAt(y, column.terrainHeight(), column.intent());
    }

    private record BlockColumn(int terrainHeight, int waterHeight, BiomeIntent intent) {
        int top() {
            return Math.max(terrainHeight, waterHeight);
        }
    }

    private BiomeIntent surfaceIntent(int x, int z) {
        return getBiomeSource() instanceof FiniteBiomeSource finite
                ? finite.intentAtBlock(x, z) : BiomeIntent.TEMPERATE_FOREST;
    }

    @Override
    public void buildSurface(
            ChunkRegion region,
            StructureAccessor structures,
            NoiseConfig noiseConfig,
            Chunk chunk
    ) {
        // Column materials and planned water are placed during populateNoise.
    }

    @Override
    public void carve(
            ChunkRegion region,
            long seed,
            NoiseConfig noiseConfig,
            BiomeAccess biomeAccess,
            StructureAccessor structures,
            Chunk chunk
    ) {
        // No caves yet.
    }

    @Override
    public void populateEntities(
            ChunkRegion region
    ) {
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinimumY() {
        return MIN_Y;
    }

    @Override
    public int getWorldHeight() {
        return WORLD_HEIGHT;
    }

    @Override
    public void appendDebugHudText(
            List<String> text,
            NoiseConfig noiseConfig,
            BlockPos pos
    ) {
        text.add(
                "Finite Worlds generator"
        );
        text.add("Finite Worlds seed=" + (seedBound ? Long.toString(seed) : "awaiting world seed"));
        if (!seedBound) {
            return;
        }
        BlockColumn column = blockColumn(pos.getX(), pos.getZ());
        text.add("Finite Worlds ground Y=" + column.terrainHeight()
                + (column.waterHeight() > column.terrainHeight()
                ? ", water Y=" + column.waterHeight() : ""));
    }
}
