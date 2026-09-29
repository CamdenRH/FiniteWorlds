package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Hydrology Pass 2H valley/floodplain envelope for one explicit river
 * segment. Lake passages remain semantic and intentionally have no corridor
 * points of their own.
 */
public record RiverSegmentValleyCorridor(
        int segmentId,
        boolean lakePassage,
        List<RiverValleyCorridorPoint> points,
        double maximumChannelHalfWidthBlocks,
        double maximumFloodplainHalfWidthBlocks,
        double maximumValleyHalfWidthBlocks,
        double meanConfinement
) {

    public RiverSegmentValleyCorridor {
        points = List.copyOf(points);
    }

    public static RiverSegmentValleyCorridor lakePassage(
            int segmentId
    ) {
        return new RiverSegmentValleyCorridor(
                segmentId,
                true,
                List.of(),
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}
