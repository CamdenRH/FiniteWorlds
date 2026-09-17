package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.generator.CascadiaGenerator;

import java.nio.file.Path;

public final class PreviewMain {

    public static void main(String[] args)
            throws Exception {

        long seed = 12345L;
        String type = "cascadia";

        for (int i = 0; i < args.length; i++) {

            if ("--seed".equals(args[i])) {
                seed =
                        Long.parseLong(
                                args[++i]
                        );
            }

            if ("--type".equals(args[i])) {
                type =
                        args[++i];
            }
        }

        if (!type.equalsIgnoreCase("cascadia")) {
            throw new IllegalArgumentException(
                    "Unknown world type: " + type
            );
        }

        WorldConfig config =
                WorldConfig.quickTest();

        CascadiaGenerator generator =
                new CascadiaGenerator();

        System.out.println(
                "Generating world blueprint..."
        );

        WorldBlueprint blueprint =
                generator.generate(
                        seed,
                        config
                );

        Path output =
                Path.of(
                        "preview",
                        Long.toString(seed)
                );

        PreviewWriter.writeAll(
                blueprint,
                seed,
                output
        );

        System.out.println(
                "Preview written to: "
                        + output.toAbsolutePath()
        );
    }
}