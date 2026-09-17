package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PreviewWriter {

    private PreviewWriter() {}

    public static void writeAll(
            WorldBlueprint world,
            long seed,
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
}