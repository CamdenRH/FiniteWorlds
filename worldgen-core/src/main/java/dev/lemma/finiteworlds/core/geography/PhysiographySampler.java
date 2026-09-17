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
         * Do not suppress the western range merely because it reaches
         * the shoreline.  ContinentPlanner now decides whether this
         * seed gets a detached inland massif or a coastal-contact
         * massif.  The corridor itself is therefore the shoreline gate.
         */

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
