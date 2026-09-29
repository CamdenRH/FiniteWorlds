package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 2H: plans the lateral terrain envelope around the physical
 * 2G river centerlines.
 *
 * This pass remains non-destructive. It does not carve valleys, floodplains,
 * banks, or channels. It only records how much lateral terrain later erosion
 * is allowed to reorganize, based on channel scale, potential magnitude,
 * longitudinal grade, terrain province, and local cross-valley relief.
 */
public final class RiverValleyCorridorPlanner {

    private static final int WIDTH_SMOOTHING_PASSES = 2;

    private RiverValleyCorridorPlanner() {
    }

    public static void plan(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        List<RiverSegmentCenterline> centerlines =
                hydrology.riverSegmentCenterlines();

        if (centerlines.isEmpty()) {
            hydrology.setRiverSegmentValleyCorridors(
                    List.of()
            );
            return;
        }

        List<RiverSegmentValleyCorridor> result =
                new ArrayList<>(centerlines.size());

        for (RiverSegmentCenterline centerline : centerlines) {
            if (
                    centerline.lakePassage()
                            || centerline.points().size() < 2
            ) {
                result.add(
                        RiverSegmentValleyCorridor.lakePassage(
                                centerline.segmentId()
                        )
                );
                continue;
            }

            RiverSegmentMagnitude magnitude =
                    hydrology.riverSegmentMagnitude(
                            centerline.segmentId()
                    );

            result.add(
                    planSegment(
                            world,
                            centerline,
                            magnitude
                    )
            );
        }

        hydrology.setRiverSegmentValleyCorridors(
                result
        );
    }

    private static RiverSegmentValleyCorridor planSegment(
            WorldBlueprint world,
            RiverSegmentCenterline centerline,
            RiverSegmentMagnitude magnitude
    ) {
        List<RiverCenterlinePoint> source =
                centerline.points();

        int count =
                source.size();

        double[] channel =
                new double[count];

        double[] floodplain =
                new double[count];

        double[] valley =
                new double[count];

        double[] confinement =
                new double[count];

        double[] grade =
                new double[count];

        double potentialMagnitude =
                magnitude != null
                        ? clamp01(
                        magnitude.potentialMagnitude()
                )
                        : 0.0;

        for (int i = 0; i < count; i++) {
            RiverCenterlinePoint point =
                    source.get(i);

            Tangent tangent =
                    tangentAt(
                            source,
                            i
                    );

            grade[i] =
                    localGrade(
                            source,
                            i
                    );

            TerrainProvince province =
                    terrainProvinceAtBlock(
                            world,
                            point.blockX(),
                            point.blockZ()
                    );

            double confinementValue =
                    localConfinement(
                            world,
                            point,
                            tangent,
                            grade[i],
                            province
                    );

            confinement[i] =
                    confinementValue;

            channel[i] =
                    channelHalfWidth(
                            point.widthBlocks(),
                            point.depthBlocks()
                    );

            double openValleyHalfWidth =
                    openValleyHalfWidth(
                            point.widthBlocks(),
                            potentialMagnitude,
                            province
                    );

            double confinementFactor =
                    1.0
                            - 0.72
                            * Math.pow(
                            confinementValue,
                            1.15
                    );

            valley[i] =
                    clamp(
                            Math.max(
                                    channel[i] + 14.0,
                                    openValleyHalfWidth
                                            * confinementFactor
                            ),
                            channel[i] + 14.0,
                            1280.0
                    );

            double floodplainFraction =
                    floodplainFraction(
                            potentialMagnitude,
                            grade[i],
                            confinementValue,
                            province
                    );

            floodplain[i] =
                    channel[i]
                            + (
                            valley[i]
                                    - channel[i]
                    )
                            * floodplainFraction;
        }

        smoothWidths(
                channel,
                0.22
        );

        smoothWidths(
                floodplain,
                0.34
        );

        smoothWidths(
                valley,
                0.38
        );

        List<RiverValleyCorridorPoint> points =
                new ArrayList<>(count);

        double maximumChannel = 0.0;
        double maximumFloodplain = 0.0;
        double maximumValley = 0.0;
        double confinementTotal = 0.0;

        for (int i = 0; i < count; i++) {
            RiverCenterlinePoint sourcePoint =
                    source.get(i);

            floodplain[i] =
                    clamp(
                            floodplain[i],
                            channel[i],
                            valley[i]
                    );

            valley[i] =
                    Math.max(
                            valley[i],
                            floodplain[i]
                    );

            points.add(
                    new RiverValleyCorridorPoint(
                            sourcePoint.blockX(),
                            sourcePoint.blockZ(),
                            sourcePoint.distanceBlocks(),
                            channel[i],
                            floodplain[i],
                            valley[i],
                            confinement[i],
                            grade[i]
                    )
            );

            maximumChannel =
                    Math.max(
                            maximumChannel,
                            channel[i]
                    );

            maximumFloodplain =
                    Math.max(
                            maximumFloodplain,
                            floodplain[i]
                    );

            maximumValley =
                    Math.max(
                            maximumValley,
                            valley[i]
                    );

            confinementTotal +=
                    confinement[i];
        }

        return new RiverSegmentValleyCorridor(
                centerline.segmentId(),
                false,
                points,
                maximumChannel,
                maximumFloodplain,
                maximumValley,
                confinementTotal / count
        );
    }

    private static double channelHalfWidth(
            double widthBlocks,
            double depthBlocks
    ) {
        return Math.max(
                4.0,
                0.52 * Math.max(1.0, widthBlocks)
                        + 4.0
                        + 1.40
                        * Math.max(
                        0.0,
                        depthBlocks
                )
        );
    }

    private static double openValleyHalfWidth(
            double widthBlocks,
            double potentialMagnitude,
            TerrainProvince province
    ) {
        double width =
                Math.max(
                        1.0,
                        widthBlocks
                );

        double base =
                36.0
                        + 12.0
                        * Math.pow(
                        width,
                        0.90
                );

        double magnitudeFactor =
                0.90
                        + 0.25
                        * clamp01(
                        potentialMagnitude
                );

        return base
                * magnitudeFactor
                * provinceValleyFactor(
                province
        );
    }

    private static double floodplainFraction(
            double potentialMagnitude,
            double grade,
            double confinement,
            TerrainProvince province
    ) {
        double flatness =
                1.0
                        - smoothstep(
                        0.0025,
                        0.028,
                        grade
                );

        double openness =
                1.0
                        - Math.pow(
                        clamp01(confinement),
                        1.10
                );

        double magnitudeFactor =
                0.72
                        + 0.28
                        * clamp01(
                        potentialMagnitude
                );

        return clamp01(
                provinceFloodplainFactor(
                        province
                )
                        * flatness
                        * openness
                        * magnitudeFactor
                        * 1.18
        );
    }

    private static double localConfinement(
            WorldBlueprint world,
            RiverCenterlinePoint point,
            Tangent tangent,
            double grade,
            TerrainProvince province
    ) {
        double probeDistance =
                clamp(
                        96.0
                                + point.widthBlocks()
                                * 5.0,
                        96.0,
                        480.0
                );

        double normalX =
                -tangent.z();

        double normalZ =
                tangent.x();

        double leftRise =
                sideRise(
                        world,
                        point,
                        normalX,
                        normalZ,
                        probeDistance
                );

        double rightRise =
                sideRise(
                        world,
                        point,
                        -normalX,
                        -normalZ,
                        probeDistance
                );

        double leftScore =
                smoothstep(
                        10.0,
                        90.0,
                        leftRise
                );

        double rightScore =
                smoothstep(
                        10.0,
                        90.0,
                        rightRise
                );

        double bilateralRelief =
                0.72
                        * Math.min(
                        leftScore,
                        rightScore
                )
                        + 0.28
                        * Math.max(
                        leftScore,
                        rightScore
                );

        double gradeScore =
                smoothstep(
                        0.010,
                        0.075,
                        grade
                );

        return clamp01(
                0.60 * bilateralRelief
                        + 0.25
                        * provinceConfinementPrior(
                        province
                )
                        + 0.15 * gradeScore
        );
    }

    private static double sideRise(
            WorldBlueprint world,
            RiverCenterlinePoint point,
            double normalX,
            double normalZ,
            double probeDistance
    ) {
        double nearElevation =
                world.elevationAtBlock(
                        point.blockX()
                                + normalX
                                * probeDistance
                                * 0.52,
                        point.blockZ()
                                + normalZ
                                * probeDistance
                                * 0.52
                );

        double farElevation =
                world.elevationAtBlock(
                        point.blockX()
                                + normalX
                                * probeDistance,
                        point.blockZ()
                                + normalZ
                                * probeDistance
                );

        double nearRise =
                Math.max(
                        0.0,
                        nearElevation
                                - point.plannedBedElevation()
                );

        double farRise =
                Math.max(
                        0.0,
                        farElevation
                                - point.plannedBedElevation()
                );

        return 0.58 * nearRise
                + 0.42 * farRise;
    }

    private static Tangent tangentAt(
            List<RiverCenterlinePoint> points,
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

        RiverCenterlinePoint a =
                points.get(before);

        RiverCenterlinePoint b =
                points.get(after);

        double dx =
                b.blockX() - a.blockX();

        double dz =
                b.blockZ() - a.blockZ();

        double length =
                Math.hypot(
                        dx,
                        dz
                );

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

    private static double localGrade(
            List<RiverCenterlinePoint> points,
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

        if (before == after) {
            return 0.0;
        }

        RiverCenterlinePoint a =
                points.get(before);

        RiverCenterlinePoint b =
                points.get(after);

        double distance =
                Math.hypot(
                        b.blockX() - a.blockX(),
                        b.blockZ() - a.blockZ()
                );

        if (distance < 1.0e-6) {
            return 0.0;
        }

        return Math.abs(
                b.plannedBedElevation()
                        - a.plannedBedElevation()
        ) / distance;
    }

    private static void smoothWidths(
            double[] values,
            double strength
    ) {
        if (values.length < 3) {
            return;
        }

        double[] scratch =
                new double[values.length];

        for (int pass = 0; pass < WIDTH_SMOOTHING_PASSES; pass++) {
            scratch[0] =
                    values[0];

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

    private static TerrainProvince terrainProvinceAtBlock(
            WorldBlueprint world,
            double blockX,
            double blockZ
    ) {
        double halfWorld =
                world.config().worldSizeBlocks()
                        / 2.0;

        double u =
                (blockX + halfWorld)
                        / world.config().worldSizeBlocks();

        double v =
                (blockZ + halfWorld)
                        / world.config().worldSizeBlocks();

        int x =
                clampInt(
                        (int) Math.round(
                                u
                                        * (world.resolution() - 1)
                        ),
                        0,
                        world.resolution() - 1
                );

        int z =
                clampInt(
                        (int) Math.round(
                                v
                                        * (world.resolution() - 1)
                        ),
                        0,
                        world.resolution() - 1
                );

        return world.terrainProvince(
                x,
                z
        );
    }

    private static double provinceValleyFactor(
            TerrainProvince province
    ) {
        return switch (province) {
            case OCEAN -> 0.55;
            case COASTAL -> 1.12;
            case COAST_RANGE -> 0.66;
            case WESTERN_LOWLAND -> 1.28;
            case CASCADE_FOOTHILLS -> 0.78;
            case CASCADE_CORE -> 0.56;
            case EASTERN_SLOPES -> 0.86;
            case INTERIOR_PLATEAU -> 1.05;
        };
    }

    private static double provinceFloodplainFactor(
            TerrainProvince province
    ) {
        return switch (province) {
            case OCEAN -> 0.0;
            case COASTAL -> 1.00;
            case COAST_RANGE -> 0.18;
            case WESTERN_LOWLAND -> 1.00;
            case CASCADE_FOOTHILLS -> 0.36;
            case CASCADE_CORE -> 0.08;
            case EASTERN_SLOPES -> 0.55;
            case INTERIOR_PLATEAU -> 0.82;
        };
    }

    private static double provinceConfinementPrior(
            TerrainProvince province
    ) {
        return switch (province) {
            case OCEAN -> 0.0;
            case COASTAL -> 0.12;
            case COAST_RANGE -> 0.68;
            case WESTERN_LOWLAND -> 0.12;
            case CASCADE_FOOTHILLS -> 0.55;
            case CASCADE_CORE -> 0.82;
            case EASTERN_SLOPES -> 0.43;
            case INTERIOR_PLATEAU -> 0.25;
        };
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
}
