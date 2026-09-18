package dev.lemma.finiteworlds.core.geography;

/**
 * One globally planned landmark Cascade volcano.
 *
 * Coordinates are expressed in Cascade-local space:
 * signedDistance is perpendicular to the mountain spine (positive west),
 * while arcPosition is distance along the spine.
 *
 * The landmark volcano intentionally owns a vertical budget that is separate
 * from ordinary Cascade uplift. This lets normal mountains retain their
 * existing hierarchy while guaranteeing one summit that is unmistakably
 * dominant at world scale.
 *
 * Pass 2 also stores stable morphology controls. These are not final erosion
 * products; they define the volcanic edifice that later hydrology, glaciers,
 * and geology will modify.
 */
public record CascadeVolcano(
        double signedDistance,
        double arcPosition,
        double radiusAcross,
        double radiusAlong,
        double heightMultiplier,
        double rotationRadians,
        double asymmetry,
        double asymmetryPhase,
        double summitOffsetAcross,
        double summitOffsetAlong,
        int buttressCount,
        double buttressPhase,
        double buttressStrength,
        double craterRadiusFraction,
        double craterDepthFraction,
        double flankRoughness
) {

    public double maximumRadius() {
        return Math.max(
                radiusAcross,
                radiusAlong
        );
    }
}
