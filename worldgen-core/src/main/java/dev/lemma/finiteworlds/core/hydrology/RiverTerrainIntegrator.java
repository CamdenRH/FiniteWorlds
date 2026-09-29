package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.List;

/**
 * Hydrology Pass 2K: first integration of the planned river valleys into the
 * authored macro terrain.
 *
 * The pass consumes the 2J target elevations and hard cut/fill budgets. It
 * shapes valley floors and floodplains but deliberately stops at bank height;
 * final bankfull channel incision and water-surface placement belong to 2L.
 */
public final class RiverTerrainIntegrator {

    private static final double MINIMUM_WEIGHT = 1.0e-8;

    private RiverTerrainIntegrator() {
    }

    public static void integrate(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology = world.hydrology();
        List<RiverSegmentCarvingConstraints> constraints =
                hydrology.riverSegmentCarvingConstraints();

        int size = world.resolution();
        int cellCount = size * size;

        double[] targetWeighted = new double[cellCount];
        double[] spatialWeight = new double[cellCount];
        double[] cutStrengthWeighted = new double[cellCount];
        double[] fillStrengthWeighted = new double[cellCount];
        double[] cutBudgetWeighted = new double[cellCount];
        double[] fillBudgetWeighted = new double[cellCount];
        double[] floorStrength = new double[cellCount];

        if (!constraints.isEmpty()) {
            rasterizeConstraints(
                    world,
                    constraints,
                    targetWeighted,
                    spatialWeight,
                    cutStrengthWeighted,
                    fillStrengthWeighted,
                    cutBudgetWeighted,
                    fillBudgetWeighted,
                    floorStrength
            );
        }

        applyIntegratedSurface(
                world,
                targetWeighted,
                spatialWeight,
                cutStrengthWeighted,
                fillStrengthWeighted,
                cutBudgetWeighted,
                fillBudgetWeighted,
                floorStrength
        );
    }

    private static void rasterizeConstraints(
            WorldBlueprint world,
            List<RiverSegmentCarvingConstraints> constraints,
            double[] targetWeighted,
            double[] spatialWeight,
            double[] cutStrengthWeighted,
            double[] fillStrengthWeighted,
            double[] cutBudgetWeighted,
            double[] fillBudgetWeighted,
            double[] floorStrength
    ) {
        HydrologyGrid hydrology = world.hydrology();
        int size = world.resolution();
        double blocksPerCell = hydrology.blocksPerCell();
        double halfWorld = world.config().worldSizeBlocks() / 2.0;
        double minimumSampleSpacing = Math.max(8.0, blocksPerCell * 0.60);

        for (RiverSegmentCarvingConstraints segment : constraints) {
            if (segment.lakePassage()) {
                continue;
            }

            List<RiverCarvingConstraintPoint> points = segment.points();
            double previousSampleDistance = Double.NEGATIVE_INFINITY;

            for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
                RiverCarvingConstraintPoint point = points.get(pointIndex);
                boolean endpoint =
                        pointIndex == 0
                                || pointIndex == points.size() - 1;

                if (
                        !endpoint
                                && point.distanceBlocks() - previousSampleDistance
                                < minimumSampleSpacing
                ) {
                    continue;
                }

                previousSampleDistance = point.distanceBlocks();

                double valleyRadius = effectiveValleyRadius(point);

                if (valleyRadius <= 0.0) {
                    continue;
                }

                double centerX =
                        (point.blockX() + halfWorld)
                                / world.config().worldSizeBlocks()
                                * (size - 1);

                double centerZ =
                        (point.blockZ() + halfWorld)
                                / world.config().worldSizeBlocks()
                                * (size - 1);

                int radiusCells = Math.max(
                        1,
                        (int) Math.ceil(valleyRadius / blocksPerCell) + 1
                );

                int minimumX = clampInt(
                        (int) Math.floor(centerX) - radiusCells,
                        0,
                        size - 1
                );

                int maximumX = clampInt(
                        (int) Math.ceil(centerX) + radiusCells,
                        0,
                        size - 1
                );

                int minimumZ = clampInt(
                        (int) Math.floor(centerZ) - radiusCells,
                        0,
                        size - 1
                );

                int maximumZ = clampInt(
                        (int) Math.ceil(centerZ) + radiusCells,
                        0,
                        size - 1
                );

                for (int z = minimumZ; z <= maximumZ; z++) {
                    for (int x = minimumX; x <= maximumX; x++) {
                        if (world.landMask(x, z) < 0.5f) {
                            continue;
                        }

                        double dx = (x - centerX) * blocksPerCell;
                        double dz = (z - centerZ) * blocksPerCell;
                        double distance = Math.hypot(dx, dz);

                        Influence influence = influenceAt(
                                point,
                                distance
                        );

                        if (influence.weight() <= 0.0) {
                            continue;
                        }

                        int index = z * size + x;
                        double reachScale =
                                reachModificationScale(point.reachType());

                        double weight =
                                influence.weight()
                                        * point.junctionBlend()
                                        * reachScale;

                        targetWeighted[index] +=
                                influence.targetElevation() * weight;

                        spatialWeight[index] += weight;

                        cutStrengthWeighted[index] +=
                                point.cutStrength() * weight;

                        fillStrengthWeighted[index] +=
                                point.fillStrength() * weight;

                        cutBudgetWeighted[index] +=
                                point.maximumCutDepthBlocks() * weight;

                        fillBudgetWeighted[index] +=
                                point.maximumFillHeightBlocks() * weight;

                        floorStrength[index] = Math.max(
                                floorStrength[index],
                                influence.floorStrength()
                                        * point.junctionBlend()
                        );
                    }
                }
            }
        }
    }

    private static Influence influenceAt(
            RiverCarvingConstraintPoint point,
            double distance
    ) {
        double channel = Math.max(
                1.0,
                point.channelBlendHalfWidthBlocks()
        );

        double floodplain = Math.max(
                channel,
                point.floodplainBlendHalfWidthBlocks()
        );

        double valley = Math.max(
                floodplain,
                point.valleyBlendHalfWidthBlocks()
        );

        if (distance > valley) {
            return Influence.NONE;
        }

        if (distance <= channel) {
            double local = smoothstep(
                    0.0,
                    channel,
                    distance
            );

            return new Influence(
                    lerp(
                            point.targetBankElevation(),
                            point.targetFloodplainElevation(),
                            0.10 * local
                    ),
                    0.82,
                    0.78
            );
        }

        if (distance <= floodplain) {
            double local = smoothstep(
                    channel,
                    floodplain,
                    distance
            );

            double floodplainWeight =
                    0.62
                            * Math.pow(
                            1.0 - 0.70 * local,
                            1.35
                    );

            return new Influence(
                    lerp(
                            point.targetBankElevation(),
                            point.targetFloodplainElevation(),
                            0.58 * local
                    ),
                    floodplainWeight,
                    0.52 * (1.0 - 0.60 * local)
            );
        }

        double local = smoothstep(
                floodplain,
                valley,
                distance
        );

        double outerWeight = Math.pow(
                1.0 - local,
                2.35
        );

        return new Influence(
                point.targetFloodplainElevation(),
                outerWeight * 0.12,
                outerWeight * 0.06
        );
    }

    private static void applyIntegratedSurface(
            WorldBlueprint world,
            double[] targetWeighted,
            double[] spatialWeight,
            double[] cutStrengthWeighted,
            double[] fillStrengthWeighted,
            double[] cutBudgetWeighted,
            double[] fillBudgetWeighted,
            double[] floorStrength
    ) {
        HydrologyGrid hydrology = world.hydrology();
        int size = world.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                double original = world.elevation(x, z);
                double integrated = original;

                if (
                        world.landMask(x, z) >= 0.5f
                                && spatialWeight[index] > MINIMUM_WEIGHT
                ) {
                    double weight = spatialWeight[index];
                    double target = targetWeighted[index] / weight;
                    double cutStrength = clamp01(
                            cutStrengthWeighted[index] / weight
                    );
                    double fillStrength = clamp01(
                            fillStrengthWeighted[index] / weight
                    );
                    double cutBudget = Math.max(
                            0.0,
                            cutBudgetWeighted[index] / weight
                    );
                    double fillBudget = Math.max(
                            0.0,
                            fillBudgetWeighted[index] / weight
                    );

                    double difference = target - original;
                    double applied;

                    if (difference < 0.0) {
                        applied = -Math.min(
                                cutBudget,
                                -difference * cutStrength
                        );
                    } else {
                        double conservativeFillStrength =
                                fillStrength * 0.32;

                        applied = Math.min(
                                fillBudget * 0.45,
                                difference * conservativeFillStrength
                        );
                    }

                    /*
                     * Keep 2K conservative. Even if overlapping valley samples
                     * collectively request an extreme change, the per-cell cap
                     * prevents this first terrain pass from erasing authored
                     * relief. 2L still owns bankfull channel incision.
                     */
                    applied = clamp(
                            applied,
                            -42.0,
                            4.0
                    );

                    integrated = original + applied;
                }

                world.setElevation(
                        x,
                        z,
                        (float) integrated
                );

                hydrology.setRiverTerrainIntegrationCell(
                        x,
                        z,
                        (float) integrated,
                        (float) (integrated - original),
                        (float) clamp01(floorStrength[index])
                );
            }
        }
    }

    private static double effectiveValleyRadius(
            RiverCarvingConstraintPoint point
    ) {
        double floodplain = Math.max(
                point.channelBlendHalfWidthBlocks(),
                point.floodplainBlendHalfWidthBlocks()
        );

        double valley = Math.max(
                floodplain,
                point.valleyBlendHalfWidthBlocks()
        );

        double outerFraction =
                switch (point.reachType()) {
                    case MOUNTAIN_CONFINED -> 0.72;
                    case V_VALLEY -> 0.68;
                    case FOOTHILL -> 0.54;
                    case ALLUVIAL -> 0.38;
                    case LOWLAND_FLOODPLAIN -> 0.30;
                };

        return floodplain
                + (valley - floodplain) * outerFraction;
    }

    private static double reachModificationScale(
            RiverReachType reachType
    ) {
        return switch (reachType) {
            case MOUNTAIN_CONFINED -> 1.00;
            case V_VALLEY -> 0.92;
            case FOOTHILL -> 0.78;
            case ALLUVIAL -> 0.62;
            case LOWLAND_FLOODPLAIN -> 0.52;
        };
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

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a + (b - a) * clamp01(t);
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

    private static int clampInt(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }

    private record Influence(
            double targetElevation,
            double weight,
            double floorStrength
    ) {
        private static final Influence NONE =
                new Influence(0.0, 0.0, 0.0);
    }
}
