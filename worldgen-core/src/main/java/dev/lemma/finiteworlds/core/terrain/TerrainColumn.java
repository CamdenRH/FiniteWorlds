package dev.lemma.finiteworlds.core.terrain;

/** The solid surface and optional water surface of one block column. */
public record TerrainColumn(
        double terrainElevation,
        double waterSurfaceElevation,
        boolean river,
        boolean lake
) {
    public boolean hasWater() {
        return Double.isFinite(waterSurfaceElevation)
                && Math.floor(waterSurfaceElevation) > Math.floor(terrainElevation);
    }
}
