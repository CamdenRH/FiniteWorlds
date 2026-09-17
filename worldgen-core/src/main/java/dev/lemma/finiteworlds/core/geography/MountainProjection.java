package dev.lemma.finiteworlds.core.geography;

/**
 * Closest-point information for a sampled mountain spine.
 *
 * signedDistance follows the existing Cascadia convention:
 * positive = west side, negative = east side.
 * progress runs from 0 at the first spine control to 1 at the last.
 */
public record MountainProjection(
        double signedDistance,
        double progress,
        double closestX,
        double closestZ,
        double tangentX,
        double tangentZ
) {
}
