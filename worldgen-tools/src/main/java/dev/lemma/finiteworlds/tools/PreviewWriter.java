package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.geography.CascadeMorphologySample;
import dev.lemma.finiteworlds.core.geography.CascadeMorphologySampler;
import dev.lemma.finiteworlds.core.geography.CascadeVolcano;
import dev.lemma.finiteworlds.core.geography.ContinentPlan;
import dev.lemma.finiteworlds.core.geography.GeoPoint;
import dev.lemma.finiteworlds.core.geography.MountainSpine;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;
import dev.lemma.finiteworlds.core.hydrology.CompoundBasin;
import dev.lemma.finiteworlds.core.hydrology.Depression;
import dev.lemma.finiteworlds.core.hydrology.DepressionClass;
import dev.lemma.finiteworlds.core.hydrology.DepressionClassification;
import dev.lemma.finiteworlds.core.hydrology.DepressionResolutionAction;
import dev.lemma.finiteworlds.core.hydrology.DepressionResolutionPlan;
import dev.lemma.finiteworlds.core.hydrology.FlowDirection;
import dev.lemma.finiteworlds.core.hydrology.HydrologyGrid;
import dev.lemma.finiteworlds.core.hydrology.HydrologyRouteType;
import dev.lemma.finiteworlds.core.hydrology.Lake;
import dev.lemma.finiteworlds.core.hydrology.LakeSourceType;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PreviewWriter {

    private PreviewWriter() {}

    public static void writeAll(
            WorldBlueprint world,
            long seed,
            ContinentPlan plan,
            Path directory
    ) throws IOException {

        Files.createDirectories(directory);

        writeLandMask(
                world,
                directory.resolve(
                        "landmask.png"
                )
        );

        writeElevation(
                world,
                directory.resolve(
                        "elevation.png"
                )
        );

        writeScalarField(
                world,
                directory.resolve(
                        "elevation-landmark-scale.png"
                ),
                -200.0,
                700.0,
                world::elevation
        );

        writeRelief(
                world,
                directory.resolve(
                        "relief.png"
                )
        );

        writeOverview(
                world,
                directory.resolve(
                        "overview.png"
                )
        );

        writeTerrain(
                world,
                seed,
                directory.resolve(
                        "terrain.png"
                )
        );

        writeContinentPlan(
                plan,
                world.resolution(),
                directory.resolve(
                        "continent-plan.png"
                )
        );

        writeCoastDistance(
                world,
                directory.resolve(
                        "coast-distance.png"
                )
        );

        writeMountainMask(
                world,
                plan,
                directory.resolve(
                        "mountain-mask.png"
                )
        );

        writeMountainUplift(
                world,
                plan,
                directory.resolve(
                        "mountain-uplift.png"
                )
        );

        writeTerrainProvinces(
                world,
                directory.resolve(
                        "terrain-provinces.png"
                )
        );

        writeScalarField(
                world,
                directory.resolve(
                        "base-elevation.png"
                ),
                -200.0,
                220.0,
                world::baseElevation
        );

        writeScalarField(
                world,
                directory.resolve(
                        "coast-range-uplift.png"
                ),
                0.0,
                130.0,
                world::coastRangeUplift
        );

        writeScalarField(
                world,
                directory.resolve(
                        "cascade-uplift.png"
                ),
                0.0,
                plan.mountainSystem()
                        .maximumUplift(),
                world::cascadeUplift
        );

        writeScalarField(
                world,
                directory.resolve(
                        "cascade-uplift-landmark-scale.png"
                ),
                0.0,
                plan.mountainSystem()
                        .maximumUplift() * 3.10,
                world::cascadeUplift
        );

        writeCascadeMorphologyDebug(
                world,
                seed,
                plan,
                directory
        );

        writeCascadeVolcanoDetail(
                world,
                seed,
                plan,
                directory
        );

        writeScalarField(
                world,
                directory.resolve(
                        "plateau-uplift.png"
                ),
                0.0,
                90.0,
                world::plateauUplift
        );

        writeSlope(
                world,
                directory.resolve(
                        "slope.png"
                )
        );

        writeHydrologyFlowDirection(
                world,
                directory.resolve(
                        "hydrology-flow-direction.png"
                )
        );

        writeHydrologySinks(
                world,
                directory.resolve(
                        "hydrology-sinks.png"
                )
        );

        writeHydrologyDepressions(
                world,
                directory.resolve(
                        "hydrology-depressions.png"
                )
        );

        writeHydrologyDepressionDepth(
                world,
                directory.resolve(
                        "hydrology-depression-depth.png"
                )
        );

        writeHydrologySpillPoints(
                world,
                directory.resolve(
                        "hydrology-spill-points.png"
                )
        );

        writeHydrologyDepressionClasses(
                world,
                directory.resolve(
                        "hydrology-depression-classes.png"
                )
        );

        writeHydrologyDepressionChains(
                world,
                directory.resolve(
                        "hydrology-depression-chains.png"
                )
        );

        writeHydrologyCompoundBasins(
                world,
                directory.resolve(
                        "hydrology-compound-basins.png"
                )
        );

        writeHydrologyCompoundOutlets(
                world,
                directory.resolve(
                        "hydrology-compound-outlets.png"
                )
        );

        writeHydrologyLakes(
                world,
                directory.resolve(
                        "hydrology-lakes.png"
                )
        );

        writeHydrologyLakeDepth(
                world,
                directory.resolve(
                        "hydrology-lake-depth.png"
                )
        );

        writeHydrologyLakeOutlets(
                world,
                directory.resolve(
                        "hydrology-lake-outlets.png"
                )
        );

        writeHydrologyDepressionResolution(
                world,
                directory.resolve(
                        "hydrology-depression-resolution.png"
                )
        );

        writeHydrologyConditionedElevation(
                world,
                directory.resolve(
                        "hydrology-conditioned-elevation.png"
                )
        );

        writeHydrologyFillDelta(
                world,
                directory.resolve(
                        "hydrology-fill-delta.png"
                )
        );

        writeHydrologyBreachPaths(
                world,
                directory.resolve(
                        "hydrology-breach-paths.png"
                )
        );

        writeHydrologyBreachDelta(
                world,
                directory.resolve(
                        "hydrology-breach-delta.png"
                )
        );

        writeHydrologyConditionedFlowDirection(
                world,
                directory.resolve(
                        "hydrology-conditioned-flow-direction.png"
                )
        );

        writeHydrologyConditionedSinks(
                world,
                directory.resolve(
                        "hydrology-conditioned-sinks.png"
                )
        );

        writeHydrologyFinalFlowDirection(
                world,
                directory.resolve(
                        "hydrology-final-flow-direction.png"
                )
        );

        writeHydrologyLakeRouting(
                world,
                directory.resolve(
                        "hydrology-lake-routing.png"
                )
        );

        writeHydrologyResidualRouting(
                world,
                directory.resolve(
                        "hydrology-residual-routing.png"
                )
        );

        writeHydrologyFinalSinks(
                world,
                directory.resolve(
                        "hydrology-final-sinks.png"
                )
        );
    }

    private static void writeLandMask(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size = world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                int value =
                        clamp255(
                                world.landMask(x, z)
                                        * 255
                        );

                int rgb =
                        (value << 16)
                                | (value << 8)
                                | value;

                image.setRGB(x, z, rgb);
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeElevation(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size = world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        double min = -200;
        double max = 350;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                double elevation =
                        world.elevation(x, z);

                double normalized =
                        (elevation - min)
                                / (max - min);

                int value =
                        clamp255(
                                normalized * 255
                        );

                int rgb =
                        (value << 16)
                                | (value << 8)
                                | value;

                image.setRGB(x, z, rgb);
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeRelief(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size = world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 1; z < size - 1; z++) {
            for (int x = 1; x < size - 1; x++) {

                double dx =
                        world.elevation(x + 1, z)
                                - world.elevation(x - 1, z);

                double dz =
                        world.elevation(x, z + 1)
                                - world.elevation(x, z - 1);

                /*
                 * Very simple northwest-style
                 * hill shading.
                 */

                double light =
                        0.55
                                - dx * 0.007
                                - dz * 0.007;

                int value =
                        clamp255(
                                light * 255
                        );

                int rgb =
                        (value << 16)
                                | (value << 8)
                                | value;

                image.setRGB(x, z, rgb);
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeOverview(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size = world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                double elevation =
                        world.elevation(x, z);

                int r;
                int g;
                int b;

                if (elevation < 64) {

                    double depth =
                            Math.min(
                                    1.0,
                                    (64 - elevation) / 220.0
                            );

                    r = (int) (30 - depth * 20);
                    g = (int) (90 - depth * 45);
                    b = (int) (145 - depth * 55);

                } else {

                    double height =
                            Math.min(
                                    1.0,
                                    (elevation - 64) / 250.0
                            );

                    r =
                            (int) (
                                    60
                                            + height * 110
                            );

                    g =
                            (int) (
                                    125
                                            - height * 35
                            );

                    b =
                            (int) (
                                    60
                                            - height * 20
                            );
                }

                int rgb =
                        (clamp255(r) << 16)
                                | (clamp255(g) << 8)
                                | clamp255(b);

                image.setRGB(x, z, rgb);
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static int clamp255(double value) {
        return Math.max(
                0,
                Math.min(
                        255,
                        (int) Math.round(value)
                )
        );
    }

    private static void writeTerrain(
            WorldBlueprint world,
            long seed,
            Path path
    ) throws IOException {

        /*
         * Higher-resolution preview than
         * the 1024x1024 macro blueprint.
         */

        int imageSize =
                2048;

        BufferedImage image =
                new BufferedImage(
                        imageSize,
                        imageSize,
                        BufferedImage.TYPE_INT_RGB
                );

        TerrainSampler sampler =
                new TerrainSampler(
                        world,
                        seed
                );

        double worldSize =
                world.config()
                        .worldSizeBlocks();

        double halfWorld =
                worldSize / 2.0;

        for (
                int pixelZ = 0;
                pixelZ < imageSize;
                pixelZ++
        ) {

            for (
                    int pixelX = 0;
                    pixelX < imageSize;
                    pixelX++
            ) {

                double normalizedX =
                        (pixelX + 0.5)
                                / imageSize;

                double normalizedZ =
                        (pixelZ + 0.5)
                                / imageSize;

                double worldX =
                        normalizedX
                                * worldSize
                                - halfWorld;

                double worldZ =
                        normalizedZ
                                * worldSize
                                - halfWorld;

                double elevation =
                        sampler
                                .surfaceElevationAt(
                                        worldX,
                                        worldZ
                                );

                int r;
                int g;
                int b;

                if (
                        elevation
                                < world.config()
                                .seaLevel()
                ) {

                    double depth =
                            Math.min(
                                    1.0,
                                    (
                                            world.config()
                                                    .seaLevel()
                                                    - elevation
                                    )
                                            / 220.0
                            );

                    r =
                            (int) (
                                    30
                                            - depth
                                            * 20
                            );

                    g =
                            (int) (
                                    90
                                            - depth
                                            * 45
                            );

                    b =
                            (int) (
                                    145
                                            - depth
                                            * 55
                            );

                } else {

                    double height =
                            Math.min(
                                    1.0,
                                    (
                                            elevation
                                                    - world.config()
                                                    .seaLevel()
                                    )
                                            / 250.0
                            );

                    r =
                            (int) (
                                    60
                                            + height
                                            * 110
                            );

                    g =
                            (int) (
                                    125
                                            - height
                                            * 35
                            );

                    b =
                            (int) (
                                    60
                                            - height
                                            * 20
                            );
                }

                int rgb =
                        (clamp255(r) << 16)
                                | (clamp255(g) << 8)
                                | clamp255(b);

                image.setRGB(
                        pixelX,
                        pixelZ,
                        rgb
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeContinentPlan(
            ContinentPlan plan,
            int size,
            Path path
    ) throws IOException {

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (
                int z = 0;
                z < size;
                z++
        ) {

            for (
                    int x = 0;
                    x < size;
                    x++
            ) {

                double nx =
                        (
                                (x + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double nz =
                        (
                                (z + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double value =
                        plan.sampleBase(
                                nx,
                                nz
                        );

                double normalized =
                        smoothstep(
                                -0.05,
                                0.05,
                                value
                        );

                int gray =
                        clamp255(
                                normalized
                                        * 255.0
                        );

                int rgb =
                        (gray << 16)
                                | (gray << 8)
                                | gray;

                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
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

        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );

        return t
                * t
                * (3.0 - 2.0 * t);
    }

    private static void writeCoastDistance(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        double displayDistance =
                world.config()
                        .worldSizeBlocks()
                        * 0.15;

        for (
                int z = 0;
                z < size;
                z++
        ) {

            for (
                    int x = 0;
                    x < size;
                    x++
            ) {

                double distance =
                        world.coastDistance(
                                x,
                                z
                        );

                double normalized =
                        distance
                                / displayDistance;

                normalized =
                        Math.max(
                                -1.0,
                                Math.min(
                                        1.0,
                                        normalized
                                )
                        );

                /*
                 * Coastline = middle gray.
                 *
                 * Deep ocean = dark.
                 * Deep inland = bright.
                 */

                int gray =
                        clamp255(
                                (
                                        normalized
                                                * 0.5
                                                + 0.5
                                ) * 255.0
                        );

                int rgb =
                        (gray << 16)
                                | (gray << 8)
                                | gray;

                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeMountainMask(
            WorldBlueprint world,
            ContinentPlan plan,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {

                double nx =
                        (
                                (x + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double nz =
                        (
                                (z + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double mask =
                        plan.mountainSystem()
                                .corridorMask(
                                        nx,
                                        nz
                                );

                int gray =
                        clamp255(
                                mask * 255.0
                        );

                int rgb =
                        (gray << 16)
                                | (gray << 8)
                                | gray;

                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeMountainUplift(
            WorldBlueprint world,
            ContinentPlan plan,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        /*
         * Keep this in sync with the prototype
         * maximum uplift used by MountainSystem.
         */
        double displayMaximum =
                190.0;

        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {

                double nx =
                        (
                                (x + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double nz =
                        (
                                (z + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double uplift =
                        world.cascadeUplift(
                                x,
                                z
                        );

                double normalized =
                        Math.min(
                                1.0,
                                uplift
                                        / displayMaximum
                        );

                int gray =
                        clamp255(
                                normalized
                                        * 255.0
                        );

                int rgb =
                        (gray << 16)
                                | (gray << 8)
                                | gray;

                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeCascadeMorphologyDebug(
            WorldBlueprint world,
            long seed,
            ContinentPlan plan,
            Path directory
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage envelopeImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage regionalHeightImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage rangeImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage crestImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage ridgeImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage peakImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoFoothillImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoStructuralRidgeImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoReliefImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoUpperConeImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoRadialImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoCraterImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage volcanoUpliftImage =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        CascadeMorphologySampler sampler =
                new CascadeMorphologySampler(
                        seed,
                        plan.cascadeMountainSystem(),
                        world
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                double nx =
                        (
                                (x + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                double nz =
                        (
                                (z + 0.5)
                                        / size
                        ) * 2.0
                                - 1.0;

                CascadeMorphologySample sample =
                        world.landMask(x, z) >= 0.5f
                                ? sampler.sample(
                                nx,
                                nz
                        )
                                : CascadeMorphologySample.empty();

                int envelopeGray =
                        clamp255(
                                sample.envelope()
                                        * 255.0
                        );

                int regionalHeightGray =
                        clamp255(
                                sample.regionalHeight()
                                        * 255.0
                        );

                int rangeGray =
                        clamp255(
                                sample.rangeRelief()
                                        * 255.0
                        );

                int crestGray =
                        clamp255(
                                sample.crestStructure()
                                        * 255.0
                        );

                int ridgeGray =
                        clamp255(
                                sample.ridgeRelief()
                                        * 255.0
                        );

                int peakGray =
                        clamp255(
                                sample.peakRelief()
                                        * 255.0
                        );

                int volcanoFoothillGray =
                        clamp255(
                                sample.volcanoFoothillRelief()
                                        * 255.0
                        );

                int volcanoStructuralRidgeGray =
                        clamp255(
                                sample.volcanoStructuralRidgeRelief()
                                        * 255.0
                        );

                int volcanoReliefGray =
                        clamp255(
                                sample.volcanoRelief()
                                        * 255.0
                        );

                int volcanoUpperConeGray =
                        clamp255(
                                sample.volcanoUpperCone()
                                        * 255.0
                        );

                int volcanoRadialGray =
                        clamp255(
                                sample.volcanoRadialStructure()
                                        * 255.0
                        );

                int volcanoCraterGray =
                        clamp255(
                                sample.volcanoCraterMask()
                                        * 255.0
                        );

                int volcanoUpliftGray =
                        clamp255(
                                sample.volcanoUplift()
                                        / (plan.mountainSystem().maximumUplift() * 2.30)
                                        * 255.0
                        );

                envelopeImage.setRGB(
                        x,
                        z,
                        (envelopeGray << 16)
                                | (envelopeGray << 8)
                                | envelopeGray
                );

                regionalHeightImage.setRGB(
                        x,
                        z,
                        (regionalHeightGray << 16)
                                | (regionalHeightGray << 8)
                                | regionalHeightGray
                );

                rangeImage.setRGB(
                        x,
                        z,
                        (rangeGray << 16)
                                | (rangeGray << 8)
                                | rangeGray
                );

                crestImage.setRGB(
                        x,
                        z,
                        (crestGray << 16)
                                | (crestGray << 8)
                                | crestGray
                );

                ridgeImage.setRGB(
                        x,
                        z,
                        (ridgeGray << 16)
                                | (ridgeGray << 8)
                                | ridgeGray
                );

                peakImage.setRGB(
                        x,
                        z,
                        (peakGray << 16)
                                | (peakGray << 8)
                                | peakGray
                );

                volcanoFoothillImage.setRGB(
                        x,
                        z,
                        (volcanoFoothillGray << 16)
                                | (volcanoFoothillGray << 8)
                                | volcanoFoothillGray
                );

                volcanoStructuralRidgeImage.setRGB(
                        x,
                        z,
                        (volcanoStructuralRidgeGray << 16)
                                | (volcanoStructuralRidgeGray << 8)
                                | volcanoStructuralRidgeGray
                );

                volcanoReliefImage.setRGB(
                        x,
                        z,
                        (volcanoReliefGray << 16)
                                | (volcanoReliefGray << 8)
                                | volcanoReliefGray
                );

                volcanoUpperConeImage.setRGB(
                        x,
                        z,
                        (volcanoUpperConeGray << 16)
                                | (volcanoUpperConeGray << 8)
                                | volcanoUpperConeGray
                );

                volcanoRadialImage.setRGB(
                        x,
                        z,
                        (volcanoRadialGray << 16)
                                | (volcanoRadialGray << 8)
                                | volcanoRadialGray
                );

                volcanoCraterImage.setRGB(
                        x,
                        z,
                        (volcanoCraterGray << 16)
                                | (volcanoCraterGray << 8)
                                | volcanoCraterGray
                );

                volcanoUpliftImage.setRGB(
                        x,
                        z,
                        (volcanoUpliftGray << 16)
                                | (volcanoUpliftGray << 8)
                                | volcanoUpliftGray
                );
            }
        }

        ImageIO.write(
                envelopeImage,
                "PNG",
                directory.resolve(
                        "cascade-envelope.png"
                ).toFile()
        );

        ImageIO.write(
                regionalHeightImage,
                "PNG",
                directory.resolve(
                        "cascade-regional-height.png"
                ).toFile()
        );

        ImageIO.write(
                rangeImage,
                "PNG",
                directory.resolve(
                        "cascade-range-relief.png"
                ).toFile()
        );

        ImageIO.write(
                crestImage,
                "PNG",
                directory.resolve(
                        "cascade-crest-structure.png"
                ).toFile()
        );

        ImageIO.write(
                ridgeImage,
                "PNG",
                directory.resolve(
                        "cascade-ridge-relief.png"
                ).toFile()
        );

        ImageIO.write(
                peakImage,
                "PNG",
                directory.resolve(
                        "cascade-peak-relief.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoFoothillImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-foothills.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoStructuralRidgeImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-structural-ridges.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoReliefImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-relief.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoUpperConeImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-upper-cone.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoRadialImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-radial-structure.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoCraterImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-crater-mask.png"
                ).toFile()
        );

        ImageIO.write(
                volcanoUpliftImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-uplift.png"
                ).toFile()
        );
    }


    private static void writeCascadeVolcanoDetail(
            WorldBlueprint world,
            long seed,
            ContinentPlan plan,
            Path directory
    ) throws IOException {
        final int detailSize =
                512;

        CascadeMorphologySampler sampler =
                new CascadeMorphologySampler(
                        seed,
                        plan.cascadeMountainSystem(),
                        world
                );

        CascadeVolcano volcano =
                sampler.landmarkVolcano();

        SpineFrame frame =
                frameAtArc(
                        plan.cascadeMountainSystem().spine(),
                        volcano.arcPosition()
                );

        double halfExtent =
                volcano.maximumRadius()
                        * 2.10;

        BufferedImage reliefImage =
                new BufferedImage(
                        detailSize,
                        detailSize,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage structuralRidgeImage =
                new BufferedImage(
                        detailSize,
                        detailSize,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage radialImage =
                new BufferedImage(
                        detailSize,
                        detailSize,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage craterImage =
                new BufferedImage(
                        detailSize,
                        detailSize,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < detailSize; z++) {
            for (int x = 0; x < detailSize; x++) {
                double crossOffset =
                        lerp(
                                -halfExtent,
                                halfExtent,
                                (x + 0.5) / detailSize
                        );

                double alongOffset =
                        lerp(
                                -halfExtent,
                                halfExtent,
                                (z + 0.5) / detailSize
                        );

                double nx =
                        frame.x()
                                - frame.tangentZ() * crossOffset
                                + frame.tangentX() * alongOffset;

                double nz =
                        frame.z()
                                + frame.tangentX() * crossOffset
                                + frame.tangentZ() * alongOffset;

                CascadeMorphologySample sample =
                        sampler.sample(
                                nx,
                                nz
                        );

                int reliefGray =
                        clamp255(
                                sample.volcanoRelief()
                                        * 255.0
                        );

                int structuralRidgeGray =
                        clamp255(
                                sample.volcanoStructuralRidgeRelief()
                                        * 255.0
                        );

                int radialGray =
                        clamp255(
                                sample.volcanoRadialStructure()
                                        * 255.0
                        );

                int craterGray =
                        clamp255(
                                sample.volcanoCraterMask()
                                        * 255.0
                        );

                reliefImage.setRGB(
                        x,
                        z,
                        (reliefGray << 16)
                                | (reliefGray << 8)
                                | reliefGray
                );

                structuralRidgeImage.setRGB(
                        x,
                        z,
                        (structuralRidgeGray << 16)
                                | (structuralRidgeGray << 8)
                                | structuralRidgeGray
                );

                radialImage.setRGB(
                        x,
                        z,
                        (radialGray << 16)
                                | (radialGray << 8)
                                | radialGray
                );

                craterImage.setRGB(
                        x,
                        z,
                        (craterGray << 16)
                                | (craterGray << 8)
                                | craterGray
                );
            }
        }

        ImageIO.write(
                reliefImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-detail-relief.png"
                ).toFile()
        );

        ImageIO.write(
                structuralRidgeImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-detail-structural-ridges.png"
                ).toFile()
        );

        ImageIO.write(
                radialImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-detail-radial.png"
                ).toFile()
        );

        ImageIO.write(
                craterImage,
                "PNG",
                directory.resolve(
                        "cascade-volcano-detail-crater.png"
                ).toFile()
        );
    }

    private static SpineFrame frameAtArc(
            MountainSpine spine,
            double arcPosition
    ) {
        List<GeoPoint> points =
                spine.points();

        double target =
                Math.max(
                        0.0,
                        Math.min(
                                spine.totalLength(),
                                arcPosition
                        )
                );

        double running =
                0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            GeoPoint a =
                    points.get(i);

            GeoPoint b =
                    points.get(i + 1);

            double dx =
                    b.x() - a.x();

            double dz =
                    b.z() - a.z();

            double length =
                    Math.hypot(dx, dz);

            if (length <= 1.0e-12) {
                continue;
            }

            if (running + length >= target || i == points.size() - 2) {
                double t =
                        Math.max(
                                0.0,
                                Math.min(
                                        1.0,
                                        (target - running) / length
                                )
                        );

                return new SpineFrame(
                        lerp(a.x(), b.x(), t),
                        lerp(a.z(), b.z(), t),
                        dx / length,
                        dz / length
                );
            }

            running +=
                    length;
        }

        GeoPoint last =
                points.getLast();

        return new SpineFrame(
                last.x(),
                last.z(),
                0.0,
                1.0
        );
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a + (b - a) * t;
    }

    private record SpineFrame(
            double x,
            double z,
            double tangentX,
            double tangentZ
    ) {
    }

    private static void writeTerrainProvinces(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );


        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {

                TerrainProvince province =
                        world.terrainProvince(
                                x,
                                z
                        );


                int rgb =
                        switch (province) {

                            case OCEAN ->
                                    rgb(
                                            15,
                                            45,
                                            90
                                    );

                            case COASTAL ->
                                    rgb(
                                            210,
                                            190,
                                            120
                                    );

                            case COAST_RANGE ->
                                    rgb(
                                            50,
                                            105,
                                            60
                                    );

                            case WESTERN_LOWLAND ->
                                    rgb(
                                            100,
                                            165,
                                            90
                                    );

                            case CASCADE_FOOTHILLS ->
                                    rgb(
                                            145,
                                            150,
                                            90
                                    );

                            case CASCADE_CORE ->
                                    rgb(
                                            235,
                                            235,
                                            235
                                    );

                            case EASTERN_SLOPES ->
                                    rgb(
                                            190,
                                            140,
                                            70
                                    );

                            case INTERIOR_PLATEAU ->
                                    rgb(
                                            150,
                                            105,
                                            60
                                    );
                        };


                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }


        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeScalarField(
            WorldBlueprint world,
            Path path,
            double min,
            double max,
            CellValue field
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );


        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {

                double value =
                        field.sample(
                                x,
                                z
                        );


                double normalized =
                        (
                                value - min
                        )
                                / (
                                max - min
                        );


                int gray =
                        clamp255(
                                normalized
                                        * 255.0
                        );


                int rgb =
                        (gray << 16)
                                | (gray << 8)
                                | gray;


                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }


        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeSlope(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );


        double cellSize =
                world.config()
                        .blocksPerCell();


        for (int z = 1; z < size - 1; z++) {

            for (int x = 1; x < size - 1; x++) {

                double dx =
                        (
                                world.elevation(
                                        x + 1,
                                        z
                                )
                                        - world.elevation(
                                        x - 1,
                                        z
                                )
                        )
                                / (
                                2.0 * cellSize
                        );


                double dz =
                        (
                                world.elevation(
                                        x,
                                        z + 1
                                )
                                        - world.elevation(
                                        x,
                                        z - 1
                                )
                        )
                                / (
                                2.0 * cellSize
                        );


                double gradient =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );


                double slopeDegrees =
                        Math.toDegrees(
                                Math.atan(
                                        gradient
                                )
                        );


                int gray =
                        clamp255(
                                Math.min(
                                        1.0,
                                        slopeDegrees
                                                / 45.0
                                ) * 255.0
                        );


                int rgb =
                        (gray << 16)
                                | (gray << 8)
                                | gray;


                image.setRGB(
                        x,
                        z,
                        rgb
                );
            }
        }


        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyFlowDirection(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.flowDirection(
                                x,
                                z
                        );

                int color =
                        hydrologyFlowColor(
                                direction
                        );

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologySinks(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.flowDirection(
                                x,
                                z
                        );

                int color =
                        switch (direction) {
                            case SINK -> rgb(255, 45, 45);
                            case OUTLET -> rgb(35, 220, 235);
                            default -> rgb(0, 0, 0);
                        };

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyDepressions(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int id =
                        hydrology.depressionId(
                                x,
                                z
                        );

                if (id < 0) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                float hue =
                        (float) ((id * 0.6180339887498949) % 1.0);

                int color =
                        Color.HSBtoRGB(
                                hue,
                                0.72f,
                                0.95f
                        ) & 0x00FFFFFF;

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyDepressionDepth(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        double maximumDepth =
                Math.max(
                        hydrology.maximumDepressionDepth(),
                        1.0e-9
                );

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double depth =
                        hydrology.depressionDepth(
                                x,
                                z
                        );

                if (depth <= 0.0) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double normalized =
                        Math.sqrt(
                                Math.min(
                                        1.0,
                                        depth / maximumDepth
                                )
                        );

                int intensity =
                        clamp255(
                                (int) Math.round(
                                        normalized * 255.0
                                )
                        );

                int color =
                        rgb(
                                intensity / 5,
                                (int) Math.round(intensity * 0.68),
                                intensity
                        );

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologySpillPoints(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(
                        x,
                        z,
                        hydrology.depressionId(x, z) >= 0
                                ? rgb(45, 45, 45)
                                : rgb(0, 0, 0)
                );
            }
        }

        for (Depression depression : hydrology.depressions()) {
            drawMarker(
                    image,
                    depression.sinkX(),
                    depression.sinkZ(),
                    rgb(255, 55, 55)
            );

            if (!depression.hasMeasuredSpill()) {
                continue;
            }

            drawMarker(
                    image,
                    depression.spillX(),
                    depression.spillZ(),
                    rgb(255, 220, 55)
            );

            drawMarker(
                    image,
                    depression.spillTargetX(),
                    depression.spillTargetZ(),
                    rgb(40, 220, 240)
            );
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyDepressionClasses(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int id =
                        hydrology.depressionId(
                                x,
                                z
                        );

                DepressionClassification classification =
                        hydrology.depressionClassification(id);

                if (classification == null) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                int color =
                        switch (classification.depressionClass()) {
                            case MICRO_PIT -> rgb(245, 65, 65);
                            case SHALLOW_BASIN -> rgb(245, 205, 55);
                            case ALPINE_BASIN -> rgb(55, 220, 235);
                            case MAJOR_INTERIOR_BASIN -> rgb(65, 105, 245);
                            case COMPOUND_BASIN -> rgb(230, 65, 220);
                        };

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyDepressionChains(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(
                        x,
                        z,
                        hydrology.depressionId(x, z) >= 0
                                ? rgb(28, 28, 28)
                                : rgb(0, 0, 0)
                );
            }
        }

        List<Depression> depressions =
                hydrology.depressions();

        for (Depression depression : depressions) {
            DepressionClassification classification =
                    hydrology.depressionClassification(
                            depression.id()
                    );

            if (classification == null) {
                continue;
            }

            int targetId =
                    classification.downstreamDepressionId();

            if (
                    targetId >= 0
                            && targetId < depressions.size()
            ) {
                Depression target =
                        depressions.get(targetId);

                drawLine(
                        image,
                        depression.sinkX(),
                        depression.sinkZ(),
                        target.sinkX(),
                        target.sinkZ(),
                        classification.belongsToCompoundGroup()
                                ? rgb(155, 40, 150)
                                : rgb(75, 90, 120)
                );
            }
        }

        for (Depression depression : depressions) {
            DepressionClassification classification =
                    hydrology.depressionClassification(
                            depression.id()
                    );

            if (classification == null) {
                continue;
            }

            int markerColor;

            if (classification.belongsToCompoundGroup()) {
                markerColor =
                        rgb(245, 70, 230);
            } else if (classification.drainsDirectlyOutsideDepressionSystem()) {
                markerColor =
                        rgb(45, 225, 235);
            } else {
                int intensity =
                        Math.min(
                                255,
                                105 + classification.chainDepth() * 28
                        );

                markerColor =
                        rgb(
                                intensity,
                                Math.max(75, 235 - classification.chainDepth() * 24),
                                55
                        );
            }

            drawMarker(
                    image,
                    depression.sinkX(),
                    depression.sinkZ(),
                    markerColor
            );
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyCompoundBasins(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int rawDepressionId =
                        hydrology.rawCatchmentDepressionId(
                                x,
                                z
                        );

                DepressionClassification classification =
                        hydrology.depressionClassification(
                                rawDepressionId
                        );

                if (
                        classification == null
                                || !classification.belongsToCompoundGroup()
                ) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                int groupId =
                        classification.compoundGroupId();

                int baseColor =
                        compoundGroupColor(
                                groupId
                        );

                int footprintId =
                        hydrology.depressionId(
                                x,
                                z
                        );

                DepressionClassification footprintClassification =
                        hydrology.depressionClassification(
                                footprintId
                        );

                boolean inMeasuredFootprint =
                        footprintClassification != null
                                && footprintClassification.compoundGroupId()
                                == groupId;

                image.setRGB(
                        x,
                        z,
                        inMeasuredFootprint
                                ? baseColor
                                : darkenRgb(baseColor, 0.30)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyCompoundOutlets(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int rawDepressionId =
                        hydrology.rawCatchmentDepressionId(
                                x,
                                z
                        );

                DepressionClassification classification =
                        hydrology.depressionClassification(
                                rawDepressionId
                        );

                image.setRGB(
                        x,
                        z,
                        classification != null
                                && classification.belongsToCompoundGroup()
                                ? rgb(30, 24, 34)
                                : rgb(0, 0, 0)
                );
            }
        }

        List<Depression> depressions =
                hydrology.depressions();

        for (CompoundBasin basin : hydrology.compoundBasins()) {
            if (basin.memberDepressionIds().isEmpty()) {
                continue;
            }

            int representativeId =
                    basin.memberDepressionIds().getFirst();

            if (
                    representativeId >= 0
                            && representativeId < depressions.size()
                            && basin.hasExternalSpill()
            ) {
                Depression representative =
                        depressions.get(
                                representativeId
                        );

                drawLine(
                        image,
                        representative.sinkX(),
                        representative.sinkZ(),
                        basin.spillX(),
                        basin.spillZ(),
                        compoundGroupColor(
                                basin.groupId()
                        )
                );
            }

            for (int memberId : basin.memberDepressionIds()) {
                if (memberId < 0 || memberId >= depressions.size()) {
                    continue;
                }

                Depression member =
                        depressions.get(memberId);

                drawMarker(
                        image,
                        member.sinkX(),
                        member.sinkZ(),
                        rgb(235, 75, 225)
                );
            }

            if (!basin.hasExternalSpill()) {
                continue;
            }

            drawMarker(
                    image,
                    basin.spillX(),
                    basin.spillZ(),
                    rgb(255, 235, 70)
            );

            if (
                    basin.spillTargetX() >= 0
                            && basin.spillTargetZ() >= 0
            ) {
                drawMarker(
                        image,
                        basin.spillTargetX(),
                        basin.spillTargetZ(),
                        basin.drainsDirectlyOutsideDepressionSystem()
                                ? rgb(65, 235, 245)
                                : rgb(245, 245, 245)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static int compoundGroupColor(
            int groupId
    ) {
        float hue =
                (float) ((groupId * 0.6180339887498949) % 1.0);

        return Color.HSBtoRGB(
                hue,
                0.76f,
                0.96f
        ) & 0x00FFFFFF;
    }


    private static int darkenRgb(
            int color,
            double factor
    ) {
        int red =
                (color >> 16) & 0xFF;

        int green =
                (color >> 8) & 0xFF;

        int blue =
                color & 0xFF;

        return rgb(
                (int) Math.round(red * factor),
                (int) Math.round(green * factor),
                (int) Math.round(blue * factor)
        );
    }


    private static void writeHydrologyLakes(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int lakeId =
                        hydrology.lakeId(
                                x,
                                z
                        );

                if (lakeId < 0) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                Lake lake =
                        hydrology.lake(
                                lakeId
                        );

                if (lake == null) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double depthFraction =
                        lake.maximumDepth() > 0.0f
                                ? clamp01(
                                hydrology.lakeDepth(x, z)
                                        / lake.maximumDepth()
                        )
                                : 0.0;

                int baseColor =
                        lakeColor(
                                lake
                        );

                image.setRGB(
                        x,
                        z,
                        darkenRgb(
                                baseColor,
                                0.48
                                        + 0.52
                                        * Math.sqrt(depthFraction)
                        )
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyLakeDepth(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        float maximumDepth =
                Math.max(
                        hydrology.maximumLakeDepth(),
                        1.0e-6f
                );

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                float depth =
                        hydrology.lakeDepth(
                                x,
                                z
                        );

                if (depth <= 0.0f) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double normalized =
                        Math.sqrt(
                                clamp01(
                                        depth / maximumDepth
                                )
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        18.0
                                                + normalized * 65.0
                                ),
                                clamp255(
                                        55.0
                                                + normalized * 150.0
                                ),
                                clamp255(
                                        105.0
                                                + normalized * 150.0
                                )
                        )
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyLakeOutlets(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int lakeId =
                        hydrology.lakeId(
                                x,
                                z
                        );

                Lake lake =
                        hydrology.lake(
                                lakeId
                        );

                image.setRGB(
                        x,
                        z,
                        lake != null
                                ? darkenRgb(
                                lakeColor(lake),
                                0.25
                        )
                                : rgb(0, 0, 0)
                );
            }
        }

        for (Lake lake : hydrology.lakes()) {
            if (!lake.hasOutlet()) {
                continue;
            }

            if (
                    lake.outletTargetX() >= 0
                            && lake.outletTargetZ() >= 0
            ) {
                drawLine(
                        image,
                        lake.outletX(),
                        lake.outletZ(),
                        lake.outletTargetX(),
                        lake.outletTargetZ(),
                        lake.drainsToAnotherLake()
                                ? rgb(245, 245, 245)
                                : lake.drainsToOpenBoundary()
                                ? rgb(65, 235, 245)
                                : rgb(255, 150, 55)
                );
            }

            drawMarker(
                    image,
                    lake.outletX(),
                    lake.outletZ(),
                    rgb(255, 235, 70)
            );

            if (
                    lake.outletTargetX() >= 0
                            && lake.outletTargetZ() >= 0
            ) {
                drawMarker(
                        image,
                        lake.outletTargetX(),
                        lake.outletTargetZ(),
                        lake.drainsToAnotherLake()
                                ? rgb(245, 245, 245)
                                : lake.drainsToOpenBoundary()
                                ? rgb(65, 235, 245)
                                : rgb(255, 150, 55)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static int lakeColor(
            Lake lake
    ) {
        double golden =
                (lake.id() * 0.6180339887498949) % 1.0;

        float hue =
                lake.sourceType()
                        == LakeSourceType.SIMPLE_DEPRESSION
                        ? (float) (0.54 + golden * 0.08)
                        : (float) (0.69 + golden * 0.10);

        return Color.HSBtoRGB(
                hue,
                0.74f,
                0.96f
        ) & 0x00FFFFFF;
    }


    private static void writeHydrologyDepressionResolution(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int id =
                        hydrology.depressionId(
                                x,
                                z
                        );

                DepressionResolutionPlan plan =
                        hydrology.depressionResolutionPlan(
                                id
                        );

                if (plan == null) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                int color =
                        switch (plan.action()) {
                            case FILL -> rgb(235, 80, 45);
                            case BREACH -> rgb(245, 205, 55);
                            case PRESERVE_LAKE -> rgb(45, 145, 245);
                            case MERGE_COMPOUND -> rgb(225, 65, 220);
                        };

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        /*
         * Mark breach spill saddles so the diagnostic communicates both the
         * proposed action and where a later cut would leave the basin.
         */
        for (Depression depression : hydrology.depressions()) {
            DepressionResolutionPlan plan =
                    hydrology.depressionResolutionPlan(
                            depression.id()
                    );

            if (
                    plan == null
                            || plan.action() != DepressionResolutionAction.BREACH
                            || !depression.hasMeasuredSpill()
            ) {
                continue;
            }

            drawMarker(
                    image,
                    depression.spillX(),
                    depression.spillZ(),
                    rgb(255, 255, 255)
            );
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyConditionedElevation(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        double minimum = -200.0;
        double maximum = 350.0;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double normalized =
                        (hydrology.conditionedElevation(x, z) - minimum)
                                / (maximum - minimum);

                int value =
                        clamp255(
                                normalized * 255.0
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(value, value, value)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyFillDelta(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        double maximumDelta =
                Math.max(
                        0.25,
                        hydrology.maximumFillDelta()
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double delta =
                        hydrology.fillDelta(
                                x,
                                z
                        );

                if (delta <= 0.0) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double normalized =
                        Math.min(
                                1.0,
                                delta / maximumDelta
                        );

                int red =
                        clamp255(
                                90.0 + normalized * 165.0
                        );

                int green =
                        clamp255(
                                35.0 + normalized * 175.0
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(red, green, 35)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyBreachPaths(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (!hydrology.breachPath(x, z)) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double cut =
                        hydrology.breachCutDepth(
                                x,
                                z
                        );

                image.setRGB(
                        x,
                        z,
                        cut > 0.0
                                ? rgb(255, 195, 40)
                                : rgb(45, 220, 235)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyBreachDelta(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        double maximumDelta =
                Math.max(
                        0.25,
                        hydrology.maximumBreachCutDepth()
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double delta =
                        hydrology.breachCutDepth(
                                x,
                                z
                        );

                if (delta <= 0.0) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double normalized =
                        Math.min(
                                1.0,
                                delta / maximumDelta
                        );

                int red =
                        clamp255(
                                70.0 + normalized * 185.0
                        );

                int green =
                        clamp255(
                                35.0 + normalized * 90.0
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(red, green, 25)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyConditionedFlowDirection(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(
                        x,
                        z,
                        hydrologyFlowColor(
                                hydrology.conditionedFlowDirection(
                                        x,
                                        z
                                )
                        )
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyConditionedSinks(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.conditionedFlowDirection(
                                x,
                                z
                        );

                int color =
                        switch (direction) {
                            case SINK -> rgb(255, 45, 45);
                            case OUTLET -> rgb(35, 220, 235);
                            default -> rgb(0, 0, 0);
                        };

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyFinalFlowDirection(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(
                        x,
                        z,
                        hydrologyFlowColor(
                                hydrology.routedFlowDirection(
                                        x,
                                        z
                                )
                        )
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyLakeRouting(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                HydrologyRouteType type =
                        hydrology.routeType(
                                x,
                                z
                        );

                int color =
                        switch (type) {
                            case ORIGINAL -> rgb(0, 0, 0);
                            case LAKE_INTERIOR -> rgb(45, 145, 245);
                            case LAKE_OUTLET -> rgb(255, 225, 65);
                            case COMPOUND_ESCAPE -> rgb(225, 70, 230);
                            case CYCLE_ESCAPE -> rgb(255, 135, 35);
                            case RESIDUAL_ESCAPE -> rgb(70, 235, 110);
                        };

                if (hydrology.routedCycle(x, z)) {
                    color = rgb(255, 40, 40);
                }

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyResidualRouting(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int color =
                        rgb(0, 0, 0);

                if (
                        hydrology.routeType(x, z)
                                == HydrologyRouteType.RESIDUAL_ESCAPE
                ) {
                    color = rgb(70, 235, 110);
                }

                if (
                        hydrology.routedFlowDirection(x, z)
                                == FlowDirection.SINK
                ) {
                    color = rgb(255, 45, 45);
                }

                if (hydrology.routedCycle(x, z)) {
                    color = rgb(235, 55, 235);
                }

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyFinalSinks(
            WorldBlueprint world,
            Path path
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                FlowDirection direction =
                        hydrology.routedFlowDirection(
                                x,
                                z
                        );

                int color =
                        switch (direction) {
                            case SINK -> rgb(255, 45, 45);
                            case OUTLET -> rgb(35, 220, 235);
                            default -> rgb(0, 0, 0);
                        };

                if (hydrology.routedCycle(x, z)) {
                    color = rgb(235, 55, 235);
                }

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static int hydrologyFlowColor(
            FlowDirection direction
    ) {
        return switch (direction) {
            case OCEAN -> rgb(0, 0, 0);
            case SINK -> rgb(255, 255, 255);
            case OUTLET -> rgb(150, 150, 150);

            case NORTH -> rgb(230, 45, 45);
            case NORTH_EAST -> rgb(235, 135, 40);
            case EAST -> rgb(220, 210, 45);
            case SOUTH_EAST -> rgb(70, 190, 75);
            case SOUTH -> rgb(45, 195, 195);
            case SOUTH_WEST -> rgb(55, 105, 225);
            case WEST -> rgb(135, 70, 215);
            case NORTH_WEST -> rgb(220, 60, 175);
        };
    }


    private static void drawLine(
            BufferedImage image,
            int x0,
            int z0,
            int x1,
            int z1,
            int color
    ) {
        int dx =
                Math.abs(x1 - x0);

        int dz =
                Math.abs(z1 - z0);

        int stepX =
                x0 < x1
                        ? 1
                        : -1;

        int stepZ =
                z0 < z1
                        ? 1
                        : -1;

        int error =
                dx - dz;

        int x =
                x0;

        int z =
                z0;

        while (true) {
            if (
                    x >= 0
                            && x < image.getWidth()
                            && z >= 0
                            && z < image.getHeight()
            ) {
                image.setRGB(
                        x,
                        z,
                        color
                );
            }

            if (x == x1 && z == z1) {
                break;
            }

            int doubledError =
                    error * 2;

            if (doubledError > -dz) {
                error -= dz;
                x += stepX;
            }

            if (doubledError < dx) {
                error += dx;
                z += stepZ;
            }
        }
    }


    private static void drawMarker(
            BufferedImage image,
            int centerX,
            int centerZ,
            int color
    ) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = centerX + dx;
                int z = centerZ + dz;

                if (
                        x < 0
                                || x >= image.getWidth()
                                || z < 0
                                || z >= image.getHeight()
                ) {
                    continue;
                }

                image.setRGB(
                        x,
                        z,
                        color
                );
            }
        }
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


    private static int rgb(
            int r,
            int g,
            int b
    ) {

        return (
                clamp255(r) << 16
        )
                | (
                clamp255(g) << 8
        )
                | clamp255(b);
    }


    @FunctionalInterface
    private interface CellValue {

        double sample(
                int x,
                int z
        );
    }
}

