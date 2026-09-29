package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Directed macro river segment between two explicit graph nodes.
 *
 * cellPath stores hydrology-grid indices from the start node through the end
 * node. It is diagnostic/topological only; later high-resolution river
 * synthesis will replace this coarse D8 geometry with smooth channels.
 */
public record RiverSegment(
        int id,
        RiverSegmentType type,
        int startNodeId,
        int endNodeId,
        List<Integer> cellPath,
        int lakeId,
        StreamClass maximumStreamClass,
        long startAccumulation,
        long endAccumulation,
        double lengthBlocks,
        double elevationDrop
) {

    public RiverSegment {
        cellPath =
                List.copyOf(cellPath);
    }
}
