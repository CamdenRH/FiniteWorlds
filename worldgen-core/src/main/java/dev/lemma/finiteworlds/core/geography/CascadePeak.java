package dev.lemma.finiteworlds.core.geography;

/**
 * One explicitly planned Cascade summit in Cascade-local coordinates.
 *
 * signedDistance is positive west of the Cascade spine and negative east.
 * arcPosition is physical distance along the sampled mountain spine.
 */
public record CascadePeak(
        CascadePeakClass peakClass,
        double signedDistance,
        double arcPosition,
        double radiusAcross,
        double radiusAlong,
        double strength,
        double sharpness
) {

    public double maximumRadius() {
        return Math.max(
                radiusAcross,
                radiusAlong
        );
    }
}
