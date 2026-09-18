package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hydrology Pass 1D: diagnostic depression classification and spill-network
 * analysis.
 *
 * This pass still performs no hydrologic conditioning. It labels raw basins,
 * measures mean depth, detects reciprocal/compound spill cycles, and records
 * how each basin connects downstream. Pass 1E can use this information to
 * decide which basins should be filled, breached, merged, or preserved.
 */
public final class DepressionClassifier {

    private DepressionClassifier() {
    }

    public static void classify(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        List<Depression> depressions =
                hydrology.depressions();

        if (depressions.isEmpty()) {
            hydrology.setDepressionClassifications(
                    List.of(),
                    0
            );
            return;
        }

        int count =
                depressions.size();

        double[] depthSum =
                new double[count];

        int[] measuredDepthCells =
                new int[count];

        accumulateDepthStatistics(
                hydrology,
                depthSum,
                measuredDepthCells
        );

        int[] directUpstreamCount =
                countDirectUpstream(
                        depressions
                );

        CompoundGroups compounds =
                findCompoundGroups(
                        depressions
                );

        NetworkPosition[] networkPositions =
                resolveNetworkPositions(
                        depressions,
                        compounds.groupByDepression()
                );

        List<DepressionClassification> result =
                new ArrayList<>(count);

        for (int id = 0; id < count; id++) {
            Depression depression =
                    depressions.get(id);

            float meanDepth =
                    measuredDepthCells[id] > 0
                            ? (float) (
                            depthSum[id]
                                    / measuredDepthCells[id]
                    )
                            : 0.0f;

            TerrainProvince province =
                    world.terrainProvince(
                            depression.sinkX(),
                            depression.sinkZ()
                    );

            float coastDistance =
                    Math.max(
                            0.0f,
                            world.coastDistance(
                                    depression.sinkX(),
                                    depression.sinkZ()
                            )
                    );

            int compoundGroup =
                    compounds.groupByDepression()[id];

            int compoundMembers =
                    compoundGroup >= 0
                            ? compounds.groupSizes()[compoundGroup]
                            : 0;

            DepressionClass depressionClass =
                    classifyDepression(
                            depression,
                            meanDepth,
                            province,
                            compoundGroup >= 0
                    );

            NetworkPosition network =
                    networkPositions[id];

            result.add(
                    new DepressionClassification(
                            id,
                            depressionClass,
                            meanDepth,
                            coastDistance,
                            province,
                            depression.spillTargetDepressionId(),
                            network.terminalDepressionId(),
                            network.chainDepth(),
                            directUpstreamCount[id],
                            compoundGroup,
                            compoundMembers
                    )
            );
        }

        hydrology.setDepressionClassifications(
                result,
                compounds.groupSizes().length
        );
    }

    private static void accumulateDepthStatistics(
            HydrologyGrid hydrology,
            double[] depthSum,
            int[] depthCells
    ) {
        int size =
                hydrology.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int id =
                        hydrology.depressionId(
                                x,
                                z
                        );

                if (id < 0) {
                    continue;
                }

                depthSum[id] +=
                        hydrology.depressionDepth(
                                x,
                                z
                        );

                depthCells[id]++;
            }
        }
    }

    private static int[] countDirectUpstream(
            List<Depression> depressions
    ) {
        int[] result =
                new int[depressions.size()];

        for (Depression depression : depressions) {
            int target =
                    depression.spillTargetDepressionId();

            if (
                    target >= 0
                            && target < result.length
            ) {
                result[target]++;
            }
        }

        return result;
    }

    private static CompoundGroups findCompoundGroups(
            List<Depression> depressions
    ) {
        int count =
                depressions.size();

        int[] groupByDepression =
                new int[count];

        Arrays.fill(
                groupByDepression,
                -1
        );

        boolean[] processed =
                new boolean[count];

        List<Integer> groupSizes =
                new ArrayList<>();

        for (int start = 0; start < count; start++) {
            if (processed[start]) {
                continue;
            }

            List<Integer> path =
                    new ArrayList<>();

            Map<Integer, Integer> pathPosition =
                    new HashMap<>();

            int current =
                    start;

            while (
                    current >= 0
                            && current < count
                            && !processed[current]
                            && !pathPosition.containsKey(current)
            ) {
                pathPosition.put(
                        current,
                        path.size()
                );

                path.add(current);

                current =
                        depressions.get(current)
                                .spillTargetDepressionId();
            }

            Integer cycleStart =
                    pathPosition.get(current);

            if (cycleStart != null) {
                int cycleLength =
                        path.size()
                                - cycleStart;

                /*
                 * A one-node self-loop is not expected from Pass 1C and does
                 * not represent a useful compound group. True mutual cycles
                 * contain two or more raw depressions.
                 */
                if (cycleLength >= 2) {
                    int groupId =
                            groupSizes.size();

                    groupSizes.add(
                            cycleLength
                    );

                    for (
                            int i = cycleStart;
                            i < path.size();
                            i++
                    ) {
                        groupByDepression[path.get(i)] =
                                groupId;
                    }
                }
            }

            for (int id : path) {
                processed[id] = true;
            }
        }

        int[] sizes =
                new int[groupSizes.size()];

        for (int i = 0; i < sizes.length; i++) {
            sizes[i] =
                    groupSizes.get(i);
        }

        return new CompoundGroups(
                groupByDepression,
                sizes
        );
    }

    private static NetworkPosition[] resolveNetworkPositions(
            List<Depression> depressions,
            int[] compoundGroup
    ) {
        NetworkPosition[] result =
                new NetworkPosition[depressions.size()];

        for (int start = 0; start < depressions.size(); start++) {
            if (result[start] != null) {
                continue;
            }

            List<Integer> path =
                    new ArrayList<>();

            int current =
                    start;

            while (
                    current >= 0
                            && current < depressions.size()
                            && result[current] == null
                            && compoundGroup[current] < 0
            ) {
                path.add(current);

                current =
                        depressions.get(current)
                                .spillTargetDepressionId();
            }

            int terminal;
            int depth;

            if (
                    current >= 0
                            && current < depressions.size()
                            && compoundGroup[current] >= 0
            ) {
                terminal =
                        compoundRepresentative(
                                compoundGroup[current],
                                compoundGroup
                        );

                depth = 0;

                int groupId =
                        compoundGroup[current];

                for (int id = 0; id < compoundGroup.length; id++) {
                    if (compoundGroup[id] == groupId) {
                        result[id] =
                                new NetworkPosition(
                                        terminal,
                                        0
                                );
                    }
                }
            } else if (
                    current >= 0
                            && current < depressions.size()
                            && result[current] != null
            ) {
                terminal =
                        result[current]
                                .terminalDepressionId();

                depth =
                        result[current]
                                .chainDepth();
            } else {
                terminal =
                        -1;

                depth =
                        0;
            }

            for (int i = path.size() - 1; i >= 0; i--) {
                depth++;

                result[path.get(i)] =
                        new NetworkPosition(
                                terminal,
                                depth
                        );
            }
        }

        return result;
    }

    private static int compoundRepresentative(
            int groupId,
            int[] groupByDepression
    ) {
        for (int id = 0; id < groupByDepression.length; id++) {
            if (groupByDepression[id] == groupId) {
                return id;
            }
        }

        return -1;
    }

    private static DepressionClass classifyDepression(
            Depression depression,
            float meanDepth,
            TerrainProvince province,
            boolean compound
    ) {
        if (compound) {
            return DepressionClass.COMPOUND_BASIN;
        }

        if (
                depression.footprintCellCount() <= 2
                        && depression.catchmentCellCount() <= 24
                        && depression.maximumDepth() <= 1.50f
        ) {
            return DepressionClass.MICRO_PIT;
        }

        if (
                depression.footprintCellCount() >= 96
                        || depression.catchmentCellCount() >= 2_000
                        || depression.maximumDepth() >= 10.0f
                        || (
                        depression.footprintCellCount() >= 48
                                && depression.maximumDepth() >= 5.0f
                )
                        || meanDepth >= 3.0f
        ) {
            return DepressionClass.MAJOR_INTERIOR_BASIN;
        }

        if (
                isMountainProvince(province)
                        && (
                        depression.minimumElevation() >= 130.0f
                                || depression.maximumDepth() >= 2.0f
                )
        ) {
            return DepressionClass.ALPINE_BASIN;
        }

        return DepressionClass.SHALLOW_BASIN;
    }

    private static boolean isMountainProvince(
            TerrainProvince province
    ) {
        return switch (province) {
            case COAST_RANGE,
                 CASCADE_FOOTHILLS,
                 CASCADE_CORE,
                 EASTERN_SLOPES -> true;

            default -> false;
        };
    }

    private record CompoundGroups(
            int[] groupByDepression,
            int[] groupSizes
    ) {
    }

    private record NetworkPosition(
            int terminalDepressionId,
            int chainDepth
    ) {
    }
}
