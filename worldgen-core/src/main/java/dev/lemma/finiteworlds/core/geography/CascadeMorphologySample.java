package dev.lemma.finiteworlds.core.geography;

/**
 * Decomposed Cascade mountain morphology for one normalized world position.
 *
 * All morphology fields except uplift are normalized to 0..1.  The uplift
 * value is expressed in terrain elevation units and is ready to be added to
 * the macro elevation field (subject to any shoreline fade applied by the
 * caller).
 */
public record CascadeMorphologySample(
        double envelope,
        double crestStructure,
        double ridgeRelief,
        double passSuppression,
        double uplift
) {

    public static CascadeMorphologySample empty() {
        return new CascadeMorphologySample(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}
