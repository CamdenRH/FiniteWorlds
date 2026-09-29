package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Derived network hierarchy metadata for one explicit river segment.
 *
 * This information is computed from the directed river graph and is entirely
 * non-destructive. It is intended for later discharge, width, valley, and
 * river-significance decisions.
 */
public record RiverSegmentHierarchy(
        int segmentId,
        int strahlerOrder,
        List<Integer> upstreamSegmentIds,
        int downstreamSegmentId,
        int upstreamSegmentCount,
        double totalUpstreamNetworkLengthBlocks,
        double longestSourceDistanceBlocks,
        double distanceToMouthBlocks
) {

    public RiverSegmentHierarchy {
        upstreamSegmentIds =
                List.copyOf(upstreamSegmentIds);
    }
}
