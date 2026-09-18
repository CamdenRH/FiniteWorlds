package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 1E: proposes how each raw depression should eventually be
 * conditioned, without changing elevation or flow direction.
 *
 * The purpose of this pass is to make the policy visible and tunable before
 * any destructive terrain operation occurs.  Pass 1F can later consume these
 * plans to fill, breach, preserve, or jointly resolve compound basins.
 */
public final class DepressionResolutionPlanner {

    private DepressionResolutionPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        List<Depression> depressions =
                hydrology.depressions();

        if (depressions.isEmpty()) {
            hydrology.setDepressionResolutionPlans(
                    List.of()
            );
            return;
        }

        double[] depthSum =
                new double[depressions.size()];

        accumulateDepth(
                hydrology,
                depthSum
        );

        double cellAreaBlocks =
                hydrology.blocksPerCell()
                        * hydrology.blocksPerCell();

        List<DepressionResolutionPlan> result =
                new ArrayList<>(depressions.size());

        for (Depression depression : depressions) {
            DepressionClassification classification =
                    hydrology.depressionClassification(
                            depression.id()
                    );

            if (classification == null) {
                continue;
            }

            double fillVolumeBlocks =
                    depthSum[depression.id()]
                            * cellAreaBlocks;

            double lakeScore =
                    lakeSuitabilityScore(
                            depression,
                            classification
                    );

            DepressionResolutionAction action =
                    chooseAction(
                            depression,
                            classification,
                            lakeScore
                    );

            result.add(
                    new DepressionResolutionPlan(
                            depression.id(),
                            action,
                            fillVolumeBlocks,
                            lakeScore
                    )
            );
        }

        hydrology.setDepressionResolutionPlans(
                result
        );
    }

    private static void accumulateDepth(
            HydrologyGrid hydrology,
            double[] depthSum
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

                if (
                        id < 0
                                || id >= depthSum.length
                ) {
                    continue;
                }

                depthSum[id] +=
                        hydrology.depressionDepth(
                                x,
                                z
                        );
            }
        }
    }

    private static DepressionResolutionAction chooseAction(
            Depression depression,
            DepressionClassification classification,
            double lakeScore
    ) {
        if (classification.belongsToCompoundGroup()) {
            return DepressionResolutionAction.MERGE_COMPOUND;
        }

        if (
                classification.depressionClass()
                        == DepressionClass.MICRO_PIT
        ) {
            return DepressionResolutionAction.FILL;
        }

        /*
         * Preserve only reasonably developed basins.  A single deep grid
         * cell is not enough: at production scale one hydrology cell spans
         * 64 blocks, so a lake candidate should have both measurable depth
         * and a multi-cell footprint.
         */
        if (lakeScore >= 0.58) {
            return DepressionResolutionAction.PRESERVE_LAKE;
        }

        /*
         * Filling is reserved for genuinely cheap, local defects. Larger
         * shallow bowls are better breach candidates because filling them
         * would erase more of the existing macro terrain.
         */
        if (
                depression.footprintCellCount() <= 4
                        && depression.catchmentCellCount() <= 72
                        && depression.maximumDepth() <= 1.75f
                        && classification.meanDepth() <= 0.80f
        ) {
            return DepressionResolutionAction.FILL;
        }

        return DepressionResolutionAction.BREACH;
    }

    private static double lakeSuitabilityScore(
            Depression depression,
            DepressionClassification classification
    ) {
        double score =
                0.0;

        switch (classification.depressionClass()) {
            case ALPINE_BASIN ->
                    score += 0.28;

            case MAJOR_INTERIOR_BASIN ->
                    score += 0.24;

            case SHALLOW_BASIN,
                 MICRO_PIT,
                 COMPOUND_BASIN -> {
                /* no class bonus */
            }
        }

        score +=
                clamp01(
                        (depression.maximumDepth() - 1.25)
                                / 7.0
                ) * 0.30;

        score +=
                clamp01(
                        (classification.meanDepth() - 0.25)
                                / 2.5
                ) * 0.16;

        score +=
                clamp01(
                        (depression.footprintCellCount() - 1.0)
                                / 18.0
                ) * 0.14;

        score +=
                clamp01(
                        (depression.catchmentCellCount() - 16.0)
                                / 240.0
                ) * 0.07;

        score +=
                clamp01(
                        (classification.coastDistanceBlocks() - 256.0)
                                / 1536.0
                ) * 0.05;

        /*
         * Very near-coast depressions are usually better treated as drainage
         * or estuary problems than as permanent inland lakes.
         */
        if (classification.coastDistanceBlocks() < 192.0f) {
            score -= 0.20;
        }

        /*
         * Extremely broad but shallow footprints tend to be lowland grading
         * artifacts. Prefer breaching those rather than preserving a huge,
         * implausibly shallow lake.
         */
        if (
                depression.footprintCellCount() >= 256
                        && classification.meanDepth() < 0.85f
        ) {
            score -= 0.18;
        }

        return clamp01(score);
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
}
