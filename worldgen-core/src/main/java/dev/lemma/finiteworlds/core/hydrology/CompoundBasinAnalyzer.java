package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 1H: collapses each cyclic first-spill group into one
 * hydrologic unit and measures the group's lowest external spill saddle.
 *
 * The analysis uses the raw D8 catchments recorded in Pass 1C, but measures
 * saddle elevations against the current conditioned surface after Pass 1G.
 * No elevation or flow direction is changed here.
 */
public final class CompoundBasinAnalyzer {

    private static final double SPILL_EPSILON =
            1.0e-5;

    private CompoundBasinAnalyzer() {
    }

    public static void analyze(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        int groupCount =
                hydrology.compoundGroupCount();

        if (groupCount <= 0) {
            hydrology.setCompoundBasins(
                    List.of()
            );
            return;
        }

        List<List<Integer>> members =
                createMemberLists(
                        hydrology,
                        groupCount
                );

        int[] footprintCellCount =
                new int[groupCount];

        int[] catchmentCellCount =
                new int[groupCount];

        float[] minimumElevation =
                new float[groupCount];

        float[] mergeElevation =
                new float[groupCount];

        float[] externalSpillElevation =
                new float[groupCount];

        int[] spillIndex =
                new int[groupCount];

        int[] spillTargetIndex =
                new int[groupCount];

        java.util.Arrays.fill(
                minimumElevation,
                Float.POSITIVE_INFINITY
        );

        java.util.Arrays.fill(
                mergeElevation,
                Float.NEGATIVE_INFINITY
        );

        java.util.Arrays.fill(
                externalSpillElevation,
                Float.POSITIVE_INFINITY
        );

        java.util.Arrays.fill(
                spillIndex,
                -1
        );

        java.util.Arrays.fill(
                spillTargetIndex,
                -1
        );

        measureMemberGeometry(
                hydrology,
                members,
                mergeElevation
        );

        scanCombinedCatchments(
                world,
                hydrology,
                footprintCellCount,
                catchmentCellCount,
                minimumElevation,
                externalSpillElevation,
                spillIndex,
                spillTargetIndex
        );

        List<CompoundBasin> result =
                new ArrayList<>(groupCount);

        int size =
                hydrology.resolution();

        for (int groupId = 0; groupId < groupCount; groupId++) {
            float minimum =
                    minimumElevation[groupId];

            if (!Float.isFinite(minimum)) {
                minimum = 0.0f;
            }

            float merge =
                    mergeElevation[groupId];

            if (!Float.isFinite(merge)) {
                merge = minimum;
            }

            float externalSpill =
                    externalSpillElevation[groupId];

            if (!Float.isFinite(externalSpill)) {
                externalSpill = merge;
            }

            int spillCell =
                    spillIndex[groupId];

            int targetCell =
                    spillTargetIndex[groupId];

            int spillX =
                    spillCell >= 0
                            ? spillCell % size
                            : -1;

            int spillZ =
                    spillCell >= 0
                            ? spillCell / size
                            : -1;

            int targetX =
                    targetCell >= 0
                            ? targetCell % size
                            : -1;

            int targetZ =
                    targetCell >= 0
                            ? targetCell / size
                            : -1;

            int targetDepression =
                    targetCell >= 0
                            ? hydrology.rawCatchmentDepressionId(
                            targetX,
                            targetZ
                    )
                            : -1;

            int targetGroup =
                    compoundGroupForDepression(
                            hydrology,
                            targetDepression
                    );

            result.add(
                    new CompoundBasin(
                            groupId,
                            members.get(groupId),
                            footprintCellCount[groupId],
                            catchmentCellCount[groupId],
                            minimum,
                            merge,
                            externalSpill,
                            Math.max(
                                    0.0f,
                                    externalSpill - minimum
                            ),
                            Math.max(
                                    0.0f,
                                    externalSpill - merge
                            ),
                            spillX,
                            spillZ,
                            targetX,
                            targetZ,
                            targetDepression,
                            targetGroup
                    )
            );
        }

        hydrology.setCompoundBasins(
                result
        );
    }

    private static List<List<Integer>> createMemberLists(
            HydrologyGrid hydrology,
            int groupCount
    ) {
        List<List<Integer>> result =
                new ArrayList<>(groupCount);

        for (int i = 0; i < groupCount; i++) {
            result.add(
                    new ArrayList<>()
            );
        }

        for (DepressionClassification classification
                : hydrology.depressionClassifications()) {

            if (!classification.belongsToCompoundGroup()) {
                continue;
            }

            int groupId =
                    classification.compoundGroupId();

            if (
                    groupId >= 0
                            && groupId < groupCount
            ) {
                result.get(groupId)
                        .add(
                                classification.depressionId()
                        );
            }
        }

        for (int i = 0; i < result.size(); i++) {
            result.set(
                    i,
                    List.copyOf(result.get(i))
            );
        }

        return result;
    }

    private static void measureMemberGeometry(
            HydrologyGrid hydrology,
            List<List<Integer>> members,
            float[] mergeElevation
    ) {
        List<Depression> depressions =
                hydrology.depressions();

        for (int groupId = 0; groupId < members.size(); groupId++) {
            for (int depressionId : members.get(groupId)) {
                if (
                        depressionId < 0
                                || depressionId >= depressions.size()
                ) {
                    continue;
                }

                Depression depression =
                        depressions.get(depressionId);

                mergeElevation[groupId] =
                        Math.max(
                                mergeElevation[groupId],
                                depression.spillElevation()
                        );
            }
        }
    }

    private static void scanCombinedCatchments(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            int[] footprintCellCount,
            int[] catchmentCellCount,
            float[] minimumElevation,
            float[] externalSpillElevation,
            int[] spillIndex,
            int[] spillTargetIndex
    ) {
        int size =
                hydrology.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int rawDepressionId =
                        hydrology.rawCatchmentDepressionId(
                                x,
                                z
                        );

                int groupId =
                        compoundGroupForDepression(
                                hydrology,
                                rawDepressionId
                        );

                if (groupId < 0) {
                    continue;
                }

                catchmentCellCount[groupId]++;

                float currentElevation =
                        hydrology.conditionedElevation(
                                x,
                                z
                        );

                minimumElevation[groupId] =
                        Math.min(
                                minimumElevation[groupId],
                                currentElevation
                        );

                int footprintDepressionId =
                        hydrology.depressionId(
                                x,
                                z
                        );

                if (
                        compoundGroupForDepression(
                                hydrology,
                                footprintDepressionId
                        ) == groupId
                ) {
                    footprintCellCount[groupId]++;
                }

                scanExternalBoundaryAtCell(
                        world,
                        hydrology,
                        x,
                        z,
                        groupId,
                        currentElevation,
                        externalSpillElevation,
                        spillIndex,
                        spillTargetIndex
                );
            }
        }
    }

    private static void scanExternalBoundaryAtCell(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            int x,
            int z,
            int groupId,
            float currentElevation,
            float[] externalSpillElevation,
            int[] spillIndex,
            int[] spillTargetIndex
    ) {
        int size =
                hydrology.resolution();

        int currentIndex =
                index(x, z, size);

        for (FlowDirection direction : FlowDirection.values()) {
            if (!direction.routesToNeighbor()) {
                continue;
            }

            int nx =
                    x + direction.dx();

            int nz =
                    z + direction.dz();

            if (
                    nx < 0
                            || nx >= size
                            || nz < 0
                            || nz >= size
            ) {
                considerExternalSpill(
                        groupId,
                        currentElevation,
                        -1,
                        currentIndex,
                        -1,
                        externalSpillElevation,
                        spillIndex,
                        spillTargetIndex
                );
                continue;
            }

            int neighborDepressionId =
                    hydrology.rawCatchmentDepressionId(
                            nx,
                            nz
                    );

            int neighborGroup =
                    compoundGroupForDepression(
                            hydrology,
                            neighborDepressionId
                    );

            if (neighborGroup == groupId) {
                continue;
            }

            float neighborElevation =
                    hydrology.conditionedElevation(
                            nx,
                            nz
                    );

            float candidateSpill =
                    Math.max(
                            currentElevation,
                            neighborElevation
                    );

            if (world.landMask(nx, nz) < 0.5f) {
                neighborDepressionId = -1;
            }

            considerExternalSpill(
                    groupId,
                    candidateSpill,
                    neighborDepressionId,
                    currentIndex,
                    index(nx, nz, size),
                    externalSpillElevation,
                    spillIndex,
                    spillTargetIndex
            );
        }
    }

    private static void considerExternalSpill(
            int groupId,
            float candidateElevation,
            int candidateTargetDepression,
            int candidateSpillIndex,
            int candidateTargetIndex,
            float[] externalSpillElevation,
            int[] spillIndex,
            int[] spillTargetIndex
    ) {
        float currentBest =
                externalSpillElevation[groupId];

        if (candidateElevation < currentBest - SPILL_EPSILON) {
            externalSpillElevation[groupId] = candidateElevation;
            spillIndex[groupId] = candidateSpillIndex;
            spillTargetIndex[groupId] = candidateTargetIndex;
            return;
        }

        /*
         * Equal saddles are intentionally left in deterministic scan order.
         * The important Pass 1H invariant is the lowest external saddle;
         * outlet preference can be applied later when a compound group is
         * actually conditioned.
         */
    }

    private static int compoundGroupForDepression(
            HydrologyGrid hydrology,
            int depressionId
    ) {
        DepressionClassification classification =
                hydrology.depressionClassification(
                        depressionId
                );

        if (
                classification == null
                        || !classification.belongsToCompoundGroup()
        ) {
            return -1;
        }

        return classification.compoundGroupId();
    }

    private static int index(
            int x,
            int z,
            int size
    ) {
        return z * size + x;
    }
}
