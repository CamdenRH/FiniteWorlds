package dev.lemma.finiteworlds.core.geography;

import java.util.ArrayList;
import java.util.List;

public final class MountainSpine {

    private final List<GeoPoint> points;

    public MountainSpine(
            List<GeoPoint> controlPoints,
            int samplesPerSegment
    ) {

        if (controlPoints.size() < 4) {
            throw new IllegalArgumentException(
                    "MountainSpine requires at least 4 control points."
            );
        }

        this.points =
                sampleCatmullRom(
                        controlPoints,
                        samplesPerSegment
                );
    }

    public List<GeoPoint> points() {
        return points;
    }


    /*
     * Returns the shortest normalized-coordinate
     * distance from the supplied location to the
     * sampled mountain spine.
     */
    public double distanceTo(
            double x,
            double z
    ) {

        double minimum =
                Double.POSITIVE_INFINITY;

        for (
                int i = 0;
                i < points.size() - 1;
                i++
        ) {

            GeoPoint a =
                    points.get(i);

            GeoPoint b =
                    points.get(i + 1);

            double distance =
                    distanceToSegment(
                            x,
                            z,
                            a.x(),
                            a.z(),
                            b.x(),
                            b.z()
                    );

            minimum =
                    Math.min(
                            minimum,
                            distance
                    );
        }

        return minimum;
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

        double abLengthSquared =
                abX * abX
                        + abZ * abZ;

        if (abLengthSquared == 0.0) {

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
                        / abLengthSquared;

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


    private static List<GeoPoint> sampleCatmullRom(
            List<GeoPoint> controls,
            int samplesPerSegment
    ) {

        List<GeoPoint> result =
                new ArrayList<>();

        for (
                int i = 0;
                i < controls.size() - 1;
                i++
        ) {

            GeoPoint p0 =
                    controls.get(
                            Math.max(
                                    0,
                                    i - 1
                            )
                    );

            GeoPoint p1 =
                    controls.get(i);

            GeoPoint p2 =
                    controls.get(i + 1);

            GeoPoint p3 =
                    controls.get(
                            Math.min(
                                    controls.size() - 1,
                                    i + 2
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

        result.add(
                controls.get(
                        controls.size() - 1
                )
        );

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
}