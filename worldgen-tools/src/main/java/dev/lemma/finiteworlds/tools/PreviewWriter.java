package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.geography.CascadeMorphologySample;
import dev.lemma.finiteworlds.core.geography.CascadeMorphologySampler;
import dev.lemma.finiteworlds.core.geography.CascadeVolcano;
import dev.lemma.finiteworlds.core.geography.ContinentPlan;
import dev.lemma.finiteworlds.core.geography.GeoPoint;
import dev.lemma.finiteworlds.core.geography.MountainSpine;
import dev.lemma.finiteworlds.core.geography.TerrainProvince;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;

import javax.imageio.ImageIO;
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
                        * 1.18;

        BufferedImage reliefImage =
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

