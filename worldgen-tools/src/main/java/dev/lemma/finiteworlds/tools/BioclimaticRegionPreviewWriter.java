package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.climate.BioclimaticRegion;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Diagnostic previews for Climate Pass 3F.
 */
public final class BioclimaticRegionPreviewWriter {

    private BioclimaticRegionPreviewWriter() {
    }

    public static void writeAll(
            WorldBlueprint world,
            Path directory
    ) throws IOException {
        writeRegions(
                world,
                directory.resolve(
                        "climate-bioclimatic-regions.png"
                ),
                false
        );

        writeRegions(
                world,
                directory.resolve(
                        "climate-bioclimatic-regions-relief.png"
                ),
                true
        );
    }

    private static void writeRegions(
            WorldBlueprint world,
            Path path,
            boolean relief
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
                BioclimaticRegion region =
                        world.climate()
                                .bioclimaticRegion(
                                        x,
                                        z
                                );

                int color =
                        regionColor(
                                region
                        );

                if (
                        !relief
                                || region == BioclimaticRegion.OCEAN
                ) {
                    image.setRGB(
                            x,
                            z,
                            color
                    );

                    continue;
                }

                int west =
                        Math.max(
                                0,
                                x - 1
                        );

                int east =
                        Math.min(
                                size - 1,
                                x + 1
                        );

                int north =
                        Math.max(
                                0,
                                z - 1
                        );

                int south =
                        Math.min(
                                size - 1,
                                z + 1
                        );

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                double shade =
                        Math.max(
                                0.62,
                                Math.min(
                                        1.18,
                                        0.88
                                                - dx * 0.0018
                                                - dz * 0.0018
                                )
                        );

                int red =
                        (color >> 16) & 0xFF;

                int green =
                        (color >> 8) & 0xFF;

                int blue =
                        color & 0xFF;

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(red * shade),
                                clamp255(green * shade),
                                clamp255(blue * shade)
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

    private static int regionColor(
            BioclimaticRegion region
    ) {
        return switch (region) {
            case OCEAN ->
                    rgb(22, 48, 77);

            case PERMANENT_SNOW ->
                    rgb(245, 249, 252);

            case ALPINE_TUNDRA ->
                    rgb(184, 201, 199);

            case SUBALPINE_FOREST ->
                    rgb(71, 125, 104);

            case MONTANE_FOREST ->
                    rgb(45, 104, 70);

            case TEMPERATE_RAINFOREST ->
                    rgb(21, 92, 54);

            case TEMPERATE_FOREST ->
                    rgb(76, 137, 68);

            case DRY_FOREST ->
                    rgb(128, 145, 67);

            case SHRUB_STEPPE ->
                    rgb(183, 157, 89);

            case COLD_STEPPE ->
                    rgb(133, 148, 114);
        };
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
}
