package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Hydrology Pass 2C: converts the classified stream mask into an explicit
 * directed river graph.
 *
 * Nodes represent sources, confluences, lake inlets/outlets, and coastal
 * mouths. Channel segments connect those nodes through the existing macro D8
 * network; lake passages preserve the topology across lake interiors without
 * pretending that the coarse interior routing is a visible river channel.
 *
 * This pass is non-destructive. It does not carve terrain, assign final river
 * widths, smooth channel geometry, or place Minecraft water.
 */
public final class RiverGraphBuilder {

    private RiverGraphBuilder() {
    }

    public static void build(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        if (hydrology.streamChannelCellCount() <= 0) {
            hydrology.setRiverGraph(
                    List.of(),
                    List.of()
            );
            return;
        }

        int size =
                hydrology.resolution();

        int cellCount =
                size * size;

        boolean[] channel =
                new boolean[cellCount];

        int[] upstreamChannelCount =
                new int[cellCount];

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int cell =
                        index(x, z, size);

                channel[cell] =
                        hydrology.streamClass(x, z)
                                .isChannel();
            }
        }

        int channelEdges =
                0;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int cell =
                        index(x, z, size);

                if (!channel[cell]) {
                    continue;
                }

                int target =
                        downstreamIndex(
                                hydrology,
                                x,
                                z,
                                size
                        );

                if (target < 0 || !channel[target]) {
                    continue;
                }

                upstreamChannelCount[target]++;
                channelEdges++;
            }
        }

        int[] nodeIdByCell =
                new int[cellCount];

        Arrays.fill(
                nodeIdByCell,
                -1
        );

        List<RiverNode> nodes =
                new ArrayList<>();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int cell =
                        index(x, z, size);

                if (!channel[cell]) {
                    continue;
                }

                int outletLakeId =
                        routedUpstreamLakeId(
                                hydrology,
                                x,
                                z,
                                size
                        );

                boolean lakeOutlet =
                        outletLakeId >= 0;

                boolean source =
                        upstreamChannelCount[cell] == 0
                                && !lakeOutlet;

                boolean confluence =
                        upstreamChannelCount[cell] >= 2;

                int target =
                        downstreamIndex(
                                hydrology,
                                x,
                                z,
                                size
                        );

                boolean lakeInlet =
                        target >= 0
                                && hydrology.lakeId(
                                target % size,
                                target / size
                        ) >= 0
                                && hydrology.lakeId(x, z) < 0;

                boolean mouth =
                        isMouth(
                                hydrology,
                                x,
                                z,
                                size
                        );

                if (
                        !source
                                && !confluence
                                && !lakeInlet
                                && !lakeOutlet
                                && !mouth
                ) {
                    continue;
                }

                int inletLakeId =
                        lakeInlet
                                ? hydrology.lakeId(
                                target % size,
                                target / size
                        )
                                : -1;

                int nodeId =
                        nodes.size();

                nodeIdByCell[cell] =
                        nodeId;

                nodes.add(
                        new RiverNode(
                                nodeId,
                                x,
                                z,
                                source,
                                confluence,
                                lakeInlet,
                                lakeOutlet,
                                mouth,
                                inletLakeId,
                                outletLakeId,
                                hydrology.streamClass(x, z),
                                hydrology.flowAccumulation(x, z),
                                hydrology.conditionedElevation(x, z)
                        )
                );
            }
        }

        List<RiverSegment> segments =
                new ArrayList<>();

        int representedChannelEdges =
                buildChannelSegments(
                        hydrology,
                        nodes,
                        nodeIdByCell,
                        channel,
                        size,
                        segments
                );

        buildLakePassages(
                hydrology,
                nodes,
                nodeIdByCell,
                size,
                segments
        );

        int lakeInletCount =
                0;

        for (RiverNode node : nodes) {
            if (node.lakeInlet()) {
                lakeInletCount++;
            }
        }

        int lakePassageCount =
                0;

        for (RiverSegment segment : segments) {
            if (segment.type() == RiverSegmentType.LAKE_PASSAGE) {
                lakePassageCount++;
            }
        }

        if (lakePassageCount != lakeInletCount) {
            throw new IllegalStateException(
                    "River graph created "
                            + lakePassageCount
                            + " lake passages for "
                            + lakeInletCount
                            + " lake inlets"
            );
        }

        if (representedChannelEdges != channelEdges) {
            throw new IllegalStateException(
                    "River graph represented "
                            + representedChannelEdges
                            + " channel edges but stream mask contains "
                            + channelEdges
            );
        }

        hydrology.setRiverGraph(
                nodes,
                segments
        );
    }

    private static int buildChannelSegments(
            HydrologyGrid hydrology,
            List<RiverNode> nodes,
            int[] nodeIdByCell,
            boolean[] channel,
            int size,
            List<RiverSegment> segments
    ) {
        int representedEdges =
                0;

        for (RiverNode start : nodes) {
            /*
             * Lake inlets hand off to an explicit LAKE_PASSAGE segment. If
             * the first routed lake cell also happens to be an explicit
             * outlet/channel cell, creating a CHANNEL here would duplicate
             * the same semantic downstream edge.
             */
            if (start.lakeInlet()) {
                continue;
            }

            int startCell =
                    index(
                            start.x(),
                            start.z(),
                            size
                    );

            int next =
                    downstreamIndex(
                            hydrology,
                            start.x(),
                            start.z(),
                            size
                    );

            if (next < 0 || !channel[next]) {
                continue;
            }

            List<Integer> path =
                    new ArrayList<>();

            path.add(startCell);

            int current =
                    startCell;

            StreamClass maximumClass =
                    start.streamClass();

            int safety =
                    0;

            while (true) {
                int cx =
                        current % size;

                int cz =
                        current / size;

                int target =
                        downstreamIndex(
                                hydrology,
                                cx,
                                cz,
                                size
                        );

                if (target < 0 || !channel[target]) {
                    break;
                }

                path.add(target);
                representedEdges++;

                int tx =
                        target % size;

                int tz =
                        target / size;

                maximumClass =
                        stronger(
                                maximumClass,
                                hydrology.streamClass(tx, tz)
                        );

                int endNodeId =
                        nodeIdByCell[target];

                if (endNodeId >= 0) {
                    RiverNode end =
                            nodes.get(endNodeId);

                    segments.add(
                            createSegment(
                                    hydrology,
                                    segments.size(),
                                    RiverSegmentType.CHANNEL,
                                    start.id(),
                                    end.id(),
                                    path,
                                    -1,
                                    maximumClass,
                                    size
                            )
                    );
                    break;
                }

                current =
                        target;

                safety++;

                if (safety > size * size) {
                    throw new IllegalStateException(
                            "River channel segment exceeded graph safety limit"
                    );
                }
            }
        }

        return representedEdges;
    }

    private static void buildLakePassages(
            HydrologyGrid hydrology,
            List<RiverNode> nodes,
            int[] nodeIdByCell,
            int size,
            List<RiverSegment> segments
    ) {
        for (RiverNode start : nodes) {
            if (!start.lakeInlet() || start.inletLakeId() < 0) {
                continue;
            }

            int startCell =
                    index(
                            start.x(),
                            start.z(),
                            size
                    );

            List<Integer> path =
                    new ArrayList<>();

            path.add(startCell);

            int current =
                    startCell;

            int endNodeId =
                    -1;

            int safety =
                    0;

            while (safety++ <= size * size) {
                int cx =
                        current % size;

                int cz =
                        current / size;

                int target =
                        downstreamIndex(
                                hydrology,
                                cx,
                                cz,
                                size
                        );

                if (target < 0) {
                    break;
                }

                path.add(target);

                int targetNodeId =
                        nodeIdByCell[target];

                if (targetNodeId >= 0 && targetNodeId != start.id()) {
                    endNodeId =
                            targetNodeId;
                    break;
                }

                current =
                        target;
            }

            if (endNodeId < 0) {
                throw new IllegalStateException(
                        "Lake inlet node "
                                + start.id()
                                + " could not follow final routed flow to a downstream river node"
                );
            }

            RiverNode end =
                    nodes.get(endNodeId);

            segments.add(
                    createSegment(
                            hydrology,
                            segments.size(),
                            RiverSegmentType.LAKE_PASSAGE,
                            start.id(),
                            end.id(),
                            path,
                            start.inletLakeId(),
                            stronger(
                                    start.streamClass(),
                                    end.streamClass()
                            ),
                            size
                    )
            );
        }
    }

    private static RiverSegment createSegment(
            HydrologyGrid hydrology,
            int id,
            RiverSegmentType type,
            int startNodeId,
            int endNodeId,
            List<Integer> path,
            int lakeId,
            StreamClass maximumClass,
            int size
    ) {
        int startCell =
                path.getFirst();

        int endCell =
                path.getLast();

        int startX =
                startCell % size;

        int startZ =
                startCell / size;

        int endX =
                endCell % size;

        int endZ =
                endCell / size;

        double lengthBlocks =
                pathLengthBlocks(
                        path,
                        hydrology.blocksPerCell(),
                        size
                );

        double elevationDrop =
                hydrology.conditionedElevation(
                        startX,
                        startZ
                )
                        - hydrology.conditionedElevation(
                        endX,
                        endZ
                );

        return new RiverSegment(
                id,
                type,
                startNodeId,
                endNodeId,
                path,
                lakeId,
                maximumClass,
                hydrology.flowAccumulation(
                        startX,
                        startZ
                ),
                hydrology.flowAccumulation(
                        endX,
                        endZ
                ),
                lengthBlocks,
                elevationDrop
        );
    }

    private static boolean isMouth(
            HydrologyGrid hydrology,
            int x,
            int z,
            int size
    ) {
        FlowDirection direction =
                hydrology.routedFlowDirection(
                        x,
                        z
                );

        if (direction == FlowDirection.OUTLET) {
            return true;
        }

        if (!direction.routesToNeighbor()) {
            return false;
        }

        int nx =
                x + direction.dx();

        int nz =
                z + direction.dz();

        return !inside(nx, nz, size)
                || hydrology.routedFlowDirection(nx, nz)
                == FlowDirection.OCEAN;
    }

    private static int routedUpstreamLakeId(
            HydrologyGrid hydrology,
            int x,
            int z,
            int size
    ) {
        int result =
                -1;

        int targetCell =
                index(x, z, size);

        for (FlowDirection direction : FlowDirection.values()) {
            if (!direction.routesToNeighbor()) {
                continue;
            }

            int nx =
                    x + direction.dx();

            int nz =
                    z + direction.dz();

            if (!inside(nx, nz, size)) {
                continue;
            }

            int lakeId =
                    hydrology.lakeId(nx, nz);

            if (lakeId < 0) {
                continue;
            }

            int routedTarget =
                    downstreamIndex(
                            hydrology,
                            nx,
                            nz,
                            size
                    );

            if (routedTarget != targetCell) {
                continue;
            }

            /*
             * Multiple neighboring lakes can legitimately discharge into
             * the same visible river cell. RiverNode keeps one representative
             * outletLakeId for diagnostics; the individual LAKE_PASSAGE
             * segments retain the exact source lake IDs.
             */
            if (result < 0 || lakeId < result) {
                result = lakeId;
            }
        }

        return result;
    }

    private static int downstreamIndex(
            HydrologyGrid hydrology,
            int x,
            int z,
            int size
    ) {
        FlowDirection direction =
                hydrology.routedFlowDirection(
                        x,
                        z
                );

        if (!direction.routesToNeighbor()) {
            return -1;
        }

        int nx =
                x + direction.dx();

        int nz =
                z + direction.dz();

        if (!inside(nx, nz, size)) {
            return -1;
        }

        return index(
                nx,
                nz,
                size
        );
    }

    private static double pathLengthBlocks(
            List<Integer> path,
            double blocksPerCell,
            int size
    ) {
        double length =
                0.0;

        for (int i = 1; i < path.size(); i++) {
            int a =
                    path.get(i - 1);

            int b =
                    path.get(i);

            int ax =
                    a % size;

            int az =
                    a / size;

            int bx =
                    b % size;

            int bz =
                    b / size;

            int dx =
                    Math.abs(bx - ax);

            int dz =
                    Math.abs(bz - az);

            length +=
                    (dx != 0 && dz != 0
                            ? Math.sqrt(2.0)
                            : 1.0)
                            * blocksPerCell;
        }

        return length;
    }

    private static StreamClass stronger(
            StreamClass a,
            StreamClass b
    ) {
        return a.ordinal() >= b.ordinal()
                ? a
                : b;
    }

    private static boolean inside(
            int x,
            int z,
            int size
    ) {
        return x >= 0
                && x < size
                && z >= 0
                && z < size;
    }

    private static int index(
            int x,
            int z,
            int size
    ) {
        return z * size + x;
    }
}
