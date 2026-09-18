package dev.lemma.finiteworlds.core.geography;

/**
 * Decomposed Cascade mountain morphology for one normalized world position.
 *
 * Normalized morphology masks use 0..1. volcanoUplift and uplift are
 * expressed in terrain elevation units and are ready to be added to the macro
 * elevation field (subject to any shoreline fade applied by the caller).
 */
public record CascadeMorphologySample(
        double envelope,
        double regionalHeight,
        double rangeRelief,
        double crestStructure,
        double ridgeRelief,
        double peakRelief,
        double volcanoFoothillRelief,
        double volcanoRelief,
        double volcanoUpperCone,
        double volcanoRadialStructure,
        double volcanoCraterMask,
        double volcanoUplift,
        double passSuppression,
        double uplift
) {

    public static CascadeMorphologySample empty() {
        return new CascadeMorphologySample(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}
