package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hydrology Pass 1I: converts the non-destructive lake decisions produced by
 * earlier passes into explicit hydrologic lake objects.
 *
 * This pass still does not modify elevation or place Minecraft water blocks.
 * It only records lake footprint, water level, depth, catchment scale, and
 * outlet relationships so the drainage graph can treat lakes as through-flow
 * features rather than terminal sinks in later passes.
 */
public final class LakePlanner {

    private static final float DEPTH_EPSILON =
            1.0e-4f;

    private static final double COMPOUND_LAKE_SCORE_THRESHOLD =
            0.58;

    private LakePlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        hydrology.clearLakeAnalysis();

        List<LakeDraft> drafts =
                new ArrayList<>();

        int depressionCount =
                hydrology.depressions().size();

        int compoundCount =
                hydrology.compoundBasins().size();

        int[] lakeByDepression =
                new int[depressionCount];

        int[] lakeByCompound =
                new int[compoundCount];

        Arrays.fill(
                lakeByDepression,
                -1
        );

        Arrays.fill(
                lakeByCompound,
                -1
        );

        createSimpleLakeDrafts(
                hydrology,
                drafts,
                lakeByDepression
        );

        createCompoundLakeDrafts(
                hydrology,
                drafts,
                lakeByCompound
        );

        populateSimpleLakeFootprints(
                hydrology,
                drafts,
                lakeByDepression
        );

        populateCompoundLakeFootprints(
                hydrology,
                drafts,
                lakeByCompound
        );

        List<Lake> lakes =
                finalizeLakes(
                        hydrology,
                        drafts,
                        lakeByDepression,
                        lakeByCompound
                );

        hydrology.setLakes(
                lakes
        );
    }

    private static void createSimpleLakeDrafts(
            HydrologyGrid hydrology,
            List<LakeDraft> drafts,
            int[] lakeByDepression
    ) {
        List<Depression> depressions =
                hydrology.depressions();

        for (DepressionResolutionPlan plan
                : hydrology.depressionResolutionPlans()) {

            if (
                    plan.action()
                            != DepressionResolutionAction.PRESERVE_LAKE
            ) {
                continue;
            }

            int depressionId =
                    plan.depressionId();

            if (
                    depressionId < 0
                            || depressionId >= depressions.size()
            ) {
                continue;
            }

            Depression depression =
                    depressions.get(
                            depressionId
                    );

            DepressionClassification targetClassification =
                    hydrology.depressionClassification(
                            depression.spillTargetDepressionId()
                    );

            int targetCompound =
                    targetClassification != null
                            && targetClassification.belongsToCompoundGroup()
                            ? targetClassification.compoundGroupId()
                            : -1;

            int lakeId =
                    drafts.size();

            lakeByDepression[depressionId] =
                    lakeId;

            drafts.add(
                    new LakeDraft(
                            lakeId,
                            LakeSourceType.SIMPLE_DEPRESSION,
                            depressionId,
                            List.of(depressionId),
                            depression.catchmentCellCount(),
                            depression.spillElevation(),
                            plan.lakeSuitabilityScore(),
                            depression.spillX(),
                            depression.spillZ(),
                            depression.spillTargetX(),
                            depression.spillTargetZ(),
                            depression.spillTargetDepressionId(),
                            targetCompound
                    )
            );
        }
    }

    private static void createCompoundLakeDrafts(
            HydrologyGrid hydrology,
            List<LakeDraft> drafts,
            int[] lakeByCompound
    ) {
        for (CompoundBasin basin
                : hydrology.compoundBasins()) {

            double score =
                    compoundLakeSuitabilityScore(
                            basin
                    );

            if (
                    !basin.hasExternalSpill()
                            || basin.externalSpillElevation()
                            <= basin.mergeElevation() + DEPTH_EPSILON
                            || score < COMPOUND_LAKE_SCORE_THRESHOLD
            ) {
                continue;
            }

            int lakeId =
                    drafts.size();

            lakeByCompound[basin.groupId()] =
                    lakeId;

            drafts.add(
                    new LakeDraft(
                            lakeId,
                            LakeSourceType.COMPOUND_BASIN,
                            basin.groupId(),
                            basin.memberDepressionIds(),
                            basin.catchmentCellCount(),
                            basin.externalSpillElevation(),
                            score,
                            basin.spillX(),
                            basin.spillZ(),
                            basin.spillTargetX(),
                            basin.spillTargetZ(),
                            basin.spillTargetDepressionId(),
                            basin.spillTargetCompoundGroupId()
                    )
            );
        }
    }

    private static void populateSimpleLakeFootprints(
            HydrologyGrid hydrology,
            List<LakeDraft> drafts,
            int[] lakeByDepression
    ) {
        int size =
                hydrology.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int depressionId =
                        hydrology.depressionId(
                                x,
                                z
                        );

                if (
                        depressionId < 0
                                || depressionId >= lakeByDepression.length
                ) {
                    continue;
                }

                int lakeId =
                        lakeByDepression[depressionId];

                if (lakeId < 0) {
                    continue;
                }

                LakeDraft draft =
                        drafts.get(lakeId);

                float depth =
                        draft.waterSurfaceElevation
                                - hydrology.conditionedElevation(
                                x,
                                z
                        );

                if (depth <= DEPTH_EPSILON) {
                    continue;
                }

                addLakeCell(
                        hydrology,
                        draft,
                        x,
                        z,
                        depth
                );
            }
        }
    }

    private static void populateCompoundLakeFootprints(
            HydrologyGrid hydrology,
            List<LakeDraft> drafts,
            int[] lakeByCompound
    ) {
        if (lakeByCompound.length == 0) {
            return;
        }

        int size =
                hydrology.resolution();

        int[] groupByDepression =
                buildCompoundGroupLookup(
                        hydrology
                );

        boolean[] visited =
                new boolean[
                        size * size
                ];

        ArrayDeque<Integer> queue =
                new ArrayDeque<>();

        List<Depression> depressions =
                hydrology.depressions();

        for (CompoundBasin basin
                : hydrology.compoundBasins()) {

            int groupId =
                    basin.groupId();

            if (
                    groupId < 0
                            || groupId >= lakeByCompound.length
            ) {
                continue;
            }

            int lakeId =
                    lakeByCompound[groupId];

            if (lakeId < 0) {
                continue;
            }

            LakeDraft draft =
                    drafts.get(lakeId);

            queue.clear();

            for (int depressionId
                    : basin.memberDepressionIds()) {

                if (
                        depressionId < 0
                                || depressionId >= depressions.size()
                ) {
                    continue;
                }

                Depression depression =
                        depressions.get(depressionId);

                enqueueCompoundCell(
                        hydrology,
                        draft,
                        groupByDepression,
                        groupId,
                        depression.sinkX(),
                        depression.sinkZ(),
                        visited,
                        queue,
                        size
                );
            }

            while (!queue.isEmpty()) {
                int current =
                        queue.removeFirst();

                int x =
                        current % size;

                int z =
                        current / size;

                for (FlowDirection direction
                        : FlowDirection.values()) {

                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int nx =
                            x + direction.dx();

                    int nz =
                            z + direction.dz();

                    enqueueCompoundCell(
                            hydrology,
                            draft,
                            groupByDepression,
                            groupId,
                            nx,
                            nz,
                            visited,
                            queue,
                            size
                    );
                }
            }
        }
    }

    private static void enqueueCompoundCell(
            HydrologyGrid hydrology,
            LakeDraft draft,
            int[] groupByDepression,
            int groupId,
            int x,
            int z,
            boolean[] visited,
            ArrayDeque<Integer> queue,
            int size
    ) {
        if (
                x < 0
                        || x >= size
                        || z < 0
                        || z >= size
        ) {
            return;
        }

        int index =
                z * size + x;

        if (visited[index]) {
            return;
        }

        int depressionId =
                hydrology.rawCatchmentDepressionId(
                        x,
                        z
                );

        if (
                depressionId < 0
                        || depressionId >= groupByDepression.length
                        || groupByDepression[depressionId] != groupId
        ) {
            return;
        }

        float depth =
                draft.waterSurfaceElevation
                        - hydrology.conditionedElevation(
                        x,
                        z
                );

        if (depth <= DEPTH_EPSILON) {
            return;
        }

        visited[index] = true;

        addLakeCell(
                hydrology,
                draft,
                x,
                z,
                depth
        );

        queue.addLast(
                index
        );
    }

    private static int[] buildCompoundGroupLookup(
            HydrologyGrid hydrology
    ) {
        int count =
                hydrology.depressions().size();

        int[] result =
                new int[count];

        Arrays.fill(
                result,
                -1
        );

        for (DepressionClassification classification
                : hydrology.depressionClassifications()) {

            if (classification.belongsToCompoundGroup()) {
                result[classification.depressionId()] =
                        classification.compoundGroupId();
            }
        }

        return result;
    }

    private static void addLakeCell(
            HydrologyGrid hydrology,
            LakeDraft draft,
            int x,
            int z,
            float depth
    ) {
        hydrology.setLakeCell(
                x,
                z,
                draft.id,
                depth
        );

        draft.footprintCellCount++;
        draft.depthSum += depth;
        draft.minimumBedElevation =
                Math.min(
                        draft.minimumBedElevation,
                        hydrology.conditionedElevation(
                                x,
                                z
                        )
                );
        draft.maximumDepth =
                Math.max(
                        draft.maximumDepth,
                        depth
                );
    }

    private static List<Lake> finalizeLakes(
            HydrologyGrid hydrology,
            List<LakeDraft> drafts,
            int[] lakeByDepression,
            int[] lakeByCompound
    ) {
        double cellArea =
                hydrology.blocksPerCell()
                        * hydrology.blocksPerCell();

        List<Lake> result =
                new ArrayList<>(drafts.size());

        for (LakeDraft draft : drafts) {
            int downstreamLake =
                    downstreamLakeId(
                            draft,
                            hydrology,
                            lakeByDepression,
                            lakeByCompound
                    );

            float minimumBed =
                    Float.isFinite(
                            draft.minimumBedElevation
                    )
                            ? draft.minimumBedElevation
                            : draft.waterSurfaceElevation;

            float meanDepth =
                    draft.footprintCellCount > 0
                            ? (float) (
                            draft.depthSum
                                    / draft.footprintCellCount
                    )
                            : 0.0f;

            result.add(
                    new Lake(
                            draft.id,
                            draft.sourceType,
                            draft.sourceId,
                            draft.sourceDepressionIds,
                            draft.footprintCellCount,
                            draft.catchmentCellCount,
                            draft.waterSurfaceElevation,
                            minimumBed,
                            draft.maximumDepth,
                            meanDepth,
                            draft.footprintCellCount
                                    * cellArea,
                            draft.catchmentCellCount
                                    * cellArea,
                            draft.depthSum
                                    * cellArea,
                            draft.suitabilityScore,
                            draft.outletX,
                            draft.outletZ,
                            draft.outletTargetX,
                            draft.outletTargetZ,
                            draft.outletTargetDepressionId,
                            draft.outletTargetCompoundGroupId,
                            downstreamLake
                    )
            );
        }

        return List.copyOf(result);
    }

    private static int downstreamLakeId(
            LakeDraft draft,
            HydrologyGrid hydrology,
            int[] lakeByDepression,
            int[] lakeByCompound
    ) {
        int targetGroup =
                draft.outletTargetCompoundGroupId;

        if (
                targetGroup >= 0
                        && targetGroup < lakeByCompound.length
        ) {
            int candidate =
                    lakeByCompound[targetGroup];

            if (candidate >= 0 && candidate != draft.id) {
                return candidate;
            }
        }

        int targetDepression =
                draft.outletTargetDepressionId;

        if (
                targetDepression >= 0
                        && targetDepression < lakeByDepression.length
        ) {
            int candidate =
                    lakeByDepression[targetDepression];

            if (candidate >= 0 && candidate != draft.id) {
                return candidate;
            }

            DepressionClassification classification =
                    hydrology.depressionClassification(
                            targetDepression
                    );

            if (
                    classification != null
                            && classification.belongsToCompoundGroup()
            ) {
                int group =
                        classification.compoundGroupId();

                if (
                        group >= 0
                                && group < lakeByCompound.length
                ) {
                    candidate =
                            lakeByCompound[group];

                    if (candidate >= 0 && candidate != draft.id) {
                        return candidate;
                    }
                }
            }
        }

        return -1;
    }

    private static double compoundLakeSuitabilityScore(
            CompoundBasin basin
    ) {
        double score =
                0.0;

        score +=
                clamp01(
                        (basin.maximumDepthToExternalSpill() - 1.0)
                                / 8.0
                ) * 0.40;

        score +=
                clamp01(
                        (basin.footprintCellCount() - 4.0)
                                / 60.0
                ) * 0.25;

        score +=
                clamp01(
                        (basin.catchmentCellCount() - 64.0)
                                / 600.0
                ) * 0.15;

        score +=
                clamp01(
                        (basin.externalRiseAboveMerge() - 0.10)
                                / 3.0
                ) * 0.20;

        return clamp01(
                score
        );
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static final class LakeDraft {

        private final int id;
        private final LakeSourceType sourceType;
        private final int sourceId;
        private final List<Integer> sourceDepressionIds;
        private final int catchmentCellCount;
        private final float waterSurfaceElevation;
        private final double suitabilityScore;
        private final int outletX;
        private final int outletZ;
        private final int outletTargetX;
        private final int outletTargetZ;
        private final int outletTargetDepressionId;
        private final int outletTargetCompoundGroupId;

        private int footprintCellCount;
        private double depthSum;
        private float minimumBedElevation;
        private float maximumDepth;

        private LakeDraft(
                int id,
                LakeSourceType sourceType,
                int sourceId,
                List<Integer> sourceDepressionIds,
                int catchmentCellCount,
                float waterSurfaceElevation,
                double suitabilityScore,
                int outletX,
                int outletZ,
                int outletTargetX,
                int outletTargetZ,
                int outletTargetDepressionId,
                int outletTargetCompoundGroupId
        ) {
            this.id = id;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.sourceDepressionIds =
                    List.copyOf(sourceDepressionIds);
            this.catchmentCellCount = catchmentCellCount;
            this.waterSurfaceElevation = waterSurfaceElevation;
            this.suitabilityScore = suitabilityScore;
            this.outletX = outletX;
            this.outletZ = outletZ;
            this.outletTargetX = outletTargetX;
            this.outletTargetZ = outletTargetZ;
            this.outletTargetDepressionId = outletTargetDepressionId;
            this.outletTargetCompoundGroupId = outletTargetCompoundGroupId;

            this.footprintCellCount = 0;
            this.depthSum = 0.0;
            this.minimumBedElevation = Float.POSITIVE_INFINITY;
            this.maximumDepth = 0.0f;
        }
    }
}
