package dev.lemma.finiteworlds.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class FiniteChunkGenerator
        extends ChunkGenerator {

    public static final int SEA_LEVEL = 64;

    public static final int MIN_Y = -64;
    public static final int WORLD_HEIGHT = 384;

    private final TerrainSampler terrainSampler;

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
                                    .optionalFieldOf(
                                            "seed",
                                            12345L
                                    )
                                    .forGetter(
                                            generator ->
                                                    generator.seed
                                    )

                    ).apply(
                            instance,
                            FiniteChunkGenerator::new
                    )
            );

    private final long seed;

    private final WorldBlueprint blueprint;

    public FiniteChunkGenerator(
            BiomeSource biomeSource,
            long seed
    ) {

        super(biomeSource);

        this.seed =
                seed;

        WorldConfig config =
                WorldConfig.quickTest();

        this.blueprint =
                new CascadiaGenerator()
                        .generate(
                                seed,
                                config
                        );

        this.terrainSampler =
                new TerrainSampler(
                        blueprint,
                        seed
                );
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

                int height =
                        surfaceHeight(
                                worldX,
                                worldZ
                        );

                for (
                        int y = MIN_Y;
                        y <= height;
                        y++
                ) {

                    BlockState state;

                    if (y == height) {
                        state =
                                Blocks.GRASS_BLOCK
                                        .getDefaultState();

                    } else if (y >= height - 3) {
                        state =
                                Blocks.DIRT
                                        .getDefaultState();

                    } else {
                        state =
                                Blocks.STONE
                                        .getDefaultState();
                    }

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

                if (height < SEA_LEVEL) {

                    for (
                            int y = height + 1;
                            y <= SEA_LEVEL;
                            y++
                    ) {

                        pos.set(
                                worldX,
                                y,
                                worldZ
                        );

                        chunk.setBlockState(
                                pos,
                                Blocks.WATER
                                        .getDefaultState(),
                                0
                        );
                    }
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
        return surfaceHeight(x, z) + 1;
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

        int surface =
                surfaceHeight(x, z);

        for (
                int y = MIN_Y;
                y <= surface;
                y++
        ) {
            states[y - MIN_Y] =
                    y == surface
                            ? Blocks.GRASS_BLOCK
                            .getDefaultState()
                            : Blocks.STONE
                            .getDefaultState();
        }

        if (surface < SEA_LEVEL) {
            for (
                    int y = surface + 1;
                    y <= SEA_LEVEL;
                    y++
            ) {
                states[y - MIN_Y] =
                        Blocks.WATER
                                .getDefaultState();
            }
        }

        return new VerticalBlockSample(
                MIN_Y,
                states
        );
    }

    private int surfaceHeight(
            int worldX,
            int worldZ
    ) {

        double elevation =
                terrainSampler
                        .surfaceElevationAt(
                                worldX,
                                worldZ
                        );

        int height =
                (int) Math.round(
                        elevation
                );

        int maximumY =
                MIN_Y
                        + WORLD_HEIGHT
                        - 1;

        return Math.max(
                MIN_Y,
                Math.min(
                        maximumY,
                        height
                )
        );
    }

    @Override
    public void buildSurface(
            ChunkRegion region,
            StructureAccessor structures,
            NoiseConfig noiseConfig,
            Chunk chunk
    ) {
        // Surface already placed for prototype.
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
    }
}