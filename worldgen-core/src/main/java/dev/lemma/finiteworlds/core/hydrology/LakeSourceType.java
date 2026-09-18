package dev.lemma.finiteworlds.core.hydrology;

/**
 * Origin of one planned hydrologic lake.
 *
 * A SIMPLE_DEPRESSION lake comes directly from a Pass 1E PRESERVE_LAKE
 * decision. A COMPOUND_BASIN lake represents a cyclic depression group from
 * Pass 1H that is deep / broad enough to behave as one connected water body
 * before reaching its lowest external spill.
 */
public enum LakeSourceType {
    SIMPLE_DEPRESSION,
    COMPOUND_BASIN
}
