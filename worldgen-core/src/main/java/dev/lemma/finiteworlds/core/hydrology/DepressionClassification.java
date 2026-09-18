package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.geography.TerrainProvince;

/**
 * Diagnostic classification and network metadata for one raw depression.
 *
 * The downstream graph is intentionally left untouched. A compound group is
 * a true directed cycle in the first-spill graph (usually two neighboring raw
 * basins that identify the same saddle as their cheapest mutual escape).
 */
public record DepressionClassification(
        int depressionId,
        DepressionClass depressionClass,
        float meanDepth,
        float coastDistanceBlocks,
        TerrainProvince terrainProvince,
        int downstreamDepressionId,
        int terminalDepressionId,
        int chainDepth,
        int directUpstreamCount,
        int compoundGroupId,
        int compoundMemberCount
) {

    public boolean drainsDirectlyOutsideDepressionSystem() {
        return downstreamDepressionId < 0;
    }

    public boolean belongsToCompoundGroup() {
        return compoundGroupId >= 0;
    }
}
