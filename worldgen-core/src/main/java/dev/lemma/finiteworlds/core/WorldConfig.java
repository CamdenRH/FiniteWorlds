package dev.lemma.finiteworlds.core;

public record WorldConfig(
        int worldSizeBlocks,
        int blueprintResolution,
        int seaLevel
) {

    public static WorldConfig quickTest() {
        return new WorldConfig(
                16_384,
                1024,
                64
        );
    }

    public static WorldConfig production() {
        return new WorldConfig(
                65_536,
                1024,
                64
        );
    }

    public double blocksPerCell() {
        return (double) worldSizeBlocks
                / blueprintResolution;
    }
}