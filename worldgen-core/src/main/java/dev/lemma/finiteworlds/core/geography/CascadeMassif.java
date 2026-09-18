package dev.lemma.finiteworlds.core.geography;

/**
 * One planned high-mountain group along the Cascade spine.
 *
 * Positions are expressed in Cascade-local arc-length coordinates so later
 * systems can attach peaks, ridges, glaciers, and drainage features to the
 * same persistent mountain structure.
 */
public record CascadeMassif(
        double centerArc,
        double halfWidth,
        double strength
) {
}
