package dev.lemma.finiteworlds.core.geography;

import java.util.ArrayList;
import java.util.List;

public final class TaperedSplineShape
        implements GeoShape {

    private final List<GeoPoint> path;

    private final double startWidth;
    private final double endWidth;

    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    public TaperedSplineShape(
            List<GeoPoint> controlPoints,
            int samplesPerSegment,
            double startWidth,
            double endWidth
    ) {

        if (controlPoints.size() < 2) {
            throw new IllegalArgumentException(
                    "TaperedSplineShape requires at least 2 control points."
            );
        }

        this.path =
                sampleSpline(
                        controlPoints,
                        samplesPerSegment
                );

        double maxWidth =
                Math.max(
                        startWidth,
                        endWidth
                );

        double localMinX =
                Double.POSITIVE_INFINITY;

        double localMaxX =
                Double.NEGATIVE_INFINITY;

        double localMinZ =
                Double.POSITIVE_INFINITY;

        double localMaxZ =
                Double.NEGATIVE_INFINITY;


        for (
                GeoPoint point :
                path
        ) {

            localMinX =
                    Math.min(
                            localMinX,
                            point.x()
                    );

            localMaxX =
                    Math.max(
                            localMaxX,
                            point.x()
                    );

            localMinZ =
                    Math.min(
                            localMinZ,
                            point.z()
                    );

            localMaxZ =
                    Math.max(
                            localMaxZ,
                            point.z()
                    );
        }


        this.minX =
                localMinX
                        - maxWidth;

        this.maxX =
                localMaxX
                        + maxWidth;

        this.minZ =
                localMinZ
                        - maxWidth;

        this.maxZ =
                localMaxZ
                        + maxWidth;

        this.startWidth =
                startWidth;

        this.endWidth =
                endWidth;
    }

    @Override
    public double sample(
            double x,
            double z
    ) {

        if (
                x < minX
                        || x > maxX
                        || z < minZ
                        || z > maxZ
        ) {

            /*
             * Definitely outside the cutout.
             */
            return -1.0;
        }

        double best =
                -Double.MAX_VALUE;

        int segmentCount =
                path.size() - 1;

        for (
                int i = 0;
                i < segmentCount;
                i++
        ) {

            GeoPoint a =
                    path.get(i);

            GeoPoint b =
                    path.get(i + 1);

            SegmentResult result =
                    distanceToSegment(
                            x,
                            z,
                            a,
                            b
                    );

            double globalT =
                    (
                            i + result.t()
                    )
                            / segmentCount;

            /*
             * Power > 1 keeps the mouth wide for longer,
             * then narrows more aggressively inland.
             */
            double taperT =
                    Math.pow(
                            globalT,
                            1.35
                    );

            double width =
                    lerp(
                            startWidth,
                            endWidth,
                            taperT
                    );

            double field =
                    1.0
                            - result.distance()
                            / width;

            best =
                    Math.max(
                            best,
                            field
                    );
        }

        return best;
    }


    private static List<GeoPoint> sampleSpline(
            List<GeoPoint> controls,
            int samplesPerSegment
    ) {

        if (controls.size() == 2) {
            return List.copyOf(
                    controls
            );
        }

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

                result.add(
                        new GeoPoint(
                                catmullRom(
                                        p0.x(),
                                        p1.x(),
                                        p2.x(),
                                        p3.x(),
                                        t
                                ),

                                catmullRom(
                                        p0.z(),
                                        p1.z(),
                                        p2.z(),
                                        p3.z(),
                                        t
                                )
                        )
                );
            }
        }

        result.add(
                controls.getLast()
        );

        return List.copyOf(
                result
        );
    }


    private static SegmentResult distanceToSegment(
            double px,
            double pz,
            GeoPoint a,
            GeoPoint b
    ) {

        double abX =
                b.x() - a.x();

        double abZ =
                b.z() - a.z();

        double apX =
                px - a.x();

        double apZ =
                pz - a.z();

        double lengthSquared =
                abX * abX
                        + abZ * abZ;

        if (lengthSquared <= 0.0) {

            double dx =
                    px - a.x();

            double dz =
                    pz - a.z();

            return new SegmentResult(
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    ),
                    0.0
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
                a.x()
                        + abX * t;

        double closestZ =
                a.z()
                        + abZ * t;

        double dx =
                px - closestX;

        double dz =
                pz - closestZ;

        return new SegmentResult(
                Math.sqrt(
                        dx * dx
                                + dz * dz
                ),
                t
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


    private record SegmentResult(
            double distance,
            double t
    ) {
    }
}