package dev.lemma.finiteworlds.worldgen;

import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Coastal layers follow semantic intent even when gravel and rock share a biome fallback. */
final class CoastalSurfaceMaterials {
    private CoastalSurfaceMaterials() {
    }

    static BlockState stateAt(int y, int surface, BiomeIntent intent) {
        int depth = surface - y;
        if (depth < 0) {
            return Blocks.AIR.getDefaultState();
        }
        return switch (intent) {
            case SANDY_BEACH, COLD_BEACH -> (depth <= 3 ? Blocks.SAND
                    : depth <= 7 ? Blocks.SANDSTONE : Blocks.STONE).getDefaultState();
            case GRAVEL_BEACH -> (depth <= 3 ? Blocks.GRAVEL : Blocks.STONE).getDefaultState();
            case ROCKY_COAST -> Blocks.STONE.getDefaultState();
            default -> (depth == 0 ? Blocks.GRASS_BLOCK
                    : depth <= 3 ? Blocks.DIRT : Blocks.STONE).getDefaultState();
        };
    }
}
