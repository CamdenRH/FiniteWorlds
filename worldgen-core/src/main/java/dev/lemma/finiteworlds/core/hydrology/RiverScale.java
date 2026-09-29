package dev.lemma.finiteworlds.core.hydrology;

/**
 * Provisional physical scale for a macro river segment.
 *
 * These classes are not final Minecraft channel widths. They are derived from
 * the current uniform-runoff hydrology model and will later be revised by
 * climate/runoff and high-resolution channel synthesis.
 */
public enum RiverScale {
    CREEK,
    STREAM,
    SMALL_RIVER,
    RIVER,
    LARGE_RIVER,
    MAJOR_RIVER
}
