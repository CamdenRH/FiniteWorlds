package dev.lemma.finiteworlds.core.hydrology;

/**
 * Hydrology Pass 2E metadata describing the provisional hydraulic significance
 * and physical scale of one explicit river segment.
 *
 * potentialMagnitude is dimensionless and assumes uniform runoff. It should be
 * interpreted as relative drainage capacity, not real-world discharge.
 */
public record RiverSegmentMagnitude(
        int segmentId,
        RiverScale riverScale,
        double potentialMagnitude,
        double channelSlope,
        double provisionalWidthBlocks,
        double provisionalDepthBlocks
) {
}
