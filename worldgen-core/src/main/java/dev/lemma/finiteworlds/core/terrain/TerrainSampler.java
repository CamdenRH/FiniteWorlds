package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.WorldBlueprint;

public final class TerrainSampler {

    private final WorldBlueprint blueprint;

    private final LocalTerrainSampler localTerrain;

    private final HydrologicTerrainSampler hydrologicTerrain;

    public TerrainSampler(
            WorldBlueprint blueprint,
            long worldSeed
    ) {

        this.blueprint =
                blueprint;

        this.localTerrain =
                new LocalTerrainSampler(
                        worldSeed,
                        blueprint
                );

        this.hydrologicTerrain = new HydrologicTerrainSampler(blueprint);
    }

    public double surfaceElevationAt(
            double x,
            double z
    ) {

        return sampleColumn(x, z).terrainElevation();
    }

    public TerrainColumn sampleColumn(double x, double z) {

        double macro =
                blueprint.smoothElevationAtBlock(
                        x,
                        z
                );

        double local =
                localTerrain.sample(
                        x,
                        z,
                        macro
                );

        return hydrologicTerrain.sample(x, z, macro + local);
    }

    public double macroElevationAt(
            double x,
            double z
    ) {

        return blueprint
                .smoothElevationAtBlock(
                        x,
                        z
                );
    }
}
