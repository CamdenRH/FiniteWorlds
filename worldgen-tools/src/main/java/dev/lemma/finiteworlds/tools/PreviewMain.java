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

import java.nio.file.Path;

public final class PreviewMain {

    public static void main(String[] args)
            throws Exception {

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

        Path output =
                Path.of(
                        "preview",
                        Long.toString(seed)
                );

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