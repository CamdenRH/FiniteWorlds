package dev.lemma.finiteworlds.core.geography;

public record PhysiographySample(
        TerrainProvince province,
        double coastalMask,
        double coastRangeMask,
        double westernLowlandMask,
        double cascadeMask,
        double cascadeCoreMask,
        double cascadeFoothillMask,
        double easternSlopeMask,
        double plateauMask,
        double signedCascadeDistance,
        double cascadeProgress
) {

    public static PhysiographySample ocean() {
        return new PhysiographySample(
                TerrainProvince.OCEAN,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}
