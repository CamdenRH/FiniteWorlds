package dev.lemma.finiteworlds.core.hydrology;

/**
 * One Hydrology Pass 2I planned cross-section sample. No terrain is modified
 * by this record; it describes the geometry later carving passes should build.
 */
public record RiverCrossSectionPoint(
        double blockX,
        double blockZ,
        double distanceBlocks,
        RiverReachType reachType,
        double plannedBedElevation,
        double bankfullHalfWidthBlocks,
        double bankfullDepthBlocks,
        double leftBankOffsetBlocks,
        double rightBankOffsetBlocks,
        double leftBankSlope,
        double rightBankSlope,
        double floodplainElevation,
        double innerValleyHalfWidthBlocks,
        double lateralBias,
        double confinement,
        double localGrade
) {
}
