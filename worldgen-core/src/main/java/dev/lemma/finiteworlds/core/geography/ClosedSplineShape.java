package dev.lemma.finiteworlds.core.geography;

import java.util.ArrayList;
import java.util.List;

public final class ClosedSplineShape implements GeoShape {

    private final List<GeoPoint> boundary;

    /*
     * Converts normalized-coordinate distance
     * into a field roughly comparable with the
     * existing GeoShape values.
     */
    private final double distanceScale;

    public ClosedSplineShape(
            List<GeoPoint> controlPoints,
            int samplesPerSegment,
            double distanceScale
    ) {
        if (controlPoints.size() < 4) {
            throw new IllegalArgumentException(
                    "ClosedSplineShape requires at least 4 control points."
            );
        }

        this.boundary =
                sampleClosedCatmullRom(
                        controlPoints,
                        samplesPerSegment
                );

        this.distanceScale =
                distanceScale;
    }

    @Override
    public double sample(
            double x,
            double z
    ) {
        double minimumDistance =
                Double.POSITIVE_INFINITY;

        for (int i = 0; i < boundary.size(); i++) {

            GeoPoint a =
                    boundary.get(i);

            GeoPoint b =
                    boundary.get(
                            (i + 1)
                                    % boundary.size()
                    );

            double distance =
                    distanceToSegment(
                            x,
                            z,
                            a.x(),
                            a.z(),
                            b.x(),
                            b.z()
                    );

            minimumDistance =
                    Math.min(
                            minimumDistance,
                            distance
                    );
        }

        boolean inside =
                isInside(
                        x,
                        z
                );

        double signedDistance =
                inside
                        ? minimumDistance
                        : -minimumDistance;

        return signedDistance
                / distanceScale;
    }

    public List<GeoPoint> boundary() {
        return boundary;
    }

    private boolean isInside(
            double x,
            double z
    ) {
        boolean inside =
                false;

        int count =
                boundary.size();

        for (
                int i = 0,
                j = count - 1;
                i < count;
                j = i++
        ) {

            GeoPoint a =
                    boundary.get(i);

            GeoPoint b =
                    boundary.get(j);

            boolean intersects =
                    (
                            (a.z() > z)
                                    != (b.z() > z)
                    )
                            &&
                            (
                                    x
                                            < (
                                            b.x()
                                                    - a.x()
                                    )
                                            * (
                                            z
                                                    - a.z()
                                    )
                                            / (
                                            b.z()
                                                    - a.z()
                                    )
                                            + a.x()
                            );

            if (intersects) {
                inside =
                        !inside;
            }
        }

        return inside;
    }

    private static double distanceToSegment(
            double px,
            double pz,
            double ax,
            double az,
            double bx,
            double bz
    ) {

        double abX =
                bx - ax;

        double abZ =
                bz - az;

        double apX =
                px - ax;

        double apZ =
                pz - az;

        double lengthSquared =
                abX * abX
                        + abZ * abZ;

        if (lengthSquared <= 0.0) {

            double dx =
                    px - ax;

            double dz =
                    pz - az;

            return Math.sqrt(
                    dx * dx
                            + dz * dz
            );
        }

        double t =
                (
                        apX * abX
                                + apZ * abZ
                )
                        / lengthSquared;

        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );

        double closestX =
                ax + abX * t;

        double closestZ =
                az + abZ * t;

        double dx =
                px - closestX;

        double dz =
                pz - closestZ;

        return Math.sqrt(
                dx * dx
                        + dz * dz
        );
    }

    private static List<GeoPoint>
    sampleClosedCatmullRom(
            List<GeoPoint> controls,
            int samplesPerSegment
    ) {

        List<GeoPoint> result =
                new ArrayList<>();

        int count =
                controls.size();

        for (int i = 0; i < count; i++) {

            GeoPoint p0 =
                    controls.get(
                            mod(
                                    i - 1,
                                    count
                            )
                    );

            GeoPoint p1 =
                    controls.get(i);

            GeoPoint p2 =
                    controls.get(
                            mod(
                                    i + 1,
                                    count
                            )
                    );

            GeoPoint p3 =
                    controls.get(
                            mod(
                                    i + 2,
                                    count
                            )
                    );

            for (
                    int sample = 0;
                    sample < samplesPerSegment;
                    sample++
            ) {

                double t =
                        sample
                                / (double) samplesPerSegment;

                double x =
                        catmullRom(
                                p0.x(),
                                p1.x(),
                                p2.x(),
                                p3.x(),
                                t
                        );

                double z =
                        catmullRom(
                                p0.z(),
                                p1.z(),
                                p2.z(),
                                p3.z(),
                                t
                        );

                result.add(
                        new GeoPoint(
                                x,
                                z
                        )
                );
            }
        }

        return List.copyOf(
                result
        );
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
                        + (-p0 + p2) * t
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