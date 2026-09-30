package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Diagnostic previews for Climate Pass 3E.
 *
 * Kept separate from PreviewWriter so Phase-3 climate iteration does not
 * create large, fragile patch hunks in the main preview class.
 */
public final class RunoffDischargePreviewWriter {

    private RunoffDischargePreviewWriter() {
    }

    public static void writeAll(
            WorldBlueprint world,
            Path directory
    ) throws IOException {
        writeRunoffPotential(
                world,
                directory.resolve(
                        "climate-runoff-potential.png"
                )
        );

        writeSnowStorage(
                world,
                directory.resolve(
                        "climate-snow-storage.png"
                )
        );

        writeEffectiveDischarge(
                world,
                directory.resolve(
                        "hydrology-effective-discharge-log.png"
                )
        );

        writeSpecificDischarge(
                world,
                directory.resolve(
                        "hydrology-specific-discharge.png"
                )
        );
    }

    private static void writeRunoffPotential(
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
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(
                            x,
                            z,
                            rgb(17, 38, 58)
                    );
                    continue;
                }

                double runoff =
                        world.climate()
                                .runoffPotential(
                                        x,
                                        z
                                );

                double normalized =
                        clamp01(
                                runoff / 0.30
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        126.0
                                                - 91.0 * normalized
                                ),
                                clamp255(
                                        91.0
                                                + 127.0 * normalized
                                ),
                                clamp255(
                                        48.0
                                                + 158.0 * normalized
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

    private static void writeSnowStorage(
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
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(
                            x,
                            z,
                            rgb(14, 29, 45)
                    );
                    continue;
                }

                double snow =
                        clamp01(
                                world.climate()
                                        .snowStorageFraction(
                                                x,
                                                z
                                        )
                        );

                /*
                 * Gamma-expand moderate seasonal storage so the alpine belt is
                 * visible without requiring near-1.0 persistent snow values.
                 */
                double visibleSnow =
                        Math.pow(
                                snow,
                                0.55
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        28.0
                                                + 218.0 * visibleSnow
                                ),
                                clamp255(
                                        43.0
                                                + 208.0 * visibleSnow
                                ),
                                clamp255(
                                        62.0
                                                + 193.0 * visibleSnow
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

    private static void writeEffectiveDischarge(
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
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                double discharge =
                        clamp01(
                                world.climate()
                                        .normalizedEffectiveDischarge(
                                                x,
                                                z
                                        )
                        );

                double visible =
                        Math.pow(
                                discharge,
                                1.55
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        7.0
                                                + 46.0 * visible
                                ),
                                clamp255(
                                        12.0
                                                + 174.0 * visible
                                ),
                                clamp255(
                                        18.0
                                                + 237.0 * visible
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

    private static void writeSpecificDischarge(
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
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(
                            x,
                            z,
                            rgb(18, 35, 49)
                    );
                    continue;
                }

                double specific =
                        world.climate()
                                .specificDischarge(
                                        x,
                                        z
                                );

                double normalized =
                        clamp01(
                                specific / 0.30
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        139.0
                                                - 100.0 * normalized
                                ),
                                clamp255(
                                        91.0
                                                + 126.0 * normalized
                                ),
                                clamp255(
                                        42.0
                                                + 175.0 * normalized
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

    private static int rgb(
            int red,
            int green,
            int blue
    ) {
        return (red << 16)
                | (green << 8)
                | blue;
    }

    private static int clamp255(
            double value
    ) {
        return (int) Math.round(
                Math.max(
                        0.0,
                        Math.min(
                                255.0,
                                value
                        )
                )
        );
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
