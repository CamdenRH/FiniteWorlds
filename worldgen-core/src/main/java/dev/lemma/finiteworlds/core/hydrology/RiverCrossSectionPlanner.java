package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 2I: converts the 2G centerline plus the 2H valley permission
 * envelope into a concrete, non-destructive channel cross-section plan.
 *
 * The output defines bankfull geometry, bank slopes, floodplain elevation,
 * inner-valley width, reach morphology, and a slowly varying lateral bias.
 * Actual terrain carving is deliberately deferred to later passes.
 */
public final class RiverCrossSectionPlanner {

    private static final int BIAS_SMOOTHING_PASSES = 3;
    private static final int GEOMETRY_SMOOTHING_PASSES = 2;

    private RiverCrossSectionPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology = world.hydrology();
        List<RiverSegmentCenterline> centerlines =
                hydrology.riverSegmentCenterlines();

        if (centerlines.isEmpty()) {
            hydrology.setRiverSegmentCrossSections(List.of());
            return;
        }

        List<RiverSegmentCrossSection> result =
                new ArrayList<>(centerlines.size());

        for (RiverSegmentCenterline centerline : centerlines) {
            RiverSegmentValleyCorridor corridor =
                    hydrology.riverSegmentValleyCorridor(centerline.segmentId());

            if (
                    centerline.lakePassage()
                            || corridor == null
                            || corridor.lakePassage()
                            || centerline.points().size() < 2
                            || centerline.points().size() != corridor.points().size()
            ) {
                result.add(RiverSegmentCrossSection.lakePassage(
                        centerline.segmentId()
                ));
                continue;
            }

            result.add(planSegment(
                    centerline,
                    corridor
            ));
        }

        hydrology.setRiverSegmentCrossSections(result);
    }

    private static RiverSegmentCrossSection planSegment(
            RiverSegmentCenterline centerline,
            RiverSegmentValleyCorridor corridor
    ) {
        List<RiverCenterlinePoint> centerlinePoints = centerline.points();
        List<RiverValleyCorridorPoint> corridorPoints = corridor.points();
        int count = centerlinePoints.size();

        double[] halfWidth = new double[count];
        double[] depth = new double[count];
        double[] leftSlope = new double[count];
        double[] rightSlope = new double[count];
        double[] floodplainElevation = new double[count];
        double[] innerValley = new double[count];
        double[] bias = new double[count];
        RiverReachType[] reachType = new RiverReachType[count];

        for (int i = 0; i < count; i++) {
            RiverCenterlinePoint center = centerlinePoints.get(i);
            RiverValleyCorridorPoint valley = corridorPoints.get(i);

            reachType[i] = classifyReach(valley);

            double availableChannel = Math.max(
                    2.0,
                    valley.channelHalfWidthBlocks() - 2.0
            );

            halfWidth[i] = clamp(
                    Math.max(1.5, 0.5 * center.widthBlocks()),
                    1.5,
                    availableChannel
            );

            depth[i] = clamp(
                    Math.max(1.0, center.depthBlocks())
                            * depthFactor(reachType[i]),
                    1.0,
                    24.0
            );

            double bankSteepness = bankSteepness(reachType[i]);
            double rawBias = deterministicBias(
                    centerline.segmentId(),
                    center.blockX(),
                    center.blockZ(),
                    center.distanceBlocks()
            );

            double mobility = lateralMobility(
                    reachType[i],
                    valley.confinement(),
                    valley.localGrade()
            );

            bias[i] = clamp(
                    rawBias * mobility,
                    -0.38,
                    0.38
            );

            leftSlope[i] = clamp(
                    bankSteepness * (1.0 - 0.35 * bias[i]),
                    0.10,
                    2.40
            );

            rightSlope[i] = clamp(
                    bankSteepness * (1.0 + 0.35 * bias[i]),
                    0.10,
                    2.40
            );

            double freeboard = floodplainFreeboard(
                    reachType[i],
                    depth[i]
            );

            floodplainElevation[i] =
                    center.plannedBedElevation()
                            + depth[i]
                            + freeboard;

            innerValley[i] = clamp(
                    halfWidth[i]
                            + (
                            valley.floodplainHalfWidthBlocks()
                                    - halfWidth[i]
                    ) * innerValleyFraction(reachType[i]),
                    halfWidth[i] + 2.0,
                    valley.valleyHalfWidthBlocks()
            );
        }

        smooth(bias, 0.42, BIAS_SMOOTHING_PASSES);
        smooth(halfWidth, 0.22, GEOMETRY_SMOOTHING_PASSES);
        smooth(depth, 0.20, GEOMETRY_SMOOTHING_PASSES);
        smooth(floodplainElevation, 0.18, GEOMETRY_SMOOTHING_PASSES);
        smooth(innerValley, 0.28, GEOMETRY_SMOOTHING_PASSES);

        List<RiverCrossSectionPoint> points = new ArrayList<>(count);
        double maximumHalfWidth = 0.0;
        double maximumDepth = 0.0;
        double biasTotal = 0.0;

        for (int i = 0; i < count; i++) {
            RiverCenterlinePoint center = centerlinePoints.get(i);
            RiverValleyCorridorPoint valley = corridorPoints.get(i);

            double limitedBias = clamp(bias[i], -0.38, 0.38);
            double leftOffset = halfWidth[i] * (1.0 + limitedBias);
            double rightOffset = halfWidth[i] * (1.0 - limitedBias);

            double slopeBase = bankSteepness(reachType[i]);
            leftSlope[i] = clamp(
                    slopeBase * (1.0 - 0.35 * limitedBias),
                    0.10,
                    2.40
            );
            rightSlope[i] = clamp(
                    slopeBase * (1.0 + 0.35 * limitedBias),
                    0.10,
                    2.40
            );

            innerValley[i] = clamp(
                    innerValley[i],
                    halfWidth[i] + 2.0,
                    valley.valleyHalfWidthBlocks()
            );

            points.add(new RiverCrossSectionPoint(
                    center.blockX(),
                    center.blockZ(),
                    center.distanceBlocks(),
                    reachType[i],
                    center.plannedBedElevation(),
                    halfWidth[i],
                    depth[i],
                    leftOffset,
                    rightOffset,
                    leftSlope[i],
                    rightSlope[i],
                    floodplainElevation[i],
                    innerValley[i],
                    limitedBias,
                    valley.confinement(),
                    valley.localGrade()
            ));

            maximumHalfWidth = Math.max(maximumHalfWidth, halfWidth[i]);
            maximumDepth = Math.max(maximumDepth, depth[i]);
            biasTotal += Math.abs(limitedBias);
        }

        return new RiverSegmentCrossSection(
                centerline.segmentId(),
                false,
                points,
                maximumHalfWidth,
                maximumDepth,
                biasTotal / count
        );
    }

    private static RiverReachType classifyReach(
            RiverValleyCorridorPoint point
    ) {
        double confinement = clamp01(point.confinement());
        double grade = Math.max(0.0, point.localGrade());
        double floodplainRatio = point.valleyHalfWidthBlocks() > 1.0e-6
                ? point.floodplainHalfWidthBlocks() / point.valleyHalfWidthBlocks()
                : 0.0;

        if (confinement >= 0.62 || grade >= 0.055) {
            return RiverReachType.MOUNTAIN_CONFINED;
        }

        if (confinement >= 0.42 || grade >= 0.030) {
            return RiverReachType.V_VALLEY;
        }

        if (confinement >= 0.24 || grade >= 0.014) {
            return RiverReachType.FOOTHILL;
        }

        if (grade <= 0.0065 && floodplainRatio >= 0.62) {
            return RiverReachType.LOWLAND_FLOODPLAIN;
        }

        return RiverReachType.ALLUVIAL;
    }

    private static double depthFactor(
            RiverReachType type
    ) {
        return switch (type) {
            case MOUNTAIN_CONFINED -> 1.16;
            case V_VALLEY -> 1.10;
            case FOOTHILL -> 1.04;
            case ALLUVIAL -> 0.96;
            case LOWLAND_FLOODPLAIN -> 0.90;
        };
    }

    private static double bankSteepness(
            RiverReachType type
    ) {
        return switch (type) {
            case MOUNTAIN_CONFINED -> 1.55;
            case V_VALLEY -> 1.18;
            case FOOTHILL -> 0.82;
            case ALLUVIAL -> 0.46;
            case LOWLAND_FLOODPLAIN -> 0.28;
        };
    }

    private static double floodplainFreeboard(
            RiverReachType type,
            double depth
    ) {
        double base = switch (type) {
            case MOUNTAIN_CONFINED -> 1.80;
            case V_VALLEY -> 1.45;
            case FOOTHILL -> 1.10;
            case ALLUVIAL -> 0.85;
            case LOWLAND_FLOODPLAIN -> 0.65;
        };

        return base + 0.06 * depth;
    }

    private static double innerValleyFraction(
            RiverReachType type
    ) {
        return switch (type) {
            case MOUNTAIN_CONFINED -> 0.18;
            case V_VALLEY -> 0.26;
            case FOOTHILL -> 0.40;
            case ALLUVIAL -> 0.58;
            case LOWLAND_FLOODPLAIN -> 0.76;
        };
    }

    private static double lateralMobility(
            RiverReachType type,
            double confinement,
            double grade
    ) {
        double typeFactor = switch (type) {
            case MOUNTAIN_CONFINED -> 0.18;
            case V_VALLEY -> 0.35;
            case FOOTHILL -> 0.62;
            case ALLUVIAL -> 0.90;
            case LOWLAND_FLOODPLAIN -> 1.00;
        };

        double openness = 1.0 - 0.72 * clamp01(confinement);
        double gradeFactor = 1.0 - smoothstep(0.015, 0.060, grade) * 0.68;

        return clamp01(typeFactor * openness * gradeFactor);
    }

    private static double deterministicBias(
            int segmentId,
            double blockX,
            double blockZ,
            double distanceBlocks
    ) {
        double phase =
                segmentId * 0.731
                        + blockX * 0.00093
                        + blockZ * 0.00117
                        + distanceBlocks * 0.0037;

        return 0.66 * Math.sin(phase)
                + 0.24 * Math.sin(phase * 0.43 + 1.8)
                + 0.10 * Math.sin(phase * 1.91 - 0.6);
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
                double neighborAverage = 0.5 * (values[i - 1] + values[i + 1]);
                scratch[i] = values[i]
                        + strength * (neighborAverage - values[i]);
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

        double t = clamp01((value - edge0) / (edge1 - edge0));
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
        return Math.max(minimum, Math.min(maximum, value));
    }
}
