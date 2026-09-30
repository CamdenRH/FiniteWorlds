package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.generator.CascadiaGenerator;
import dev.lemma.finiteworlds.core.geography.ContinentPlan;
import dev.lemma.finiteworlds.core.hydrology.CompoundBasin;
import dev.lemma.finiteworlds.core.hydrology.DepressionClass;
import dev.lemma.finiteworlds.core.hydrology.DepressionResolutionAction;
import dev.lemma.finiteworlds.core.hydrology.HydrologyRouteType;
import dev.lemma.finiteworlds.core.hydrology.LakeSourceType;
import dev.lemma.finiteworlds.core.hydrology.RiverNodeType;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentType;
import dev.lemma.finiteworlds.core.hydrology.RiverScale;
import dev.lemma.finiteworlds.core.hydrology.StreamClass;

import java.nio.file.Path;

public final class PreviewMain {

    public static void main(String[] args)
            throws Exception {

        System.setProperty(
                "java.awt.headless",
                "true"
        );

        long seed = 12345L;
        String type = "cascadia";

        for (int i = 0; i < args.length; i++) {

            if ("--seed".equals(args[i])) {
                seed =
                        Long.parseLong(
                                args[++i]
                        );
            }

            if ("--type".equals(args[i])) {
                type =
                        args[++i];
            }
        }

        if (!type.equalsIgnoreCase("cascadia")) {
            throw new IllegalArgumentException(
                    "Unknown world type: " + type
            );
        }

        WorldConfig config =
                WorldConfig.production();

        CascadiaGenerator generator =
                new CascadiaGenerator();

        System.out.println(
                "Generating world blueprint..."
        );

        ContinentPlan plan =
                generator.createPlan(
                        seed
                );

        WorldBlueprint blueprint =
                generator.generate(
                        seed,
                        config
                );

        System.out.println(
                "Interior hydrology sinks: "
                        + blueprint.hydrology().sinkCount()
        );

        System.out.println(
                "Measured depressions: "
                        + blueprint.hydrology().depressionCount()
        );

        System.out.println(
                "Depression footprint cells: "
                        + blueprint.hydrology().depressionCellCount()
        );

        System.out.printf(
                "Deepest raw depression: %.3f elevation units%n",
                blueprint.hydrology().maximumDepressionDepth()
        );

        System.out.println(
                "Compound depression groups: "
                        + blueprint.hydrology().compoundGroupCount()
        );

        for (DepressionClass depressionClass : DepressionClass.values()) {
            System.out.println(
                    "  "
                            + depressionClass
                            + ": "
                            + blueprint.hydrology()
                            .countDepressionsByClass(
                                    depressionClass
                            )
            );
        }

        System.out.println(
                "Proposed depression resolutions:"
        );

        for (DepressionResolutionAction action : DepressionResolutionAction.values()) {
            System.out.println(
                    "  "
                            + action
                            + ": "
                            + blueprint.hydrology()
                            .countResolutionActions(
                                    action
                            )
            );
        }

        System.out.println(
                "Conditioned hydrology sinks after FILL + BREACH pass: "
                        + blueprint.hydrology().conditionedSinkCount()
        );

        System.out.printf(
                "Maximum hydrology fill delta: %.4f elevation units%n",
                blueprint.hydrology().maximumFillDelta()
        );

        System.out.println(
                "Applied hydrology breaches: "
                        + blueprint.hydrology().appliedBreachCount()
        );

        System.out.println(
                "Deferred hydrology breaches: "
                        + blueprint.hydrology().skippedBreachCount()
        );

        System.out.printf(
                "Maximum hydrology breach cut: %.4f elevation units%n",
                blueprint.hydrology().maximumBreachCutDepth()
        );

        System.out.println(
                "Resolved compound hydrologic basins: "
                        + blueprint.hydrology().compoundBasins().size()
        );

        int compoundExternalSpills = 0;
        int compoundOpenSpills = 0;
        float maximumCompoundRise = 0.0f;

        for (CompoundBasin basin : blueprint.hydrology().compoundBasins()) {
            if (basin.hasExternalSpill()) {
                compoundExternalSpills++;
            }

            if (basin.drainsDirectlyOutsideDepressionSystem()) {
                compoundOpenSpills++;
            }

            maximumCompoundRise =
                    Math.max(
                            maximumCompoundRise,
                            basin.externalRiseAboveMerge()
                    );
        }

        System.out.println(
                "Compound basins with measured external spills: "
                        + compoundExternalSpills
        );

        System.out.println(
                "Compound basins spilling directly outside the depression system: "
                        + compoundOpenSpills
        );

        System.out.printf(
                "Largest compound spill rise above internal merge level: %.3f elevation units%n",
                maximumCompoundRise
        );

        System.out.println(
                "Planned hydrologic lakes: "
                        + blueprint.hydrology().lakeCount()
        );

        for (LakeSourceType sourceType : LakeSourceType.values()) {
            System.out.println(
                    "  "
                            + sourceType
                            + ": "
                            + blueprint.hydrology()
                            .countLakesBySourceType(sourceType)
            );
        }

        System.out.println(
                "Lake footprint cells: "
                        + blueprint.hydrology().lakeCellCount()
        );

        System.out.printf(
                "Deepest planned lake: %.3f elevation units%n",
                blueprint.hydrology().maximumLakeDepth()
        );

        System.out.printf(
                "Largest planned lake surface area: %.0f blocks^2%n",
                blueprint.hydrology()
                        .largestLakeSurfaceAreaBlocksSquared()
        );

        System.out.println(
                "Final routed hydrology sinks: "
                        + blueprint.hydrology().routedSinkCount()
        );

        System.out.println(
                "Lake interior routed cells: "
                        + blueprint.hydrology().routedCellCount(
                        HydrologyRouteType.LAKE_INTERIOR
                )
        );

        System.out.println(
                "Lake outlet routed cells: "
                        + blueprint.hydrology().routedCellCount(
                        HydrologyRouteType.LAKE_OUTLET
                )
        );

        System.out.println(
                "Compound escape routed cells: "
                        + blueprint.hydrology().routedCellCount(
                        HydrologyRouteType.COMPOUND_ESCAPE
                )
        );

        System.out.println(
                "Cycle escape routed cells: "
                        + blueprint.hydrology().routedCellCount(
                        HydrologyRouteType.CYCLE_ESCAPE
                )
        );

        System.out.println(
                "Residual escape routed cells: "
                        + blueprint.hydrology().routedCellCount(
                        HydrologyRouteType.RESIDUAL_ESCAPE
                )
        );

        System.out.println(
                "Drainage routing failures: "
                        + blueprint.hydrology().routingFailureCount()
        );

        System.out.println(
                "Final routing cycles: "
                        + blueprint.hydrology().routedCycleCount()
                        + " ("
                        + blueprint.hydrology().routedCycleCellCount()
                        + " cells)"
        );

        System.out.println(
                "Flow accumulation processed cells: "
                        + blueprint.hydrology()
                        .flowAccumulationProcessedCellCount()
        );

        System.out.println(
                "Maximum flow accumulation: "
                        + blueprint.hydrology()
                        .maximumFlowAccumulation()
                        + " contributing cells"
        );

        System.out.printf(
                "Largest contributing area: %.0f blocks^2%n",
                blueprint.hydrology()
                        .maximumContributingAreaBlocksSquared()
        );

        System.out.println(
                "Provisional stream-network cells: "
                        + blueprint.hydrology()
                        .streamChannelCellCount()
        );

        System.out.println(
                "Provisional headwater sources: "
                        + blueprint.hydrology()
                        .streamHeadwaterSourceCount()
        );

        for (StreamClass streamClass : StreamClass.values()) {
            if (streamClass == StreamClass.NONE) {
                continue;
            }

            System.out.println(
                    "  "
                            + streamClass
                            + ": "
                            + blueprint.hydrology()
                            .countStreamClass(streamClass)
            );
        }

        System.out.println(
                "Explicit river-graph nodes: "
                        + blueprint.hydrology().riverNodes().size()
        );

        for (RiverNodeType nodeType : RiverNodeType.values()) {
            System.out.println(
                    "  "
                            + nodeType
                            + ": "
                            + blueprint.hydrology()
                            .riverNodeCount(nodeType)
            );
        }

        System.out.println(
                "Explicit river-graph segments: "
                        + blueprint.hydrology().riverSegments().size()
        );

        for (RiverSegmentType segmentType : RiverSegmentType.values()) {
            System.out.println(
                    "  "
                            + segmentType
                            + ": "
                            + blueprint.hydrology()
                            .riverSegmentCount(segmentType)
            );
        }

        System.out.printf(
                "Total macro river-graph length: %.0f blocks%n",
                blueprint.hydrology()
                        .totalRiverGraphLengthBlocks()
        );

        System.out.println(
                "Maximum Strahler order: "
                        + blueprint.hydrology()
                        .maximumStrahlerOrder()
        );

        for (int order = 1;
             order <= blueprint.hydrology().maximumStrahlerOrder();
             order++) {
            System.out.println(
                    "  order "
                            + order
                            + ": "
                            + blueprint.hydrology()
                            .riverSegmentCountForStrahlerOrder(order)
                            + " segments"
            );
        }

        System.out.printf(
                "Maximum potential river magnitude: %.3f%n",
                blueprint.hydrology()
                        .maximumPotentialRiverMagnitude()
        );

        System.out.printf(
                "Maximum provisional river width: %.2f blocks%n",
                blueprint.hydrology()
                        .maximumProvisionalRiverWidthBlocks()
        );

        System.out.printf(
                "Maximum provisional river depth: %.2f blocks%n",
                blueprint.hydrology()
                        .maximumProvisionalRiverDepthBlocks()
        );

        for (RiverScale scale : RiverScale.values()) {
            System.out.println(
                    "  "
                            + scale
                            + ": "
                            + blueprint.hydrology()
                            .riverSegmentCount(scale)
                            + " segments"
            );
        }

        System.out.printf(
                "Maximum continuity-smoothed river width: %.2f blocks%n",
                blueprint.hydrology()
                        .maximumSmoothedRiverWidthBlocks()
        );

        System.out.printf(
                "Maximum continuity-smoothed river depth: %.2f blocks%n",
                blueprint.hydrology()
                        .maximumSmoothedRiverDepthBlocks()
        );

        System.out.printf(
                "Maximum river width continuity adjustment: %.1f%%%n",
                blueprint.hydrology()
                        .maximumRiverWidthAdjustmentFraction()
                        * 100.0
        );

        System.out.println(
                "River segments adjusted >=25% for continuity: "
                        + blueprint.hydrology()
                        .riverContinuityAdjustmentCount(0.25)
        );

        System.out.println(
                "River segments with uphill longitudinal breaks: "
                        + blueprint.hydrology()
                        .riverUphillSegmentCount()
        );

        System.out.printf(
                "Maximum uphill river-profile rise: %.3f elevation units%n",
                blueprint.hydrology()
                        .maximumRiverUphillRise()
        );

        System.out.printf(
                "Maximum extra river grade correction: %.3f blocks%n",
                blueprint.hydrology()
                        .maximumRiverGradeCorrection()
        );

        System.out.printf(
                "Maximum planned river incision: %.3f blocks%n",
                blueprint.hydrology()
                        .maximumPlannedRiverIncision()
        );

        System.out.println(
                "River segments needing >=4 blocks extra grade correction: "
                        + blueprint.hydrology()
                        .riverGradeCorrectionCount(4.0)
        );

        System.out.println(
                "River segments needing >=8 blocks extra grade correction: "
                        + blueprint.hydrology()
                        .riverGradeCorrectionCount(8.0)
        );

        System.out.println(
                "River segments needing >=16 blocks extra grade correction: "
                        + blueprint.hydrology()
                        .riverGradeCorrectionCount(16.0)
        );

        System.out.println(
                "Uphill samples remaining in planned river grade: "
                        + blueprint.hydrology()
                        .riverGradeUphillSampleCount()
        );

        System.out.printf(
                "Maximum local planned river grade: %.4f%n",
                blueprint.hydrology()
                        .maximumPlannedRiverGrade()
        );

        System.out.println(
                "High-resolution river centerline points: "
                        + blueprint.hydrology()
                        .riverCenterlinePointCount()
        );

        System.out.printf(
                "Total high-resolution river centerline length: %.0f blocks%n",
                blueprint.hydrology()
                        .totalRiverCenterlineLengthBlocks()
        );

        System.out.printf(
                "Mean river centerline sinuosity: %.3f%n",
                blueprint.hydrology()
                        .meanRiverCenterlineSinuosity()
        );

        System.out.printf(
                "Maximum river centerline sinuosity: %.3f%n",
                blueprint.hydrology()
                        .maximumRiverCenterlineSinuosity()
        );

        System.out.printf(
                "Maximum synthesized lateral river offset: %.2f blocks%n",
                blueprint.hydrology()
                        .maximumRiverCenterlineOffsetBlocks()
        );

        System.out.println(
                "River valley-corridor samples: "
                        + blueprint.hydrology()
                        .riverValleyCorridorPointCount()
        );

        System.out.printf(
                "Maximum channel corridor width: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverChannelCorridorWidthBlocks()
        );

        System.out.printf(
                "Maximum floodplain corridor width: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverFloodplainWidthBlocks()
        );

        System.out.printf(
                "Maximum valley corridor width: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverValleyCorridorWidthBlocks()
        );

        System.out.printf(
                "Mean river valley confinement: %.3f%n",
                blueprint.hydrology()
                        .meanRiverValleyConfinement()
        );

        System.out.println(
                "River cross-section samples: "
                        + blueprint.hydrology()
                        .riverCrossSectionPointCount()
        );

        System.out.printf(
                "Maximum bankfull river width: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverBankfullWidthBlocks()
        );

        System.out.printf(
                "Maximum bankfull river depth: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverBankfullDepthBlocks()
        );

        System.out.printf(
                "Mean absolute river lateral bias: %.3f%n",
                blueprint.hydrology()
                        .meanAbsoluteRiverLateralBias()
        );

        System.out.println(
                "River terrain-constraint samples: "
                        + blueprint.hydrology()
                        .riverCarvingConstraintPointCount()
        );

        System.out.printf(
                "Maximum river terrain cut budget: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverTerrainCutBudgetBlocks()
        );

        System.out.printf(
                "Maximum river terrain fill budget: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverTerrainFillBudgetBlocks()
        );

        System.out.printf(
                "Mean river terrain cut strength: %.3f%n",
                blueprint.hydrology()
                        .meanRiverTerrainCutStrength()
        );

        System.out.println(
                "River-integrated terrain cells: "
                        + blueprint.hydrology()
                        .riverTerrainModifiedCellCount()
        );

        System.out.printf(
                "Maximum applied river terrain cut: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverTerrainCutAppliedBlocks()
        );

        System.out.printf(
                "Maximum applied river terrain fill: %.1f blocks%n",
                blueprint.hydrology()
                        .maximumRiverTerrainFillAppliedBlocks()
        );

        Path output =
                Path.of(
                        "preview",
                        Long.toString(seed)
                );

        TerrainReviewWriter.write(blueprint, seed, output);

        PreviewWriter.writeAll(
                blueprint,
                seed,
                plan,
                output
        );

        System.out.println(
                "Preview written to: "
                        + output.toAbsolutePath()
        );

    }
}