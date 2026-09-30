package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 2G: converts the coarse 64-block-scale river graph into
 * smooth block-space centerlines while preserving the validated macro
 * topology from Passes 2A-2F.1.
 *
 * The synthesis is deliberately non-destructive. Centerline points carry the
 * planned bed grade and continuity-smoothed channel dimensions forward, but
 * terrain carving is deferred to later valley/channel passes.
 */
public final class RiverCenterlineSynthesizer {

    private static final double SAMPLE_SPACING_BLOCKS = 8.0;

    private static final int CHAIKIN_ITERATIONS = 2;
    private static final int OFFSET_SMOOTHING_PASSES = 3;

    private static final double MINIMUM_CORRIDOR_BLOCKS = 5.0;
    private static final double MAXIMUM_CORRIDOR_CELLS = 3.5;

    private static final double TERRAIN_PROBE_MIN_BLOCKS = 6.0;
    private static final double TERRAIN_PROBE_MAX_BLOCKS = 28.0;

    private RiverCenterlineSynthesizer() {
    }

    public static void synthesize(
            WorldBlueprint world,
            long seed
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        List<RiverSegment> segments =
                hydrology.riverSegments();

        List<RiverSegmentProfile> profiles =
                hydrology.riverSegmentProfiles();

        List<RiverSegmentMagnitude> magnitudes =
                hydrology.riverSegmentMagnitudes();

        List<RiverSegmentGradePlan> gradePlans =
                hydrology.riverSegmentGradePlans();

        if (segments.isEmpty()) {
            hydrology.setRiverSegmentCenterlines(
                    List.of()
            );
            return;
        }

        if (
                profiles.size() != segments.size()
                        || magnitudes.size() != segments.size()
                        || gradePlans.size() != segments.size()
        ) {
            throw new IllegalStateException(
                    "River centerline synthesis requires complete Pass-2E, Pass-2F, and Pass-2F.1 metadata"
            );
        }

        ValueNoise wanderNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "hydrology-river-centerline-wander"
                        )
                );

        ValueNoise secondaryNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                seed,
                                "hydrology-river-centerline-secondary"
                        )
                );

        List<RiverSegmentCenterline> result =
                new ArrayList<>(segments.size());

        for (RiverSegment segment : segments) {
            int segmentId =
                    segment.id();

            if (segment.type() == RiverSegmentType.LAKE_PASSAGE) {
                result.add(
                        RiverSegmentCenterline.lakePassage(
                                segmentId
                        )
                );
                continue;
            }

            RiverSegmentProfile profile =
                    profiles.get(segmentId);

            RiverSegmentMagnitude magnitude =
                    magnitudes.get(segmentId);

            RiverSegmentGradePlan gradePlan =
                    gradePlans.get(segmentId);

            if (
                    gradePlan == null
                            || gradePlan.lakePassage()
                            || gradePlan.sampleCount() <= 0
            ) {
                throw new IllegalStateException(
                        "Physical river segment "
                                + segmentId
                                + " is missing its planned bed grade"
                );
            }

            List<Vec2> macroPoints =
                    macroBlockPoints(
                            world,
                            segment
                    );

            if (macroPoints.size() < 2) {
                result.add(
                        singlePointFallback(
                                segment,
                                profile,
                                gradePlan,
                                macroPoints
                        )
                );
                continue;
            }

            List<Vec2> smoothedReference =
                    chaikinSmooth(
                            macroPoints,
                            CHAIKIN_ITERATIONS
                    );

            List<SamplePoint> samples =
                    resamplePolyline(
                            smoothedReference,
                            SAMPLE_SPACING_BLOCKS
                    );

            double lateralFreedom =
                    lateralFreedom(
                            world,
                            hydrology,
                            segment,
                            magnitude
                    );

            double corridorBlocks =
                    corridorBlocks(
                            hydrology,
                            profile,
                            lateralFreedom
                    );

            double[] lateralOffsets =
                    buildLateralOffsets(
                            world,
                            samples,
                            wanderNoise,
                            secondaryNoise,
                            corridorBlocks,
                            lateralFreedom
                    );

            List<Vec2> displaced =
                    displaceSamples(
                            world,
                            samples,
                            lateralOffsets
                    );

            double centerlineLength =
                    polylineLength(displaced);

            double referenceLength =
                    Math.max(
                            1.0,
                            polylineLength(smoothedReference)
                    );

            double[] centerlineDistances =
                    cumulativeDistances(displaced);

            double maximumOffset =
                    0.0;

            List<RiverCenterlinePoint> points =
                    new ArrayList<>(displaced.size());

            for (int i = 0; i < displaced.size(); i++) {
                Vec2 point =
                        displaced.get(i);

                double t =
                        centerlineLength <= 1.0e-9
                                ? 0.0
                                : centerlineDistances[i]
                                / centerlineLength;

                t =
                        clamp01(t);

                double easedT =
                        smoothstep01(t);

                double plannedBed =
                        interpolateGrade(
                                gradePlan,
                                t
                        );

                double width =
                        lerp(
                                profile.smoothedStartWidthBlocks(),
                                profile.smoothedEndWidthBlocks(),
                                easedT
                        );

                double depth =
                        lerp(
                                profile.smoothedStartDepthBlocks(),
                                profile.smoothedEndDepthBlocks(),
                                easedT
                        );

                maximumOffset =
                        Math.max(
                                maximumOffset,
                                Math.abs(lateralOffsets[i])
                        );

                points.add(
                        new RiverCenterlinePoint(
                                point.x(),
                                point.z(),
                                centerlineDistances[i],
                                plannedBed,
                                width,
                                depth,
                                lateralOffsets[i]
                        )
                );
            }

            enforceExactEndpoints(
                    points,
                    macroPoints,
                    profile,
                    gradePlan
            );

            result.add(
                    new RiverSegmentCenterline(
                            segmentId,
                            false,
                            points,
                            centerlineLength,
                            referenceLength,
                            centerlineLength / referenceLength,
                            maximumOffset,
                            lateralFreedom
                    )
            );
        }

        hydrology.setRiverSegmentCenterlines(
                result
        );
    }

    private static RiverSegmentCenterline singlePointFallback(
            RiverSegment segment,
            RiverSegmentProfile profile,
            RiverSegmentGradePlan gradePlan,
            List<Vec2> macroPoints
    ) {
        if (macroPoints.isEmpty()) {
            return new RiverSegmentCenterline(
                    segment.id(),
                    false,
                    List.of(),
                    0.0,
                    0.0,
                    1.0,
                    0.0,
                    0.0
            );
        }

        Vec2 point =
                macroPoints.getFirst();

        return new RiverSegmentCenterline(
                segment.id(),
                false,
                List.of(
                        new RiverCenterlinePoint(
                                point.x(),
                                point.z(),
                                0.0,
                                gradePlan.plannedStartBedElevation(),
                                profile.smoothedStartWidthBlocks(),
                                profile.smoothedStartDepthBlocks(),
                                0.0
                        )
                ),
                0.0,
                0.0,
                1.0,
                0.0,
                0.0
        );
    }

    private static List<Vec2> macroBlockPoints(
            WorldBlueprint world,
            RiverSegment segment
    ) {
        int resolution =
                world.resolution();

        double worldSize =
                world.config().worldSizeBlocks();

        double halfWorld =
                worldSize * 0.5;

        double gridSpacing =
                worldSize
                        / Math.max(
                        1.0,
                        resolution - 1.0
                );

        List<Vec2> points =
                new ArrayList<>(segment.cellPath().size());

        for (int cell : segment.cellPath()) {
            int x =
                    cell % resolution;

            int z =
                    cell / resolution;

            Vec2 point =
                    new Vec2(
                            -halfWorld + x * gridSpacing,
                            -halfWorld + z * gridSpacing
                    );

            if (
                    points.isEmpty()
                            || distanceSquared(
                            points.getLast(),
                            point
                    ) > 1.0e-12
            ) {
                points.add(point);
            }
        }

        return List.copyOf(points);
    }

    private static List<Vec2> chaikinSmooth(
            List<Vec2> input,
            int iterations
    ) {
        if (input.size() <= 2) {
            return input;
        }

        List<Vec2> current =
                new ArrayList<>(input);

        for (int iteration = 0; iteration < iterations; iteration++) {
            List<Vec2> next =
                    new ArrayList<>(current.size() * 2);

            next.add(
                    current.getFirst()
            );

            for (int i = 0; i < current.size() - 1; i++) {
                Vec2 a =
                        current.get(i);

                Vec2 b =
                        current.get(i + 1);

                next.add(
                        lerp(
                                a,
                                b,
                                0.25
                        )
                );

                next.add(
                        lerp(
                                a,
                                b,
                                0.75
                        )
                );
            }

            next.add(
                    current.getLast()
            );

            current =
                    next;
        }

        return List.copyOf(current);
    }

    private static List<SamplePoint> resamplePolyline(
            List<Vec2> polyline,
            double spacing
    ) {
        if (polyline.size() <= 1) {
            List<SamplePoint> single =
                    new ArrayList<>();

            if (!polyline.isEmpty()) {
                single.add(
                        new SamplePoint(
                                polyline.getFirst(),
                                0.0
                        )
                );
            }

            return List.copyOf(single);
        }

        double[] cumulative =
                cumulativeDistances(polyline);

        double totalLength =
                cumulative[cumulative.length - 1];

        if (totalLength <= 1.0e-9) {
            return List.of(
                    new SamplePoint(
                            polyline.getFirst(),
                            0.0
                    )
            );
        }

        int sampleCount =
                Math.max(
                        2,
                        (int) Math.ceil(
                                totalLength / spacing
                        ) + 1
                );

        List<SamplePoint> result =
                new ArrayList<>(sampleCount);

        int segmentIndex =
                0;

        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            double targetDistance =
                    sampleIndex == sampleCount - 1
                            ? totalLength
                            : Math.min(
                            totalLength,
                            sampleIndex * spacing
                    );

            while (
                    segmentIndex < polyline.size() - 2
                            && cumulative[segmentIndex + 1] < targetDistance
            ) {
                segmentIndex++;
            }

            double segmentStart =
                    cumulative[segmentIndex];

            double segmentEnd =
                    cumulative[segmentIndex + 1];

            double t =
                    segmentEnd <= segmentStart + 1.0e-12
                            ? 0.0
                            : (targetDistance - segmentStart)
                            / (segmentEnd - segmentStart);

            result.add(
                    new SamplePoint(
                            lerp(
                                    polyline.get(segmentIndex),
                                    polyline.get(segmentIndex + 1),
                                    clamp01(t)
                            ),
                            targetDistance / totalLength
                    )
            );
        }

        return List.copyOf(result);
    }

    private static double lateralFreedom(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            RiverSegment segment,
            RiverSegmentMagnitude magnitude
    ) {
        TerrainProvince province =
                dominantProvince(
                        world,
                        segment
                );

        double provinceFreedom =
                switch (province) {
                    case WESTERN_LOWLAND -> 0.92;
                    case COASTAL -> 0.84;
                    case INTERIOR_PLATEAU -> 0.76;
                    case EASTERN_SLOPES -> 0.52;
                    case CASCADE_FOOTHILLS -> 0.44;
                    case COAST_RANGE -> 0.36;
                    case CASCADE_CORE -> 0.20;
                    case OCEAN -> 0.0;
                };

        double slope =
                Math.max(
                        0.0,
                        magnitude.channelSlope()
                );

        double slopeFreedom =
                1.0
                        / (1.0 + 32.0 * slope);

        double widthFactor =
                clamp01(
                        magnitude.provisionalWidthBlocks()
                                / 32.0
                );

        return clamp01(
                provinceFreedom
                        * (0.50 + 0.50 * slopeFreedom)
                        * (0.82 + 0.18 * widthFactor)
        );
    }

    private static TerrainProvince dominantProvince(
            WorldBlueprint world,
            RiverSegment segment
    ) {
        int[] counts =
                new int[TerrainProvince.values().length];

        int resolution =
                world.resolution();

        for (int cell : segment.cellPath()) {
            int x =
                    cell % resolution;

            int z =
                    cell / resolution;

            TerrainProvince province =
                    world.terrainProvince(
                            x,
                            z
                    );

            counts[province.ordinal()]++;
        }

        TerrainProvince best =
                TerrainProvince.COASTAL;

        int bestCount =
                -1;

        for (TerrainProvince province : TerrainProvince.values()) {
            if (
                    province == TerrainProvince.OCEAN
                            || counts[province.ordinal()] <= bestCount
            ) {
                continue;
            }

            best =
                    province;

            bestCount =
                    counts[province.ordinal()];
        }

        return best;
    }

    private static double corridorBlocks(
            HydrologyGrid hydrology,
            RiverSegmentProfile profile,
            double lateralFreedom
    ) {
        double blocksPerCell =
                hydrology.blocksPerCell();

        double widthInfluence =
                Math.min(
                        blocksPerCell * 0.12,
                        profile.averageSmoothedWidthBlocks() * 0.28
                );

        double corridor =
                blocksPerCell
                        * (
                        0.14
                                + 3.0 * lateralFreedom
                )
                        + widthInfluence;

        return clamp(
                corridor,
                MINIMUM_CORRIDOR_BLOCKS,
                blocksPerCell * MAXIMUM_CORRIDOR_CELLS
        );
    }

    private static double[] buildLateralOffsets(
            WorldBlueprint world,
            List<SamplePoint> samples,
            ValueNoise wanderNoise,
            ValueNoise secondaryNoise,
            double corridorBlocks,
            double lateralFreedom
    ) {
        double[] offsets =
                new double[samples.size()];

        if (samples.size() <= 2) {
            return offsets;
        }

        double primaryWavelength =
                lerp(
                        420.0,
                        1_250.0,
                        lateralFreedom
                );

        double secondaryWavelength =
                lerp(
                        190.0,
                        520.0,
                        lateralFreedom
                );

        double reachLength = 0;
        for (int i=1; i<samples.size(); i++) reachLength += Math.sqrt(distanceSquared(samples.get(i-1).point(), samples.get(i).point()));
        corridorBlocks = Math.min(corridorBlocks, reachLength * 0.16);
        double phase = wanderNoise.sample(samples.getFirst().point().x()/1900, samples.getFirst().point().z()/1900) * Math.PI;

        for (int i = 1; i < samples.size() - 1; i++) {
            SamplePoint sample =
                    samples.get(i);

            Vec2 tangent =
                    tangent(
                            samples,
                            i
                    );

            Vec2 normal =
                    new Vec2(
                            -tangent.z(),
                            tangent.x()
                    );

            double envelope =
                    Math.pow(
                            Math.sin(
                                    Math.PI
                                            * clamp01(sample.normalizedDistance())
                            ),
                            1.15
                    );

            double primary =
                    wanderNoise.fbm(
                            sample.point().x() / primaryWavelength,
                            sample.point().z() / primaryWavelength,
                            3,
                            2.0,
                            0.52
                    );

            double secondary =
                    secondaryNoise.fbm(
                            sample.point().x() / secondaryWavelength,
                            sample.point().z() / secondaryWavelength,
                            2,
                            2.0,
                            0.45
                    );

            double noiseOffset =
                    corridorBlocks
                            * (
                            0.42 * primary
                                    + 0.12 * secondary
                                    + 0.64 * Math.sin(sample.normalizedDistance() * reachLength / primaryWavelength * Math.PI * 2 + phase)
                    );

            double probeDistance =
                    clamp(
                            corridorBlocks * 0.72,
                            TERRAIN_PROBE_MIN_BLOCKS,
                            TERRAIN_PROBE_MAX_BLOCKS
                    );

            double positiveElevation =
                    world.smoothElevationAtBlock(
                            sample.point().x()
                                    + normal.x() * probeDistance,
                            sample.point().z()
                                    + normal.z() * probeDistance
                    );

            double negativeElevation =
                    world.smoothElevationAtBlock(
                            sample.point().x()
                                    - normal.x() * probeDistance,
                            sample.point().z()
                                    - normal.z() * probeDistance
                    );

            /*
             * Positive normal should be favored when that side is lower.
             * Terrain guidance stays intentionally weaker than the coherent
             * long-wave wander so the macro routing graph remains dominant.
             */
            double terrainPreference =
                    clamp(
                            (negativeElevation - positiveElevation)
                                    / 10.0,
                            -1.0,
                            1.0
                    );

            double terrainOffset =
                    terrainPreference
                            * corridorBlocks
                            * (
                            0.14
                                    + 0.18 * (1.0 - lateralFreedom)
                    );

            offsets[i] =
                    clamp(
                            (noiseOffset + terrainOffset)
                                    * envelope,
                            -corridorBlocks,
                            corridorBlocks
                    );
        }

        for (int pass = 0; pass < OFFSET_SMOOTHING_PASSES; pass++) {
            offsets =
                    smoothOffsets(
                            offsets,
                            corridorBlocks
                    );
        }

        offsets[0] = 0.0;
        offsets[offsets.length - 1] = 0.0;

        return offsets;
    }

    private static double[] smoothOffsets(
            double[] input,
            double corridorBlocks
    ) {
        if (input.length <= 2) {
            return input;
        }

        double[] result =
                input.clone();

        for (int i = 1; i < input.length - 1; i++) {
            double value =
                    0.18 * input[Math.max(0, i - 2)]
                            + 0.22 * input[i - 1]
                            + 0.20 * input[i]
                            + 0.22 * input[i + 1]
                            + 0.18 * input[Math.min(input.length - 1, i + 2)];

            result[i] =
                    clamp(
                            value,
                            -corridorBlocks,
                            corridorBlocks
                    );
        }

        result[0] = 0.0;
        result[result.length - 1] = 0.0;

        return result;
    }

    private static List<Vec2> displaceSamples(
            WorldBlueprint world,
            List<SamplePoint> samples,
            double[] offsets
    ) {
        double halfWorld =
                world.config().worldSizeBlocks() * 0.5;

        List<Vec2> result =
                new ArrayList<>(samples.size());

        for (int i = 0; i < samples.size(); i++) {
            SamplePoint sample =
                    samples.get(i);

            Vec2 tangent =
                    tangent(
                            samples,
                            i
                    );

            Vec2 normal =
                    new Vec2(
                            -tangent.z(),
                            tangent.x()
                    );

            Vec2 point =
                    new Vec2(
                            clamp(
                                    sample.point().x()
                                            + normal.x() * offsets[i],
                                    -halfWorld,
                                    halfWorld
                            ),
                            clamp(
                                    sample.point().z()
                                            + normal.z() * offsets[i],
                                    -halfWorld,
                                    halfWorld
                            )
                    );

            result.add(point);
        }

        return List.copyOf(result);
    }

    private static Vec2 tangent(
            List<SamplePoint> samples,
            int index
    ) {
        Vec2 before =
                samples.get(
                        Math.max(
                                0,
                                index - 1
                        )
                ).point();

        Vec2 after =
                samples.get(
                        Math.min(
                                samples.size() - 1,
                                index + 1
                        )
                ).point();

        double dx =
                after.x() - before.x();

        double dz =
                after.z() - before.z();

        double length =
                Math.hypot(
                        dx,
                        dz
                );

        if (length <= 1.0e-9) {
            return new Vec2(
                    1.0,
                    0.0
            );
        }

        return new Vec2(
                dx / length,
                dz / length
        );
    }

    private static void enforceExactEndpoints(
            List<RiverCenterlinePoint> points,
            List<Vec2> macroPoints,
            RiverSegmentProfile profile,
            RiverSegmentGradePlan gradePlan
    ) {
        if (points.isEmpty()) {
            return;
        }

        RiverCenterlinePoint first =
                points.getFirst();

        Vec2 macroStart =
                macroPoints.getFirst();

        points.set(
                0,
                new RiverCenterlinePoint(
                        macroStart.x(),
                        macroStart.z(),
                        0.0,
                        gradePlan.plannedStartBedElevation(),
                        profile.smoothedStartWidthBlocks(),
                        profile.smoothedStartDepthBlocks(),
                        0.0
                )
        );

        if (points.size() <= 1) {
            return;
        }

        RiverCenterlinePoint last =
                points.getLast();

        Vec2 macroEnd =
                macroPoints.getLast();

        points.set(
                points.size() - 1,
                new RiverCenterlinePoint(
                        macroEnd.x(),
                        macroEnd.z(),
                        last.distanceBlocks(),
                        gradePlan.plannedEndBedElevation(),
                        profile.smoothedEndWidthBlocks(),
                        profile.smoothedEndDepthBlocks(),
                        0.0
                )
        );
    }

    private static double interpolateGrade(
            RiverSegmentGradePlan plan,
            double t
    ) {
        int sampleCount =
                plan.sampleCount();

        if (sampleCount <= 1) {
            return plan.plannedStartBedElevation();
        }

        double position =
                clamp01(t)
                        * (sampleCount - 1);

        int lower =
                Math.min(
                        sampleCount - 1,
                        (int) Math.floor(position)
                );

        int upper =
                Math.min(
                        sampleCount - 1,
                        lower + 1
                );

        double localT =
                position - lower;

        return lerp(
                plan.plannedBedElevationAt(lower),
                plan.plannedBedElevationAt(upper),
                localT
        );
    }

    private static double[] cumulativeDistances(
            List<Vec2> points
    ) {
        double[] result =
                new double[points.size()];

        for (int i = 1; i < points.size(); i++) {
            result[i] =
                    result[i - 1]
                            + distance(
                            points.get(i - 1),
                            points.get(i)
                    );
        }

        return result;
    }

    private static double polylineLength(
            List<Vec2> points
    ) {
        if (points.size() <= 1) {
            return 0.0;
        }

        double[] distances =
                cumulativeDistances(points);

        return distances[distances.length - 1];
    }

    private static double distance(
            Vec2 a,
            Vec2 b
    ) {
        return Math.hypot(
                b.x() - a.x(),
                b.z() - a.z()
        );
    }

    private static double distanceSquared(
            Vec2 a,
            Vec2 b
    ) {
        double dx =
                b.x() - a.x();

        double dz =
                b.z() - a.z();

        return dx * dx + dz * dz;
    }

    private static Vec2 lerp(
            Vec2 a,
            Vec2 b,
            double t
    ) {
        return new Vec2(
                lerp(
                        a.x(),
                        b.x(),
                        t
                ),
                lerp(
                        a.z(),
                        b.z(),
                        t
                )
        );
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a + (b - a) * t;
    }

    private static double smoothstep01(
            double t
    ) {
        t = clamp01(t);
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

    private record Vec2(
            double x,
            double z
    ) {
    }

    private record SamplePoint(
            Vec2 point,
            double normalizedDistance
    ) {
    }
}
