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

    private final int[] depressionId;
    private final float[] depressionDepth;
    private List<Depression> depressions;
    private List<DepressionClassification> depressionClassifications;
    private int compoundGroupCount;

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

        this.depressionId =
                new int[cellCount];

        Arrays.fill(
                depressionId,
                -1
        );

        this.depressionDepth =
                new float[cellCount];

        this.depressions =
                List.of();

        this.depressionClassifications =
                List.of();

        this.compoundGroupCount =
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

        this.depressionClassifications =
                List.of();

        this.compoundGroupCount =
                0;
    }

    void setDepressionClassifications(
            List<DepressionClassification> classifications,
            int compoundGroupCount
    ) {
        this.depressionClassifications =
                List.copyOf(classifications);

        this.compoundGroupCount =
                compoundGroupCount;
    }

    public int sinkCount() {
        int result = 0;

        for (byte encoded : flowDirection) {
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
