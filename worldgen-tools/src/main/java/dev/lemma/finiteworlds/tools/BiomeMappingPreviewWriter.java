package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import dev.lemma.finiteworlds.core.biome.BiomeIntentPlan;
import dev.lemma.finiteworlds.core.biome.BiomeIntentPlanner;
import dev.lemma.finiteworlds.core.biome.BiomeTarget;
import dev.lemma.finiteworlds.core.biome.BiomeTargetResolver;
import dev.lemma.finiteworlds.core.geography.CoastalMorphologyPlanner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Diagnostic writer for Climate Pass 3G.
 *
 * Outputs:
 *  - biome-intent.png
 *  - biome-intent-relief.png
 *  - biome-provider.png
 *  - biome-coastal-source.png
 *  - biome-targets.csv
 */
public final class BiomeMappingPreviewWriter {

    private BiomeMappingPreviewWriter() {
    }

    public static void writeAll(
            WorldBlueprint world,
            Path directory
    ) throws IOException {
        BiomeIntentPlan plan =
                BiomeIntentPlanner.plan(
                        world
                );

        writeIntent(
                world,
                plan,
                directory.resolve(
                        "biome-intent.png"
                ),
                false
        );

        writeIntent(
                world,
                plan,
                directory.resolve(
                        "biome-intent-relief.png"
                ),
                true
        );

        writeProvider(
                plan,
                directory.resolve(
                        "biome-provider.png"
                )
        );

        writeCoastalSource(
                world,
                directory.resolve(
                        "biome-coastal-source.png"
                )
        );

        writeTargetsCsv(
                plan,
                directory.resolve(
                        "biome-targets.csv"
                )
        );
    }

    private static void writeIntent(
            WorldBlueprint world,
            BiomeIntentPlan plan,
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
                int color =
                        intentColor(
                                plan.intent(
                                        x,
                                        z
                                )
                        );

                if (
                        !relief
                                || world.landMask(x, z) < 0.5f
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
                        clamp(
                                0.90
                                        - dx * 0.0017
                                        - dz * 0.0017,
                                0.58,
                                1.18
                        );

                image.setRGB(
                        x,
                        z,
                        shade(
                                color,
                                shade
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

    private static void writeProvider(
            BiomeIntentPlan plan,
            Path path
    ) throws IOException {
        int size =
                plan.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                BiomeTarget target =
                        BiomeTargetResolver.target(
                                plan.intent(
                                        x,
                                        z
                                )
                        );

                int color =
                        target.prefersTerralith()
                                ? rgb(
                                94,
                                167,
                                109
                        )
                                : rgb(
                                97,
                                126,
                                184
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

    private static void writeCoastalSource(
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
                            rgb(18, 37, 55)
                    );
                    continue;
                }

                double beach =
                        clamp(
                                CoastalMorphologyPlanner.beachStrength(
                                        world,
                                        x,
                                        z
                                ),
                                0.0,
                                1.0
                        );

                double headland =
                        clamp(
                                CoastalMorphologyPlanner.headlandStrength(
                                        world,
                                        x,
                                        z
                                ),
                                0.0,
                                1.0
                        );

                if (
                        beach <= 1.0e-5
                                && headland <= 1.0e-5
                ) {
                    image.setRGB(
                            x,
                            z,
                            rgb(28, 32, 30)
                    );
                    continue;
                }

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        52.0
                                                + 203.0 * beach
                                                + 70.0 * headland
                                ),
                                clamp255(
                                        48.0
                                                + 166.0 * beach
                                                + 44.0 * headland
                                ),
                                clamp255(
                                        43.0
                                                + 72.0 * beach
                                                + 92.0 * headland
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


    private static void writeTargetsCsv(
            BiomeIntentPlan plan,
            Path path
    ) throws IOException {
        StringBuilder csv =
                new StringBuilder();

        csv.append(
                "intent,count,preferred_id,vanilla_fallback_id\n"
        );

        for (BiomeIntent intent : BiomeIntent.values()) {
            BiomeTarget target =
                    BiomeTargetResolver.target(
                            intent
                    );

            csv.append(
                    intent.name()
            );

            csv.append(',');
            csv.append(
                    plan.count(intent)
            );

            csv.append(',');
            csv.append(
                    target.preferredId()
            );

            csv.append(',');
            csv.append(
                    target.vanillaFallbackId()
            );

            csv.append('\n');
        }

        Files.writeString(
                path,
                csv.toString()
        );
    }

    private static int intentColor(
            BiomeIntent intent
    ) {
        return switch (intent) {
            case OCEAN ->
                    rgb(24, 55, 84);

            case RIVER ->
                    rgb(38, 111, 168);

            case ESTUARY ->
                    rgb(63, 139, 154);

            case SANDY_BEACH ->
                    rgb(222, 204, 146);

            case GRAVEL_BEACH ->
                    rgb(146, 143, 132);

            case COLD_BEACH ->
                    rgb(207, 215, 216);

            case ROCKY_COAST ->
                    rgb(103, 108, 109);

            case PERMANENT_SNOWFIELD ->
                    rgb(242, 247, 250);

            case FROZEN_CLIFFS ->
                    rgb(186, 207, 218);

            case ALPINE_HIGHLANDS ->
                    rgb(175, 190, 175);

            case ROCKY_ALPINE ->
                    rgb(133, 139, 135);

            case SUBALPINE_GROVE ->
                    rgb(69, 118, 92);

            case MONTANE_FOREST ->
                    rgb(48, 102, 68);

            case WET_HIGHLAND_FOREST ->
                    rgb(29, 89, 61);

            case TEMPERATE_RAINFOREST ->
                    rgb(22, 92, 53);

            case TEMPERATE_FOREST ->
                    rgb(77, 136, 70);

            case DRY_FOREST ->
                    rgb(127, 143, 70);

            case SHRUB_STEPPE ->
                    rgb(183, 157, 89);

            case COLD_STEPPE ->
                    rgb(133, 148, 114);
        };
    }

    private static int shade(
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
                clamp255(
                        red * factor
                ),
                clamp255(
                        green * factor
                ),
                clamp255(
                        blue * factor
                )
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
                clamp(
                        value,
                        0.0,
                        255.0
                )
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
}
