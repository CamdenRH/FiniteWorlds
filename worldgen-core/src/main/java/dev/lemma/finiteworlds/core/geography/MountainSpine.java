package dev.lemma.finiteworlds.core.geography;

import java.util.ArrayList;
import java.util.List;

public final class MountainSpine {

    private final List<GeoPoint> points;
    private final double[] cumulativeLength;
    private final double totalLength;

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

        this.cumulativeLength =
                new double[points.size()];

        double runningLength =
                0.0;

        for (
                int i = 1;
                i < points.size();
                i++
        ) {
            GeoPoint previous =
                    points.get(i - 1);

            GeoPoint current =
                    points.get(i);

            runningLength +=
                    Math.hypot(
                            current.x() - previous.x(),
                            current.z() - previous.z()
                    );

            cumulativeLength[i] =
                    runningLength;
        }

        this.totalLength =
                runningLength;
    }

    public List<GeoPoint> points() {
        return points;
    }

    public double totalLength() {
        return totalLength;
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

        return Math.abs(
                project(
                        x,
                        z
                ).signedDistance()
        );
    }

    /*
     * Signed distance to the sampled spine.
     *
     * Positive = western side of the range.
     * Negative = eastern side.
     *
     * This relies on Cascadia's control points being
     * ordered generally south -> north, which they are
     * in ContinentPlanner.
     */
    public double signedDistanceTo(
            double x,
            double z
    ) {

        return project(
                x,
                z
        ).signedDistance();
    }

    /**
     * Project a point onto the sampled spine and return both its signed
     * cross-range distance and its position along the range.
     */
    public MountainProjection project(
            double x,
            double z
    ) {

        double minimumSquared =
                Double.POSITIVE_INFINITY;

        double bestSignedDistance =
                0.0;

        double bestProgress =
                0.0;

        double bestClosestX =
                points.getFirst().x();

        double bestClosestZ =
                points.getFirst().z();

        double bestTangentX =
                0.0;

        double bestTangentZ =
                1.0;

        for (
                int i = 0;
                i < points.size() - 1;
                i++
        ) {

            GeoPoint a =
                    points.get(i);

            GeoPoint b =
                    points.get(i + 1);

            double abX =
                    b.x() - a.x();

            double abZ =
                    b.z() - a.z();

            double apX =
                    x - a.x();

            double apZ =
                    z - a.z();

            double abLengthSquared =
                    abX * abX
                            + abZ * abZ;

            double t;

            if (abLengthSquared <= 1.0e-12) {
                t = 0.0;
            } else {

                t =
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
            }

            double closestX =
                    a.x() + abX * t;

            double closestZ =
                    a.z() + abZ * t;

            double offsetX =
                    x - closestX;

            double offsetZ =
                    z - closestZ;

            double distanceSquared =
                    offsetX * offsetX
                            + offsetZ * offsetZ;

            if (distanceSquared < minimumSquared) {

                minimumSquared =
                        distanceSquared;

                double cross =
                        abX * offsetZ
                                - abZ * offsetX;

                double sign =
                        cross >= 0.0
                                ? 1.0
                                : -1.0;

                bestSignedDistance =
                        Math.sqrt(
                                distanceSquared
                        ) * sign;

                double segmentLength =
                        Math.sqrt(
                                abLengthSquared
                        );

                double alongLength =
                        cumulativeLength[i]
                                + segmentLength * t;

                bestProgress =
                        totalLength > 1.0e-12
                                ? alongLength / totalLength
                                : 0.0;

                bestClosestX =
                        closestX;

                bestClosestZ =
                        closestZ;

                if (segmentLength > 1.0e-12) {
                    bestTangentX =
                            abX / segmentLength;

                    bestTangentZ =
                            abZ / segmentLength;
                }
            }
        }

        return new MountainProjection(
                bestSignedDistance,
                bestProgress,
                bestClosestX,
                bestClosestZ,
                bestTangentX,
                bestTangentZ
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
