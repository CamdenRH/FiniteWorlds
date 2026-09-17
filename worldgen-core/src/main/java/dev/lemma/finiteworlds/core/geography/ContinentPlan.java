package dev.lemma.finiteworlds.core.geography;

import java.util.List;

public final class ContinentPlan {

    /*
     * Controls how strongly neighboring geographic
     * primitives blend together.
     *
     * Higher values make transitions smoother.
     */
    private static final double LAND_BLEND =
            0.055;

    private static final double CUTOUT_BLEND =
            0.040;


    private final List<GeoShape> landShapes;
    private final List<GeoShape> cutoutShapes;

    private final MountainSystem mountainSystem;


    public ContinentPlan(
            List<GeoShape> landShapes,
            List<GeoShape> cutoutShapes,
            MountainSystem mountainSystem
    ) {

        this.landShapes =
                List.copyOf(
                        landShapes
                );

        this.cutoutShapes =
                List.copyOf(
                        cutoutShapes
                );

        this.mountainSystem =
                mountainSystem;
    }


    /*
     * Raw macro-geographic field.
     *
     * This is NOT the final coastline.
     *
     * ContinentSampler will heavily deform this
     * before turning it into the actual land mask.
     */
    public double sampleBase(
            double x,
            double z
    ) {

        if (landShapes.isEmpty()) {
            return -1.0;
        }


        /*
         * =====================================================
         * SMOOTH UNION OF LAND PRIMITIVES
         * =====================================================
         */

        double score =
                landShapes
                        .getFirst()
                        .sample(
                                x,
                                z
                        );

        for (
                int i = 1;
                i < landShapes.size();
                i++
        ) {

            double next =
                    landShapes
                            .get(i)
                            .sample(
                                    x,
                                    z
                            );

            score =
                    smoothMax(
                            score,
                            next,
                            LAND_BLEND
                    );
        }


        /*
         * =====================================================
         * SMOOTH SUBTRACTION OF BAYS / CHANNELS
         * =====================================================
         */

        for (
                GeoShape cutout :
                cutoutShapes
        ) {

            double cut =
                    -cutout.sample(
                            x,
                            z
                    );

            score =
                    smoothMin(
                            score,
                            cut,
                            CUTOUT_BLEND
                    );
        }

        return score;
    }


    public List<GeoShape> landShapes() {
        return landShapes;
    }


    public List<GeoShape> cutoutShapes() {
        return cutoutShapes;
    }


    public MountainSystem mountainSystem() {
        return mountainSystem;
    }


    /*
     * =========================================================
     * SMOOTH BOOLEAN OPERATIONS
     * =========================================================
     */

    private static double smoothMax(
            double a,
            double b,
            double blend
    ) {

        if (blend <= 0.0) {
            return Math.max(
                    a,
                    b
            );
        }

        double h =
                clamp01(
                        0.5
                                + 0.5
                                * (
                                a - b
                        )
                                / blend
                );

        return lerp(
                b,
                a,
                h
        )
                + blend
                * h
                * (
                1.0 - h
        );
    }


    private static double smoothMin(
            double a,
            double b,
            double blend
    ) {

        return -smoothMax(
                -a,
                -b,
                blend
        );
    }


    private static double lerp(
            double a,
            double b,
            double t
    ) {

        return a
                + (
                b - a
        ) * t;
    }


    private static double clamp01(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

}