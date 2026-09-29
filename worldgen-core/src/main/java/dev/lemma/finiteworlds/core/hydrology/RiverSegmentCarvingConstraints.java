package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Hydrology Pass 2J terrain-modification limits for one explicit river segment.
 */
public record RiverSegmentCarvingConstraints(
        int segmentId,
        boolean lakePassage,
        List<RiverCarvingConstraintPoint> points,
        double maximumCutDepthBlocks,
        double maximumFillHeightBlocks,
        double meanCutStrength
) {

    public RiverSegmentCarvingConstraints {
        points = List.copyOf(points);
    }

    public static RiverSegmentCarvingConstraints lakePassage(
            int segmentId
    ) {
        return new RiverSegmentCarvingConstraints(
                segmentId,
                true,
                List.of(),
                0.0,
                0.0,
                0.0
        );
    }
}
