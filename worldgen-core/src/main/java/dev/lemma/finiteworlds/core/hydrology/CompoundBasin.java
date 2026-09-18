package dev.lemma.finiteworlds.core.hydrology;

import java.util.List;

/**
 * Hydrologic description of one collapsed compound depression group.
 *
 * A compound basin contains two or more raw depressions whose first-spill
 * graph forms a directed cycle. Pass 1H treats the cycle as one hydrologic
 * unit and measures the lowest saddle that escapes the combined catchment.
 *
 * This record is diagnostic only. It does not imply whether the combined
 * basin should later be breached, filled, or preserved as a lake complex.
 */
public record CompoundBasin(
        int groupId,
        List<Integer> memberDepressionIds,
        int footprintCellCount,
        int catchmentCellCount,
        float minimumElevation,
        float mergeElevation,
        float externalSpillElevation,
        float maximumDepthToExternalSpill,
        float externalRiseAboveMerge,
        int spillX,
        int spillZ,
        int spillTargetX,
        int spillTargetZ,
        int spillTargetDepressionId,
        int spillTargetCompoundGroupId
) {

    public CompoundBasin {
        memberDepressionIds =
                List.copyOf(memberDepressionIds);
    }

    public int memberCount() {
        return memberDepressionIds.size();
    }

    public boolean hasExternalSpill() {
        return spillX >= 0
                && spillZ >= 0;
    }

    public boolean drainsDirectlyOutsideDepressionSystem() {
        return spillTargetDepressionId < 0;
    }

    public boolean spillsToAnotherCompoundGroup() {
        return spillTargetCompoundGroupId >= 0
                && spillTargetCompoundGroupId != groupId;
    }
}
