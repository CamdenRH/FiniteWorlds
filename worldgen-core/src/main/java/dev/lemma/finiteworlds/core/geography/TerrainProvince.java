package dev.lemma.finiteworlds.core.geography;

public enum TerrainProvince {
    OCEAN,
    COASTAL,
    COAST_RANGE,
    WESTERN_LOWLAND,
    CASCADE_FOOTHILLS,
    CASCADE_CORE,
    EASTERN_SLOPES,
    INTERIOR_PLATEAU;

    private static final TerrainProvince[] VALUES =
            values();

    public static TerrainProvince fromOrdinal(
            int ordinal
    ) {
        return VALUES[ordinal];
    }
}