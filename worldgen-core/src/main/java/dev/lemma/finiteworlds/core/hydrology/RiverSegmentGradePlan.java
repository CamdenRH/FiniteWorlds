package dev.lemma.finiteworlds.core.hydrology;

import java.util.Arrays;

/**
 * Hydrology Pass 2F.1 planned longitudinal channel grade for one explicit
 * river segment.
 *
 * The plan is metadata only. It records the terrain surface, the ordinary
 * depth-offset bed target from Pass 2F, and a monotonic downstream bed profile
 * that later high-resolution channel synthesis can carve toward. Lake passage
 * segments remain semantic and therefore do not carry a physical bed profile.
 */
public final class RiverSegmentGradePlan {

    private final int segmentId;
    private final boolean lakePassage;

    private final float[] terrainElevation;
    private final float[] targetBedElevation;
    private final float[] plannedBedElevation;

    private final double maximumGradeCorrection;
    private final double meanGradeCorrection;
    private final double maximumTotalIncision;
    private final double meanTotalIncision;
    private final double averagePlannedGrade;
    private final double maximumLocalGrade;

    public RiverSegmentGradePlan(
            int segmentId,
            boolean lakePassage,
            float[] terrainElevation,
            float[] targetBedElevation,
            float[] plannedBedElevation,
            double maximumGradeCorrection,
            double meanGradeCorrection,
            double maximumTotalIncision,
            double meanTotalIncision,
            double averagePlannedGrade,
            double maximumLocalGrade
    ) {
        this.segmentId = segmentId;
        this.lakePassage = lakePassage;
        this.terrainElevation = Arrays.copyOf(terrainElevation, terrainElevation.length);
        this.targetBedElevation = Arrays.copyOf(targetBedElevation, targetBedElevation.length);
        this.plannedBedElevation = Arrays.copyOf(plannedBedElevation, plannedBedElevation.length);
        this.maximumGradeCorrection = maximumGradeCorrection;
        this.meanGradeCorrection = meanGradeCorrection;
        this.maximumTotalIncision = maximumTotalIncision;
        this.meanTotalIncision = meanTotalIncision;
        this.averagePlannedGrade = averagePlannedGrade;
        this.maximumLocalGrade = maximumLocalGrade;
    }

    public static RiverSegmentGradePlan lakePassage(
            int segmentId
    ) {
        return new RiverSegmentGradePlan(
                segmentId,
                true,
                new float[0],
                new float[0],
                new float[0],
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    public int segmentId() {
        return segmentId;
    }

    public boolean lakePassage() {
        return lakePassage;
    }

    public int sampleCount() {
        return plannedBedElevation.length;
    }

    public float terrainElevationAt(
            int sampleIndex
    ) {
        return terrainElevation[sampleIndex];
    }

    public float targetBedElevationAt(
            int sampleIndex
    ) {
        return targetBedElevation[sampleIndex];
    }

    public float plannedBedElevationAt(
            int sampleIndex
    ) {
        return plannedBedElevation[sampleIndex];
    }

    public double gradeCorrectionAt(
            int sampleIndex
    ) {
        return Math.max(
                0.0,
                targetBedElevation[sampleIndex]
                        - plannedBedElevation[sampleIndex]
        );
    }

    public double totalIncisionAt(
            int sampleIndex
    ) {
        return Math.max(
                0.0,
                terrainElevation[sampleIndex]
                        - plannedBedElevation[sampleIndex]
        );
    }

    public double plannedStartBedElevation() {
        if (plannedBedElevation.length == 0) {
            return Double.NaN;
        }

        return plannedBedElevation[0];
    }

    public double plannedEndBedElevation() {
        if (plannedBedElevation.length == 0) {
            return Double.NaN;
        }

        return plannedBedElevation[plannedBedElevation.length - 1];
    }

    public double maximumGradeCorrection() {
        return maximumGradeCorrection;
    }

    public double meanGradeCorrection() {
        return meanGradeCorrection;
    }

    public double maximumTotalIncision() {
        return maximumTotalIncision;
    }

    public double meanTotalIncision() {
        return meanTotalIncision;
    }

    public double averagePlannedGrade() {
        return averagePlannedGrade;
    }

    public double maximumLocalGrade() {
        return maximumLocalGrade;
    }
}
