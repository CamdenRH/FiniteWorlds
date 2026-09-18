package dev.lemma.finiteworlds.core.hydrology;

/**
 * Diagnostic description of one raw closed drainage basin.
 *
 * A depression is anchored to one raw D8 SINK.  Pass 1C measures the
 * catchment that terminates at that sink, finds the lowest saddle through
 * which that catchment could spill, and records the portion of the catchment
 * lying below that spill elevation.
 *
 * Nothing in this record implies how the basin will eventually be resolved.
 * Later passes may fill it, breach it, merge it with another depression, or
 * preserve it as a lake / closed basin.
 */
public record Depression(
        int id,
        int sinkX,
        int sinkZ,
        int footprintCellCount,
        int catchmentCellCount,
        float minimumElevation,
        float spillElevation,
        float maximumDepth,
        int spillX,
        int spillZ,
        int spillTargetX,
        int spillTargetZ,
        int spillTargetDepressionId
) {

    public boolean hasMeasuredSpill() {
        return spillX >= 0
                && spillZ >= 0
                && spillTargetX >= 0
                && spillTargetZ >= 0;
    }

    public boolean spillsOutsideDepressionSystem() {
        return spillTargetDepressionId < 0;
    }
}
