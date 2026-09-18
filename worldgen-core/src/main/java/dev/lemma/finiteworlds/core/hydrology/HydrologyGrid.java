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
