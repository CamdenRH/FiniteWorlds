package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

public final class PhysiographySampler {

    private final WorldBlueprint blueprint;
    private final ContinentPlan plan;

    private final ValueNoise boundaryNoise;
    private final ValueNoise coastRangeNoise;
    private final ValueNoise plateauNoise;

    public PhysiographySampler(
            long worldSeed,
            WorldBlueprint blueprint,
            ContinentPlan plan
    ) {
        this.blueprint = blueprint;
        this.plan = plan;

        this.boundaryNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "province-boundaries"
                        )
                );

        this.coastRangeNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "province-coast-range"
                        )
                );

        this.plateauNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                worldSeed,
                                "province-plateau"
                        )
                );
    }

    public PhysiographySample sample(
            int cellX,
            int cellZ,
            double nx,
            double nz
    ) {

        if (blueprint.landMask(cellX, cellZ) < 0.5f) {
            return PhysiographySample.ocean();
        }

        double worldSize =
                blueprint.config()
                        .worldSizeBlocks();

        double coastDistance =
                Math.max(
                        0.0,
                        blueprint.coastDistance(
                                cellX,
                                cellZ
                        )
                );

        MountainProjection cascadeProjection =
                plan.mountainSystem()
                        .projectToSpine(
                                nx,
                                nz
                        );

        double signedCascadeDistance =
                cascadeProjection.signedDistance();

        double cascadeProgress =
                cascadeProjection.progress();

        double westOfCascades =
                smoothstep(
                        -0.035,
                        0.045,
                        signedCascadeDistance
                );

        double eastOfCascades =
                1.0 - westOfCascades;

        double cascadeMask =
                plan.mountainSystem()
                        .corridorMaskFromDistance(
                                signedCascadeDistance
                        );

        double cascadeCoreMask =
                Math.pow(
                        cascadeMask,
                        2.35
                );

        double boundaryShift =
                boundaryNoise.fbm(
                        nx * 2.4,
                        nz * 2.4,
                        3,
                        2.0,
                        0.5
                ) * worldSize * 0.010;

        double effectiveCoastDistance =
                Math.max(
                        0.0,
                        coastDistance + boundaryShift
                );

        double coastalMask =
                1.0
                        - smoothstep(
                        worldSize * 0.008,
                        worldSize * 0.030,
                        effectiveCoastDistance
                );

        /*
         * =====================================================
         * LOCALIZED WESTERN / COAST RANGE
         * =====================================================
         *
         * IMPORTANT:
         * The old implementation built the Coast Range as a
         * coast-distance band.  That made it run along nearly
         * the entire western coastline no matter what the
         * continent planner did.
         *
         * The planner now owns the macro placement.  Its short
         * western mountain spine supplies the localization
         * envelope, while this sampler still supplies the
         * province-scale irregularity.
         */
        double westernRangeCorridor =
                plan.westernMountainSystem()
                        .corridorMask(
                                nx,
                                nz
                        );

        /*
         * Slightly soften/broaden the planner corridor without
         * changing where it exists.  Outside the planned range
         * this remains exactly zero.
         */
        double westernRangeEnvelope =
                Math.pow(
                        clamp01(
                                westernRangeCorridor
                        ),
                        0.72
                );

        /*
         * Coastal-contact western ranges are still allowed, but their broad
         * mountain body should not arrive at the waterline at full strength.
         * Without an apron the outer corridor can turn an entire stretch of
         * shoreline into one continuous wall.
         *
         * The first ~0.35% of world width stays strongly coastal, then the
         * mountain body eases in over roughly the next 1.65%.  At production
         * scale this leaves enough room for beaches, pocket coves, coastal
         * benches, and lower foothills before the main massif rises.
         */
        double coastalRangeApron =
                smoothstep(
                        worldSize * 0.0035,
                        worldSize * 0.0200,
                        coastDistance
                );

        /*
         * Do not turn every coastal-contact range into a detached inland
         * range.  A small number of narrow, high-confidence range cores may
         * still punch through the apron as rocky headlands / mountain spires.
         *
         * A higher-frequency deterministic field breaks those contacts into
         * short sections instead of recreating a continuous sea cliff.
         */
        double coastalHeadlandNoise =
                clamp01(
                        coastRangeNoise.fbm(
                                nx * 7.0 + 31.0,
                                nz * 7.0 - 17.0,
                                3,
                                2.0,
                                0.52
                        ) * 0.5 + 0.5
                );

        double coastalHeadlandCore =
                smoothstep(
                        0.78,
                        0.96,
                        westernRangeEnvelope
                )
                        * smoothstep(
                        0.60,
                        0.82,
                        coastalHeadlandNoise
                );

        double shorelineContact =
                1.0
                        - smoothstep(
                        worldSize * 0.0015,
                        worldSize * 0.0100,
                        coastDistance
                );

        /*
         * Most shoreline cells retain only a small fraction of western-range
         * influence.  Exceptional core/headland cells can retain up to ~45%,
         * enough for dramatic ocean-facing spires without letting the whole
         * massif form a sheer wall.
         */
        double shorelineRangeScale =
                Math.max(
                        0.10
                                + 0.90
                                * coastalRangeApron,
                        coastalHeadlandCore
                                * shorelineContact
                                * 0.45
                );

        double coastRangeVariation =
                0.90
                        + coastRangeNoise.fbm(
                        nx * 3.2,
                        nz * 3.2,
                        3,
                        2.0,
                        0.5
                ) * 0.10;

        double coastRangeMask =
                clamp01(
                        westernRangeEnvelope
                                * westOfCascades
                                * (1.0 - cascadeMask * 0.75)
                                * coastRangeVariation
                                * shorelineRangeScale
                );

        double cascadeFoothillMask =
                clamp01(
                        (cascadeMask - cascadeCoreMask * 0.72)
                                * westOfCascades
                                * 1.30
                );

        double easternSlopeMask =
                clamp01(
                        cascadeMask
                                * eastOfCascades
                                * (1.0 - cascadeCoreMask * 0.40)
                );

        double westernLowlandMask =
                clamp01(
                        westOfCascades
                                * (1.0 - coastalMask)
                                * (1.0 - coastRangeMask)
                                * (1.0 - cascadeMask)
                );

        double plateauVariation =
                0.92
                        + plateauNoise.fbm(
                        nx * 2.6,
                        nz * 2.6,
                        3,
                        2.0,
                        0.5
                ) * 0.08;

        double plateauMask =
                clamp01(
                        eastOfCascades
                                * (1.0 - coastalMask)
                                * (1.0 - cascadeMask * 0.90)
                                * plateauVariation
                );

        TerrainProvince province;

        if (cascadeCoreMask > 0.48) {
            province = TerrainProvince.CASCADE_CORE;
        } else if (easternSlopeMask > 0.34) {
            province = TerrainProvince.EASTERN_SLOPES;
        } else if (cascadeFoothillMask > 0.30) {
            province = TerrainProvince.CASCADE_FOOTHILLS;
        } else if (coastRangeMask > 0.34) {
            /*
             * Strong planned mountain corridor wins over the generic
             * coastal province, allowing a true mountains-to-ocean
             * contact where the planner deliberately created one.
             * Softer corridor fringes still resolve as COASTAL below.
             */
            province = TerrainProvince.COAST_RANGE;
        } else if (coastalMask > 0.52) {
            province = TerrainProvince.COASTAL;
        } else if (westOfCascades >= 0.50) {
            province = TerrainProvince.WESTERN_LOWLAND;
        } else {
            province = TerrainProvince.INTERIOR_PLATEAU;
        }

        return new PhysiographySample(
                province,
                coastalMask,
                coastRangeMask,
                westernLowlandMask,
                cascadeMask,
                cascadeCoreMask,
                cascadeFoothillMask,
                easternSlopeMask,
                plateauMask,
                signedCascadeDistance,
                cascadeProgress
        );
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double t =
                (value - edge0)
                        / (edge1 - edge0);

        t = clamp01(t);

        return t
                * t
                * (3.0 - 2.0 * t);
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
