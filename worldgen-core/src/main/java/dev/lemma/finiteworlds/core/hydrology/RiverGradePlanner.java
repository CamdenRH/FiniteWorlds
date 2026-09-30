package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Hydrology Pass 2F.1: derives a physically valid monotonic channel-bed grade
 * from the topologically correct Pass-2F river graph.
 *
 * The planned grade never raises terrain. It begins with the normal
 * Pass-2F channel depth target, then only lowers sections that would otherwise
 * force water to climb downstream. Confluences share a common planned node
 * elevation; lakes intentionally break the bed-grade constraint because their
 * flat water surface is represented semantically by LAKE_PASSAGE segments.
 *
 * This pass remains metadata-only. Terrain is not carved here.
 */
public final class RiverGradePlanner {

    private static final double EPSILON = 1.0e-6;

    private RiverGradePlanner() {
    }

    public static void analyze(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        List<RiverNode> nodes =
                hydrology.riverNodes();

        List<RiverSegment> segments =
                hydrology.riverSegments();

        List<RiverSegmentHierarchy> hierarchy =
                hydrology.riverSegmentHierarchy();

        List<RiverSegmentProfile> profiles =
                hydrology.riverSegmentProfiles();

        if (segments.isEmpty()) {
            hydrology.setRiverSegmentGradePlans(
                    List.of()
            );
            return;
        }

        if (
                hierarchy.size() != segments.size()
                        || profiles.size() != segments.size()
        ) {
            throw new IllegalStateException(
                    "River grade planning requires complete Pass-2D and Pass-2F metadata"
            );
        }

        List<Integer> topologicalSegments =
                new ArrayList<>(segments.size());

        for (RiverSegment segment : segments) {
            if (segment.type() == RiverSegmentType.CHANNEL) {
                topologicalSegments.add(segment.id());
            }
        }

        topologicalSegments.sort(
                Comparator.comparingDouble(
                        segmentId -> hierarchy.get(segmentId)
                                .longestSourceDistanceBlocks()
                )
        );

        double[] plannedNodeBed =
                new double[nodes.size()];

        Arrays.fill(
                plannedNodeBed,
                Double.NaN
        );

        /*
         * Forward graph pass. Determine the lowest bed elevation required at
         * each graph node so no downstream CHANNEL segment begins above the
         * channels feeding that node. LAKE_PASSAGE deliberately does not
         * propagate this constraint across a water body.
         */
        for (int segmentId : topologicalSegments) {
            RiverSegment segment =
                    segments.get(segmentId);

            RiverSegmentProfile profile =
                    profiles.get(segmentId);

            TargetBedSamples samples =
                    buildTargetBedSamples(
                            world,
                            segment,
                            profile
                    );

            if (samples.targetBed().length == 0) {
                continue;
            }

            int startNodeId =
                    segment.startNodeId();

            int endNodeId =
                    segment.endNodeId();

            double startTarget =
                    samples.targetBed()[0];

            double startBed =
                    Double.isFinite(plannedNodeBed[startNodeId])
                            ? Math.min(
                            plannedNodeBed[startNodeId],
                            startTarget
                    )
                            : startTarget;

            plannedNodeBed[startNodeId] =
                    startBed;

            double naturalEnd =
                    naturalMonotonicEnd(
                            samples.targetBed(),
                            startBed
                    );

            double candidateEnd =
                    Math.min(
                            samples.targetBed()[samples.targetBed().length - 1],
                            naturalEnd
                    );

            if (
                    !Double.isFinite(plannedNodeBed[endNodeId])
                            || candidateEnd < plannedNodeBed[endNodeId]
            ) {
                plannedNodeBed[endNodeId] =
                        candidateEnd;
            }
        }

        List<RiverSegmentGradePlan> result =
                new ArrayList<>(segments.size());

        int uphillSampleCount =
                0;

        for (RiverSegment segment : segments) {
            int segmentId =
                    segment.id();

            if (segment.type() == RiverSegmentType.LAKE_PASSAGE) {
                result.add(
                        RiverSegmentGradePlan.lakePassage(
                                segmentId
                        )
                );
                continue;
            }

            RiverSegmentProfile profile =
                    profiles.get(segmentId);

            TargetBedSamples samples =
                    buildTargetBedSamples(
                            world,
                            segment,
                            profile
                    );

            float[] terrain =
                    samples.terrain();

            float[] target =
                    samples.targetBed();

            if (target.length == 0) {
                result.add(
                        new RiverSegmentGradePlan(
                                segmentId,
                                false,
                                terrain,
                                target,
                                new float[0],
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                0.0
                        )
                );
                continue;
            }

            double startBed =
                    Double.isFinite(
                            plannedNodeBed[segment.startNodeId()]
                    )
                            ? Math.min(
                            plannedNodeBed[segment.startNodeId()],
                            target[0]
                    )
                            : target[0];

            double naturalEnd =
                    naturalMonotonicEnd(
                            target,
                            startBed
                    );

            double endBed =
                    Double.isFinite(
                            plannedNodeBed[segment.endNodeId()]
                    )
                            ? Math.min(
                            plannedNodeBed[segment.endNodeId()],
                            naturalEnd
                    )
                            : naturalEnd;

            float[] planned =
                    buildFinalProfile(
                            target,
                            startBed,
                            endBed
                    );

            double maximumGradeCorrection =
                    0.0;

            double totalGradeCorrection =
                    0.0;

            double maximumTotalIncision =
                    0.0;

            double totalIncision =
                    0.0;

            double maximumLocalGrade =
                    0.0;

            for (int i = 0; i < planned.length; i++) {
                double gradeCorrection =
                        Math.max(
                                0.0,
                                target[i] - planned[i]
                        );

                double totalSampleIncision =
                        Math.max(
                                0.0,
                                terrain[i] - planned[i]
                        );

                maximumGradeCorrection =
                        Math.max(
                                maximumGradeCorrection,
                                gradeCorrection
                        );

                totalGradeCorrection +=
                        gradeCorrection;

                maximumTotalIncision =
                        Math.max(
                                maximumTotalIncision,
                                totalSampleIncision
                        );

                totalIncision +=
                        totalSampleIncision;

                if (i <= 0) {
                    continue;
                }

                if (planned[i] > planned[i - 1] + EPSILON) {
                    uphillSampleCount++;
                }

                double sampleDistance =
                        sampleDistanceBlocks(
                                segment.cellPath().get(i - 1),
                                segment.cellPath().get(i),
                                hydrology.resolution(),
                                hydrology.blocksPerCell()
                        );

                if (sampleDistance > 0.0) {
                    maximumLocalGrade =
                            Math.max(
                                    maximumLocalGrade,
                                    Math.max(
                                            0.0,
                                            planned[i - 1]
                                                    - planned[i]
                                    ) / sampleDistance
                            );
                }
            }

            double plannedDrop =
                    Math.max(
                            0.0,
                            planned[0]
                                    - planned[planned.length - 1]
                    );

            double averagePlannedGrade =
                    segment.lengthBlocks() > 0.0
                            ? plannedDrop
                            / segment.lengthBlocks()
                            : 0.0;

            int sampleCount =
                    Math.max(
                            1,
                            planned.length
                    );

            result.add(
                    new RiverSegmentGradePlan(
                            segmentId,
                            false,
                            terrain,
                            target,
                            planned,
                            maximumGradeCorrection,
                            totalGradeCorrection / sampleCount,
                            maximumTotalIncision,
                            totalIncision / sampleCount,
                            averagePlannedGrade,
                            maximumLocalGrade
                    )
            );
        }

        if (uphillSampleCount != 0) {
            throw new IllegalStateException(
                    "River grade planner left "
                            + uphillSampleCount
                            + " uphill channel samples"
            );
        }

        hydrology.setRiverSegmentGradePlans(
                result
        );
    }

    private static TargetBedSamples buildTargetBedSamples(
            WorldBlueprint world,
            RiverSegment segment,
            RiverSegmentProfile profile
    ) {
        HydrologyGrid hydrology = world.hydrology();
        int sampleCount =
                segment.cellPath().size();

        float[] terrain =
                new float[sampleCount];

        float[] target =
                new float[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            int cell =
                    segment.cellPath().get(i);

            int x =
                    cell % hydrology.resolution();

            int z =
                    cell / hydrology.resolution();

            float surface =
                    hydrology.conditionedElevation(
                            x,
                            z
                    );

            double t =
                    sampleCount <= 1
                            ? 0.0
                            : i / (double) (sampleCount - 1);

            double depth =
                    lerp(
                            profile.smoothedStartDepthBlocks(),
                            profile.smoothedEndDepthBlocks(),
                            t
                    );

            terrain[i] =
                    surface;

            target[i] =
                    (float) (
                            surface
                                    - depth - CatchmentIncision.depthAt(world, segment, i, surface)
                    );
        }

        return new TargetBedSamples(
                terrain,
                target
        );
    }

    private static double naturalMonotonicEnd(
            float[] targetBed,
            double startBed
    ) {
        if (targetBed.length == 0) {
            return startBed;
        }

        double current =
                Math.min(
                        targetBed[0],
                        startBed
                );

        for (int i = 1; i < targetBed.length; i++) {
            current =
                    Math.min(
                            current,
                            targetBed[i]
                    );
        }

        return current;
    }

    private static float[] buildFinalProfile(
            float[] targetBed,
            double startBed,
            double endBed
    ) {
        int sampleCount =
                targetBed.length;

        float[] natural =
                new float[sampleCount];

        if (sampleCount == 0) {
            return natural;
        }

        natural[0] =
                (float) Math.min(
                        targetBed[0],
                        startBed
                );

        for (int i = 1; i < sampleCount; i++) {
            natural[i] =
                    (float) Math.min(
                            targetBed[i],
                            natural[i - 1]
                    );
        }

        double additionalEndDrop =
                Math.max(
                        0.0,
                        natural[sampleCount - 1]
                                - endBed
                );

        float[] result =
                new float[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            double t =
                    sampleCount <= 1
                            ? 1.0
                            : i / (double) (sampleCount - 1);

            double downstreamBlend =
                    smoothstep01(t);

            result[i] =
                    (float) (
                            natural[i]
                                    - additionalEndDrop
                                    * downstreamBlend
                    );
        }

        /*
         * Floating-point safety only. The construction above is already
         * monotonic because natural[] never rises and the subtracted blend
         * only grows downstream.
         */
        for (int i = 1; i < sampleCount; i++) {
            if (result[i] > result[i - 1]) {
                result[i] =
                        result[i - 1];
            }
        }

        return result;
    }

    private static double sampleDistanceBlocks(
            int a,
            int b,
            int resolution,
            double blocksPerCell
    ) {
        int ax =
                a % resolution;

        int az =
                a / resolution;

        int bx =
                b % resolution;

        int bz =
                b / resolution;

        return Math.hypot(
                bx - ax,
                bz - az
        ) * blocksPerCell;
    }

    private static double smoothstep01(
            double t
    ) {
        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );

        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (b - a) * t;
    }

    private record TargetBedSamples(
            float[] terrain,
            float[] targetBed
    ) {
    }
}
