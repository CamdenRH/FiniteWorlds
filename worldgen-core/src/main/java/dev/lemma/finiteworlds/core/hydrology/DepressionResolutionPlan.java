package dev.lemma.finiteworlds.core.hydrology;

/**
 * Diagnostic-only proposed resolution for one raw depression.
 *
 * estimatedFillVolumeBlocks is the approximate volume that would be required
 * to raise the measured below-spill footprint to its first spill elevation.
 * It is recorded now so later passes can compare the cost of filling against
 * breaching without recomputing depression geometry.
 */
public record DepressionResolutionPlan(
        int depressionId,
        DepressionResolutionAction action,
        double estimatedFillVolumeBlocks,
        double lakeSuitabilityScore
) {
}
