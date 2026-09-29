package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.List;

/**
 * Hydrology Pass 2L: final macro channel incision and water-surface planning.
 *
 * The surrounding valley/floodplain surface has already been integrated by
 * 2K.1. This pass therefore acts only inside the bankfull channel and immediate
 * bank transition. It is strictly cut-only and preserves the wider terrain.
 *
 * Water surfaces are derived from the conditioned bed/depth geometry, smoothed
 * longitudinally, and constrained to never rise downstream within a segment.
 */
public final class RiverChannelIntegrator {

    private static final double MINIMUM_WEIGHT = 1.0e-8;
    private static final int WATER_SMOOTHING_PASSES = 2;

    private RiverChannelIntegrator() {
    }

    public static void integrate(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology = world.hydrology();
        List<RiverSegmentCrossSection> sections =
                hydrology.riverSegmentCrossSections();

        int size = world.resolution();
        int cellCount = size * size;

        double[] bedWeighted = new double[cellCount];
        double[] waterWeighted = new double[cellCount];
        double[] weight = new double[cellCount];
        double[] strength = new double[cellCount];
        double[] cutBudget = new double[cellCount];

        for (RiverSegmentCrossSection segment : sections) {
            if (
                    segment.lakePassage()
                            || segment.points().size() < 2
            ) {
                continue;
            }

            RiverSegmentCarvingConstraints constraints =
                    hydrology.riverSegmentCarvingConstraints(
                            segment.segmentId()
                    );

            if (
                    constraints == null
                            || constraints.lakePassage()
                            || constraints.points().size()
                            != segment.points().size()
            ) {
                continue;
            }

            rasterizeSegment(
                    world,
                    segment,
                    constraints,
                    bedWeighted,
                    waterWeighted,
                    weight,
                    strength,
                    cutBudget
            );
        }

        applyChannelSurface(
                world,
                bedWeighted,
                waterWeighted,
                weight,
                strength,
                cutBudget
        );
    }

    private static void rasterizeSegment(
            WorldBlueprint world,
            RiverSegmentCrossSection segment,
            RiverSegmentCarvingConstraints constraints,
            double[] bedWeighted,
            double[] waterWeighted,
            double[] weight,
            double[] strength,
            double[] cutBudget
    ) {
        HydrologyGrid hydrology = world.hydrology();
        List<RiverCrossSectionPoint> points = segment.points();
        List<RiverCarvingConstraintPoint> limits =
                constraints.points();

        int size = world.resolution();
        double blocksPerCell = hydrology.blocksPerCell();
        double halfWorld =
                world.config().worldSizeBlocks() / 2.0;

        double[] waterSurface =
                plannedWaterSurface(points);

        double minimumSampleSpacing =
                Math.max(
                        4.0,
                        blocksPerCell * 0.32
                );

        double previousSampleDistance =
                Double.NEGATIVE_INFINITY;

        for (int i = 0; i < points.size(); i++) {
            RiverCrossSectionPoint point = points.get(i);
            RiverCarvingConstraintPoint limit = limits.get(i);

            boolean endpoint =
                    i == 0 || i == points.size() - 1;

            if (
                    !endpoint
                            && point.distanceBlocks()
                            - previousSampleDistance
                            < minimumSampleSpacing
            ) {
                continue;
            }

            previousSampleDistance =
                    point.distanceBlocks();

            Tangent tangent =
                    tangentAt(points, i);

            double bankTransition =
                    bankTransitionWidth(
                            point,
                            blocksPerCell
                    );

            double rasterRadius =
                    Math.max(
                            Math.max(
                                    point.leftBankOffsetBlocks(),
                                    point.rightBankOffsetBlocks()
                            ) + bankTransition,
                            blocksPerCell * 0.58
                    );

            double centerX =
                    (point.blockX() + halfWorld)
                            / world.config().worldSizeBlocks()
                            * (size - 1);

            double centerZ =
                    (point.blockZ() + halfWorld)
                            / world.config().worldSizeBlocks()
                            * (size - 1);

            int radiusCells =
                    Math.max(
                            1,
                            (int) Math.ceil(
                                    rasterRadius / blocksPerCell
                            ) + 1
                    );

            int minimumX =
                    clampInt(
                            (int) Math.floor(centerX)
                                    - radiusCells,
                            0,
                            size - 1
                    );

            int maximumX =
                    clampInt(
                            (int) Math.ceil(centerX)
                                    + radiusCells,
                            0,
                            size - 1
                    );

            int minimumZ =
                    clampInt(
                            (int) Math.floor(centerZ)
                                    - radiusCells,
                            0,
                            size - 1
                    );

            int maximumZ =
                    clampInt(
                            (int) Math.ceil(centerZ)
                                    + radiusCells,
                            0,
                            size - 1
                    );

            for (int z = minimumZ; z <= maximumZ; z++) {
                for (int x = minimumX; x <= maximumX; x++) {
                    if (world.landMask(x, z) < 0.5f) {
                        continue;
                    }

                    double dx =
                            (x - centerX) * blocksPerCell;

                    double dz =
                            (z - centerZ) * blocksPerCell;

                    double lateral =
                            dx * (-tangent.z())
                                    + dz * tangent.x();

                    double longitudinal =
                            dx * tangent.x()
                                    + dz * tangent.z();

                    /*
                     * Keep each raster sample local along the centerline so a
                     * tight bend cannot carve an oversized circular pit.
                     */
                    double longitudinalLimit =
                            Math.max(
                                    blocksPerCell * 0.90,
                                    minimumSampleSpacing * 0.85
                            );

                    if (
                            Math.abs(longitudinal)
                                    > longitudinalLimit
                    ) {
                        continue;
                    }

                    double sideBankWidth =
                            lateral >= 0.0
                                    ? point.leftBankOffsetBlocks()
                                    : point.rightBankOffsetBlocks();

                    double sideBankSlope =
                            lateral >= 0.0
                                    ? point.leftBankSlope()
                                    : point.rightBankSlope();

                    double absoluteLateral =
                            Math.abs(lateral);

                    ChannelInfluence influence =
                            channelInfluence(
                                    point,
                                    absoluteLateral,
                                    sideBankWidth,
                                    sideBankSlope,
                                    blocksPerCell
                            );

                    if (influence.weight() <= 0.0) {
                        continue;
                    }

                    int index =
                            z * size + x;

                    double localWeight =
                            influence.weight()
                                    * limit.junctionBlend();

                    bedWeighted[index] +=
                            influence.targetElevation()
                                    * localWeight;

                    waterWeighted[index] +=
                            waterSurface[i]
                                    * localWeight;

                    weight[index] +=
                            localWeight;

                    strength[index] =
                            Math.max(
                                    strength[index],
                                    influence.strength()
                                            * limit.junctionBlend()
                            );

                    double localBudget =
                            Math.min(
                                    channelCutCap(point),
                                    limit.maximumCutDepthBlocks()
                            );

                    cutBudget[index] =
                            Math.max(
                                    cutBudget[index],
                                    localBudget
                            );
                }
            }
        }
    }

    private static ChannelInfluence channelInfluence(
            RiverCrossSectionPoint point,
            double lateralDistance,
            double bankWidth,
            double bankSlope,
            double blocksPerCell
    ) {
        double depth =
                Math.max(
                        0.75,
                        point.bankfullDepthBlocks()
                );

        /*
         * The macro blueprint can be coarser than the physical channel. Keep a
         * sub-cell antialiasing radius so small rivers remain visible without
         * granting them valley-scale authority.
         */
        double effectiveBankWidth =
                Math.max(
                        bankWidth,
                        blocksPerCell * 0.32
                );

        double transition =
                Math.max(
                        depth
                                / Math.max(
                                0.12,
                                bankSlope
                        ),
                        blocksPerCell * 0.20
                );

        if (lateralDistance <= effectiveBankWidth) {
            double normalized =
                    effectiveBankWidth > 1.0e-6
                            ? lateralDistance
                            / effectiveBankWidth
                            : 0.0;

            /*
             * A slightly concave bed keeps the center deepest while avoiding
             * an unrealistically flat-bottomed macro trench.
             */
            double bedRise =
                    depth
                            * 0.12
                            * normalized
                            * normalized;

            return new ChannelInfluence(
                    point.plannedBedElevation()
                            + bedRise,
                    1.0,
                    1.0 - 0.12 * normalized
            );
        }

        if (
                lateralDistance
                        >= effectiveBankWidth + transition
        ) {
            return ChannelInfluence.NONE;
        }

        double local =
                (lateralDistance - effectiveBankWidth)
                        / transition;

        double eased =
                smoothstep(0.0, 1.0, local);

        double target =
                lerp(
                        point.plannedBedElevation()
                                + depth * 0.12,
                        point.plannedBedElevation()
                                + depth,
                        eased
                );

        double localWeight =
                Math.pow(
                        1.0 - local,
                        1.75
                );

        return new ChannelInfluence(
                target,
                localWeight * 0.86,
                localWeight * 0.72
        );
    }

    private static double[] plannedWaterSurface(
            List<RiverCrossSectionPoint> points
    ) {
        double[] result =
                new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            RiverCrossSectionPoint point =
                    points.get(i);

            result[i] =
                    point.plannedBedElevation()
                            + Math.max(
                            0.55,
                            point.bankfullDepthBlocks()
                                    * 0.72
                    );
        }

        smooth(
                result,
                0.26,
                WATER_SMOOTHING_PASSES
        );

        /*
         * Centerline points are ordered downstream. Prevent local water humps
         * introduced by changing depth or smoothing while allowing flat water
         * surfaces in low-gradient reaches.
         */
        for (int i = 1; i < result.length; i++) {
            result[i] =
                    Math.min(
                            result[i],
                            result[i - 1]
                    );
        }

        return result;
    }

    private static void applyChannelSurface(
            WorldBlueprint world,
            double[] bedWeighted,
            double[] waterWeighted,
            double[] weight,
            double[] strength,
            double[] cutBudget
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        int size =
                world.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index =
                        z * size + x;

                double original =
                        world.elevation(x, z);

                double integrated =
                        original;

                double waterSurface =
                        0.0;

                double localStrength =
                        clamp01(strength[index]);

                if (
                        weight[index] > MINIMUM_WEIGHT
                                && localStrength > 1.0e-6
                ) {
                    double targetBed =
                            bedWeighted[index]
                                    / weight[index];

                    waterSurface =
                            waterWeighted[index]
                                    / weight[index];

                    double desiredCut =
                            Math.max(
                                    0.0,
                                    original - targetBed
                            );

                    double appliedCut =
                            Math.min(
                                    desiredCut * localStrength,
                                    Math.max(
                                            0.0,
                                            cutBudget[index]
                                    )
                            );

                    /*
                     * 2L is intentionally conservative at macro resolution.
                     * Minecraft-scale channel detail can later refine this
                     * metadata without requiring giant blueprint trenches.
                     */
                    appliedCut =
                            Math.min(
                                    appliedCut,
                                    18.0
                            );

                    integrated =
                            original - appliedCut;

                    /*
                     * A water surface may never lie below the carved bed.
                     */
                    waterSurface =
                            Math.max(
                                    waterSurface,
                                    integrated + 0.35
                            );

                    world.setElevation(
                            x,
                            z,
                            (float) integrated
                    );
                }

                hydrology.setRiverChannelIntegrationCell(
                        x,
                        z,
                        (float) integrated,
                        (float) (integrated - original),
                        (float) waterSurface,
                        (float) localStrength
                );
            }
        }
    }

    private static double bankTransitionWidth(
            RiverCrossSectionPoint point,
            double blocksPerCell
    ) {
        double left =
                point.bankfullDepthBlocks()
                        / Math.max(
                        0.12,
                        point.leftBankSlope()
                );

        double right =
                point.bankfullDepthBlocks()
                        / Math.max(
                        0.12,
                        point.rightBankSlope()
                );

        return Math.max(
                Math.max(left, right),
                blocksPerCell * 0.20
        );
    }

    private static double channelCutCap(
            RiverCrossSectionPoint point
    ) {
        double typeScale =
                switch (point.reachType()) {
                    case MOUNTAIN_CONFINED -> 1.35;
                    case V_VALLEY -> 1.25;
                    case FOOTHILL -> 1.12;
                    case ALLUVIAL -> 1.00;
                    case LOWLAND_FLOODPLAIN -> 0.88;
                };

        return clamp(
                2.0
                        + point.bankfullDepthBlocks()
                        * 1.55
                        * typeScale,
                2.0,
                18.0
        );
    }

    private static Tangent tangentAt(
            List<RiverCrossSectionPoint> points,
            int index
    ) {
        int before =
                Math.max(
                        0,
                        index - 1
                );

        int after =
                Math.min(
                        points.size() - 1,
                        index + 1
                );

        RiverCrossSectionPoint a =
                points.get(before);

        RiverCrossSectionPoint b =
                points.get(after);

        double dx =
                b.blockX() - a.blockX();

        double dz =
                b.blockZ() - a.blockZ();

        double length =
                Math.hypot(dx, dz);

        if (length < 1.0e-6) {
            return new Tangent(
                    1.0,
                    0.0
            );
        }

        return new Tangent(
                dx / length,
                dz / length
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

        double[] scratch =
                new double[values.length];

        for (int pass = 0; pass < passes; pass++) {
            scratch[0] = values[0];
            scratch[values.length - 1] =
                    values[values.length - 1];

            for (int i = 1; i < values.length - 1; i++) {
                double neighborAverage =
                        0.5
                                * (
                                values[i - 1]
                                        + values[i + 1]
                        );

                scratch[i] =
                        values[i]
                                + strength
                                * (
                                neighborAverage
                                        - values[i]
                        );
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
            return value >= edge1
                    ? 1.0
                    : 0.0;
        }

        double t =
                clamp01(
                        (value - edge0)
                                / (edge1 - edge0)
                );

        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (b - a) * clamp01(t);
    }

    private static double clamp01(
            double value
    ) {
        return clamp(
                value,
                0.0,
                1.0
        );
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    private static int clampInt(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    private record Tangent(
            double x,
            double z
    ) {
    }

    private record ChannelInfluence(
            double targetElevation,
            double weight,
            double strength
    ) {
        private static final ChannelInfluence NONE =
                new ChannelInfluence(
                        0.0,
                        0.0,
                        0.0
                );
    }
}
