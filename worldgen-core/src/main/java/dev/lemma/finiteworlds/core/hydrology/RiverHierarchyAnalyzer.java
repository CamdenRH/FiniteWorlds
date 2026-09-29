package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hydrology Pass 2D: derives stream-order and network-distance metadata from
 * the explicit directed river graph created by Pass 2C.
 *
 * No terrain, flow direction, or river geometry is modified here.
 */
public final class RiverHierarchyAnalyzer {

    private RiverHierarchyAnalyzer() {
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

        if (segments.isEmpty()) {
            hydrology.setRiverSegmentHierarchy(
                    List.of()
            );
            return;
        }

        int nodeCount =
                nodes.size();

        int segmentCount =
                segments.size();

        @SuppressWarnings("unchecked")
        List<Integer>[] incomingByNode =
                new List[nodeCount];

        @SuppressWarnings("unchecked")
        List<Integer>[] outgoingByNode =
                new List[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            incomingByNode[i] =
                    new ArrayList<>();

            outgoingByNode[i] =
                    new ArrayList<>();
        }

        for (int i = 0; i < segmentCount; i++) {
            RiverSegment segment =
                    segments.get(i);

            if (segment.id() != i) {
                throw new IllegalStateException(
                        "River segment IDs must remain dense and index-aligned"
                );
            }

            validateNodeId(
                    segment.startNodeId(),
                    nodeCount,
                    segment.id(),
                    "start"
            );

            validateNodeId(
                    segment.endNodeId(),
                    nodeCount,
                    segment.id(),
                    "end"
            );

            outgoingByNode[segment.startNodeId()]
                    .add(segment.id());

            incomingByNode[segment.endNodeId()]
                    .add(segment.id());
        }

        /*
         * The routed hydrology graph has exactly one downstream destination
         * per cell, so an explicit river node must not split into multiple
         * downstream river segments. Catch this now rather than allowing
         * ambiguous hierarchy metadata later.
         */
        for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
            if (outgoingByNode[nodeId].size() > 1) {
                throw new IllegalStateException(
                        "River node "
                                + nodeId
                                + " has "
                                + outgoingByNode[nodeId].size()
                                + " downstream segments"
                );
            }
        }

        int[] remainingIncoming =
                new int[nodeCount];

        for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
            remainingIncoming[nodeId] =
                    incomingByNode[nodeId].size();
        }

        ArrayDeque<Integer> readyNodes =
                new ArrayDeque<>();

        for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
            if (remainingIncoming[nodeId] == 0) {
                readyNodes.addLast(nodeId);
            }
        }

        int[] strahlerOrder =
                new int[segmentCount];

        int[] upstreamSegmentCount =
                new int[segmentCount];

        double[] upstreamNetworkLength =
                new double[segmentCount];

        double[] longestSourceDistance =
                new double[segmentCount];

        int[] downstreamSegmentId =
                new int[segmentCount];

        Arrays.fill(
                downstreamSegmentId,
                -1
        );

        List<Integer> topologicalSegments =
                new ArrayList<>(segmentCount);

        int processedNodes =
                0;

        while (!readyNodes.isEmpty()) {
            int nodeId =
                    readyNodes.removeFirst();

            processedNodes++;

            List<Integer> incoming =
                    incomingByNode[nodeId];

            int nodeOrder =
                    strahlerForNode(
                            incoming,
                            strahlerOrder
                    );

            int upstreamCountAtNode =
                    0;

            double upstreamLengthAtNode =
                    0.0;

            double farthestSourceAtNode =
                    0.0;

            for (int incomingSegmentId : incoming) {
                RiverSegment incomingSegment =
                        segments.get(incomingSegmentId);

                upstreamCountAtNode +=
                        1
                                + upstreamSegmentCount[incomingSegmentId];

                upstreamLengthAtNode +=
                        incomingSegment.lengthBlocks()
                                + upstreamNetworkLength[incomingSegmentId];

                farthestSourceAtNode =
                        Math.max(
                                farthestSourceAtNode,
                                longestSourceDistance[incomingSegmentId]
                        );
            }

            for (int outgoingSegmentId : outgoingByNode[nodeId]) {
                RiverSegment outgoing =
                        segments.get(outgoingSegmentId);

                strahlerOrder[outgoingSegmentId] =
                        nodeOrder;

                upstreamSegmentCount[outgoingSegmentId] =
                        upstreamCountAtNode;

                upstreamNetworkLength[outgoingSegmentId] =
                        upstreamLengthAtNode;

                longestSourceDistance[outgoingSegmentId] =
                        farthestSourceAtNode
                                + outgoing.lengthBlocks();

                topologicalSegments.add(
                        outgoingSegmentId
                );

                int endNodeId =
                        outgoing.endNodeId();

                remainingIncoming[endNodeId]--;

                if (remainingIncoming[endNodeId] == 0) {
                    readyNodes.addLast(endNodeId);
                }
            }
        }

        if (topologicalSegments.size() != segmentCount) {
            throw new IllegalStateException(
                    "River hierarchy could only order "
                            + topologicalSegments.size()
                            + " of "
                            + segmentCount
                            + " segments; graph may contain a cycle"
            );
        }

        if (processedNodes != nodeCount) {
            throw new IllegalStateException(
                    "River hierarchy could only process "
                            + processedNodes
                            + " of "
                            + nodeCount
                            + " nodes"
            );
        }

        for (RiverSegment segment : segments) {
            List<Integer> downstream =
                    outgoingByNode[segment.endNodeId()];

            if (!downstream.isEmpty()) {
                downstreamSegmentId[segment.id()] =
                        downstream.getFirst();
            }
        }

        double[] distanceToMouth =
                new double[segmentCount];

        for (int i = topologicalSegments.size() - 1; i >= 0; i--) {
            int segmentId =
                    topologicalSegments.get(i);

            RiverSegment segment =
                    segments.get(segmentId);

            int downstream =
                    downstreamSegmentId[segmentId];

            distanceToMouth[segmentId] =
                    segment.lengthBlocks()
                            + (
                            downstream >= 0
                                    ? distanceToMouth[downstream]
                                    : 0.0
                    );
        }

        List<RiverSegmentHierarchy> result =
                new ArrayList<>(segmentCount);

        for (RiverSegment segment : segments) {
            int segmentId =
                    segment.id();

            result.add(
                    new RiverSegmentHierarchy(
                            segmentId,
                            strahlerOrder[segmentId],
                            incomingByNode[segment.startNodeId()],
                            downstreamSegmentId[segmentId],
                            upstreamSegmentCount[segmentId],
                            upstreamNetworkLength[segmentId],
                            longestSourceDistance[segmentId],
                            distanceToMouth[segmentId]
                    )
            );
        }

        hydrology.setRiverSegmentHierarchy(
                result
        );
    }

    private static int strahlerForNode(
            List<Integer> incoming,
            int[] segmentOrders
    ) {
        if (incoming.isEmpty()) {
            return 1;
        }

        int maximum =
                0;

        int maximumCount =
                0;

        for (int segmentId : incoming) {
            int order =
                    segmentOrders[segmentId];

            if (order > maximum) {
                maximum = order;
                maximumCount = 1;
            } else if (order == maximum) {
                maximumCount++;
            }
        }

        if (maximum <= 0) {
            throw new IllegalStateException(
                    "River hierarchy encountered an uninitialized upstream Strahler order"
            );
        }

        return maximumCount >= 2
                ? maximum + 1
                : maximum;
    }

    private static void validateNodeId(
            int nodeId,
            int nodeCount,
            int segmentId,
            String role
    ) {
        if (nodeId < 0 || nodeId >= nodeCount) {
            throw new IllegalStateException(
                    "River segment "
                            + segmentId
                            + " has invalid "
                            + role
                            + " node ID "
                            + nodeId
            );
        }
    }
}
