package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.noise.ValueNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Cascade-specific macro morphology.
 *
 * MountainSystem owns where the Cascade corridor exists. This sampler owns
 * what the terrain looks like inside that corridor:
 *
 *  - a broad asymmetric mountain envelope,
 *  - 3-5 distinct high mountain massifs separated by lower passes,
 *  - a continuous secondary-mountain relief field across the range body,
 *  - an explicit hierarchy of non-volcanic alpine summits,
 *  - exactly one globally dominant landmark volcano,
 *  - a smaller hierarchy of major ridge systems,
 *  - secondary ridge branches that emerge from those major systems,
 *  - longer / broader western ridges and shorter / steeper eastern ridges.
 *
 * Fine volcanic morphology plus explicit glacial / drainage erosion are
 * intentionally left for later passes so the macro mountain body and landmark
 * silhouette can be evaluated first.
 */
public final class CascadeMorphologySampler {

    private final MountainSystem cascadeSystem;
    private final double spineLength;

    private final ValueNoise clusterNoise;
    private final ValueNoise crestNoise;
    private final ValueNoise ridgeTextureNoise;
    private final ValueNoise rangeReliefNoise;
    private final ValueNoise rangeWarpNoise;
    private final ValueNoise regionalHeightNoise;
    private final ValueNoise volcanoMorphologyNoise;
    private final ValueNoise passNoise;

    private final List<CascadeMassif> massifs;
    private final List<CascadePeak> peaks;
    private final CascadeVolcano landmarkVolcano;
    private final List<CascadeVolcanicRidge> volcanicRidges;
    private final List<RidgePath> ridgeSpurs;

    public CascadeMorphologySampler(
            long worldSeed,
            MountainSystem cascadeSystem
    ) {
        this(
                worldSeed,
                cascadeSystem,
                null
        );
    }

    public CascadeMorphologySampler(
            long worldSeed,
            MountainSystem cascadeSystem,
            WorldBlueprint blueprint
    ) {
        this.cascadeSystem = cascadeSystem;
        this.spineLength =
                cascadeSystem.spine()
                        .totalLength();

        long morphologySeed =
                SeedUtil.derive(
                        worldSeed,
                        "cascade-morphology"
                );

        this.clusterNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "clusters"
                        )
                );

        this.crestNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "crest"
                        )
                );

        this.ridgeTextureNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "ridge-texture"
                        )
                );

        this.rangeReliefNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "range-relief"
                        )
                );

        this.rangeWarpNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "range-warp"
                        )
                );

        this.regionalHeightNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "regional-height"
                        )
                );

        this.volcanoMorphologyNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "landmark-volcano-morphology"
                        )
                );

        this.passNoise =
                new ValueNoise(
                        SeedUtil.derive(
                                morphologySeed,
                                "passes"
                        )
                );

        this.massifs =
                createMassifs(
                        SeedUtil.derive(
                                morphologySeed,
                                "massifs"
                        )
                );

        this.peaks =
                CascadePeakPlanner.generate(
                        worldSeed,
                        spineLength,
                        massifs
                );

        this.landmarkVolcano =
                CascadeVolcanoPlanner.generate(
                        worldSeed,
                        cascadeSystem,
                        massifs,
                        peaks,
                        blueprint
                );

        this.volcanicRidges =
                CascadeVolcanicRidgePlanner.generate(
                        worldSeed,
                        landmarkVolcano
                );

        this.ridgeSpurs =
                createRidgeSpurs(
                        SeedUtil.derive(
                                morphologySeed,
                                "ridge-spurs"
                        )
                );
    }

    /**
     * Convenience path used by preview tooling.
     */
    public CascadeMorphologySample sample(
            double nx,
            double nz
    ) {
        MountainProjection projection =
                cascadeSystem.projectToSpine(
                        nx,
                        nz
                );

        double corridorMask =
                cascadeSystem.corridorMaskFromDistance(
                        projection.signedDistance()
                );

        return sample(
                nx,
                nz,
                projection.signedDistance(),
                projection.progress(),
                corridorMask
        );
    }

    /**
     * Fast path used by the generator. PhysiographySampler already knows
     * the closest spine projection, so the expensive polyline scan is not
     * repeated for every blueprint cell.
     */
    public CascadeMorphologySample sample(
            double nx,
            double nz,
            double signedCascadeDistance,
            double cascadeProgress,
            double cascadeMask
    ) {
        if (cascadeMask <= 0.0) {
            return CascadeMorphologySample.empty();
        }

        double absoluteDistance =
                Math.abs(
                        signedCascadeDistance
                );

        double arcPosition =
                clamp01(
                        cascadeProgress
                ) * spineLength;

        /*
         * =========================================================
         * BROAD ASYMMETRIC ENVELOPE
         * =========================================================
         *
         * Positive signed distance is west of the Cascade spine.
         * West side: broader and more gradual.
         * East side: narrower and steeper.
         */
        double envelope;

        if (signedCascadeDistance >= 0.0) {
            envelope =
                    1.0
                            - smoothstep(
                            0.035,
                            0.180,
                            absoluteDistance
                    );
        } else {
            envelope =
                    1.0
                            - smoothstep(
                            0.032,
                            0.150,
                            absoluteDistance
                    );
        }

        envelope =
                clamp01(
                        envelope
                );

        /*
         * =========================================================
         * DISTINCT CREST MASSIFS + SADDLES
         * =========================================================
         *
         * Instead of relying on one continuous low-frequency noise ribbon,
         * the range now owns 3-5 explicit seeded high-mountain groups.
         * The field falls nearly to zero between those groups, creating
         * recognizable lower passes / saddles.
         */
        double clusterStrength =
                massifStrengthAt(
                        arcPosition
                );

        double clusterTextureSignal =
                clusterNoise.fbm(
                        arcPosition * 6.2 + 17.0,
                        11.0,
                        2,
                        2.0,
                        0.5
                );

        double clusterTexture =
                0.92
                        + 0.08
                        * clamp01(
                        clusterTextureSignal * 0.5 + 0.5
                );

        clusterStrength =
                clamp01(
                        clusterStrength
                                * clusterTexture
                );

        double crestEnvelope =
                1.0
                        - smoothstep(
                        0.018,
                        0.074,
                        absoluteDistance
                );

        double crestDetailSignal =
                crestNoise.fbm(
                        nx * 8.0,
                        nz * 11.0,
                        4,
                        2.0,
                        0.5
                );

        double crestDetail =
                0.82
                        + 0.18
                        * clamp01(
                        crestDetailSignal * 0.5 + 0.5
                );

        /*
         * The explicit massif field is the primary source of saddles.
         * This smaller noise pass adds an occasional local notch inside a
         * massif without turning the crest back into a continuous ribbon.
         */
        double passSignal =
                passNoise.fbm(
                        arcPosition * 8.0 + 43.0,
                        -7.0,
                        2,
                        2.0,
                        0.5
                );

        double normalizedPassSignal =
                clamp01(
                        passSignal * 0.5 + 0.5
                );

        double localPass =
                smoothstep(
                        0.68,
                        0.88,
                        normalizedPassSignal
                )
                        * clusterStrength;

        double saddleSuppression =
                Math.pow(
                        1.0 - clusterStrength,
                        1.15
                );

        double passSuppression =
                clamp01(
                        saddleSuppression * 0.82
                                + localPass * 0.30
                );

        double passMultiplier =
                1.0
                        - passSuppression
                        * 0.62;

        double crestClusterFactor =
                Math.pow(
                        clusterStrength,
                        0.82
                );

        double crestStructure =
                clamp01(
                        crestEnvelope
                                * crestClusterFactor
                                * crestDetail
                                * passMultiplier
                );

        /*
         * =========================================================
         * HIERARCHICAL SECONDARY RIDGE SYSTEMS
         * =========================================================
         *
         * Major ridges are concentrated around the seeded massifs. Each
         * major ridge may spawn one or two weaker branches partway down its
         * flank. Ridge paths meander as they move away from the crest and
         * narrow toward their tips rather than broadening into uniform ribs.
         */
        double ridgeRelief =
                ridgeSpurField(
                        signedCascadeDistance,
                        arcPosition
                );

        double ridgeTexture =
                ridgeTextureNoise.fbm(
                        nx * 8.0,
                        nz * 8.0,
                        3,
                        2.0,
                        0.5
                );

        ridgeTexture =
                0.82
                        + 0.18
                        * clamp01(
                        ridgeTexture * 0.5 + 0.5
                );

        /*
         * Strong massifs support denser / stronger ridge systems. Saddles
         * still retain a little relief so the range does not look cut into
         * totally independent islands of terrain.
         */
        double ridgeClusterMultiplier =
                0.58
                        + 0.42
                        * Math.pow(
                        clusterStrength,
                        0.75
                );

        ridgeRelief =
                clamp01(
                        ridgeRelief
                                * envelope
                                * ridgeTexture
                                * ridgeClusterMultiplier
                );

        /*
         * =========================================================
         * REGIONAL ELEVATION ZONES
         * =========================================================
         *
         * The Cascade envelope should not behave like one uniformly raised
         * slab.  This very-low-frequency field establishes long high and low
         * sections of the range before individual secondary mountains and
         * summits are added.  It is intentionally independent of massif
         * strength: massifs are exceptional high-mountain concentrations,
         * while regional height controls the baseline the whole range sits on.
         */
        double regionalHeight =
                regionalHeightField(
                        arcPosition,
                        signedCascadeDistance,
                        localPass
                );

        /*
         * =========================================================
         * CONTINUOUS SECONDARY MOUNTAIN RELIEF
         * =========================================================
         *
         * The explicit massifs identify exceptional high-mountain groups;
         * they should not be the only mountainous terrain in the Cascade
         * envelope.  This lower-amplitude field fills the broader range body
         * with broken secondary ridges and mountain masses.  It is largely
         * independent of massif strength, so the terrain remains mountainous
         * between high-mountain concentrations, while local pass notches can
         * still lower short sections of the chain.
         */
        double rangeRelief =
                rangeReliefField(
                        nx,
                        nz,
                        signedCascadeDistance,
                        arcPosition,
                        envelope,
                        crestStructure,
                        localPass,
                        regionalHeight
                );

        /*
         * =========================================================
         * EXPLICIT SUMMIT FIELD
         * =========================================================
         *
         * The massif determines where a high-mountain group exists. Peaks
         * determine where the individual summits inside that group exist.
         * This field is intentionally independent of the ridge network so
         * later passes can attach ridges, glaciers, and drainage to known
         * summit locations instead of hoping noise creates suitable maxima.
         */
        double peakRelief =
                peakField(
                        signedCascadeDistance,
                        arcPosition,
                        localPass
                );

        peakRelief =
                clamp01(
                        peakRelief
                                * envelope
                );

        /*
         * =========================================================
         * LANDMARK VOLCANO
         * =========================================================
         *
         * The volcano owns a separate relief field and vertical budget.  It
         * is not another ordinary CascadePeak: every world receives exactly
         * one, and its summit is allowed to rise far above maximumUplift.
         */
        VolcanoMorphology volcanoMorphology =
                volcanoMorphology(
                        signedCascadeDistance,
                        arcPosition
                );

        double volcanoFoothillRelief =
                volcanoMorphology.foothillRelief();

        double volcanoRelief =
                volcanoMorphology.relief();

        double volcanoStructuralRidgeRelief =
                volcanoStructuralRidgeField(
                        signedCascadeDistance,
                        arcPosition
                );

        double volcanoUplift =
                cascadeSystem.maximumUplift()
                        * landmarkVolcano.heightMultiplier()
                        * volcanoRelief;

        /*
         * =========================================================
         * FINAL UPLIFT
         * =========================================================
         *
         * Preserve the same overall vertical budget. The continuous broad
         * body prevents the Cascades from breaking into disconnected hills;
         * the explicit massifs now control most of the alpine crest height.
         */
        double maximumUplift =
                cascadeSystem.maximumUplift();

        /*
         * Regional baseline elevation now swings aggressively instead of
         * assigning the whole range one fixed pedestal.  Low zones remain
         * mountainous but can sit dozens of terrain units beneath neighboring
         * high zones before any massif / summit relief is considered.
         */
        double regionalBaseFraction =
                lerp(
                        0.085,
                        0.285,
                        Math.pow(
                                regionalHeight,
                                1.08
                        )
                );

        double broadUplift =
                maximumUplift
                        * regionalBaseFraction
                        * envelope
                        * (
                        1.0
                                - localPass * 0.28
                );

        /*
         * Treat the secondary range field as signed relief around the
         * regional baseline.  Dark portions therefore carve intermountain
         * lows while bright portions create substantial secondary mountain
         * blocks.  This is still macro physiography, not erosion.
         */
        double rangeDeviation =
                (
                        rangeRelief
                                - 0.43
                )
                        * maximumUplift
                        * 0.30
                        * envelope
                        * (
                        0.80
                                + regionalHeight * 0.38
                );

        double crestUplift =
                maximumUplift
                        * 0.36
                        * crestStructure;

        double ridgeUplift =
                maximumUplift
                        * 0.09
                        * ridgeRelief;

        double peakUplift =
                maximumUplift
                        * 0.20
                        * peakRelief;

        double volcanoFoothillUplift =
                maximumUplift
                        * 0.24
                        * volcanoFoothillRelief;

        double volcanoStructuralRidgeUplift =
                maximumUplift
                        * 0.22
                        * volcanoStructuralRidgeRelief;

        double ordinaryUplift =
                clamp(
                        broadUplift
                                + rangeDeviation
                                + crestUplift
                                + ridgeUplift
                                + peakUplift
                                + volcanoFoothillUplift,
                        0.0,
                        maximumUplift
                );

        /*
         * The upper cone should visually bury a little of the pre-existing
         * ridge / peak noise beneath it.  The lower apron leaves the ordinary
         * terrain almost untouched so the volcano still grows naturally out
         * of the surrounding Cascades.
         */
        double volcanoDominance =
                volcanoMorphology.dominance();

        ordinaryUplift *=
                1.0
                        - volcanoDominance * 0.30;

        double uplift =
                ordinaryUplift
                        + volcanoStructuralRidgeUplift
                        + volcanoUplift;

        return new CascadeMorphologySample(
                envelope,
                clamp01(
                        regionalHeight
                                * envelope
                ),
                rangeRelief,
                crestStructure,
                ridgeRelief,
                peakRelief,
                volcanoFoothillRelief,
                volcanoStructuralRidgeRelief,
                volcanoRelief,
                volcanoMorphology.upperCone(),
                volcanoMorphology.radialStructure(),
                volcanoMorphology.craterMask(),
                volcanoUplift,
                passSuppression,
                uplift
        );
    }

    /*
     * =============================================================
     * MASSIF FIELD
     * =============================================================
     */

    private double massifStrengthAt(
            double arcPosition
    ) {
        double result =
                0.0;

        for (CascadeMassif massif : massifs) {
            double normalizedDistance =
                    Math.abs(
                            arcPosition
                                    - massif.centerArc()
                    ) / massif.halfWidth();

            if (normalizedDistance >= 1.0) {
                continue;
            }

            /*
             * Compact cosine lobe. It reaches exactly zero at the edge of
             * the massif instead of leaving a low continuous pedestal along
             * the entire crest.
             */
            double influence =
                    0.5
                            + 0.5
                            * Math.cos(
                            normalizedDistance
                                    * Math.PI
                    );

            influence =
                    Math.pow(
                            influence,
                            0.82
                    );

            result =
                    Math.max(
                            result,
                            influence
                                    * massif.strength()
                    );
        }

        return clamp01(
                result
        );
    }

    private List<CascadeMassif> createMassifs(
            long seed
    ) {
        SplittableRandom random =
                new SplittableRandom(
                        seed
                );

        int massifCount =
                3 + random.nextInt(3);

        int dominantIndex =
                random.nextInt(
                        massifCount
                );

        List<CascadeMassif> result =
                new ArrayList<>();

        for (int i = 0; i < massifCount; i++) {
            double baseProgress =
                    (i + 0.5)
                            / massifCount;

            double centerProgress =
                    clamp(
                            baseProgress
                                    + range(
                                    random,
                                    -0.040,
                                    0.040
                            ),
                            0.07,
                            0.93
                    );

            /*
             * Slightly narrower individual groups when there are more of
             * them, preserving visible saddles for both 3- and 5-massif
             * seeds.
             */
            double halfWidthProgress =
                    0.045
                            + 0.12
                            / massifCount
                            + range(
                            random,
                            -0.010,
                            0.012
                    );

            halfWidthProgress =
                    clamp(
                            halfWidthProgress,
                            0.060,
                            0.102
                    );

            double strength =
                    range(
                            random,
                            0.78,
                            0.98
                    );

            if (i == dominantIndex) {
                strength =
                        Math.min(
                                1.0,
                                strength + 0.10
                        );
            }

            result.add(
                    new CascadeMassif(
                            centerProgress
                                    * spineLength,
                            halfWidthProgress
                                    * spineLength,
                            strength
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    /*
     * =============================================================
     * CONTINUOUS RANGE-BODY RELIEF
     * =============================================================
     */

    private double regionalHeightField(
            double arcPosition,
            double signedDistance,
            double localPassSuppression
    ) {
        /*
         * The first signal changes only a few times along the entire range.
         * The second introduces broad shoulders / sub-regions so elevation
         * zones do not become simple horizontal bands.
         */
        double longSignal =
                regionalHeightNoise.fbm(
                        arcPosition * 2.35 + 71.0,
                        5.0,
                        3,
                        2.0,
                        0.50
                );

        double shoulderSignal =
                regionalHeightNoise.fbm(
                        arcPosition * 5.4 - 23.0,
                        signedDistance * 5.5 + 17.0,
                        3,
                        2.0,
                        0.52
                );

        double combined =
                longSignal * 0.68
                        + shoulderSignal * 0.32;

        double normalized =
                clamp01(
                        combined * 0.5 + 0.5
                );

        /*
         * Stretch the middle of the distribution so adjacent regional zones
         * separate visibly instead of clustering around one average height.
         */
        double zoned =
                smoothstep(
                        0.18,
                        0.82,
                        normalized
                );

        zoned =
                Math.pow(
                        zoned,
                        0.88
                );

        /*
         * Local passes lower the baseline but never erase the surrounding
         * mountain system.  The broad between-massif saddle term is not used
         * here; otherwise we'd recreate the isolated-island problem.
         */
        return clamp01(
                0.10
                        + zoned * 0.90
                        - localPassSuppression * 0.22
        );
    }

    private double rangeReliefField(
            double nx,
            double nz,
            double signedDistance,
            double arcPosition,
            double envelope,
            double crestStructure,
            double localPassSuppression,
            double regionalHeight
    ) {
        if (envelope <= 0.0) {
            return 0.0;
        }

        double warp =
                rangeWarpNoise.fbm(
                        arcPosition * 5.2 + 31.0,
                        signedDistance * 17.0 - 9.0,
                        3,
                        2.0,
                        0.5
                ) * 0.020;

        /*
         * Anisotropic sampling makes the field read as mountain groups and
         * short broken ridges rather than isotropic blobs.  The cross-range
         * frequency is intentionally higher than the along-range frequency.
         */
        double broadSignal =
                rangeReliefNoise.fbm(
                        (arcPosition + warp) * 12.0 + 7.0,
                        signedDistance * 31.0 + 19.0,
                        4,
                        2.0,
                        0.52
                );

        double detailSignal =
                rangeReliefNoise.fbm(
                        nx * 18.0 - 41.0,
                        nz * 18.0 + 23.0,
                        3,
                        2.0,
                        0.48
                );

        double normalizedBroad =
                clamp01(
                        broadSignal * 0.5 + 0.5
                );

        double ridged =
                1.0
                        - Math.abs(
                        broadSignal
                );

        ridged =
                Math.pow(
                        clamp01(ridged),
                        1.42
                );

        double normalizedDetail =
                clamp01(
                        detailSignal * 0.5 + 0.5
                );

        /*
         * Pass 4 kept too much of this field near the middle of 0..1.
         * Deliberately stretch it so the same range body contains secondary
         * mountain blocks, ordinary uplands, and broad intermountain lows.
         */
        double rawMountainField =
                normalizedBroad * 0.43
                        + ridged * 0.39
                        + normalizedDetail * 0.18;

        double brokenMountainField =
                smoothstep(
                        0.22,
                        0.78,
                        rawMountainField
                );

        brokenMountainField =
                Math.pow(
                        clamp01(brokenMountainField),
                        1.08
                );

        /*
         * Favor the upper flanks, but let the field operate almost everywhere
         * inside the range.  Higher regional zones are more rugged while low
         * zones retain deep contrast rather than simply becoming flat.
         */
        double absoluteDistance =
                Math.abs(signedDistance);

        double flankBias =
                0.84
                        + 0.22
                        * smoothstep(
                        0.012,
                        0.110,
                        absoluteDistance
                );

        double regionalRuggedness =
                0.78
                        + regionalHeight * 0.34;

        double crestReservation =
                1.0
                        - crestStructure * 0.18;

        double passMultiplier =
                1.0
                        - localPassSuppression * 0.52;

        return clamp01(
                brokenMountainField
                        * envelope
                        * flankBias
                        * regionalRuggedness
                        * crestReservation
                        * passMultiplier
        );
    }

    /*
     * =============================================================
     * PEAK FIELD
     * =============================================================
     */

    private double peakField(
            double signedDistance,
            double arcPosition,
            double localPassSuppression
    ) {
        double maximumContribution =
                0.0;

        double pedestalContribution =
                0.0;

        for (CascadePeak peak : peaks) {
            double arcDistance =
                    arcPosition
                            - peak.arcPosition();

            if (
                    Math.abs(arcDistance)
                            > peak.radiusAlong()
            ) {
                continue;
            }

            double crossDistance =
                    signedDistance
                            - peak.signedDistance();

            if (
                    Math.abs(crossDistance)
                            > peak.radiusAcross()
            ) {
                continue;
            }

            double normalizedAcross =
                    crossDistance
                            / peak.radiusAcross();

            double normalizedAlong =
                    arcDistance
                            / peak.radiusAlong();

            double normalizedDistance =
                    Math.sqrt(
                            normalizedAcross * normalizedAcross
                                    + normalizedAlong * normalizedAlong
                    );

            if (normalizedDistance >= 1.0) {
                continue;
            }

            double cone =
                    1.0
                            - smoothstep(
                            0.0,
                            1.0,
                            normalizedDistance
                    );

            double classMultiplier =
                    peak.peakClass() == CascadePeakClass.BACKGROUND_ALPINE
                            ? 1.0
                            - localPassSuppression * 0.82
                            : 1.0;

            double summit =
                    Math.pow(
                            cone,
                            peak.sharpness()
                    )
                            * peak.strength()
                            * classMultiplier;

            maximumContribution =
                    Math.max(
                            maximumContribution,
                            summit
                    );

            double pedestalWeight =
                    switch (peak.peakClass()) {
                        case BACKGROUND_ALPINE -> 0.065;
                        case ALPINE -> 0.11;
                        case MAJOR_ALPINE -> 0.17;
                        case REGIONAL_SUMMIT -> 0.24;
                    };

            double pedestal =
                    Math.pow(
                            cone,
                            0.62
                    )
                            * peak.strength()
                            * pedestalWeight
                            * classMultiplier;

            pedestalContribution +=
                    pedestal;
        }

        return clamp01(
                maximumContribution
                        + pedestalContribution
        );
    }

    /*
     * =============================================================
     * LANDMARK VOLCANO FIELD
     * =============================================================
     */

    private VolcanoMorphology volcanoMorphology(
            double signedDistance,
            double arcPosition
    ) {
        double normalizedAcross =
                (signedDistance
                        - landmarkVolcano.signedDistance())
                        / landmarkVolcano.radiusAcross();

        double normalizedAlong =
                (arcPosition
                        - landmarkVolcano.arcPosition())
                        / landmarkVolcano.radiusAlong();

        double rotation =
                landmarkVolcano.rotationRadians();

        double cosine =
                Math.cos(rotation);

        double sine =
                Math.sin(rotation);

        double localX =
                normalizedAcross * cosine
                        + normalizedAlong * sine;

        double localY =
                -normalizedAcross * sine
                        + normalizedAlong * cosine;

        double angle =
                Math.atan2(
                        localY,
                        localX
                );

        /*
         * Warp the footprint directionally instead of scaling both axes by
         * one constant. This creates a broad favored flank and a steeper
         * opposing flank while keeping the world-scale footprint intact.
         */
        double directionalStretch =
                1.0
                        + landmarkVolcano.asymmetry()
                        * Math.cos(
                        angle
                                - landmarkVolcano.asymmetryPhase()
                )
                        + landmarkVolcano.asymmetry()
                        * 0.28
                        * Math.cos(
                        angle * 2.0
                                + landmarkVolcano.asymmetryPhase() * 0.65
                );

        directionalStretch =
                clamp(
                        directionalStretch,
                        0.78,
                        1.24
                );

        double baseRadius =
                Math.hypot(
                        localX,
                        localY
                ) / directionalStretch;

        /*
         * T2 adds a separate highland / foothill transition outside the
         * formal volcanic cone.  This is intentionally ordinary-Cascade
         * scale uplift, not landmark-height uplift: the goal is to embed the
         * volcano in a mountain complex rather than enlarge the cone itself.
         */
        if (baseRadius >= 1.62) {
            return VolcanoMorphology.empty();
        }

        double foothillWindow =
                smoothstep(
                        0.52,
                        0.84,
                        baseRadius
                )
                        * (
                        1.0
                                - smoothstep(
                                1.08,
                                1.62,
                                baseRadius
                        )
                );

        double foothillNoise =
                clamp01(
                        volcanoMorphologyNoise.fbm(
                                localX * 2.10 + 71.0,
                                localY * 2.10 - 53.0,
                                3,
                                2.0,
                                0.52
                        ) * 0.5 + 0.5
                );

        double foothillLobes =
                clamp(
                        0.62
                                + Math.cos(
                                angle * 3.0
                                        + landmarkVolcano.asymmetryPhase() * 0.73
                        ) * 0.24
                                + Math.cos(
                                angle * 5.0
                                        - landmarkVolcano.buttressPhase() * 0.41
                        ) * 0.18,
                        0.18,
                        1.10
                );

        double foothillRelief =
                clamp01(
                        foothillWindow
                                * (0.48 + foothillNoise * 0.52)
                                * foothillLobes
                );

        if (baseRadius >= 1.0) {
            return new VolcanoMorphology(
                    foothillRelief,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        double remaining =
                1.0 - baseRadius;

        /*
         * T2 makes the edifice more explicitly stratovolcanic.  The lower
         * apron still occupies the same footprint, but its higher exponent
         * makes the outer flanks gentler while concentrating more slope in
         * the middle / upper mountain.
         */
        double lowerApron =
                Math.pow(
                        remaining,
                        1.42
                );

        double offsetAcross =
                landmarkVolcano.summitOffsetAcross()
                        / landmarkVolcano.radiusAcross();

        double offsetAlong =
                landmarkVolcano.summitOffsetAlong()
                        / landmarkVolcano.radiusAlong();

        double offsetX =
                offsetAcross * cosine
                        + offsetAlong * sine;

        double offsetY =
                -offsetAcross * sine
                        + offsetAlong * cosine;

        /*
         * A narrower, steeper upper cone now rises distinctly above the
         * broad apron.  Summit height remains unchanged because the combined
         * profile still reaches the same normalized ceiling.
         */
        double upperX =
                (localX - offsetX)
                        / 0.44;

        double upperY =
                (localY - offsetY)
                        / 0.40;

        double upperRadius =
                Math.hypot(
                        upperX,
                        upperY
                );

        double upperCone =
                upperRadius < 1.0
                        ? Math.pow(
                        1.0 - upperRadius,
                        1.95
                )
                        : 0.0;

        double relief =
                lowerApron * 0.60
                        + upperCone * 0.43;

        /*
         * Alternating buttresses and shallow radial swales establish the
         * drainage predisposition of a mature stratovolcano without actually
         * carving rivers. Hydrology can later preferentially occupy the
         * negative portions of this pattern.
         */
        double angularWarp =
                volcanoMorphologyNoise.fbm(
                        localX * 2.35 + 13.0,
                        localY * 2.35 - 31.0,
                        2,
                        2.0,
                        0.52
                ) * 0.11;

        double twistedAngle =
                angle
                        + angularWarp
                        + Math.sin(
                        baseRadius * Math.PI * 1.7
                                + landmarkVolcano.buttressPhase()
                ) * 0.045
                        + baseRadius
                        * 0.055
                        * Math.sin(
                        landmarkVolcano.asymmetryPhase()
                );

        double radialPrimary =
                Math.cos(
                        twistedAngle
                                * landmarkVolcano.buttressCount()
                                + landmarkVolcano.buttressPhase()
                                + baseRadius * 0.55
                );

        double radialSecondary =
                Math.cos(
                        twistedAngle
                                * (landmarkVolcano.buttressCount() + 3)
                                - landmarkVolcano.buttressPhase() * 0.72
                                - baseRadius * 0.90
                );

        double radialTertiary =
                Math.cos(
                        twistedAngle
                                * Math.max(
                                4,
                                landmarkVolcano.buttressCount() - 3
                        )
                                + landmarkVolcano.buttressPhase() * 1.31
                                + baseRadius * 1.15
                );

        double radialSignal =
                radialPrimary * 0.50
                        + radialSecondary * 0.30
                        + radialTertiary * 0.20;

        double radialWindow =
                smoothstep(
                        0.14,
                        0.31,
                        baseRadius
                )
                        * (
                        1.0
                                - smoothstep(
                                0.78,
                                0.98,
                                baseRadius
                        )
                );

        double radialDisplacement =
                radialSignal
                        * radialWindow
                        * landmarkVolcano.buttressStrength()
                        * (0.52 + remaining * 0.48);

        relief +=
                radialDisplacement * 0.55;

        /*
         * Low-amplitude coherent flank roughness breaks the perfect analytic
         * surface without replacing geomorphology with raw noise. It is kept
         * away from both the summit and the outermost apron.
         */
        double roughnessSignal =
                volcanoMorphologyNoise.fbm(
                        localX * 4.8 + 37.0,
                        localY * 4.8 - 19.0,
                        3,
                        2.0,
                        0.50
                );

        double roughnessWindow =
                smoothstep(
                        0.18,
                        0.36,
                        baseRadius
                )
                        * (
                        1.0
                                - smoothstep(
                                0.76,
                                0.97,
                                baseRadius
                        )
                );

        relief +=
                roughnessSignal
                        * landmarkVolcano.flankRoughness()
                        * roughnessWindow;

        /*
         * Summit crater. The crater is intentionally small compared with the
         * edifice and leaves a high rim, so the volcano remains the world's
         * dominant summit while gaining a recognizable volcanic cap.
         */
        double craterRadius =
                landmarkVolcano.craterRadiusFraction();

        double craterX =
                (localX - offsetX)
                        / craterRadius;

        double craterY =
                (localY - offsetY)
                        / (craterRadius * 0.86);

        double craterDistance =
                Math.hypot(
                        craterX,
                        craterY
                );

        double craterMask =
                1.0
                        - smoothstep(
                        0.0,
                        0.78,
                        craterDistance
                );

        double craterRim =
                smoothstep(
                        0.48,
                        0.82,
                        craterDistance
                )
                        * (
                        1.0
                                - smoothstep(
                                0.82,
                                1.22,
                                craterDistance
                        )
                );

        relief +=
                craterRim * 0.030;

        relief -=
                craterMask
                        * landmarkVolcano.craterDepthFraction();

        /*
         * Dominance intentionally ignores the crater and radial swales. This
         * prevents ordinary Cascade peak/ridge noise from poking through the
         * volcanic edifice wherever the Pass-2 surface has a depression.
         */
        double dominance =
                smoothstep(
                        0.28,
                        0.78,
                        lowerApron * 0.78
                                + upperCone * 0.22
                );

        double radialStructure =
                clamp01(
                        (radialSignal * 0.5 + 0.5)
                                * radialWindow
                );

        return new VolcanoMorphology(
                foothillRelief,
                clamp01(relief),
                clamp01(upperCone),
                radialStructure,
                clamp01(craterMask),
                clamp01(dominance)
        );
    }

    public CascadeVolcano landmarkVolcano() {
        return landmarkVolcano;
    }

    /*
     * =============================================================
     * VOLCANIC STRUCTURAL RIDGE FIELD
     * =============================================================
     *
     * These paths are the handful of large buttresses that should remain
     * visible even before hydrology begins carving the volcano. They start on
     * the middle / upper edifice, cross the volcanic apron, and several extend
     * into the surrounding Cascade highlands.
     */

    private double volcanoStructuralRidgeField(
            double signedDistance,
            double arcPosition
    ) {
        double result =
                0.0;

        for (CascadeVolcanicRidge ridge : volcanicRidges) {
            double padding =
                    ridge.maximumWidth()
                            * 1.15;

            if (
                    signedDistance < ridge.minimumSignedDistance() - padding
                            || signedDistance > ridge.maximumSignedDistance() + padding
                            || arcPosition < ridge.minimumArc() - padding
                            || arcPosition > ridge.maximumArc() + padding
            ) {
                continue;
            }

            PathDistance nearest =
                    closestPointOnVolcanicRidge(
                            ridge,
                            signedDistance,
                            arcPosition
                    );

            if (nearest == null) {
                continue;
            }

            double pathT =
                    nearest.progress();

            double width =
                    lerp(
                            ridge.startWidth(),
                            ridge.endWidth(),
                            smoothstep(0.0, 1.0, pathT)
                    );

            /*
             * Macro buttresses should have a broad shoulder rather than the
             * narrow capsule cross-section of ordinary Cascade ridge spurs.
             */
            double distance =
                    Math.sqrt(
                            nearest.distanceSquared()
                    );

            double crossSection =
                    1.0
                            - smoothstep(
                            width * 0.18,
                            width,
                            distance
                    );

            double rootFade =
                    smoothstep(
                            0.0,
                            ridge.secondary()
                                    ? 0.12
                                    : 0.075,
                            pathT
                    );

            double tipFade =
                    1.0
                            - smoothstep(
                            ridge.secondary()
                                    ? 0.72
                                    : 0.80,
                            1.0,
                            pathT
                    );

            double longitudinalStrength =
                    0.72
                            + (1.0 - pathT) * 0.28;

            double contribution =
                    ridge.strength()
                            * crossSection
                            * rootFade
                            * tipFade
                            * longitudinalStrength;

            result =
                    Math.max(
                            result,
                            contribution
                    );
        }

        return clamp01(result);
    }

    private static PathDistance closestPointOnVolcanicRidge(
            CascadeVolcanicRidge ridge,
            double signedDistance,
            double arcPosition
    ) {
        List<CascadeVolcanicRidge.Node> nodes =
                ridge.nodes();

        if (nodes.size() < 2) {
            return null;
        }

        double bestDistanceSquared =
                Double.POSITIVE_INFINITY;

        double bestProgress =
                0.0;

        for (int i = 0; i < nodes.size() - 1; i++) {
            CascadeVolcanicRidge.Node a =
                    nodes.get(i);

            CascadeVolcanicRidge.Node b =
                    nodes.get(i + 1);

            double dx =
                    b.signedDistance()
                            - a.signedDistance();

            double dy =
                    b.arcPosition()
                            - a.arcPosition();

            double lengthSquared =
                    dx * dx
                            + dy * dy;

            if (lengthSquared <= 1.0e-12) {
                continue;
            }

            double segmentT =
                    (
                            (signedDistance - a.signedDistance()) * dx
                                    + (arcPosition - a.arcPosition()) * dy
                    ) / lengthSquared;

            segmentT =
                    clamp(
                            segmentT,
                            0.0,
                            1.0
                    );

            double nearestSigned =
                    a.signedDistance()
                            + dx * segmentT;

            double nearestArc =
                    a.arcPosition()
                            + dy * segmentT;

            double offsetSigned =
                    signedDistance
                            - nearestSigned;

            double offsetArc =
                    arcPosition
                            - nearestArc;

            double distanceSquared =
                    offsetSigned * offsetSigned
                            + offsetArc * offsetArc;

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared =
                        distanceSquared;

                bestProgress =
                        lerp(
                                a.progress(),
                                b.progress(),
                                segmentT
                        );
            }
        }

        if (!Double.isFinite(bestDistanceSquared)) {
            return null;
        }

        return new PathDistance(
                bestDistanceSquared,
                bestProgress
        );
    }

    /*
     * =============================================================
     * RIDGE FIELD
     * =============================================================
     *
     * Ridges are explicit 2-D paths in Cascade-local coordinates:
     *
     *   outward = perpendicular distance away from the Cascade spine
     *   arc     = distance along the Cascade spine
     *
     * This is intentionally different from the previous formulation, where
     * outward distance was the independent variable and arc position was only
     * a small drift / curvature offset. That older model naturally produced
     * mostly east-west ribs. The path model below allows a ridge to spend a
     * substantial portion of its run moving north/south before curving back
     * outward, while still guaranteeing that it descends away from the crest.
     */

    private double ridgeSpurField(
            double signedDistance,
            double arcPosition
    ) {
        double result =
                0.0;

        for (RidgePath ridge : ridgeSpurs) {
            double outwardDistance =
                    signedDistance
                            * ridge.side();

            double padding =
                    ridge.maximumWidth()
                            * 1.10;

            /*
             * Cheap bounding-box rejection is important here. Preview maps
             * may sample this method millions of times, while each ridge is a
             * short polyline with several segments.
             */
            if (
                    outwardDistance < ridge.minimumOutward() - padding
                            || outwardDistance > ridge.maximumOutward() + padding
                            || arcPosition < ridge.minimumArc() - padding
                            || arcPosition > ridge.maximumArc() + padding
            ) {
                continue;
            }

            PathDistance nearest =
                    closestPointOnPath(
                            ridge,
                            outwardDistance,
                            arcPosition
                    );

            if (nearest == null) {
                continue;
            }

            double pathT =
                    nearest.progress();

            double widthT =
                    smoothstep(
                            0.0,
                            1.0,
                            pathT
                    );

            double width =
                    lerp(
                            ridge.startWidth(),
                            ridge.endWidth(),
                            widthT
                    );

            /*
             * A subtle width pulse prevents perfectly uniform capsules while
             * remaining much smoother than high-frequency coastline noise.
             */
            double widthVariation =
                    1.0
                            + Math.sin(
                            pathT
                                    * Math.PI
                                    * ridge.widthWaveCount()
                                    + ridge.widthPhase()
                    ) * 0.10
                            * Math.sin(
                            pathT * Math.PI
                    );

            width =
                    Math.max(
                            0.0032,
                            width * widthVariation
                    );

            double distance =
                    Math.sqrt(
                            nearest.distanceSquared()
                    );

            double crossSection =
                    1.0
                            - smoothstep(
                            width * 0.22,
                            width,
                            distance
                    );

            /*
             * Major ridges blend quickly into the crest. Secondary branches
             * receive a slightly longer root fade so they read as daughters
             * of the parent ridge rather than independent parallel ribs.
             */
            double rootFade =
                    smoothstep(
                            0.0,
                            ridge.secondary()
                                    ? 0.060
                                    : 0.028,
                            pathT
                    );

            double tipFade =
                    1.0
                            - smoothstep(
                            ridge.secondary()
                                    ? 0.68
                                    : 0.73,
                            1.0,
                            pathT
                    );

            double contribution =
                    ridge.strength()
                            * crossSection
                            * rootFade
                            * tipFade;

            result =
                    Math.max(
                            result,
                            contribution
                    );
        }

        return clamp01(
                result
        );
    }

    private static PathDistance closestPointOnPath(
            RidgePath ridge,
            double outward,
            double arc
    ) {
        List<RidgeNode> nodes =
                ridge.nodes();

        if (nodes.size() < 2) {
            return null;
        }

        double bestDistanceSquared =
                Double.POSITIVE_INFINITY;

        double bestProgress =
                0.0;

        for (int i = 0; i < nodes.size() - 1; i++) {
            RidgeNode a =
                    nodes.get(i);

            RidgeNode b =
                    nodes.get(i + 1);

            double dx =
                    b.outward()
                            - a.outward();

            double dy =
                    b.arc()
                            - a.arc();

            double lengthSquared =
                    dx * dx
                            + dy * dy;

            if (lengthSquared <= 1.0e-12) {
                continue;
            }

            double segmentT =
                    clamp01(
                            ((outward - a.outward()) * dx
                                    + (arc - a.arc()) * dy)
                                    / lengthSquared
                    );

            double closestOutward =
                    a.outward()
                            + dx * segmentT;

            double closestArc =
                    a.arc()
                            + dy * segmentT;

            double distanceOutward =
                    outward
                            - closestOutward;

            double distanceArc =
                    arc
                            - closestArc;

            double distanceSquared =
                    distanceOutward * distanceOutward
                            + distanceArc * distanceArc;

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared =
                        distanceSquared;

                bestProgress =
                        lerp(
                                a.progress(),
                                b.progress(),
                                segmentT
                        );
            }
        }

        if (!Double.isFinite(bestDistanceSquared)) {
            return null;
        }

        return new PathDistance(
                bestDistanceSquared,
                bestProgress
        );
    }

    private List<RidgePath> createRidgeSpurs(
            long seed
    ) {
        SplittableRandom random =
                new SplittableRandom(
                        seed
                );

        List<RidgePath> result =
                new ArrayList<>();

        /*
         * Seven to eleven major ridges per side is intentionally denser than
         * the previous pass. The hierarchy prevents that greater count from
         * becoming another evenly spaced comb: roots remain massif-driven,
         * headings vary widely, and only some ridges receive branches.
         */
        for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
            double side =
                    sideIndex == 0
                            ? 1.0
                            : -1.0;

            int majorCount =
                    7 + random.nextInt(5);

            List<CascadeMassif> assignments =
                    new ArrayList<>();

            for (
                    int i = 0;
                    i < massifs.size()
                            && assignments.size() < majorCount;
                    i++
            ) {
                assignments.add(
                        massifs.get(i)
                );
            }

            while (assignments.size() < majorCount) {
                assignments.add(
                        chooseWeightedMassif(
                                random
                        )
                );
            }

            for (CascadeMassif massif : assignments) {
                RidgePath major =
                        createMajorRidge(
                                random,
                                side,
                                massif
                        );

                result.add(
                        major
                );

                addSecondaryBranches(
                        result,
                        random,
                        major,
                        massif
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    private CascadeMassif chooseWeightedMassif(
            SplittableRandom random
    ) {
        double totalWeight =
                0.0;

        for (CascadeMassif massif : massifs) {
            totalWeight +=
                    massif.strength()
                            * massif.halfWidth();
        }

        double target =
                random.nextDouble()
                        * totalWeight;

        double running =
                0.0;

        for (CascadeMassif massif : massifs) {
            running +=
                    massif.strength()
                            * massif.halfWidth();

            if (running >= target) {
                return massif;
            }
        }

        return massifs.getLast();
    }

    private RidgePath createMajorRidge(
            SplittableRandom random,
            double side,
            CascadeMassif massif
    ) {
        double centerOffset =
                triangular(
                        random
                )
                        * massif.halfWidth()
                        * 0.92;

        double rootArc =
                clamp(
                        massif.centerArc()
                                + centerOffset,
                        spineLength * 0.035,
                        spineLength * 0.965
                );

        double localMassifStrength =
                Math.max(
                        0.45,
                        massifStrengthAt(
                                rootArc
                        )
                );

        double pathLength;
        double startWidth;
        int segmentCount;

        if (side > 0.0) {
            /* West: longer, broader ridges into the lowlands. */
            pathLength =
                    range(
                            random,
                            0.124,
                            0.205
                    );

            startWidth =
                    range(
                            random,
                            0.018,
                            0.030
                    );

            segmentCount =
                    7 + random.nextInt(3);
        } else {
            /* East: shorter, tighter ridges into the plateau. */
            pathLength =
                    range(
                            random,
                            0.076,
                            0.132
                    );

            startWidth =
                    range(
                            random,
                            0.014,
                            0.024
                    );

            segmentCount =
                    6 + random.nextInt(3);
        }

        double endWidth =
                startWidth
                        * range(
                        random,
                        0.38,
                        0.64
                );

        double strength =
                range(
                        random,
                        0.66,
                        1.00
                )
                        * (
                        0.70
                                + localMassifStrength
                                * 0.30
                );

        if (side < 0.0) {
            strength *=
                    range(
                            random,
                            0.80,
                            0.95
                    );
        }

        /*
         * Heading is measured from the outward axis:
         *
         *      0 rad  = directly west/east away from the crest
         *     +/-pi/2 = parallel to the Cascade spine
         *
         * The previous distribution still produced too many near-perpendicular
         * ridges. The new hierarchy deliberately favors strong oblique runs,
         * with a smaller set of near along-range sweepers. Only about one
         * fifth of the ridges now begin in the moderate/perpendicular family.
         */
        double headingRoll =
                random.nextDouble();

        double headingSign =
                random.nextBoolean()
                        ? 1.0
                        : -1.0;

        double initialHeading;

        if (headingRoll < 0.18) {
            /* Near along-range sweepers. */
            initialHeading =
                    headingSign
                            * range(
                            random,
                            Math.toRadians(68.0),
                            Math.toRadians(82.0)
                    );
        } else if (headingRoll < 0.78) {
            /* Dominant population: strongly oblique ridges. */
            initialHeading =
                    headingSign
                            * range(
                            random,
                            Math.toRadians(40.0),
                            Math.toRadians(68.0)
                    );
        } else {
            /* Minority population retaining some outward-facing ridges. */
            initialHeading =
                    triangular(
                            random
                    )
                            * Math.toRadians(38.0);
        }

        return buildRidgePath(
                random,
                side,
                0.0,
                rootArc,
                pathLength,
                initialHeading,
                segmentCount,
                startWidth,
                endWidth,
                strength,
                range(
                        random,
                        1.1,
                        2.3
                ),
                random.nextDouble()
                        * Math.PI
                        * 2.0,
                false,
                range(
                        random,
                        0.055,
                        0.105
                ),
                range(
                        random,
                        0.004,
                        0.014
                )
        );
    }

    private void addSecondaryBranches(
            List<RidgePath> result,
            SplittableRandom random,
            RidgePath parent,
            CascadeMassif massif
    ) {
        double massifStrength =
                massif.strength();

        int branchCount =
                0;

        if (
                random.nextDouble()
                        < 0.34
                        + massifStrength * 0.34
        ) {
            branchCount++;
        }

        if (
                random.nextDouble()
                        < 0.04
                        + massifStrength * 0.13
        ) {
            branchCount++;
        }

        for (int branch = 0; branch < branchCount; branch++) {
            double parentT =
                    range(
                            random,
                            0.30,
                            0.72
                    );

            RidgePoint branchStart =
                    pointOnPath(
                            parent,
                            parentT
                    );

            double parentHeading =
                    headingOnPath(
                            parent,
                            parentT
                    );

            double divergenceDirection =
                    random.nextBoolean()
                            ? 1.0
                            : -1.0;

            double branchHeading =
                    parentHeading
                            + divergenceDirection
                            * range(
                            random,
                            Math.toRadians(24.0),
                            Math.toRadians(48.0)
                    );

            branchHeading =
                    clamp(
                            branchHeading,
                            Math.toRadians(-82.0),
                            Math.toRadians(82.0)
                    );

            double branchLength;
            int segmentCount;

            if (parent.side() > 0.0) {
                branchLength =
                        range(
                                random,
                                0.046,
                                0.096
                        );

                segmentCount =
                        4 + random.nextInt(3);
            } else {
                branchLength =
                        range(
                                random,
                                0.034,
                                0.074
                        );

                segmentCount =
                        4 + random.nextInt(2);
            }

            double parentWidth =
                    widthAtProgress(
                            parent,
                            parentT
                    );

            double branchStartWidth =
                    parentWidth
                            * range(
                            random,
                            0.50,
                            0.70
                    );

            double branchEndWidth =
                    branchStartWidth
                            * range(
                            random,
                            0.38,
                            0.64
                    );

            RidgePath child =
                    buildRidgePath(
                            random,
                            parent.side(),
                            branchStart.outward(),
                            branchStart.arc(),
                            branchLength,
                            branchHeading,
                            segmentCount,
                            branchStartWidth,
                            branchEndWidth,
                            parent.strength()
                                    * range(
                                    random,
                                    0.42,
                                    0.64
                            ),
                            range(
                                    random,
                                    1.1,
                                    2.5
                            ),
                            random.nextDouble()
                                    * Math.PI
                                    * 2.0,
                            true,
                            range(
                                    random,
                                    0.075,
                                    0.145
                            ),
                            range(
                                    random,
                                    0.004,
                                    0.016
                            )
                    );

            result.add(
                    child
            );
        }
    }

    private RidgePath buildRidgePath(
            SplittableRandom random,
            double side,
            double startOutward,
            double startArc,
            double pathLength,
            double initialHeading,
            int segmentCount,
            double startWidth,
            double endWidth,
            double strength,
            double widthWaveCount,
            double widthPhase,
            boolean secondary,
            double turningNoise,
            double headingRelaxation
    ) {
        List<RidgeNode> nodes =
                new ArrayList<>(
                        segmentCount + 1
                );

        double outward =
                Math.max(
                        0.0,
                        startOutward
                );

        double arc =
                clamp(
                        startArc,
                        spineLength * 0.012,
                        spineLength * 0.988
                );

        double heading =
                clamp(
                        initialHeading,
                        Math.toRadians(-82.0),
                        Math.toRadians(82.0)
                );

        double angularVelocity =
                range(
                        random,
                        -0.030,
                        0.030
                );

        double bendBias =
                range(
                        random,
                        -0.018,
                        0.018
                );

        nodes.add(
                new RidgeNode(
                        outward,
                        arc,
                        0.0
                )
        );

        double baseStep =
                pathLength
                        / segmentCount;

        for (int segment = 1; segment <= segmentCount; segment++) {
            double progress =
                    (double) segment
                            / segmentCount;

            /*
             * Correlated angular motion produces smooth bends instead of
             * independent zig-zag points. The tiny relaxation nudges extreme
             * north/south sweepers gradually back toward an outward descent.
             */
            angularVelocity =
                    angularVelocity * 0.64
                            + triangular(random)
                            * turningNoise
                            + bendBias;

            heading +=
                    angularVelocity;

            heading *=
                    1.0
                            - headingRelaxation;

            heading =
                    clamp(
                            heading,
                            Math.toRadians(-82.0),
                            Math.toRadians(82.0)
                    );

            double step =
                    baseStep
                            * range(
                            random,
                            0.90,
                            1.10
                    );

            outward +=
                    Math.cos(
                            heading
                    ) * step;

            arc +=
                    Math.sin(
                            heading
                    ) * step;

            double minimumArc =
                    spineLength * 0.010;

            double maximumArc =
                    spineLength * 0.990;

            if (arc < minimumArc) {
                arc =
                        minimumArc;

                heading =
                        Math.abs(
                                heading
                        ) * 0.72;
            } else if (arc > maximumArc) {
                arc =
                        maximumArc;

                heading =
                        -Math.abs(
                                heading
                        ) * 0.72;
            }

            nodes.add(
                    new RidgeNode(
                            outward,
                            arc,
                            progress
                    )
            );
        }

        return new RidgePath(
                side,
                nodes,
                startWidth,
                endWidth,
                strength,
                widthWaveCount,
                widthPhase,
                secondary
        );
    }

    private static RidgePoint pointOnPath(
            RidgePath path,
            double progress
    ) {
        List<RidgeNode> nodes =
                path.nodes();

        double clampedProgress =
                clamp01(
                        progress
                );

        if (clampedProgress <= 0.0) {
            RidgeNode first =
                    nodes.getFirst();

            return new RidgePoint(
                    first.outward(),
                    first.arc()
            );
        }

        if (clampedProgress >= 1.0) {
            RidgeNode last =
                    nodes.getLast();

            return new RidgePoint(
                    last.outward(),
                    last.arc()
            );
        }

        for (int i = 0; i < nodes.size() - 1; i++) {
            RidgeNode a =
                    nodes.get(i);

            RidgeNode b =
                    nodes.get(i + 1);

            if (clampedProgress > b.progress()) {
                continue;
            }

            double localT =
                    (clampedProgress - a.progress())
                            / (b.progress() - a.progress());

            return new RidgePoint(
                    lerp(
                            a.outward(),
                            b.outward(),
                            localT
                    ),
                    lerp(
                            a.arc(),
                            b.arc(),
                            localT
                    )
            );
        }

        RidgeNode last =
                nodes.getLast();

        return new RidgePoint(
                last.outward(),
                last.arc()
        );
    }

    private static double headingOnPath(
            RidgePath path,
            double progress
    ) {
        List<RidgeNode> nodes =
                path.nodes();

        double clampedProgress =
                clamp01(
                        progress
                );

        for (int i = 0; i < nodes.size() - 1; i++) {
            RidgeNode a =
                    nodes.get(i);

            RidgeNode b =
                    nodes.get(i + 1);

            if (
                    clampedProgress <= b.progress()
                            || i == nodes.size() - 2
            ) {
                return Math.atan2(
                        b.arc() - a.arc(),
                        b.outward() - a.outward()
                );
            }
        }

        return 0.0;
    }

    private static double widthAtProgress(
            RidgePath path,
            double progress
    ) {
        return lerp(
                path.startWidth(),
                path.endWidth(),
                smoothstep(
                        0.0,
                        1.0,
                        clamp01(progress)
                )
        );
    }

    private static double triangular(
            SplittableRandom random
    ) {
        return random.nextDouble()
                + random.nextDouble()
                - 1.0;
    }

    private static double range(
            SplittableRandom random,
            double min,
            double max
    ) {
        return min
                + random.nextDouble()
                * (max - min);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (b - a) * t;
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


    private record VolcanoMorphology(
            double foothillRelief,
            double relief,
            double upperCone,
            double radialStructure,
            double craterMask,
            double dominance
    ) {

        private static VolcanoMorphology empty() {
            return new VolcanoMorphology(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    private record RidgeNode(
            double outward,
            double arc,
            double progress
    ) {
    }

    private record RidgePoint(
            double outward,
            double arc
    ) {
    }

    private record PathDistance(
            double distanceSquared,
            double progress
    ) {
    }

    private static final class RidgePath {

        private final double side;
        private final List<RidgeNode> nodes;
        private final double startWidth;
        private final double endWidth;
        private final double strength;
        private final double widthWaveCount;
        private final double widthPhase;
        private final boolean secondary;

        private final double minimumOutward;
        private final double maximumOutward;
        private final double minimumArc;
        private final double maximumArc;
        private final double maximumWidth;

        private RidgePath(
                double side,
                List<RidgeNode> nodes,
                double startWidth,
                double endWidth,
                double strength,
                double widthWaveCount,
                double widthPhase,
                boolean secondary
        ) {
            this.side = side;
            this.nodes = List.copyOf(nodes);
            this.startWidth = startWidth;
            this.endWidth = endWidth;
            this.strength = strength;
            this.widthWaveCount = widthWaveCount;
            this.widthPhase = widthPhase;
            this.secondary = secondary;

            double minOutward =
                    Double.POSITIVE_INFINITY;

            double maxOutward =
                    Double.NEGATIVE_INFINITY;

            double minArc =
                    Double.POSITIVE_INFINITY;

            double maxArc =
                    Double.NEGATIVE_INFINITY;

            for (RidgeNode node : nodes) {
                minOutward =
                        Math.min(
                                minOutward,
                                node.outward()
                        );

                maxOutward =
                        Math.max(
                                maxOutward,
                                node.outward()
                        );

                minArc =
                        Math.min(
                                minArc,
                                node.arc()
                        );

                maxArc =
                        Math.max(
                                maxArc,
                                node.arc()
                        );
            }

            this.minimumOutward = minOutward;
            this.maximumOutward = maxOutward;
            this.minimumArc = minArc;
            this.maximumArc = maxArc;
            this.maximumWidth =
                    Math.max(
                            startWidth,
                            endWidth
                    );
        }

        private double side() {
            return side;
        }

        private List<RidgeNode> nodes() {
            return nodes;
        }

        private double startWidth() {
            return startWidth;
        }

        private double endWidth() {
            return endWidth;
        }

        private double strength() {
            return strength;
        }

        private double widthWaveCount() {
            return widthWaveCount;
        }

        private double widthPhase() {
            return widthPhase;
        }

        private boolean secondary() {
            return secondary;
        }

        private double minimumOutward() {
            return minimumOutward;
        }

        private double maximumOutward() {
            return maximumOutward;
        }

        private double minimumArc() {
            return minimumArc;
        }

        private double maximumArc() {
            return maximumArc;
        }

        private double maximumWidth() {
            return maximumWidth;
        }
    }
}
