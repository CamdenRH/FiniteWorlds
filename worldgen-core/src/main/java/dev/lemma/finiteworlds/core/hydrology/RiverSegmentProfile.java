package dev.lemma.finiteworlds.core.hydrology;

/**
 * Hydrology Pass 2F longitudinal/continuity metadata for one explicit river
 * segment.
 *
 * Width and depth are stored at both graph nodes so later high-resolution
 * centerline synthesis can interpolate continuously through confluences and
 * lake transitions instead of treating each coarse segment independently.
 */
public record RiverSegmentProfile(
        int segmentId,
        double startDistanceFromSourceBlocks,
        double endDistanceFromSourceBlocks,
        double startDistanceToMouthBlocks,
        double endDistanceToMouthBlocks,
        double startElevation,
        double endElevation,
        double rawWidthBlocks,
        double rawDepthBlocks,
        double smoothedStartWidthBlocks,
        double smoothedEndWidthBlocks,
        double smoothedStartDepthBlocks,
        double smoothedEndDepthBlocks,
        double rawStartWidthRatio,
        double widthAdjustmentFraction,
        double depthAdjustmentFraction
) {

    public double averageSmoothedWidthBlocks() {
        return 0.5
                * (
                smoothedStartWidthBlocks
                        + smoothedEndWidthBlocks
        );
    }

    public double averageSmoothedDepthBlocks() {
        return 0.5
                * (
                smoothedStartDepthBlocks
                        + smoothedEndDepthBlocks
        );
    }

    public double longitudinalElevationDrop() {
        return Math.max(
                0.0,
                startElevation - endElevation
        );
    }
}
