package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 2J: vertically conditions the 2I cross-section plan and
 * establishes hard terrain-modification budgets for later valley/channel
 * integration.
 *
 * This pass deliberately remains non-destructive. It records target bed,
 * bank, and floodplain elevations; lateral blend envelopes; cut/fill limits;
 * and junction softening. Hydrology Pass 2K may consume these values to alter
 * the elevation field.
 */
public final class RiverCarvingConstraintPlanner {

    private static final int LONGITUDINAL_SMOOTHING_PASSES = 2;

    private RiverCarvingConstraintPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology = world.hydrology();
        List<RiverSegmentCrossSection> crossSections =
                hydrology.riverSegmentCrossSections();

        if (crossSections.isEmpty()) {
            hydrology.setRiverSegmentCarvingConstraints(List.of());
            return;
        }

        List<RiverSegmentCarvingConstraints> result =
                new ArrayList<>(crossSections.size());

        for (RiverSegmentCrossSection crossSection : crossSections) {
            RiverSegmentValleyCorridor corridor =
                    hydrology.riverSegmentValleyCorridor(crossSection.segmentId());

            if (
                    crossSection.lakePassage()
                            || corridor == null
                            || corridor.lakePassage()
                            || crossSection.points().size() < 2
                            || crossSection.points().size() != corridor.points().size()
            ) {
                result.add(RiverSegmentCarvingConstraints.lakePassage(
                        crossSection.segmentId()
                ));
                continue;
            }

            result.add(planSegment(
                    crossSection,
                    corridor
            ));
        }

        hydrology.setRiverSegmentCarvingConstraints(result);
    }

    private static RiverSegmentCarvingConstraints planSegment(
            RiverSegmentCrossSection crossSection,
            RiverSegmentValleyCorridor corridor
    ) {
        List<RiverCrossSectionPoint> sections = crossSection.points();
        List<RiverValleyCorridorPoint> valleys = corridor.points();
        int count = sections.size();

        double[] floodplain = new double[count];
        double[] cutBudget = new double[count];
        double[] fillBudget = new double[count];
        double[] cutStrength = new double[count];
        double[] fillStrength = new double[count];

        for (int i = 0; i < count; i++) {
            RiverCrossSectionPoint section = sections.get(i);
            RiverValleyCorridorPoint valley = valleys.get(i);

            double minimumFloodplain =
                    section.plannedBedElevation()
                            + section.bankfullDepthBlocks()
                            + minimumFreeboard(section.reachType());

            floodplain[i] = Math.max(
                    minimumFloodplain,
                    section.floodplainElevation()
            );

            cutBudget[i] = cutBudget(
                    section.reachType(),
                    section.bankfullDepthBlocks(),
                    valley.confinement(),
                    valley.localGrade()
            );

            fillBudget[i] = fillBudget(
                    section.reachType(),
                    valley.confinement()
            );

            cutStrength[i] = cutStrength(
                    section.reachType(),
                    valley.confinement(),
                    valley.localGrade()
            );

            fillStrength[i] = fillStrength(
                    section.reachType(),
                    valley.confinement()
            );
        }

        smooth(floodplain, 0.18, LONGITUDINAL_SMOOTHING_PASSES);
        smooth(cutBudget, 0.22, LONGITUDINAL_SMOOTHING_PASSES);
        smooth(fillBudget, 0.22, LONGITUDINAL_SMOOTHING_PASSES);
        smooth(cutStrength, 0.18, LONGITUDINAL_SMOOTHING_PASSES);
        smooth(fillStrength, 0.18, LONGITUDINAL_SMOOTHING_PASSES);

        List<RiverCarvingConstraintPoint> points = new ArrayList<>(count);
        double maximumCut = 0.0;
        double maximumFill = 0.0;
        double cutStrengthTotal = 0.0;

        double totalLength = Math.max(
                1.0,
                sections.get(count - 1).distanceBlocks()
                        - sections.get(0).distanceBlocks()
        );

        for (int i = 0; i < count; i++) {
            RiverCrossSectionPoint section = sections.get(i);
            RiverValleyCorridorPoint valley = valleys.get(i);

            double minimumFloodplain =
                    section.plannedBedElevation()
                            + section.bankfullDepthBlocks()
                            + minimumFreeboard(section.reachType());

            floodplain[i] = Math.max(
                    floodplain[i],
                    minimumFloodplain
            );

            double bankElevation =
                    section.plannedBedElevation()
                            + section.bankfullDepthBlocks();

            double distanceFromStart =
                    section.distanceBlocks()
                            - sections.get(0).distanceBlocks();

            double distanceToEnd =
                    sections.get(count - 1).distanceBlocks()
                            - section.distanceBlocks();

            double junctionBlend = junctionBlend(
                    Math.min(distanceFromStart, distanceToEnd),
                    totalLength
            );

            double channelBlend = clamp(
                    Math.max(
                            section.bankfullHalfWidthBlocks() + 3.0,
                            valley.channelHalfWidthBlocks()
                    ),
                    section.bankfullHalfWidthBlocks(),
                    valley.valleyHalfWidthBlocks()
            );

            double floodplainBlend = clamp(
                    Math.max(
                            section.innerValleyHalfWidthBlocks(),
                            valley.floodplainHalfWidthBlocks()
                    ),
                    channelBlend,
                    valley.valleyHalfWidthBlocks()
            );

            double valleyBlend = Math.max(
                    floodplainBlend,
                    valley.valleyHalfWidthBlocks()
            );

            double localCut = clamp(cutBudget[i], 2.0, 220.0);
            double localFill = clamp(fillBudget[i], 0.5, 12.0);
            double localCutStrength = clamp01(cutStrength[i] * junctionBlend);
            double localFillStrength = clamp01(fillStrength[i] * junctionBlend);

            points.add(new RiverCarvingConstraintPoint(
                    section.blockX(),
                    section.blockZ(),
                    section.distanceBlocks(),
                    section.reachType(),
                    section.plannedBedElevation(),
                    bankElevation,
                    floodplain[i],
                    localCut,
                    localFill,
                    channelBlend,
                    floodplainBlend,
                    valleyBlend,
                    localCutStrength,
                    localFillStrength,
                    junctionBlend,
                    valley.confinement(),
                    valley.localGrade()
            ));

            maximumCut = Math.max(maximumCut, localCut);
            maximumFill = Math.max(maximumFill, localFill);
            cutStrengthTotal += localCutStrength;
        }

        return new RiverSegmentCarvingConstraints(
                crossSection.segmentId(),
                false,
                points,
                maximumCut,
                maximumFill,
                cutStrengthTotal / count
        );
    }

    private static double cutBudget(
            RiverReachType type,
            double bankfullDepth,
            double confinement,
            double grade
    ) {
        double base = switch (type) {
            case MOUNTAIN_CONFINED -> 175.0;
            case V_VALLEY -> 135.0;
            case FOOTHILL -> 70.0;
            case ALLUVIAL -> 18.0;
            case LOWLAND_FLOODPLAIN -> 12.0;
        };

        double reliefAllowance =
                12.0 * clamp01(confinement)
                        + 10.0 * smoothstep(0.015, 0.075, grade);

        return clamp(
                base
                        + reliefAllowance
                        + 0.65 * Math.max(0.0, bankfullDepth),
                4.0,
                220.0
        );
    }

    private static double fillBudget(
            RiverReachType type,
            double confinement
    ) {
        double base = switch (type) {
            case MOUNTAIN_CONFINED -> 1.5;
            case V_VALLEY -> 2.5;
            case FOOTHILL -> 4.0;
            case ALLUVIAL -> 6.0;
            case LOWLAND_FLOODPLAIN -> 8.0;
        };

        return clamp(
                base * (1.0 - 0.45 * clamp01(confinement)),
                0.5,
                10.0
        );
    }

    private static double cutStrength(
            RiverReachType type,
            double confinement,
            double grade
    ) {
        double base = switch (type) {
            case MOUNTAIN_CONFINED -> 0.92;
            case V_VALLEY -> 0.86;
            case FOOTHILL -> 0.78;
            case ALLUVIAL -> 0.68;
            case LOWLAND_FLOODPLAIN -> 0.60;
        };

        return clamp01(
                base
                        + 0.06 * smoothstep(0.02, 0.07, grade)
                        - 0.05 * (1.0 - clamp01(confinement))
        );
    }

    private static double fillStrength(
            RiverReachType type,
            double confinement
    ) {
        double base = switch (type) {
            case MOUNTAIN_CONFINED -> 0.12;
            case V_VALLEY -> 0.18;
            case FOOTHILL -> 0.28;
            case ALLUVIAL -> 0.40;
            case LOWLAND_FLOODPLAIN -> 0.52;
        };

        return clamp01(
                base * (1.0 - 0.55 * clamp01(confinement))
        );
    }

    private static double minimumFreeboard(
            RiverReachType type
    ) {
        return switch (type) {
            case MOUNTAIN_CONFINED -> 0.8;
            case V_VALLEY -> 1.0;
            case FOOTHILL -> 1.3;
            case ALLUVIAL -> 1.6;
            case LOWLAND_FLOODPLAIN -> 1.9;
        };
    }

    private static double junctionBlend(
            double endpointDistance,
            double segmentLength
    ) {
        double blendDistance = clamp(
                0.16 * segmentLength,
                48.0,
                144.0
        );

        return 0.72
                + 0.28
                * smoothstep(
                0.0,
                blendDistance,
                Math.max(0.0, endpointDistance)
        );
    }

    private static void smooth(
            double[] values,
            double strength,
            int passes
    ) {
        if (values.length < 3) {
            return;
        }

        double[] scratch = new double[values.length];

        for (int pass = 0; pass < passes; pass++) {
            scratch[0] = values[0];
            scratch[values.length - 1] = values[values.length - 1];

            for (int i = 1; i < values.length - 1; i++) {
                double neighborAverage =
                        0.5 * (values[i - 1] + values[i + 1]);

                scratch[i] =
                        values[i]
                                + strength
                                * (neighborAverage - values[i]);
            }

            System.arraycopy(
                    scratch,
                    0,
                    values,
                    0,
                    values.length
            );
        }
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0 : 0.0;
        }

        double t = clamp01(
                (value - edge0) / (edge1 - edge0)
        );

        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(
            double value
    ) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }
}
