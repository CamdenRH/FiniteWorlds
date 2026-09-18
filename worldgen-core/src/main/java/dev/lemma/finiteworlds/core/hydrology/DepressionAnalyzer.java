package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hydrology Pass 1C: raw depression identification and measurement.
 *
 * This pass is intentionally diagnostic only.  It does not modify terrain,
 * fill pits, breach saddles, create lakes, or recompute D8 flow.
 *
 * The analysis proceeds in three stages:
 *
 *  1. Trace the existing raw D8 graph so every land cell either terminates at
 *     an interior SINK or escapes through an OUTLET / OCEAN cell.
 *  2. For every sink catchment, find the lowest boundary saddle leading to a
 *     different catchment or to the open boundary.
 *  3. Mark the cells inside that catchment that lie below the first spill
 *     elevation.  Those cells form the diagnostic depression footprint.
 *
 * Compound / nested depressions are deliberately not merged yet.  If two raw
 * depressions spill into one another, that relationship is recorded through
 * spillTargetDepressionId so the next pass can classify the hierarchy.
 */
public final class DepressionAnalyzer {

    private static final int UNRESOLVED =
            -2;

    private static final int OPEN_BOUNDARY =
            -1;

    private static final double DEPTH_EPSILON =
            1.0e-5;

    private DepressionAnalyzer() {
    }

    public static void analyze(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        int size =
                world.resolution();

        int cellCount =
                size * size;

        int[] terminalDepression =
                new int[cellCount];

        Arrays.fill(
                terminalDepression,
                UNRESOLVED
        );

        List<Integer> sinkIndices =
                indexSinks(
                        hydrology,
                        terminalDepression,
                        size
                );

        resolveTerminalDepressions(
                hydrology,
                terminalDepression,
                size,
                cellCount
        );

        int depressionCount =
                sinkIndices.size();

        int[] catchmentCellCount =
                new int[depressionCount];

        float[] minimumElevation =
                new float[depressionCount];

        Arrays.fill(
                minimumElevation,
                Float.POSITIVE_INFINITY
        );

        float[] spillElevation =
                new float[depressionCount];

        Arrays.fill(
                spillElevation,
                Float.POSITIVE_INFINITY
        );

        int[] spillIndex =
                new int[depressionCount];

        int[] spillTargetIndex =
                new int[depressionCount];

        Arrays.fill(spillIndex, -1);
        Arrays.fill(spillTargetIndex, -1);

        measureCatchmentsAndSpills(
                hydrology,
                terminalDepression,
                catchmentCellCount,
                minimumElevation,
                spillElevation,
                spillIndex,
                spillTargetIndex,
                size
        );

        hydrology.clearDepressionAnalysis();

        int[] footprintCellCount =
                markDepressionFootprints(
                        hydrology,
                        terminalDepression,
                        spillElevation,
                        size
                );

        List<Depression> depressions =
                buildDepressions(
                        hydrology,
                        terminalDepression,
                        sinkIndices,
                        catchmentCellCount,
                        footprintCellCount,
                        minimumElevation,
                        spillElevation,
                        spillIndex,
                        spillTargetIndex,
                        size
                );

        hydrology.setDepressions(
                depressions
        );
    }

    private static List<Integer> indexSinks(
            HydrologyGrid hydrology,
            int[] terminalDepression,
            int size
    ) {
        List<Integer> sinkIndices =
                new ArrayList<>();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index =
                        index(x, z, size);

                FlowDirection direction =
                        hydrology.flowDirection(
                                x,
                                z
                        );

                if (direction == FlowDirection.SINK) {
                    int depressionId =
                            sinkIndices.size();

                    sinkIndices.add(index);
                    terminalDepression[index] = depressionId;
                } else if (
                        direction == FlowDirection.OCEAN
                                || direction == FlowDirection.OUTLET
                ) {
                    terminalDepression[index] = OPEN_BOUNDARY;
                }
            }
        }

        return sinkIndices;
    }

    private static void resolveTerminalDepressions(
            HydrologyGrid hydrology,
            int[] terminalDepression,
            int size,
            int cellCount
    ) {
        int[] trace =
                new int[cellCount];

        for (int start = 0; start < cellCount; start++) {
            if (terminalDepression[start] != UNRESOLVED) {
                continue;
            }

            int current =
                    start;

            int traceLength =
                    0;

            while (terminalDepression[current] == UNRESOLVED) {
                trace[traceLength++] = current;

                int x =
                        current % size;

                int z =
                        current / size;

                FlowDirection direction =
                        hydrology.flowDirection(
                                x,
                                z
                        );

                if (!direction.routesToNeighbor()) {
                    terminalDepression[current] =
                            direction == FlowDirection.SINK
                                    ? terminalDepression[current]
                                    : OPEN_BOUNDARY;
                    break;
                }

                int nx =
                        x + direction.dx();

                int nz =
                        z + direction.dz();

                if (
                        nx < 0
                                || nx >= size
                                || nz < 0
                                || nz >= size
                ) {
                    terminalDepression[current] = OPEN_BOUNDARY;
                    break;
                }

                current =
                        index(nx, nz, size);
            }

            int terminal =
                    terminalDepression[current];

            for (int i = traceLength - 1; i >= 0; i--) {
                terminalDepression[trace[i]] = terminal;
            }
        }
    }

    private static void measureCatchmentsAndSpills(
            HydrologyGrid hydrology,
            int[] terminalDepression,
            int[] catchmentCellCount,
            float[] minimumElevation,
            float[] spillElevation,
            int[] spillIndex,
            int[] spillTargetIndex,
            int size
    ) {
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int currentIndex =
                        index(x, z, size);

                int depressionId =
                        terminalDepression[currentIndex];

                if (depressionId < 0) {
                    continue;
                }

                float currentElevation =
                        hydrology.surfaceElevation(
                                x,
                                z
                        );

                catchmentCellCount[depressionId]++;

                minimumElevation[depressionId] =
                        Math.min(
                                minimumElevation[depressionId],
                                currentElevation
                        );

                for (FlowDirection direction : FlowDirection.values()) {
                    if (!direction.routesToNeighbor()) {
                        continue;
                    }

                    int nx =
                            x + direction.dx();

                    int nz =
                            z + direction.dz();

                    if (
                            nx < 0
                                    || nx >= size
                                    || nz < 0
                                    || nz >= size
                    ) {
                        continue;
                    }

                    int neighborIndex =
                            index(nx, nz, size);

                    int neighborDepression =
                            terminalDepression[neighborIndex];

                    if (neighborDepression == depressionId) {
                        continue;
                    }

                    float neighborElevation =
                            hydrology.surfaceElevation(
                                    nx,
                                    nz
                            );

                    float candidateSpill =
                            Math.max(
                                    currentElevation,
                                    neighborElevation
                            );

                    if (
                            isBetterSpill(
                                    candidateSpill,
                                    neighborDepression,
                                    spillElevation[depressionId],
                                    spillTargetIndex[depressionId] < 0
                                            ? Integer.MIN_VALUE
                                            : terminalDepression[
                                            spillTargetIndex[depressionId]
                                            ]
                            )
                    ) {
                        spillElevation[depressionId] = candidateSpill;
                        spillIndex[depressionId] = currentIndex;
                        spillTargetIndex[depressionId] = neighborIndex;
                    }
                }
            }
        }
    }

    private static boolean isBetterSpill(
            float candidateElevation,
            int candidateTargetDepression,
            float currentElevation,
            int currentTargetDepression
    ) {
        if (candidateElevation < currentElevation - DEPTH_EPSILON) {
            return true;
        }

        if (
                Math.abs(
                        candidateElevation
                                - currentElevation
                ) <= DEPTH_EPSILON
        ) {
            /*
             * On an equal saddle, prefer an actual open boundary over
             * spilling into another unresolved depression.  This does not
             * alter terrain; it only makes the diagnostic hierarchy less
             * ambiguous when two escape routes have the same elevation.
             */
            return candidateTargetDepression < 0
                    && currentTargetDepression >= 0;
        }

        return false;
    }

    private static int[] markDepressionFootprints(
            HydrologyGrid hydrology,
            int[] terminalDepression,
            float[] spillElevation,
            int size
    ) {
        int[] footprintCellCount =
                new int[spillElevation.length];

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int cellIndex =
                        index(x, z, size);

                int depressionId =
                        terminalDepression[cellIndex];

                if (depressionId < 0) {
                    continue;
                }

                float spill =
                        spillElevation[depressionId];

                if (!Float.isFinite(spill)) {
                    continue;
                }

                float depth =
                        spill
                                - hydrology.surfaceElevation(
                                x,
                                z
                        );

                if (depth <= DEPTH_EPSILON) {
                    continue;
                }

                hydrology.setDepressionCell(
                        x,
                        z,
                        depressionId,
                        depth
                );

                footprintCellCount[depressionId]++;
            }
        }

        return footprintCellCount;
    }

    private static List<Depression> buildDepressions(
            HydrologyGrid hydrology,
            int[] terminalDepression,
            List<Integer> sinkIndices,
            int[] catchmentCellCount,
            int[] footprintCellCount,
            float[] minimumElevation,
            float[] spillElevation,
            int[] spillIndex,
            int[] spillTargetIndex,
            int size
    ) {
        List<Depression> result =
                new ArrayList<>(
                        sinkIndices.size()
                );

        for (int id = 0; id < sinkIndices.size(); id++) {
            int sinkIndex =
                    sinkIndices.get(id);

            int sinkX =
                    sinkIndex % size;

            int sinkZ =
                    sinkIndex / size;

            float minimum =
                    minimumElevation[id];

            if (!Float.isFinite(minimum)) {
                minimum =
                        hydrology.surfaceElevation(
                                sinkX,
                                sinkZ
                        );
            }

            float spill =
                    spillElevation[id];

            if (!Float.isFinite(spill)) {
                spill = minimum;
            }

            int spillCellIndex =
                    spillIndex[id];

            int targetCellIndex =
                    spillTargetIndex[id];

            int spillX =
                    spillCellIndex >= 0
                            ? spillCellIndex % size
                            : -1;

            int spillZ =
                    spillCellIndex >= 0
                            ? spillCellIndex / size
                            : -1;

            int targetX =
                    targetCellIndex >= 0
                            ? targetCellIndex % size
                            : -1;

            int targetZ =
                    targetCellIndex >= 0
                            ? targetCellIndex / size
                            : -1;

            int targetDepression =
                    targetCellIndex >= 0
                            ? terminalDepression[targetCellIndex]
                            : OPEN_BOUNDARY;

            result.add(
                    new Depression(
                            id,
                            sinkX,
                            sinkZ,
                            footprintCellCount[id],
                            catchmentCellCount[id],
                            minimum,
                            spill,
                            Math.max(
                                    0.0f,
                                    spill - minimum
                            ),
                            spillX,
                            spillZ,
                            targetX,
                            targetZ,
                            targetDepression
                    )
            );
        }

        return List.copyOf(result);
    }

    private static int index(
            int x,
            int z,
            int size
    ) {
        return z * size + x;
    }
}
