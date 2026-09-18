package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.generator.CascadiaGenerator;
import dev.lemma.finiteworlds.core.geography.ContinentPlan;
import dev.lemma.finiteworlds.core.hydrology.DepressionClass;

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

        ContinentPlan plan =
                generator.createPlan(
                        seed
                );

        WorldBlueprint blueprint =
                generator.generate(
                        seed,
                        config
                );

        System.out.println(
                "Interior hydrology sinks: "
                        + blueprint.hydrology().sinkCount()
        );

        System.out.println(
                "Measured depressions: "
                        + blueprint.hydrology().depressionCount()
        );

        System.out.println(
                "Depression footprint cells: "
                        + blueprint.hydrology().depressionCellCount()
        );

        System.out.printf(
                "Deepest raw depression: %.3f elevation units%n",
                blueprint.hydrology().maximumDepressionDepth()
        );

        System.out.println(
                "Compound depression groups: "
                        + blueprint.hydrology().compoundGroupCount()
        );

        for (DepressionClass depressionClass : DepressionClass.values()) {
            System.out.println(
                    "  "
                            + depressionClass
                            + ": "
                            + blueprint.hydrology()
                            .countDepressionsByClass(
                                    depressionClass
                            )
            );
        }

        Path output =
                Path.of(
                        "preview",
                        Long.toString(seed)
                );

        PreviewWriter.writeAll(
                blueprint,
                seed,
                plan,
                output
        );

        System.out.println(
                "Preview written to: "
                        + output.toAbsolutePath()
        );

    }
}