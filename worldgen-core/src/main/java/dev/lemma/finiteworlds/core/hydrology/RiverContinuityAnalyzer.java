package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hydrology Pass 2F: converts independent Pass-2E segment dimensions into
 * longitudinally continuous channel profiles.
 *
 * Confluences deliberately do not force every tributary to share one physical
 * width at the graph node. Instead, the downstream segment begins from a
 * hydraulically plausible combination of its incoming channels and transitions
 * toward its own Pass-2E target. This keeps small tributaries from inflating to
 * trunk-river scale immediately before a confluence.
 *
 * This pass remains metadata-only. It does not move the macro river graph or
 * modify terrain.
 */
public final class RiverContinuityAnalyzer {

    private static final double MINIMUM_WIDTH_BLOCKS = 1.25;
    private static final double MAXIMUM_WIDTH_BLOCKS = 64.0;

    private static final double MINIMUM_DEPTH_BLOCKS = 1.0;
    private static final double MAXIMUM_DEPTH_BLOCKS = 12.0;

    private RiverContinuityAnalyzer() {
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

        List<RiverSegmentMagnitude> magnitudes =
                hydrology.riverSegmentMagnitudes();

        if (segments.isEmpty()) {
            hydrology.setRiverSegmentProfiles(
                    List.of()
            );
            return;
        }

        if (
                hierarchy.size() != segments.size()
                        || magnitudes.size() != segments.size()
        ) {
            throw new IllegalStateException(
                    "River continuity analysis requires complete Pass-2D and Pass-2E metadata"
            );
        }

        int segmentCount =
                segments.size();

        double[] startWidth =
                new double[segmentCount];

        double[] endWidth =
                new double[segmentCount];

        double[] startDepth =
                new double[segmentCount];

        double[] endDepth =
                new double[segmentCount];

        List<Integer> topologicalSegments =
                new ArrayList<>(segmentCount);

        for (RiverSegment segment : segments) {
            topologicalSegments.add(
                    segment.id()
            );
        }

        topologicalSegments.sort(
                Comparator.comparingDouble(
                        segmentId -> hierarchy.get(segmentId)
                                .longestSourceDistanceBlocks()
                )
        );

        /*
         * Forward pass: derive each downstream segment's entry dimensions from
         * its already-computed tributaries, then transition toward the raw 2E
         * target over the length of the segment.
         */
        for (int segmentId : topologicalSegments) {
            RiverSegment segment =
                    segments.get(segmentId);

            RiverSegmentHierarchy segmentHierarchy =
                    hierarchy.get(segmentId);

            RiverSegmentMagnitude magnitude =
                    magnitudes.get(segmentId);

            List<Integer> upstream =
                    segmentHierarchy.upstreamSegmentIds();

            double rawWidth =
                    magnitude.provisionalWidthBlocks();

            double rawDepth =
                    magnitude.provisionalDepthBlocks();

            if (upstream.isEmpty()) {
                startWidth[segmentId] =
                        clampWidth(
                                rawWidth * 0.90
                        );

                startDepth[segmentId] =
                        clampDepth(
                                rawDepth * 0.94
                        );
            } else {
                double maximumIncomingWidth =
                        0.0;

                double squaredIncomingWidth =
                        0.0;

                double maximumIncomingDepth =
                        0.0;

                for (int upstreamId : upstream) {
                    double incomingWidth =
                            endWidth[upstreamId];

                    maximumIncomingWidth =
                            Math.max(
                                    maximumIncomingWidth,
                                    incomingWidth
                            );

                    squaredIncomingWidth +=
                            incomingWidth
                                    * incomingWidth;

                    maximumIncomingDepth =
                            Math.max(
                                    maximumIncomingDepth,
                                    endDepth[upstreamId]
                            );
                }

                double equivalentIncomingWidth =
                        Math.sqrt(
                                squaredIncomingWidth
                        );

                double confluenceWidth =
                        Math.max(
                                maximumIncomingWidth * 0.96,
                                equivalentIncomingWidth * 0.78
                        );

                double desiredStartWidth =
                        0.66 * rawWidth
                                + 0.34 * confluenceWidth;

                startWidth[segmentId] =
                        clampWidth(
                                clamp(
                                        desiredStartWidth,
                                        rawWidth * 0.76,
                                        rawWidth * 1.10
                                )
                        );

                double desiredStartDepth =
                        0.72 * rawDepth
                                + 0.28
                                * maximumIncomingDepth;

                startDepth[segmentId] =
                        clampDepth(
                                clamp(
                                        desiredStartDepth,
                                        rawDepth * 0.82,
                                        rawDepth * 1.10
                                )
                        );
            }

            endWidth[segmentId] =
                    clampWidth(
                            clamp(
                                    rawWidth,
                                    startWidth[segmentId]
                                            * widthRetention(
                                            segment.lengthBlocks()
                                    ),
                                    startWidth[segmentId]
                                            * widthGrowthLimit(
                                            segment.lengthBlocks()
                                    )
                            )
                    );

            endDepth[segmentId] =
                    clampDepth(
                            clamp(
                                    rawDepth,
                                    startDepth[segmentId]
                                            * depthRetention(
                                            segment.lengthBlocks()
                                    ),
                                    startDepth[segmentId]
                                            * depthGrowthLimit(
                                            segment.lengthBlocks()
                                    )
                            )
                    );
        }

        /*
         * A light reverse pass anticipates a large downstream increase only on
         * the dominant incoming main stem. Smaller tributaries keep their own
         * width instead of being inflated to the downstream trunk width.
         */
        for (int i = topologicalSegments.size() - 1; i >= 0; i--) {
            int segmentId =
                    topologicalSegments.get(i);

            RiverSegmentHierarchy segmentHierarchy =
                    hierarchy.get(segmentId);

            int downstreamId =
                    segmentHierarchy.downstreamSegmentId();

            if (downstreamId < 0) {
                continue;
            }

            RiverSegmentHierarchy downstreamHierarchy =
                    hierarchy.get(downstreamId);

            if (!isDominantIncoming(
                    segmentId,
                    downstreamHierarchy.upstreamSegmentIds(),
                    segments
            )) {
                continue;
            }

            RiverSegmentMagnitude magnitude =
                    magnitudes.get(segmentId);

            double maximumAnticipatedWidth =
                    magnitude.provisionalWidthBlocks()
                            * 1.20;

            double targetEndWidth =
                    Math.min(
                            startWidth[downstreamId],
                            maximumAnticipatedWidth
                    );

            if (targetEndWidth > endWidth[segmentId]) {
                endWidth[segmentId] =
                        clampWidth(
                                0.65 * endWidth[segmentId]
                                        + 0.35 * targetEndWidth
                        );
            }

            double maximumAnticipatedDepth =
                    magnitude.provisionalDepthBlocks()
                            * 1.15;

            double targetEndDepth =
                    Math.min(
                            startDepth[downstreamId],
                            maximumAnticipatedDepth
                    );

            if (targetEndDepth > endDepth[segmentId]) {
                endDepth[segmentId] =
                        clampDepth(
                                0.70 * endDepth[segmentId]
                                        + 0.30 * targetEndDepth
                        );
            }
        }

        List<RiverSegmentProfile> profiles =
                new ArrayList<>(segmentCount);

        for (RiverSegment segment : segments) {
            int segmentId =
                    segment.id();

            RiverSegmentHierarchy segmentHierarchy =
                    hierarchy.get(segmentId);

            RiverSegmentMagnitude magnitude =
                    magnitudes.get(segmentId);

            RiverNode startNode =
                    nodes.get(segment.startNodeId());

            RiverNode endNode =
                    nodes.get(segment.endNodeId());

            double startDistanceFromSource =
                    Math.max(
                            0.0,
                            segmentHierarchy.longestSourceDistanceBlocks()
                                    - segment.lengthBlocks()
                    );

            double endDistanceFromSource =
                    segmentHierarchy.longestSourceDistanceBlocks();

            double startDistanceToMouth =
                    segmentHierarchy.distanceToMouthBlocks();

            double endDistanceToMouth =
                    Math.max(
                            0.0,
                            startDistanceToMouth
                                    - segment.lengthBlocks()
                    );

            double rawWidth =
                    magnitude.provisionalWidthBlocks();

            double rawDepth =
                    magnitude.provisionalDepthBlocks();

            double rawStartWidthRatio =
                    rawStartWidthRatio(
                            segmentId,
                            segmentHierarchy.upstreamSegmentIds(),
                            magnitudes
                    );

            profiles.add(
                    new RiverSegmentProfile(
                            segmentId,
                            startDistanceFromSource,
                            endDistanceFromSource,
                            startDistanceToMouth,
                            endDistanceToMouth,
                            startNode.elevation(),
                            endNode.elevation(),
                            rawWidth,
                            rawDepth,
                            startWidth[segmentId],
                            endWidth[segmentId],
                            startDepth[segmentId],
                            endDepth[segmentId],
                            rawStartWidthRatio,
                            maximumRelativeAdjustment(
                                    rawWidth,
                                    startWidth[segmentId],
                                    endWidth[segmentId]
                            ),
                            maximumRelativeAdjustment(
                                    rawDepth,
                                    startDepth[segmentId],
                                    endDepth[segmentId]
                            )
                    )
            );
        }

        hydrology.setRiverSegmentProfiles(
                profiles
        );
    }

    private static boolean isDominantIncoming(
            int candidateSegmentId,
            List<Integer> incomingSegmentIds,
            List<RiverSegment> segments
    ) {
        long candidateAccumulation =
                segments.get(candidateSegmentId)
                        .endAccumulation();

        for (int incomingId : incomingSegmentIds) {
            if (
                    segments.get(incomingId)
                            .endAccumulation()
                            > candidateAccumulation
            ) {
                return false;
            }
        }

        return true;
    }

    private static double rawStartWidthRatio(
            int segmentId,
            List<Integer> incomingSegmentIds,
            List<RiverSegmentMagnitude> magnitudes
    ) {
        if (incomingSegmentIds.isEmpty()) {
            return 1.0;
        }

        double maximumIncomingWidth =
                0.0;

        for (int incomingSegmentId : incomingSegmentIds) {
            maximumIncomingWidth =
                    Math.max(
                            maximumIncomingWidth,
                            magnitudes.get(incomingSegmentId)
                                    .provisionalWidthBlocks()
                    );
        }

        if (maximumIncomingWidth <= 0.0) {
            return 1.0;
        }

        return magnitudes.get(segmentId)
                .provisionalWidthBlocks()
                / maximumIncomingWidth;
    }

    private static double maximumRelativeAdjustment(
            double rawValue,
            double startValue,
            double endValue
    ) {
        double denominator =
                Math.max(
                        1.0e-9,
                        rawValue
                );

        return Math.max(
                Math.abs(startValue - rawValue),
                Math.abs(endValue - rawValue)
        ) / denominator;
    }

    private static double widthRetention(
            double lengthBlocks
    ) {
        double t =
                clamp01(
                        lengthBlocks / 4096.0
                );

        return 0.98
                - 0.04 * t;
    }

    private static double widthGrowthLimit(
            double lengthBlocks
    ) {
        double t =
                clamp01(
                        lengthBlocks / 4096.0
                );

        return 1.32
                + 0.43 * t;
    }

    private static double depthRetention(
            double lengthBlocks
    ) {
        double t =
                clamp01(
                        lengthBlocks / 4096.0
                );

        return 0.97
                - 0.05 * t;
    }

    private static double depthGrowthLimit(
            double lengthBlocks
    ) {
        double t =
                clamp01(
                        lengthBlocks / 4096.0
                );

        return 1.24
                + 0.32 * t;
    }

    private static double clampWidth(
            double value
    ) {
        return clamp(
                value,
                MINIMUM_WIDTH_BLOCKS,
                MAXIMUM_WIDTH_BLOCKS
        );
    }

    private static double clampDepth(
            double value
    ) {
        return clamp(
                value,
                MINIMUM_DEPTH_BLOCKS,
                MAXIMUM_DEPTH_BLOCKS
        );
    }

    private static double clamp01(
            double value
    ) {
        return clamp(
                value,
                0.0,
                1.0
        );
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}
