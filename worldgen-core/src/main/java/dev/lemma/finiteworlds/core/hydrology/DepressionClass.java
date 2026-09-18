package dev.lemma.finiteworlds.core.hydrology;

/**
 * Provisional hydrologic interpretation of a raw depression.
 *
 * Pass 1D is diagnostic only. These labels describe how a basin currently
 * looks in the macro terrain and do not yet decide whether it will be filled,
 * breached, merged, or preserved as a lake.
 */
public enum DepressionClass {
    MICRO_PIT,
    SHALLOW_BASIN,
    ALPINE_BASIN,
    MAJOR_INTERIOR_BASIN,
    COMPOUND_BASIN
}
