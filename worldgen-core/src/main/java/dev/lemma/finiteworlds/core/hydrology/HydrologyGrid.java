package dev.lemma.finiteworlds.core.hydrology;

import java.util.Arrays;
import java.util.List;

/**
 * Global hydrology working grid aligned 1:1 with the WorldBlueprint grid.
 *
 * Pass 1D stores the immutable input surface, raw D8 flow direction, measured
 * depression footprints, and provisional basin classification. Later passes
 * can add conditioned elevation, accumulation, watershed IDs, river order,
 * lake state, and erosion products without changing the terrain API.
 */
public final class HydrologyGrid {

    private final int resolution;
    private final double blocksPerCell;

    private final float[] surfaceElevation;
    private final byte[] flowDirection;

    private final float[] conditionedElevation;
    private final byte[] conditionedFlowDirection;

    private final byte[] routedFlowDirection;
    private final byte[] routeType;
    private final byte[] routedCycle;

    private final long[] flowAccumulation;
    private int flowAccumulationProcessedCellCount;
    private long maximumFlowAccumulation;

    private final byte[] streamClass;
    private final short[] streamInitiationThreshold;
    private int streamChannelCellCount;
    private int streamHeadwaterSourceCount;

    private List<RiverNode> riverNodes;
    private List<RiverSegment> riverSegments;
    private List<RiverSegmentHierarchy> riverSegmentHierarchy;
    private List<RiverSegmentMagnitude> riverSegmentMagnitudes;
    private List<RiverSegmentProfile> riverSegmentProfiles;
    private List<RiverSegmentGradePlan> riverSegmentGradePlans;
    private List<RiverSegmentCenterline> riverSegmentCenterlines;
    private List<RiverSegmentValleyCorridor> riverSegmentValleyCorridors;
    private List<RiverSegmentCrossSection> riverSegmentCrossSections;
    private List<RiverSegmentCarvingConstraints> riverSegmentCarvingConstraints;

    private final float[] riverIntegratedElevation;
    private final float[] riverTerrainDelta;
    private final float[] riverValleyFloorStrength;
    private final float[] riverChannelElevation;
    private final float[] riverChannelDelta;
    private final float[] riverWaterSurfaceElevation;
    private final float[] riverChannelStrength;

    private final byte[] breachPath;
    private final float[] breachCutDepth;

    private final int[] depressionId;
    private final float[] depressionDepth;
    private final int[] rawCatchmentDepressionId;
    private final int[] lakeId;
    private final float[] lakeDepth;
    private List<Depression> depressions;
    private List<CompoundBasin> compoundBasins;
    private List<Lake> lakes;
    private List<DepressionClassification> depressionClassifications;
    private List<DepressionResolutionPlan> depressionResolutionPlans;
    private int compoundGroupCount;
    private int appliedBreachCount;
    private int skippedBreachCount;

    private int routingFailureCount;
    private int routedCycleCount;
    private int routedCycleCellCount;

    public HydrologyGrid(
            int resolution,
            double blocksPerCell
    ) {
        this.resolution = resolution;
        this.blocksPerCell = blocksPerCell;

        int cellCount =
                resolution * resolution;

        this.surfaceElevation =
                new float[cellCount];

        this.flowDirection =
                new byte[cellCount];

        this.conditionedElevation =
                new float[cellCount];

        this.conditionedFlowDirection =
                new byte[cellCount];

        this.routedFlowDirection =
                new byte[cellCount];

        this.routeType =
                new byte[cellCount];

        this.routedCycle =
                new byte[cellCount];

        this.flowAccumulation =
                new long[cellCount];

        this.flowAccumulationProcessedCellCount =
                0;

        this.maximumFlowAccumulation =
                0L;

        this.streamClass =
                new byte[cellCount];

        this.streamInitiationThreshold =
                new short[cellCount];

        this.streamChannelCellCount =
                0;

        this.streamHeadwaterSourceCount =
                0;

        this.riverNodes =
                List.of();

        this.riverSegments =
                List.of();

        this.riverSegmentHierarchy =
                List.of();

        this.riverSegmentMagnitudes =
                List.of();

        this.riverSegmentProfiles =
                List.of();

        this.riverSegmentGradePlans =
                List.of();

        this.riverSegmentCenterlines =
                List.of();

        this.riverSegmentValleyCorridors =
                List.of();

        this.riverSegmentCrossSections =
                List.of();

        this.riverSegmentCarvingConstraints =
                List.of();

        this.riverIntegratedElevation =
                new float[cellCount];

        this.riverTerrainDelta =
                new float[cellCount];

        this.riverValleyFloorStrength =
                new float[cellCount];

        this.riverChannelElevation =
                new float[cellCount];

        this.riverChannelDelta =
                new float[cellCount];

        this.riverWaterSurfaceElevation =
                new float[cellCount];

        this.riverChannelStrength =
                new float[cellCount];

        this.breachPath =
                new byte[cellCount];

        this.breachCutDepth =
                new float[cellCount];

        this.depressionId =
                new int[cellCount];

        Arrays.fill(
                depressionId,
                -1
        );

        this.depressionDepth =
                new float[cellCount];

        this.rawCatchmentDepressionId =
                new int[cellCount];

        Arrays.fill(
                rawCatchmentDepressionId,
                -1
        );

        this.lakeId =
                new int[cellCount];

        Arrays.fill(
                lakeId,
                -1
        );

        this.lakeDepth =
                new float[cellCount];

        this.depressions =
                List.of();

        this.compoundBasins =
                List.of();

        this.lakes =
                List.of();

        this.depressionClassifications =
                List.of();

        this.depressionResolutionPlans =
                List.of();

        this.compoundGroupCount =
                0;

        this.appliedBreachCount =
                0;

        this.skippedBreachCount =
                0;

        this.routingFailureCount =
                0;

        this.routedCycleCount =
                0;

        this.routedCycleCellCount =
                0;
    }

    public int resolution() {
        return resolution;
    }

    public double blocksPerCell() {
        return blocksPerCell;
    }

    public float surfaceElevation(
            int x,
            int z
    ) {
        return surfaceElevation[index(x, z)];
    }

    public void setSurfaceElevation(
            int x,
            int z,
            float value
    ) {
        surfaceElevation[index(x, z)] = value;
    }

    public FlowDirection flowDirection(
            int x,
            int z
    ) {
        return FlowDirection.fromOrdinal(
                Byte.toUnsignedInt(
                        flowDirection[index(x, z)]
                )
        );
    }

    public void setFlowDirection(
            int x,
            int z,
            FlowDirection direction
    ) {
        flowDirection[index(x, z)] =
                (byte) direction.ordinal();
    }



    public float conditionedElevation(
            int x,
            int z
    ) {
        return conditionedElevation[index(x, z)];
    }

    void setConditionedElevation(
            int x,
            int z,
            float value
    ) {
        conditionedElevation[index(x, z)] = value;
    }

    public float fillDelta(
            int x,
            int z
    ) {
        int index =
                index(x, z);

        return Math.max(
                0.0f,
                conditionedElevation[index]
                        - surfaceElevation[index]
        );
    }

    public FlowDirection conditionedFlowDirection(
            int x,
            int z
    ) {
        return FlowDirection.fromOrdinal(
                Byte.toUnsignedInt(
                        conditionedFlowDirection[index(x, z)]
                )
        );
    }

    void setConditionedFlowDirection(
            int x,
            int z,
            FlowDirection direction
    ) {
        conditionedFlowDirection[index(x, z)] =
                (byte) direction.ordinal();
    }

    void resetConditionedSurface() {
        System.arraycopy(
                surfaceElevation,
                0,
                conditionedElevation,
                0,
                surfaceElevation.length
        );

        System.arraycopy(
                flowDirection,
                0,
                conditionedFlowDirection,
                0,
                flowDirection.length
        );

        resetRoutedFlow();

        Arrays.fill(
                breachPath,
                (byte) 0
        );

        Arrays.fill(
                breachCutDepth,
                0.0f
        );

        appliedBreachCount = 0;
        skippedBreachCount = 0;
    }

    void resetRoutedFlow() {
        System.arraycopy(
                conditionedFlowDirection,
                0,
                routedFlowDirection,
                0,
                conditionedFlowDirection.length
        );

        Arrays.fill(
                routeType,
                (byte) HydrologyRouteType.ORIGINAL.ordinal()
        );

        Arrays.fill(
                routedCycle,
                (byte) 0
        );

        routingFailureCount = 0;
        routedCycleCount = 0;
        routedCycleCellCount = 0;

        clearFlowAccumulation();
    }

    public FlowDirection routedFlowDirection(
            int x,
            int z
    ) {
        return FlowDirection.fromOrdinal(
                Byte.toUnsignedInt(
                        routedFlowDirection[index(x, z)]
                )
        );
    }

    void setRoutedFlowDirection(
            int x,
            int z,
            FlowDirection direction
    ) {
        routedFlowDirection[index(x, z)] =
                (byte) direction.ordinal();
    }

    public HydrologyRouteType routeType(
            int x,
            int z
    ) {
        int ordinal =
                Byte.toUnsignedInt(
                        routeType[index(x, z)]
                );

        HydrologyRouteType[] values =
                HydrologyRouteType.values();

        if (ordinal < 0 || ordinal >= values.length) {
            return HydrologyRouteType.ORIGINAL;
        }

        return values[ordinal];
    }

    void setRouteType(
            int x,
            int z,
            HydrologyRouteType type
    ) {
        routeType[index(x, z)] =
                (byte) type.ordinal();
    }

    public boolean routedCycle(
            int x,
            int z
    ) {
        return routedCycle[index(x, z)] != 0;
    }

    void clearRoutedCycles() {
        Arrays.fill(
                routedCycle,
                (byte) 0
        );

        routedCycleCount = 0;
        routedCycleCellCount = 0;
    }

    void markRoutedCycle(
            int x,
            int z
    ) {
        routedCycle[index(x, z)] = 1;
    }

    void setRoutedCycleStats(
            int cycleCount,
            int cycleCellCount
    ) {
        this.routedCycleCount = cycleCount;
        this.routedCycleCellCount = cycleCellCount;
    }

    void recordRoutingFailure() {
        routingFailureCount++;
    }

    public int routingFailureCount() {
        return routingFailureCount;
    }

    public int routedCycleCount() {
        return routedCycleCount;
    }

    public int routedCycleCellCount() {
        return routedCycleCellCount;
    }

    public int routedSinkCount() {
        return countSinks(routedFlowDirection);
    }

    public int routedCellCount(
            HydrologyRouteType type
    ) {
        int result = 0;

        for (byte encoded : routeType) {
            int ordinal =
                    Byte.toUnsignedInt(encoded);

            if (ordinal == type.ordinal()) {
                result++;
            }
        }

        return result;
    }


    public long flowAccumulation(
            int x,
            int z
    ) {
        return flowAccumulation[index(x, z)];
    }

    public double contributingAreaBlocksSquared(
            int x,
            int z
    ) {
        double cellArea =
                blocksPerCell * blocksPerCell;

        return flowAccumulation(x, z)
                * cellArea;
    }

    void setFlowAccumulation(
            int x,
            int z,
            long value
    ) {
        flowAccumulation[index(x, z)] =
                value;
    }

    void addFlowAccumulation(
            int x,
            int z,
            long value
    ) {
        flowAccumulation[index(x, z)] +=
                value;
    }

    void clearFlowAccumulation() {
        Arrays.fill(
                flowAccumulation,
                0L
        );

        flowAccumulationProcessedCellCount = 0;
        maximumFlowAccumulation = 0L;
    }

    void setFlowAccumulationStats(
            int processedCellCount,
            long maximumAccumulation
    ) {
        this.flowAccumulationProcessedCellCount =
                processedCellCount;

        this.maximumFlowAccumulation =
                maximumAccumulation;
    }

    public int flowAccumulationProcessedCellCount() {
        return flowAccumulationProcessedCellCount;
    }

    public long maximumFlowAccumulation() {
        return maximumFlowAccumulation;
    }

    public double maximumContributingAreaBlocksSquared() {
        return maximumFlowAccumulation
                * blocksPerCell
                * blocksPerCell;
    }


    public StreamClass streamClass(
            int x,
            int z
    ) {
        return StreamClass.fromOrdinal(
                Byte.toUnsignedInt(
                        streamClass[index(x, z)]
                )
        );
    }

    void setStreamClass(
            int x,
            int z,
            StreamClass value
    ) {
        streamClass[index(x, z)] =
                (byte) value.ordinal();
    }

    public int streamInitiationThreshold(
            int x,
            int z
    ) {
        return Short.toUnsignedInt(
                streamInitiationThreshold[index(x, z)]
        );
    }

    void setStreamInitiationThreshold(
            int x,
            int z,
            int threshold
    ) {
        streamInitiationThreshold[index(x, z)] =
                (short) Math.max(
                        0,
                        Math.min(
                                Short.MAX_VALUE,
                                threshold
                        )
                );
    }

    void clearStreamNetwork() {
        Arrays.fill(
                streamClass,
                (byte) StreamClass.NONE.ordinal()
        );

        Arrays.fill(
                streamInitiationThreshold,
                (short) 0
        );

        streamChannelCellCount = 0;
        streamHeadwaterSourceCount = 0;

        riverNodes = List.of();
        riverSegments = List.of();
        riverSegmentHierarchy = List.of();
        riverSegmentMagnitudes = List.of();
    }

    void setStreamNetworkStats(
            int channelCellCount,
            int headwaterSourceCount
    ) {
        this.streamChannelCellCount =
                channelCellCount;

        this.streamHeadwaterSourceCount =
                headwaterSourceCount;
    }

    public int streamChannelCellCount() {
        return streamChannelCellCount;
    }

    public int streamHeadwaterSourceCount() {
        return streamHeadwaterSourceCount;
    }



    public List<RiverNode> riverNodes() {
        return riverNodes;
    }

    public List<RiverSegment> riverSegments() {
        return riverSegments;
    }

    void setRiverGraph(
            List<RiverNode> nodes,
            List<RiverSegment> segments
    ) {
        this.riverNodes =
                List.copyOf(nodes);

        this.riverSegments =
                List.copyOf(segments);

        this.riverSegmentHierarchy =
                List.of();

        this.riverSegmentMagnitudes =
                List.of();

        this.riverSegmentProfiles =
                List.of();

        this.riverSegmentGradePlans =
                List.of();

        this.riverSegmentCenterlines =
                List.of();

        this.riverSegmentValleyCorridors =
                List.of();
    }

    public List<RiverSegmentHierarchy> riverSegmentHierarchy() {
        return riverSegmentHierarchy;
    }

    public RiverSegmentHierarchy riverSegmentHierarchy(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentHierarchy.size()
        ) {
            return null;
        }

        return riverSegmentHierarchy.get(segmentId);
    }

    void setRiverSegmentHierarchy(
            List<RiverSegmentHierarchy> hierarchy
    ) {
        this.riverSegmentHierarchy =
                List.copyOf(hierarchy);

        this.riverSegmentMagnitudes =
                List.of();

        this.riverSegmentProfiles =
                List.of();

        this.riverSegmentGradePlans =
                List.of();

        this.riverSegmentCenterlines =
                List.of();

        this.riverSegmentValleyCorridors =
                List.of();
    }

    public List<RiverSegmentMagnitude> riverSegmentMagnitudes() {
        return riverSegmentMagnitudes;
    }

    public RiverSegmentMagnitude riverSegmentMagnitude(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentMagnitudes.size()
        ) {
            return null;
        }

        return riverSegmentMagnitudes.get(segmentId);
    }

    void setRiverSegmentMagnitudes(
            List<RiverSegmentMagnitude> magnitudes
    ) {
        this.riverSegmentMagnitudes =
                List.copyOf(magnitudes);

        this.riverSegmentProfiles =
                List.of();

        this.riverSegmentGradePlans =
                List.of();

        this.riverSegmentCenterlines =
                List.of();

        this.riverSegmentValleyCorridors =
                List.of();
    }

    public List<RiverSegmentProfile> riverSegmentProfiles() {
        return riverSegmentProfiles;
    }

    public RiverSegmentProfile riverSegmentProfile(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentProfiles.size()
        ) {
            return null;
        }

        return riverSegmentProfiles.get(segmentId);
    }

    void setRiverSegmentProfiles(
            List<RiverSegmentProfile> profiles
    ) {
        this.riverSegmentProfiles =
                List.copyOf(profiles);

        this.riverSegmentGradePlans =
                List.of();

        this.riverSegmentCenterlines =
                List.of();

        this.riverSegmentValleyCorridors =
                List.of();
    }

    public List<RiverSegmentGradePlan> riverSegmentGradePlans() {
        return riverSegmentGradePlans;
    }

    public RiverSegmentGradePlan riverSegmentGradePlan(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentGradePlans.size()
        ) {
            return null;
        }

        return riverSegmentGradePlans.get(segmentId);
    }

    void setRiverSegmentGradePlans(
            List<RiverSegmentGradePlan> gradePlans
    ) {
        this.riverSegmentGradePlans =
                List.copyOf(gradePlans);

        this.riverSegmentCenterlines =
                List.of();

        this.riverSegmentValleyCorridors =
                List.of();
    }

    public List<RiverSegmentCenterline> riverSegmentCenterlines() {
        return riverSegmentCenterlines;
    }

    public RiverSegmentCenterline riverSegmentCenterline(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentCenterlines.size()
        ) {
            return null;
        }

        return riverSegmentCenterlines.get(segmentId);
    }

    void setRiverSegmentCenterlines(
            List<RiverSegmentCenterline> centerlines
    ) {
        this.riverSegmentCenterlines =
                List.copyOf(centerlines);

        this.riverSegmentValleyCorridors =
                List.of();
    }

    public List<RiverSegmentValleyCorridor> riverSegmentValleyCorridors() {
        return riverSegmentValleyCorridors;
    }

    public RiverSegmentValleyCorridor riverSegmentValleyCorridor(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentValleyCorridors.size()
        ) {
            return null;
        }

        return riverSegmentValleyCorridors.get(segmentId);
    }

    void setRiverSegmentValleyCorridors(
            List<RiverSegmentValleyCorridor> corridors
    ) {
        this.riverSegmentValleyCorridors =
                List.copyOf(corridors);
        this.riverSegmentCrossSections =
                List.of();
    }

    public List<RiverSegmentCrossSection> riverSegmentCrossSections() {
        return riverSegmentCrossSections;
    }

    public RiverSegmentCrossSection riverSegmentCrossSection(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentCrossSections.size()
        ) {
            return null;
        }

        return riverSegmentCrossSections.get(segmentId);
    }

    void setRiverSegmentCrossSections(
            List<RiverSegmentCrossSection> crossSections
    ) {
        this.riverSegmentCrossSections =
                List.copyOf(crossSections);

        this.riverSegmentCarvingConstraints =
                List.of();
    }

    public List<RiverSegmentCarvingConstraints> riverSegmentCarvingConstraints() {
        return riverSegmentCarvingConstraints;
    }

    public RiverSegmentCarvingConstraints riverSegmentCarvingConstraints(
            int segmentId
    ) {
        if (
                segmentId < 0
                        || segmentId >= riverSegmentCarvingConstraints.size()
        ) {
            return null;
        }

        return riverSegmentCarvingConstraints.get(segmentId);
    }

    void setRiverSegmentCarvingConstraints(
            List<RiverSegmentCarvingConstraints> constraints
    ) {
        this.riverSegmentCarvingConstraints =
                List.copyOf(constraints);

        java.util.Arrays.fill(
                riverTerrainDelta,
                0.0f
        );

        java.util.Arrays.fill(
                riverValleyFloorStrength,
                0.0f
        );
    }

    public float riverIntegratedElevation(
            int x,
            int z
    ) {
        return riverIntegratedElevation[index(x, z)];
    }

    public float riverTerrainDelta(
            int x,
            int z
    ) {
        return riverTerrainDelta[index(x, z)];
    }

    public float riverValleyFloorStrength(
            int x,
            int z
    ) {
        return riverValleyFloorStrength[index(x, z)];
    }

    void setRiverTerrainIntegrationCell(
            int x,
            int z,
            float integratedElevation,
            float terrainDelta,
            float valleyFloorStrength
    ) {
        int index = index(x, z);

        riverIntegratedElevation[index] = integratedElevation;
        riverTerrainDelta[index] = terrainDelta;
        riverValleyFloorStrength[index] = valleyFloorStrength;
    }

    public float riverChannelElevation(
            int x,
            int z
    ) {
        return riverChannelElevation[index(x, z)];
    }

    public float riverChannelDelta(
            int x,
            int z
    ) {
        return riverChannelDelta[index(x, z)];
    }

    public float riverWaterSurfaceElevation(
            int x,
            int z
    ) {
        return riverWaterSurfaceElevation[index(x, z)];
    }

    public float riverChannelStrength(
            int x,
            int z
    ) {
        return riverChannelStrength[index(x, z)];
    }

    void setRiverChannelIntegrationCell(
            int x,
            int z,
            float channelElevation,
            float channelDelta,
            float waterSurfaceElevation,
            float channelStrength
    ) {
        int index = index(x, z);

        riverChannelElevation[index] = channelElevation;
        riverChannelDelta[index] = channelDelta;
        riverWaterSurfaceElevation[index] = waterSurfaceElevation;
        riverChannelStrength[index] = channelStrength;
    }

    public double maximumRiverChannelIncisionBlocks() {
        double result = 0.0;

        for (float delta : riverChannelDelta) {
            result = Math.max(
                    result,
                    Math.max(0.0, -delta)
            );
        }

        return result;
    }

    public int riverChannelModifiedCellCount() {
        int result = 0;

        for (float delta : riverChannelDelta) {
            if (delta < -1.0e-4f) {
                result++;
            }
        }

        return result;
    }

    public double minimumRiverWaterSurfaceElevation() {
        double result = Double.POSITIVE_INFINITY;

        for (int i = 0; i < riverWaterSurfaceElevation.length; i++) {
            if (riverChannelStrength[i] <= 1.0e-5f) {
                continue;
            }

            result = Math.min(
                    result,
                    riverWaterSurfaceElevation[i]
            );
        }

        return Double.isFinite(result)
                ? result
                : 0.0;
    }

    public double maximumRiverWaterSurfaceElevation() {
        double result = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < riverWaterSurfaceElevation.length; i++) {
            if (riverChannelStrength[i] <= 1.0e-5f) {
                continue;
            }

            result = Math.max(
                    result,
                    riverWaterSurfaceElevation[i]
            );
        }

        return Double.isFinite(result)
                ? result
                : 0.0;
    }

    public double maximumAbsoluteRiverTerrainDeltaBlocks() {
        double result = 0.0;

        for (float delta : riverTerrainDelta) {
            result = Math.max(
                    result,
                    Math.abs(delta)
            );
        }

        return result;
    }

    public double maximumRiverTerrainCutAppliedBlocks() {
        double result = 0.0;

        for (float delta : riverTerrainDelta) {
            result = Math.max(
                    result,
                    Math.max(0.0, -delta)
            );
        }

        return result;
    }

    public double maximumRiverTerrainFillAppliedBlocks() {
        double result = 0.0;

        for (float delta : riverTerrainDelta) {
            result = Math.max(
                    result,
                    Math.max(0.0, delta)
            );
        }

        return result;
    }

    public int riverTerrainModifiedCellCount() {
        int result = 0;

        for (float delta : riverTerrainDelta) {
            if (Math.abs(delta) > 1.0e-4f) {
                result++;
            }
        }

        return result;
    }

    public int riverCarvingConstraintPointCount() {
        int result = 0;

        for (RiverSegmentCarvingConstraints constraints : riverSegmentCarvingConstraints) {
            result += constraints.points().size();
        }

        return result;
    }

    public double maximumRiverTerrainCutBudgetBlocks() {
        double result = 0.0;

        for (RiverSegmentCarvingConstraints constraints : riverSegmentCarvingConstraints) {
            result = Math.max(
                    result,
                    constraints.maximumCutDepthBlocks()
            );
        }

        return result;
    }

    public double maximumRiverTerrainFillBudgetBlocks() {
        double result = 0.0;

        for (RiverSegmentCarvingConstraints constraints : riverSegmentCarvingConstraints) {
            result = Math.max(
                    result,
                    constraints.maximumFillHeightBlocks()
            );
        }

        return result;
    }

    public double meanRiverTerrainCutStrength() {
        double total = 0.0;
        int count = 0;

        for (RiverSegmentCarvingConstraints constraints : riverSegmentCarvingConstraints) {
            if (constraints.lakePassage() || constraints.points().isEmpty()) {
                continue;
            }

            total += constraints.meanCutStrength();
            count++;
        }

        return count > 0
                ? total / count
                : 0.0;
    }

    public int riverCrossSectionPointCount() {
        int result = 0;

        for (RiverSegmentCrossSection crossSection : riverSegmentCrossSections) {
            result += crossSection.points().size();
        }

        return result;
    }

    public double maximumRiverBankfullWidthBlocks() {
        double result = 0.0;

        for (RiverSegmentCrossSection crossSection : riverSegmentCrossSections) {
            result = Math.max(
                    result,
                    crossSection.maximumBankfullHalfWidthBlocks() * 2.0
            );
        }

        return result;
    }

    public double maximumRiverBankfullDepthBlocks() {
        double result = 0.0;

        for (RiverSegmentCrossSection crossSection : riverSegmentCrossSections) {
            result = Math.max(
                    result,
                    crossSection.maximumBankfullDepthBlocks()
            );
        }

        return result;
    }

    public double meanAbsoluteRiverLateralBias() {
        double total = 0.0;
        int count = 0;

        for (RiverSegmentCrossSection crossSection : riverSegmentCrossSections) {
            for (RiverCrossSectionPoint point : crossSection.points()) {
                total += Math.abs(point.lateralBias());
                count++;
            }
        }

        return count > 0
                ? total / count
                : 0.0;
    }

    public int riverValleyCorridorPointCount() {
        int result = 0;

        for (RiverSegmentValleyCorridor corridor : riverSegmentValleyCorridors) {
            result += corridor.points().size();
        }

        return result;
    }

    public double maximumRiverChannelCorridorWidthBlocks() {
        double result = 0.0;

        for (RiverSegmentValleyCorridor corridor : riverSegmentValleyCorridors) {
            result = Math.max(
                    result,
                    corridor.maximumChannelHalfWidthBlocks() * 2.0
            );
        }

        return result;
    }

    public double maximumRiverFloodplainWidthBlocks() {
        double result = 0.0;

        for (RiverSegmentValleyCorridor corridor : riverSegmentValleyCorridors) {
            result = Math.max(
                    result,
                    corridor.maximumFloodplainHalfWidthBlocks() * 2.0
            );
        }

        return result;
    }

    public double maximumRiverValleyCorridorWidthBlocks() {
        double result = 0.0;

        for (RiverSegmentValleyCorridor corridor : riverSegmentValleyCorridors) {
            result = Math.max(
                    result,
                    corridor.maximumValleyHalfWidthBlocks() * 2.0
            );
        }

        return result;
    }

    public double meanRiverValleyConfinement() {
        double total = 0.0;
        int count = 0;

        for (RiverSegmentValleyCorridor corridor : riverSegmentValleyCorridors) {
            if (
                    corridor.lakePassage()
                            || corridor.points().isEmpty()
            ) {
                continue;
            }

            total += corridor.meanConfinement();
            count++;
        }

        return count > 0
                ? total / count
                : 0.0;
    }

    public int riverCenterlinePointCount() {
        int result = 0;

        for (RiverSegmentCenterline centerline : riverSegmentCenterlines) {
            result +=
                    centerline.points().size();
        }

        return result;
    }

    public double totalRiverCenterlineLengthBlocks() {
        double result = 0.0;

        for (RiverSegmentCenterline centerline : riverSegmentCenterlines) {
            result +=
                    centerline.centerlineLengthBlocks();
        }

        return result;
    }

    public double maximumRiverCenterlineOffsetBlocks() {
        double result = 0.0;

        for (RiverSegmentCenterline centerline : riverSegmentCenterlines) {
            result =
                    Math.max(
                            result,
                            centerline.maximumLateralOffsetBlocks()
                    );
        }

        return result;
    }

    public double maximumRiverCenterlineSinuosity() {
        double result = 1.0;

        for (RiverSegmentCenterline centerline : riverSegmentCenterlines) {
            if (centerline.lakePassage()) {
                continue;
            }

            result =
                    Math.max(
                            result,
                            centerline.sinuosityRatio()
                    );
        }

        return result;
    }

    public double meanRiverCenterlineSinuosity() {
        double total = 0.0;
        int count = 0;

        for (RiverSegmentCenterline centerline : riverSegmentCenterlines) {
            if (
                    centerline.lakePassage()
                            || centerline.points().size() < 2
            ) {
                continue;
            }

            total +=
                    centerline.sinuosityRatio();
            count++;
        }

        return count > 0
                ? total / count
                : 1.0;
    }

    public double maximumRiverGradeCorrection() {
        double result = 0.0;

        for (RiverSegmentGradePlan plan : riverSegmentGradePlans) {
            result =
                    Math.max(
                            result,
                            plan.maximumGradeCorrection()
                    );
        }

        return result;
    }

    public double maximumPlannedRiverIncision() {
        double result = 0.0;

        for (RiverSegmentGradePlan plan : riverSegmentGradePlans) {
            result =
                    Math.max(
                            result,
                            plan.maximumTotalIncision()
                    );
        }

        return result;
    }

    public int riverGradeCorrectionCount(
            double minimumCorrection
    ) {
        int result = 0;

        for (RiverSegmentGradePlan plan : riverSegmentGradePlans) {
            if (
                    !plan.lakePassage()
                            && plan.maximumGradeCorrection() >= minimumCorrection
            ) {
                result++;
            }
        }

        return result;
    }

    public int riverGradeUphillSampleCount() {
        int result = 0;

        for (RiverSegmentGradePlan plan : riverSegmentGradePlans) {
            if (plan.lakePassage()) {
                continue;
            }

            for (int i = 1; i < plan.sampleCount(); i++) {
                if (
                        plan.plannedBedElevationAt(i)
                                > plan.plannedBedElevationAt(i - 1) + 1.0e-6
                ) {
                    result++;
                }
            }
        }

        return result;
    }

    public double maximumPlannedRiverGrade() {
        double result = 0.0;

        for (RiverSegmentGradePlan plan : riverSegmentGradePlans) {
            result =
                    Math.max(
                            result,
                            plan.maximumLocalGrade()
                    );
        }

        return result;
    }

    public double maximumSmoothedRiverWidthBlocks() {
        double result = 0.0;

        for (RiverSegmentProfile profile : riverSegmentProfiles) {
            result =
                    Math.max(
                            result,
                            Math.max(
                                    profile.smoothedStartWidthBlocks(),
                                    profile.smoothedEndWidthBlocks()
                            )
                    );
        }

        return result;
    }

    public double maximumSmoothedRiverDepthBlocks() {
        double result = 0.0;

        for (RiverSegmentProfile profile : riverSegmentProfiles) {
            result =
                    Math.max(
                            result,
                            Math.max(
                                    profile.smoothedStartDepthBlocks(),
                                    profile.smoothedEndDepthBlocks()
                            )
                    );
        }

        return result;
    }

    public double maximumRiverWidthAdjustmentFraction() {
        double result = 0.0;

        for (RiverSegmentProfile profile : riverSegmentProfiles) {
            result =
                    Math.max(
                            result,
                            profile.widthAdjustmentFraction()
                    );
        }

        return result;
    }

    public int riverContinuityAdjustmentCount(
            double minimumFraction
    ) {
        int result = 0;

        for (RiverSegmentProfile profile : riverSegmentProfiles) {
            if (profile.widthAdjustmentFraction() >= minimumFraction) {
                result++;
            }
        }

        return result;
    }

    public int riverUphillSegmentCount() {
        int result = 0;

        for (RiverSegmentProfile profile : riverSegmentProfiles) {
            if (profile.endElevation() > profile.startElevation() + 1.0e-6) {
                result++;
            }
        }

        return result;
    }

    public double maximumRiverUphillRise() {
        double result = 0.0;

        for (RiverSegmentProfile profile : riverSegmentProfiles) {
            result =
                    Math.max(
                            result,
                            profile.endElevation()
                                    - profile.startElevation()
                    );
        }

        return result;
    }

    public double maximumProvisionalRiverWidthBlocks() {
        double result = 0.0;

        for (RiverSegmentMagnitude magnitude : riverSegmentMagnitudes) {
            result =
                    Math.max(
                            result,
                            magnitude.provisionalWidthBlocks()
                    );
        }

        return result;
    }

    public double maximumProvisionalRiverDepthBlocks() {
        double result = 0.0;

        for (RiverSegmentMagnitude magnitude : riverSegmentMagnitudes) {
            result =
                    Math.max(
                            result,
                            magnitude.provisionalDepthBlocks()
                    );
        }

        return result;
    }

    public double maximumPotentialRiverMagnitude() {
        double result = 0.0;

        for (RiverSegmentMagnitude magnitude : riverSegmentMagnitudes) {
            result =
                    Math.max(
                            result,
                            magnitude.potentialMagnitude()
                    );
        }

        return result;
    }

    public int riverSegmentCount(
            RiverScale scale
    ) {
        int result = 0;

        for (RiverSegmentMagnitude magnitude : riverSegmentMagnitudes) {
            if (magnitude.riverScale() == scale) {
                result++;
            }
        }

        return result;
    }

    public int maximumStrahlerOrder() {
        int result = 0;

        for (RiverSegmentHierarchy hierarchy : riverSegmentHierarchy) {
            result =
                    Math.max(
                            result,
                            hierarchy.strahlerOrder()
                    );
        }

        return result;
    }

    public int riverSegmentCountForStrahlerOrder(
            int targetOrder
    ) {
        int result = 0;

        for (RiverSegmentHierarchy hierarchy : riverSegmentHierarchy) {
            if (hierarchy.strahlerOrder() == targetOrder) {
                result++;
            }
        }

        return result;
    }

    public int riverNodeCount(
            RiverNodeType type
    ) {
        int result = 0;

        for (RiverNode node : riverNodes) {
            boolean matches =
                    switch (type) {
                        case SOURCE -> node.source();
                        case CONFLUENCE -> node.confluence();
                        case LAKE_INLET -> node.lakeInlet();
                        case LAKE_OUTLET -> node.lakeOutlet();
                        case MOUTH -> node.mouth();
                    };

            if (matches) {
                result++;
            }
        }

        return result;
    }

    public int riverSegmentCount(
            RiverSegmentType type
    ) {
        int result = 0;

        for (RiverSegment segment : riverSegments) {
            if (segment.type() == type) {
                result++;
            }
        }

        return result;
    }

    public double totalRiverGraphLengthBlocks() {
        double result = 0.0;

        for (RiverSegment segment : riverSegments) {
            result += segment.lengthBlocks();
        }

        return result;
    }

    public int countStreamClass(
            StreamClass target
    ) {
        int result = 0;

        for (byte encoded : streamClass) {
            if (
                    Byte.toUnsignedInt(encoded)
                            == target.ordinal()
            ) {
                result++;
            }
        }

        return result;
    }


    public boolean breachPath(
            int x,
            int z
    ) {
        return breachPath[index(x, z)] != 0;
    }

    public float breachCutDepth(
            int x,
            int z
    ) {
        return breachCutDepth[index(x, z)];
    }

    void markBreachPath(
            int x,
            int z
    ) {
        breachPath[index(x, z)] = 1;
    }

    void recordBreachCut(
            int x,
            int z,
            float depth
    ) {
        if (depth <= 0.0f) {
            return;
        }

        int index =
                index(x, z);

        breachCutDepth[index] +=
                depth;
    }

    void recordAppliedBreach() {
        appliedBreachCount++;
    }

    void recordSkippedBreach() {
        skippedBreachCount++;
    }

    public int appliedBreachCount() {
        return appliedBreachCount;
    }

    public int skippedBreachCount() {
        return skippedBreachCount;
    }

    public float maximumBreachCutDepth() {
        float result =
                0.0f;

        for (float depth : breachCutDepth) {
            result =
                    Math.max(
                            result,
                            depth
                    );
        }

        return result;
    }

    public int depressionId(
            int x,
            int z
    ) {
        return depressionId[index(x, z)];
    }

    public float depressionDepth(
            int x,
            int z
    ) {
        return depressionDepth[index(x, z)];
    }

    public List<Depression> depressions() {
        return depressions;
    }

    public int rawCatchmentDepressionId(
            int x,
            int z
    ) {
        return rawCatchmentDepressionId[index(x, z)];
    }

    void setRawCatchmentDepressionIds(
            int[] assignments
    ) {
        if (assignments.length != rawCatchmentDepressionId.length) {
            throw new IllegalArgumentException(
                    "Catchment assignment length does not match hydrology grid"
            );
        }

        System.arraycopy(
                assignments,
                0,
                rawCatchmentDepressionId,
                0,
                assignments.length
        );
    }

    public List<CompoundBasin> compoundBasins() {
        return compoundBasins;
    }

    public CompoundBasin compoundBasin(
            int groupId
    ) {
        if (groupId < 0 || groupId >= compoundBasins.size()) {
            return null;
        }

        return compoundBasins.get(groupId);
    }

    void setCompoundBasins(
            List<CompoundBasin> compoundBasins
    ) {
        this.compoundBasins =
                List.copyOf(compoundBasins);

        clearLakeAnalysis();
    }


    public int lakeId(
            int x,
            int z
    ) {
        return lakeId[index(x, z)];
    }

    public float lakeDepth(
            int x,
            int z
    ) {
        return lakeDepth[index(x, z)];
    }

    public List<Lake> lakes() {
        return lakes;
    }

    public Lake lake(
            int lakeId
    ) {
        if (lakeId < 0 || lakeId >= lakes.size()) {
            return null;
        }

        return lakes.get(lakeId);
    }

    public int lakeCount() {
        return lakes.size();
    }

    public int countLakesBySourceType(
            LakeSourceType sourceType
    ) {
        int result = 0;

        for (Lake lake : lakes) {
            if (lake.sourceType() == sourceType) {
                result++;
            }
        }

        return result;
    }

    public int lakeCellCount() {
        int result = 0;

        for (int id : lakeId) {
            if (id >= 0) {
                result++;
            }
        }

        return result;
    }

    public float maximumLakeDepth() {
        float result = 0.0f;

        for (float depth : lakeDepth) {
            result = Math.max(result, depth);
        }

        return result;
    }

    public double largestLakeSurfaceAreaBlocksSquared() {
        double result = 0.0;

        for (Lake lake : lakes) {
            result = Math.max(
                    result,
                    lake.surfaceAreaBlocksSquared()
            );
        }

        return result;
    }

    void clearLakeAnalysis() {
        Arrays.fill(
                lakeId,
                -1
        );

        Arrays.fill(
                lakeDepth,
                0.0f
        );

        lakes =
                List.of();
    }

    void setLakeCell(
            int x,
            int z,
            int id,
            float depth
    ) {
        int index =
                index(x, z);

        lakeId[index] = id;
        lakeDepth[index] = Math.max(0.0f, depth);
    }

    void setLakes(
            List<Lake> lakes
    ) {
        this.lakes =
                List.copyOf(lakes);
    }

    public int depressionCount() {
        return depressions.size();
    }

    public List<DepressionClassification> depressionClassifications() {
        return depressionClassifications;
    }

    public DepressionClassification depressionClassification(
            int depressionId
    ) {
        if (
                depressionId < 0
                        || depressionId >= depressionClassifications.size()
        ) {
            return null;
        }

        return depressionClassifications.get(depressionId);
    }

    public int compoundGroupCount() {
        return compoundGroupCount;
    }

    public List<DepressionResolutionPlan> depressionResolutionPlans() {
        return depressionResolutionPlans;
    }

    public DepressionResolutionPlan depressionResolutionPlan(
            int depressionId
    ) {
        if (
                depressionId < 0
                        || depressionId >= depressionResolutionPlans.size()
        ) {
            return null;
        }

        return depressionResolutionPlans.get(depressionId);
    }

    public int countResolutionActions(
            DepressionResolutionAction action
    ) {
        int result = 0;

        for (DepressionResolutionPlan plan : depressionResolutionPlans) {
            if (plan.action() == action) {
                result++;
            }
        }

        return result;
    }

    public int countDepressionsByClass(
            DepressionClass depressionClass
    ) {
        int result = 0;

        for (DepressionClassification classification : depressionClassifications) {
            if (classification.depressionClass() == depressionClass) {
                result++;
            }
        }

        return result;
    }

    public int depressionCellCount() {
        int result = 0;

        for (int id : depressionId) {
            if (id >= 0) {
                result++;
            }
        }

        return result;
    }

    public float maximumDepressionDepth() {
        float result = 0.0f;

        for (float depth : depressionDepth) {
            result =
                    Math.max(
                            result,
                            depth
                    );
        }

        return result;
    }

    void clearDepressionAnalysis() {
        Arrays.fill(
                depressionId,
                -1
        );

        Arrays.fill(
                depressionDepth,
                0.0f
        );

        depressions =
                List.of();
    }

    void setDepressionCell(
            int x,
            int z,
            int id,
            float depth
    ) {
        int index =
                index(x, z);

        depressionId[index] = id;
        depressionDepth[index] = depth;
    }

    void setDepressions(
            List<Depression> depressions
    ) {
        this.depressions =
                List.copyOf(depressions);

        this.compoundBasins =
                List.of();

        clearLakeAnalysis();

        this.depressionClassifications =
                List.of();

        this.depressionResolutionPlans =
                List.of();

        this.compoundGroupCount =
                0;

        this.appliedBreachCount =
                0;

        this.skippedBreachCount =
                0;

        resetRoutedFlow();
    }

    void setDepressionClassifications(
            List<DepressionClassification> classifications,
            int compoundGroupCount
    ) {
        this.depressionClassifications =
                List.copyOf(classifications);

        this.depressionResolutionPlans =
                List.of();

        clearLakeAnalysis();

        this.compoundGroupCount =
                compoundGroupCount;
    }

    void setDepressionResolutionPlans(
            List<DepressionResolutionPlan> plans
    ) {
        this.depressionResolutionPlans =
                List.copyOf(plans);

        clearLakeAnalysis();
    }

    public int sinkCount() {
        return countSinks(flowDirection);
    }

    public int conditionedSinkCount() {
        return countSinks(conditionedFlowDirection);
    }

    public float maximumFillDelta() {
        float result = 0.0f;

        for (int i = 0; i < surfaceElevation.length; i++) {
            result =
                    Math.max(
                            result,
                            conditionedElevation[i]
                                    - surfaceElevation[i]
                    );
        }

        return result;
    }

    private static int countSinks(
            byte[] directions
    ) {
        int result = 0;

        for (byte encoded : directions) {
            if (
                    FlowDirection.fromOrdinal(
                            Byte.toUnsignedInt(encoded)
                    ) == FlowDirection.SINK
            ) {
                result++;
            }
        }

        return result;
    }

    private int index(
            int x,
            int z
    ) {
        return z * resolution + x;
    }
}
