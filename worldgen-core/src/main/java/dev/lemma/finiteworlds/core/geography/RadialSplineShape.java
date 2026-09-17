package dev.lemma.finiteworlds.core.geography;

public final class RadialSplineShape
        implements GeoShape {

    private static final double TWO_PI =
            Math.PI * 2.0;

    private final double centerX;
    private final double centerZ;

    private final double radiusX;
    private final double radiusZ;

    private final double rotation;

    private final double cosRotation;
    private final double sinRotation;

    private final double[] radialScale;


    public RadialSplineShape(
            double centerX,
            double centerZ,
            double radiusX,
            double radiusZ,
            double rotation,
            double[] radialScale
    ) {

        if (radialScale.length < 4) {
            throw new IllegalArgumentException(
                    "RadialSplineShape requires at least 4 radial samples."
            );
        }

        this.centerX =
                centerX;

        this.centerZ =
                centerZ;

        this.radiusX =
                radiusX;

        this.radiusZ =
                radiusZ;

        this.rotation =
                rotation;

        this.cosRotation =
                Math.cos(rotation);

        this.sinRotation =
                Math.sin(rotation);

        this.radialScale =
                radialScale.clone();
    }


    @Override
    public double sample(
            double x,
            double z
    ) {

        double dx =
                x - centerX;

        double dz =
                z - centerZ;


        /*
         * Rotate into shape-local coordinates.
         */

        double localX =
                cosRotation * dx
                        + sinRotation * dz;

        double localZ =
                -sinRotation * dx
                        + cosRotation * dz;


        /*
         * Normalize the ellipse into circular coordinates.
         */

        double nx =
                localX / radiusX;

        double nz =
                localZ / radiusZ;


        double actualRadius =
                Math.sqrt(
                        nx * nx
                                + nz * nz
                );


        /*
         * Direction from shape center.
         */

        double angle =
                Math.atan2(
                        nz,
                        nx
                );

        if (angle < 0.0) {
            angle += TWO_PI;
        }


        double position =
                angle
                        / TWO_PI
                        * radialScale.length;

        int i1 =
                (int) Math.floor(
                        position
                );

        double t =
                position - i1;


        double p0 =
                radialScale[
                        mod(
                                i1 - 1,
                                radialScale.length
                        )
                        ];

        double p1 =
                radialScale[
                        mod(
                                i1,
                                radialScale.length
                        )
                        ];

        double p2 =
                radialScale[
                        mod(
                                i1 + 1,
                                radialScale.length
                        )
                        ];

        double p3 =
                radialScale[
                        mod(
                                i1 + 2,
                                radialScale.length
                        )
                        ];


        double targetRadius =
                catmullRom(
                        p0,
                        p1,
                        p2,
                        p3,
                        t
                );


        /*
         * Positive = inside
         * Zero     = coastline
         * Negative = outside
         */

        return targetRadius
                - actualRadius;
    }


    private static double catmullRom(
            double p0,
            double p1,
            double p2,
            double p3,
            double t
    ) {

        double t2 =
                t * t;

        double t3 =
                t2 * t;

        return 0.5 * (
                2.0 * p1

                        + (-p0 + p2)
                        * t

                        + (
                        2.0 * p0
                                - 5.0 * p1
                                + 4.0 * p2
                                - p3
                ) * t2

                        + (
                        -p0
                                + 3.0 * p1
                                - 3.0 * p2
                                + p3
                ) * t3
        );
    }


    private static int mod(
            int value,
            int modulus
    ) {

        int result =
                value % modulus;

        return result < 0
                ? result + modulus
                : result;
    }
}