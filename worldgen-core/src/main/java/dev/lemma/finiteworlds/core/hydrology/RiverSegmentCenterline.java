package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * High-resolution physical centerline for one explicit river segment.
 *
 * Lake-passage segments remain semantic and intentionally contain no physical
 * points. CHANNEL segments retain the original graph endpoints while their
 * interior geometry is smoothed and allowed to wander inside a bounded
 * terrain-aware corridor around the macro D8 route.
 */
public record RiverSegmentCenterline(
        int segmentId,
        boolean lakePassage,
        List<RiverCenterlinePoint> points,
        double centerlineLengthBlocks,
        double referenceLengthBlocks,
        double sinuosityRatio,
        double maximumLateralOffsetBlocks,
        double lateralFreedom
) {

    public RiverSegmentCenterline {
        points =
                List.copyOf(points);
    }

    public static RiverSegmentCenterline lakePassage(
            int segmentId
    ) {
        return new RiverSegmentCenterline(
                segmentId,
                true,
                List.of(),
                0.0,
                0.0,
                1.0,
                0.0,
                0.0
        );
    }
}
