package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Diagnostic hydrologic lake planned from the conditioned macro surface.
 *
 * Pass 1I does not alter terrain or place water. It records the water level,
 * inundated footprint, depth, catchment scale, and downstream outlet that a
 * later hydrology / terrain pass can materialize.
 */
public record Lake(
        int id,
        LakeSourceType sourceType,
        int sourceId,
        List<Integer> sourceDepressionIds,
        int footprintCellCount,
        int catchmentCellCount,
        float waterSurfaceElevation,
        float minimumBedElevation,
        float maximumDepth,
        float meanDepth,
        double surfaceAreaBlocksSquared,
        double catchmentAreaBlocksSquared,
        double estimatedVolumeBlocksCubed,
        double suitabilityScore,
        int outletX,
        int outletZ,
        int outletTargetX,
        int outletTargetZ,
        int outletTargetDepressionId,
        int outletTargetCompoundGroupId,
        int downstreamLakeId
) {

    public Lake {
        sourceDepressionIds =
                List.copyOf(sourceDepressionIds);
    }

    /** Move the basin down without changing its footprint, depth or volume. */
    public Lake lowerWaterSurfaceTo(double ceiling) {
        float level=(float)Math.min(waterSurfaceElevation,ceiling);
        if(level>=waterSurfaceElevation)return this;
        float shift=waterSurfaceElevation-level;
        return new Lake(id,sourceType,sourceId,sourceDepressionIds,footprintCellCount,catchmentCellCount,
            level,minimumBedElevation-shift,maximumDepth,meanDepth,surfaceAreaBlocksSquared,
            catchmentAreaBlocksSquared,estimatedVolumeBlocksCubed,suitabilityScore,outletX,outletZ,
            outletTargetX,outletTargetZ,outletTargetDepressionId,outletTargetCompoundGroupId,downstreamLakeId);
    }

    public boolean hasOutlet() {
        return outletX >= 0
                && outletZ >= 0;
    }

    public boolean drainsToOpenBoundary() {
        return outletTargetDepressionId < 0
                && downstreamLakeId < 0;
    }

    public boolean drainsToAnotherLake() {
        return downstreamLakeId >= 0;
    }
}
