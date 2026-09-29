package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Hydrology Pass 2I cross-section plan for one explicit river segment.
 */
public record RiverSegmentCrossSection(
        int segmentId,
        boolean lakePassage,
        List<RiverCrossSectionPoint> points,
        double maximumBankfullHalfWidthBlocks,
        double maximumBankfullDepthBlocks,
        double meanAbsoluteLateralBias
) {

    public RiverSegmentCrossSection {
        points = List.copyOf(points);
    }

    public static RiverSegmentCrossSection lakePassage(
            int segmentId
    ) {
        return new RiverSegmentCrossSection(
                segmentId,
                true,
                List.of(),
                0.0,
                0.0,
                0.0
        );
    }
}
