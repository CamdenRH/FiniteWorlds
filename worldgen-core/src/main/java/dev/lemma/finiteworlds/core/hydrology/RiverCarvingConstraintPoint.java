package dev.lemma.finiteworlds.core.hydrology;

/**
 * Hydrology Pass 2J terrain-integration contract for one river sample.
 *
 * The values describe what later terrain passes may do; this pass itself does
 * not mutate the world elevation field.
 */
public record RiverCarvingConstraintPoint(
        double blockX,
        double blockZ,
        double distanceBlocks,
        RiverReachType reachType,
        double targetBedElevation,
        double targetBankElevation,
        double targetFloodplainElevation,
        double maximumCutDepthBlocks,
        double maximumFillHeightBlocks,
        double channelBlendHalfWidthBlocks,
        double floodplainBlendHalfWidthBlocks,
        double valleyBlendHalfWidthBlocks,
        double cutStrength,
        double fillStrength,
        double junctionBlend,
        double confinement,
        double localGrade
) {
}
