package dev.lemma.finiteworlds.core.terrain;

import dev.lemma.finiteworlds.core.WorldBlueprint;

public final class TerrainSampler {

    private final WorldBlueprint blueprint;

    private final LocalTerrainSampler localTerrain;

    public TerrainSampler(
            WorldBlueprint blueprint,
            long worldSeed
    ) {

        this.blueprint =
                blueprint;

        this.localTerrain =
                new LocalTerrainSampler(
                        worldSeed,
                        blueprint.config().seaLevel()
                );
    }

    public double surfaceElevationAt(
            double x,
            double z
    ) {

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

        return macro + local;
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