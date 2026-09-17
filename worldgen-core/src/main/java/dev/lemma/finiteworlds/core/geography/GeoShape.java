package dev.lemma.finiteworlds.core.geography;

public interface GeoShape {

    /*
     * Positive = inside shape
     * Zero     = edge
     * Negative = outside shape
     */
    double sample(
            double x,
            double z
    );
}