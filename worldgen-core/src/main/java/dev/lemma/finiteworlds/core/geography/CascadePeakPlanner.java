package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.SeedUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Plans the ordinary non-volcanic Cascade summit population.
 *
 * Peaks are explicit geographic objects rather than noise maxima. This gives
 * later passes control over summit counts, spacing, hierarchy, glaciers,
 * ridges, watersheds, and structure placement.
 */
public final class CascadePeakPlanner {

    private CascadePeakPlanner() {
    }

    public static List<CascadePeak> generate(
            long worldSeed,
            double spineLength,
            List<CascadeMassif> massifs
    ) {
        if (massifs.isEmpty()) {
            return List.of();
        }

        SplittableRandom random =
                new SplittableRandom(
                        SeedUtil.derive(
                                worldSeed,
                                "cascade-peaks"
                        )
                );

        Set<Integer> regionalMassifs =
                chooseRegionalMassifs(
                        random,
                        massifs
                );

        List<CascadePeak> result =
                new ArrayList<>();

        for (int massifIndex = 0;
             massifIndex < massifs.size();
             massifIndex++) {

            CascadeMassif massif =
                    massifs.get(massifIndex);

            double widthScale =
                    massif.halfWidth()
                            / Math.max(
                            spineLength * 0.075,
                            1.0e-9
                    );

            int targetCount =
                    (int) Math.round(
                            (16 + random.nextInt(8))
                                    * clamp(
                                    widthScale,
                                    0.84,
                                    1.32
                            )
                    );

            targetCount =
                    clampInt(
                            targetCount,
                            14,
                            28
                    );

            List<PeakCluster> clusters =
                    createClusters(
                            random,
                            massif,
                            widthScale,
                            spineLength,
                            targetCount
                    );

            if (clusters.isEmpty()) {
                continue;
            }

            boolean regionalMassif =
                    regionalMassifs.contains(massifIndex);

            int regionalCount =
                    regionalMassif
                            ? 1
                            : 0;

            int majorCount =
                    2 + random.nextInt(2);

            if (clusters.size() >= 4 && random.nextDouble() < 0.75) {
                majorCount++;
            }

            if (targetCount >= 23 && random.nextDouble() < 0.55) {
                majorCount++;
            }

            majorCount =
                    clampInt(
                            majorCount,
                            2,
                            Math.min(
                                    5,
                                    targetCount - regionalCount
                            )
                    );

            int alpineCount =
                    Math.max(
                            0,
                            targetCount
                                    - regionalCount
                                    - majorCount
                    );

            /*
             * Reserve vertical and spatial headroom for the biggest planned
             * summits first, then fill the upper flanks with dense alpine
             * groups. This should read more like several compact summit
             * clusters inside a massif rather than one vertical dotted line.
             */
            if (regionalCount > 0) {
                PeakCluster anchor =
                        clusters.stream()
                                .max(
                                        Comparator.comparingDouble(
                                                PeakCluster::importance
                                        )
                                )
                                .orElse(
                                        clusters.getFirst()
                                );

                tryPlacePeak(
                        result,
                        random,
                        massif,
                        anchor,
                        CascadePeakClass.REGIONAL_SUMMIT,
                        spineLength
                );
            }

            List<PeakCluster> majorOrder =
                    new ArrayList<>(clusters);

            majorOrder.sort(
                    Comparator.comparingDouble(
                                    PeakCluster::importance
                            )
                            .reversed()
            );

            for (int i = 0; i < majorCount; i++) {
                PeakCluster cluster =
                        majorOrder.get(
                                i % majorOrder.size()
                        );

                tryPlacePeak(
                        result,
                        random,
                        massif,
                        cluster,
                        CascadePeakClass.MAJOR_ALPINE,
                        spineLength
                );
            }

            int[] alpineTargets =
                    allocateAlpineCounts(
                            random,
                            clusters,
                            alpineCount
                    );

            for (int clusterIndex = 0;
                 clusterIndex < clusters.size();
                 clusterIndex++) {

                PeakCluster cluster =
                        clusters.get(clusterIndex);

                for (int i = 0; i < alpineTargets[clusterIndex]; i++) {
                    tryPlacePeak(
                            result,
                            random,
                            massif,
                            cluster,
                            CascadePeakClass.ALPINE,
                            spineLength
                    );
                }
            }
        }

        generateBackgroundPeaks(
                worldSeed,
                spineLength,
                massifs,
                result
        );

        return List.copyOf(result);
    }

    private static void generateBackgroundPeaks(
            long worldSeed,
            double spineLength,
            List<CascadeMassif> massifs,
            List<CascadePeak> peaks
    ) {
        SplittableRandom random =
                new SplittableRandom(
                        SeedUtil.derive(
                                worldSeed,
                                "cascade-background-peaks"
                        )
                );

        int slotCount =
                clampInt(
                        (int) Math.round(
                                spineLength / 0.0155
                        ),
                        58,
                        76
                );

        double slotLength =
                spineLength / slotCount;

        boolean previousCoreSlotEmpty =
                false;

        for (int slot = 0; slot < slotCount; slot++) {
            double slotCenter =
                    (slot + 0.5)
                            * slotLength;

            double candidateArc =
                    clamp(
                            slotCenter
                                    + range(
                                    random,
                                    -0.38,
                                    0.38
                            ) * slotLength,
                            spineLength * 0.025,
                            spineLength * 0.975
                    );

            double massifSupport =
                    massifSupportAt(
                            candidateArc,
                            massifs
                    );

            /*
             * Background summits are planned densely in several cross-range
             * bands instead of one narrow belt near the crest. The slot count
             * is intentionally aggressive at world scale: these are planning
             * anchors for ordinary mountains, not finished terrain features.
             * The dense explicit massif clusters still provide the exceptional
             * high-mountain concentrations above this continuous population.
             */
            double coreChance =
                    0.94
                            + 0.05
                            * Math.pow(
                            massifSupport,
                            0.70
                    );

            boolean placeCore =
                    previousCoreSlotEmpty
                            || random.nextDouble() < coreChance;

            if (placeCore) {
                tryPlaceBackgroundPeak(
                        peaks,
                        random,
                        candidateArc,
                        massifSupport,
                        slotLength,
                        spineLength,
                        -0.030,
                        0.036,
                        0.36,
                        0.53,
                        0.0050,
                        0.0082,
                        0.0068,
                        0.0118
                );

                previousCoreSlotEmpty = false;
            } else {
                previousCoreSlotEmpty = true;
            }

            if (
                    random.nextDouble()
                            < 0.82 + massifSupport * 0.15
            ) {
                tryPlaceBackgroundPeak(
                        peaks,
                        random,
                        candidateArc,
                        massifSupport,
                        slotLength,
                        spineLength,
                        0.026,
                        0.090,
                        0.32,
                        0.49,
                        0.0048,
                        0.0078,
                        0.0064,
                        0.0108
                );
            }

            if (
                    random.nextDouble()
                            < 0.76 + massifSupport * 0.14
            ) {
                tryPlaceBackgroundPeak(
                        peaks,
                        random,
                        candidateArc,
                        massifSupport,
                        slotLength,
                        spineLength,
                        -0.080,
                        -0.022,
                        0.31,
                        0.47,
                        0.0046,
                        0.0075,
                        0.0062,
                        0.0104
                );
            }

            /*
             * Occasional outer-flank summits keep the range body from
             * collapsing into three visible lanes.  They are intentionally
             * smaller and less frequent than the upper-flank population.
             */
            if (
                    random.nextDouble()
                            < 0.52 + massifSupport * 0.18
            ) {
                tryPlaceBackgroundPeak(
                        peaks,
                        random,
                        candidateArc,
                        massifSupport,
                        slotLength,
                        spineLength,
                        0.078,
                        0.132,
                        0.25,
                        0.41,
                        0.0042,
                        0.0068,
                        0.0058,
                        0.0096
                );
            }

            if (
                    random.nextDouble()
                            < 0.44 + massifSupport * 0.16
            ) {
                tryPlaceBackgroundPeak(
                        peaks,
                        random,
                        candidateArc,
                        massifSupport,
                        slotLength,
                        spineLength,
                        -0.112,
                        -0.066,
                        0.24,
                        0.40,
                        0.0040,
                        0.0065,
                        0.0056,
                        0.0092
                );
            }
        }
    }

    private static void tryPlaceBackgroundPeak(
            List<CascadePeak> peaks,
            SplittableRandom random,
            double slotArc,
            double massifSupport,
            double slotLength,
            double spineLength,
            double minimumSignedDistance,
            double maximumSignedDistance,
            double minimumStrength,
            double maximumStrength,
            double minimumRadiusAcross,
            double maximumRadiusAcross,
            double minimumRadiusAlong,
            double maximumRadiusAlong
    ) {
        final int maximumAttempts =
                90;

        for (int attempt = 0;
             attempt < maximumAttempts;
             attempt++) {

            double arcPosition =
                    clamp(
                            slotArc
                                    + triangular(random)
                                    * slotLength
                                    * 0.52,
                            spineLength * 0.025,
                            spineLength * 0.975
                    );

            double signedDistance =
                    range(
                            random,
                            minimumSignedDistance,
                            maximumSignedDistance
                    );

            signedDistance +=
                    triangular(random)
                            * (maximumSignedDistance
                            - minimumSignedDistance)
                            * 0.08;

            signedDistance =
                    clamp(
                            signedDistance,
                            -0.130,
                            0.150
                    );

            double strength =
                    range(
                            random,
                            minimumStrength,
                            maximumStrength
                    )
                            * (
                            0.88
                                    + massifSupport * 0.12
                    );

            CascadePeak candidate =
                    new CascadePeak(
                            CascadePeakClass.BACKGROUND_ALPINE,
                            signedDistance,
                            arcPosition,
                            range(
                                    random,
                                    minimumRadiusAcross,
                                    maximumRadiusAcross
                            ),
                            range(
                                    random,
                                    minimumRadiusAlong,
                                    maximumRadiusAlong
                            ),
                            clamp(strength, 0.0, 1.0),
                            range(random, 1.85, 2.70)
                    );

            if (
                    hasEnoughBackgroundSpacing(
                            candidate,
                            peaks
                    )
            ) {
                peaks.add(candidate);
                return;
            }
        }
    }

    private static boolean hasEnoughBackgroundSpacing(
            CascadePeak candidate,
            List<CascadePeak> peaks
    ) {
        for (CascadePeak existing : peaks) {
            double arcDifference =
                    candidate.arcPosition()
                            - existing.arcPosition();

            double crossDifference =
                    candidate.signedDistance()
                            - existing.signedDistance();

            double centerDistance =
                    Math.hypot(
                            arcDifference,
                            crossDifference
                    );

            double spacingMultiplier =
                    switch (existing.peakClass()) {
                        case BACKGROUND_ALPINE -> 0.18;
                        case ALPINE -> 0.28;
                        case MAJOR_ALPINE -> 0.40;
                        case REGIONAL_SUMMIT -> 0.52;
                    };

            double requiredDistance =
                    (
                            candidate.maximumRadius()
                                    + existing.maximumRadius()
                    )
                            * spacingMultiplier;

            if (centerDistance < requiredDistance) {
                return false;
            }
        }

        return true;
    }

    private static double massifSupportAt(
            double arcPosition,
            List<CascadeMassif> massifs
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

        return clamp(
                result,
                0.0,
                1.0
        );
    }

    private static Set<Integer> chooseRegionalMassifs(
            SplittableRandom random,
            List<CascadeMassif> massifs
    ) {
        int regionalCount =
                Math.min(
                        massifs.size(),
                        2 + random.nextInt(2)
                );

        Set<Integer> selected =
                new HashSet<>();

        while (selected.size() < regionalCount) {
            double totalWeight =
                    0.0;

            for (int i = 0; i < massifs.size(); i++) {
                if (!selected.contains(i)) {
                    totalWeight +=
                            Math.pow(
                                    massifs.get(i).strength(),
                                    2.0
                            );
                }
            }

            double target =
                    random.nextDouble()
                            * totalWeight;

            double running =
                    0.0;

            for (int i = 0; i < massifs.size(); i++) {
                if (selected.contains(i)) {
                    continue;
                }

                running +=
                        Math.pow(
                                massifs.get(i).strength(),
                                2.0
                        );

                if (running >= target) {
                    selected.add(i);
                    break;
                }
            }
        }

        return selected;
    }

    private static List<PeakCluster> createClusters(
            SplittableRandom random,
            CascadeMassif massif,
            double widthScale,
            double spineLength,
            int targetCount
    ) {
        int clusterCount =
                2;

        if (widthScale >= 0.95 || targetCount >= 18) {
            clusterCount++;
        }

        if ((widthScale >= 1.14 || targetCount >= 23)
                && random.nextDouble() < 0.72) {
            clusterCount++;
        }

        clusterCount =
                clampInt(
                        clusterCount,
                        2,
                        4
                );

        List<PeakCluster> clusters =
                new ArrayList<>();

        int attempts =
                0;

        while (clusters.size() < clusterCount && attempts < 200) {
            attempts++;

            double arcRadius =
                    clamp(
                            massif.halfWidth()
                                    * range(
                                    random,
                                    0.15,
                                    0.27
                            ),
                            spineLength * 0.010,
                            spineLength * 0.024
                    );

            double crossRadius =
                    range(
                            random,
                            0.012,
                            0.024
                    );

            double centerArc =
                    clamp(
                            massif.centerArc()
                                    + triangular(random)
                                    * massif.halfWidth()
                                    * 0.76,
                            spineLength * 0.025,
                            spineLength * 0.975
                    );

            /*
             * Keep clusters close to the crest, but not welded exactly to it.
             * A slight west-side bias is fine because the west flank is the
             * broader, more elaborate side of the range.
             */
            double centerSignedDistance =
                    clamp(
                            range(random, -0.006, 0.012)
                                    + triangular(random)
                                    * range(
                                    random,
                                    0.006,
                                    0.024
                            ),
                            -0.040,
                            0.045
                    );

            double density =
                    range(
                            random,
                            0.86,
                            1.18
                    );

            double strengthBias =
                    range(
                            random,
                            0.88,
                            1.16
                    );

            PeakCluster candidate =
                    new PeakCluster(
                            centerArc,
                            centerSignedDistance,
                            arcRadius,
                            crossRadius,
                            density,
                            strengthBias
                    );

            if (hasEnoughClusterSpacing(candidate, clusters)) {
                clusters.add(candidate);
            }
        }

        if (clusters.isEmpty()) {
            clusters.add(
                    new PeakCluster(
                            massif.centerArc(),
                            0.0,
                            clamp(
                                    massif.halfWidth() * 0.22,
                                    spineLength * 0.012,
                                    spineLength * 0.024
                            ),
                            0.018,
                            1.0,
                            1.0
                    )
            );
        }

        return List.copyOf(clusters);
    }

    private static boolean hasEnoughClusterSpacing(
            PeakCluster candidate,
            List<PeakCluster> existing
    ) {
        for (PeakCluster cluster : existing) {
            double arcDistance =
                    Math.abs(
                            candidate.centerArc()
                                    - cluster.centerArc()
                    ) / (candidate.arcRadius() + cluster.arcRadius());

            double crossDistance =
                    Math.abs(
                            candidate.signedDistance()
                                    - cluster.signedDistance()
                    ) / (candidate.crossRadius() + cluster.crossRadius());

            double normalizedDistance =
                    Math.sqrt(
                            arcDistance * arcDistance
                                    + crossDistance * crossDistance
                    );

            if (normalizedDistance < 0.82) {
                return false;
            }
        }

        return true;
    }

    private static int[] allocateAlpineCounts(
            SplittableRandom random,
            List<PeakCluster> clusters,
            int alpineCount
    ) {
        int[] result =
                new int[clusters.size()];

        if (alpineCount <= 0) {
            return result;
        }

        int remaining =
                alpineCount;

        for (int i = 0; i < clusters.size() && remaining > 0; i++) {
            result[i]++;
            remaining--;
        }

        while (remaining > 0) {
            double totalWeight =
                    0.0;

            for (PeakCluster cluster : clusters) {
                totalWeight += cluster.importance();
            }

            double target =
                    random.nextDouble() * totalWeight;

            double running =
                    0.0;

            for (int i = 0; i < clusters.size(); i++) {
                running += clusters.get(i).importance();

                if (running >= target) {
                    result[i]++;
                    remaining--;
                    break;
                }
            }
        }

        return result;
    }

    private static void tryPlacePeak(
            List<CascadePeak> peaks,
            SplittableRandom random,
            CascadeMassif massif,
            PeakCluster cluster,
            CascadePeakClass peakClass,
            double spineLength
    ) {
        final int maximumAttempts =
                240;

        for (int attempt = 0;
             attempt < maximumAttempts;
             attempt++) {

            double spacingRelaxation =
                    attempt < 170
                            ? 1.0
                            : lerp(
                            1.0,
                            0.76,
                            (attempt - 170.0)
                                    / (maximumAttempts - 170.0)
                    );

            CascadePeak candidate =
                    createCandidate(
                            random,
                            massif,
                            cluster,
                            peakClass,
                            spineLength
                    );

            if (
                    hasEnoughSpacing(
                            candidate,
                            peaks,
                            spacingRelaxation
                    )
            ) {
                peaks.add(candidate);
                return;
            }
        }

        /*
         * Narrow or crowded massifs may not fit every requested summit.
         * Omit the last candidate rather than forcing overlapping centers.
         */
    }

    private static CascadePeak createCandidate(
            SplittableRandom random,
            CascadeMassif massif,
            PeakCluster cluster,
            CascadePeakClass peakClass,
            double spineLength
    ) {
        double arcRadius;
        double crossRadius;
        double centerArc;
        double centerSignedDistance;
        double radiusAcross;
        double radiusAlong;
        double strength;
        double sharpness;

        switch (peakClass) {
            case ALPINE -> {
                arcRadius = cluster.arcRadius() * 1.12;
                crossRadius = cluster.crossRadius() * 1.08;
                centerArc = cluster.centerArc();
                centerSignedDistance = cluster.signedDistance();
                radiusAcross = range(random, 0.0065, 0.0110);
                radiusAlong = range(random, 0.0085, 0.0150);
                strength = range(random, 0.54, 0.72);
                sharpness = range(random, 1.70, 2.55);
            }

            case MAJOR_ALPINE -> {
                arcRadius = cluster.arcRadius() * 0.74;
                crossRadius = cluster.crossRadius() * 0.82;
                centerArc = cluster.centerArc();
                centerSignedDistance = cluster.signedDistance();
                radiusAcross = range(random, 0.0095, 0.0140);
                radiusAlong = range(random, 0.0125, 0.0205);
                strength = range(random, 0.72, 0.87);
                sharpness = range(random, 1.52, 2.18);
            }

            case REGIONAL_SUMMIT -> {
                arcRadius = cluster.arcRadius() * 0.50;
                crossRadius = cluster.crossRadius() * 0.62;
                centerArc = cluster.centerArc();
                centerSignedDistance = cluster.signedDistance();
                radiusAcross = range(random, 0.0125, 0.0180);
                radiusAlong = range(random, 0.0175, 0.0260);
                strength = range(random, 0.90, 1.00);
                sharpness = range(random, 1.35, 1.88);
            }

            default ->
                    throw new IllegalStateException(
                            "Unhandled peak class: "
                                    + peakClass
                    );
        }

        double arcPosition =
                clamp(
                        centerArc
                                + triangular(random) * arcRadius
                                + triangular(random) * massif.halfWidth() * 0.05,
                        spineLength * 0.025,
                        spineLength * 0.975
                );

        double signedDistance =
                clamp(
                        centerSignedDistance
                                + triangular(random) * crossRadius,
                        -0.055,
                        0.060
                );

        strength *=
                0.88
                        + massif.strength() * 0.12;

        strength *=
                0.88
                        + cluster.strengthBias() * 0.12;

        return new CascadePeak(
                peakClass,
                signedDistance,
                arcPosition,
                radiusAcross,
                radiusAlong,
                clamp(strength, 0.0, 1.0),
                sharpness
        );
    }

    private static boolean hasEnoughSpacing(
            CascadePeak candidate,
            List<CascadePeak> peaks,
            double spacingRelaxation
    ) {
        for (CascadePeak existing : peaks) {
            double arcDifference =
                    candidate.arcPosition()
                            - existing.arcPosition();

            double crossDifference =
                    candidate.signedDistance()
                            - existing.signedDistance();

            double centerDistance =
                    Math.hypot(
                            arcDifference,
                            crossDifference
                    );

            double spacingMultiplier =
                    switch (
                            dominantClass(
                                    candidate.peakClass(),
                                    existing.peakClass()
                            )
                    ) {
                        case REGIONAL_SUMMIT -> 0.74;
                        case MAJOR_ALPINE -> 0.56;
                        case ALPINE -> 0.36;
                        case BACKGROUND_ALPINE -> 0.30;
                    };

            double requiredDistance =
                    (
                            candidate.maximumRadius()
                                    + existing.maximumRadius()
                    )
                            * spacingMultiplier
                            * spacingRelaxation;

            if (centerDistance < requiredDistance) {
                return false;
            }
        }

        return true;
    }

    private static CascadePeakClass dominantClass(
            CascadePeakClass a,
            CascadePeakClass b
    ) {
        if (a == CascadePeakClass.REGIONAL_SUMMIT
                || b == CascadePeakClass.REGIONAL_SUMMIT) {
            return CascadePeakClass.REGIONAL_SUMMIT;
        }

        if (a == CascadePeakClass.MAJOR_ALPINE
                || b == CascadePeakClass.MAJOR_ALPINE) {
            return CascadePeakClass.MAJOR_ALPINE;
        }

        if (a == CascadePeakClass.ALPINE
                || b == CascadePeakClass.ALPINE) {
            return CascadePeakClass.ALPINE;
        }

        return CascadePeakClass.BACKGROUND_ALPINE;
    }

    private static double triangular(
            SplittableRandom random
    ) {
        return random.nextDouble()
                - random.nextDouble();
    }

    private static double range(
            SplittableRandom random,
            double minimum,
            double maximum
    ) {
        return minimum
                + random.nextDouble()
                * (maximum - minimum);
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (b - a)
                * clamp(t, 0.0, 1.0);
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

    private record PeakCluster(
            double centerArc,
            double signedDistance,
            double arcRadius,
            double crossRadius,
            double density,
            double strengthBias
    ) {

        private double importance() {
            return density * strengthBias;
        }
    }
}
