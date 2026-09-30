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
import dev.lemma.finiteworlds.core.hydrology.RiverNode;
import dev.lemma.finiteworlds.core.hydrology.RiverNodeType;
import dev.lemma.finiteworlds.core.hydrology.RiverSegment;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentType;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentHierarchy;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentMagnitude;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentProfile;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentGradePlan;
import dev.lemma.finiteworlds.core.hydrology.RiverCenterlinePoint;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentCenterline;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentValleyCorridor;
import dev.lemma.finiteworlds.core.hydrology.RiverValleyCorridorPoint;
import dev.lemma.finiteworlds.core.hydrology.RiverCrossSectionPoint;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentCrossSection;
import dev.lemma.finiteworlds.core.hydrology.RiverReachType;
import dev.lemma.finiteworlds.core.hydrology.RiverCarvingConstraintPoint;
import dev.lemma.finiteworlds.core.hydrology.RiverSegmentCarvingConstraints;
import dev.lemma.finiteworlds.core.hydrology.RiverScale;
import dev.lemma.finiteworlds.core.hydrology.StreamClass;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

        writeHydrologyFlowAccumulation(
                world,
                directory.resolve(
                        "hydrology-flow-accumulation.png"
                ),
                false
        );

        writeHydrologyFlowAccumulation(
                world,
                directory.resolve(
                        "hydrology-flow-accumulation-log.png"
                ),
                true
        );

        writeHydrologyStreamNetwork(
                world,
                directory.resolve(
                        "hydrology-stream-network.png"
                ),
                false
        );

        writeHydrologyStreamNetwork(
                world,
                directory.resolve(
                        "hydrology-stream-network-relief.png"
                ),
                true
        );

        writeHydrologyStreamInitiationThreshold(
                world,
                directory.resolve(
                        "hydrology-stream-initiation-threshold.png"
                )
        );

        writeHydrologyRiverGraph(
                world,
                directory.resolve(
                        "hydrology-river-graph.png"
                )
        );

        writeHydrologyRiverNodes(
                world,
                directory.resolve(
                        "hydrology-river-nodes.png"
                )
        );

        writeHydrologyRiverStrahlerOrder(
                world,
                directory.resolve(
                        "hydrology-river-strahler-order.png"
                )
        );

        writeHydrologyRiverSegmentHierarchy(
                world,
                directory.resolve(
                        "hydrology-river-segment-hierarchy.png"
                )
        );

        writeHydrologyRiverMagnitude(
                world,
                directory.resolve(
                        "hydrology-river-magnitude.png"
                )
        );

        writeHydrologyRiverWidth(
                world,
                directory.resolve(
                        "hydrology-river-width.png"
                )
        );

        writeHydrologyRiverSlope(
                world,
                directory.resolve(
                        "hydrology-river-slope.png"
                )
        );

        writeHydrologyRiverSmoothedWidth(
                world,
                directory.resolve(
                        "hydrology-river-width-smoothed.png"
                )
        );

        writeHydrologyRiverContinuity(
                world,
                directory.resolve(
                        "hydrology-river-continuity.png"
                )
        );

        writeHydrologyRiverLongitudinalProfiles(
                world,
                directory.resolve(
                        "hydrology-river-longitudinal-profiles.png"
                )
        );

        writeHydrologyRiverPlannedGrade(
                world,
                directory.resolve(
                        "hydrology-river-planned-grade.png"
                )
        );

        writeHydrologyRiverGradeCorrection(
                world,
                directory.resolve(
                        "hydrology-river-grade-correction.png"
                )
        );

        writeHydrologyRiverLongitudinalProfilesCorrected(
                world,
                directory.resolve(
                        "hydrology-river-longitudinal-profiles-corrected.png"
                )
        );

        writeHydrologyRiverCenterlines(
                world,
                directory.resolve(
                        "hydrology-river-centerlines.png"
                ),
                false
        );

        writeHydrologyRiverCenterlines(
                world,
                directory.resolve(
                        "hydrology-river-centerlines-relief.png"
                ),
                true
        );

        writeHydrologyRiverCenterlineOffset(
                world,
                directory.resolve(
                        "hydrology-river-centerline-offset.png"
                )
        );

        writeHydrologyRiverCenterlineDetail(
                world,
                directory.resolve(
                        "hydrology-river-centerline-detail.png"
                )
        );

        writeHydrologyRiverChannelCorridor(
                world,
                directory.resolve(
                        "hydrology-river-channel-corridor.png"
                )
        );

        writeHydrologyRiverValleyCorridor(
                world,
                directory.resolve(
                        "hydrology-river-valley-corridor.png"
                ),
                false
        );

        writeHydrologyRiverValleyCorridor(
                world,
                directory.resolve(
                        "hydrology-river-valley-corridor-relief.png"
                ),
                true
        );

        writeHydrologyRiverConfinement(
                world,
                directory.resolve(
                        "hydrology-river-confinement.png"
                )
        );

        writeHydrologyRiverBankfullChannel(
                world,
                directory.resolve(
                        "hydrology-river-bankfull-channel.png"
                )
        );

        writeHydrologyRiverCrossSectionType(
                world,
                directory.resolve(
                        "hydrology-river-cross-section-type.png"
                )
        );

        writeHydrologyRiverCrossSectionAsymmetry(
                world,
                directory.resolve(
                        "hydrology-river-cross-section-asymmetry.png"
                )
        );

        writeHydrologyRiverCarvingStrength(
                world,
                directory.resolve(
                        "hydrology-river-carving-strength.png"
                )
        );

        writeHydrologyRiverCutBudget(
                world,
                directory.resolve(
                        "hydrology-river-cut-budget.png"
                )
        );

        writeHydrologyRiverFloodplainTarget(
                world,
                directory.resolve(
                        "hydrology-river-floodplain-target.png"
                )
        );

        writeHydrologyRiverTerrainDelta(
                world,
                directory.resolve(
                        "hydrology-river-terrain-delta.png"
                )
        );

        writeHydrologyRiverIntegratedRelief(
                world,
                directory.resolve(
                        "hydrology-river-integrated-relief.png"
                )
        );

        writeHydrologyRiverValleyFloor(
                world,
                directory.resolve(
                        "hydrology-river-valley-floor.png"
                )
        );

        writeHydrologyRiverChannelIncision(
                world,
                directory.resolve(
                        "hydrology-river-channel-incision.png"
                )
        );

        writeHydrologyRiverWaterSurface(
                world,
                directory.resolve(
                        "hydrology-river-water-surface.png"
                )
        );

        writeHydrologyRiverFinalRelief(
                world,
                directory.resolve(
                        "hydrology-river-final-relief.png"
                )
        );

        writeClimateTemperature(
                world,
                directory.resolve(
                        "climate-temperature.png"
                ),
                false
        );

        writeClimateTemperature(
                world,
                directory.resolve(
                        "climate-temperature-relief.png"
                ),
                true
        );

        writeClimateMoistureSource(
                world,
                directory.resolve(
                        "climate-moisture-source.png"
                )
        );

        writeClimateMoistureTransport(
                world,
                directory.resolve(
                        "climate-moisture-transport.png"
                )
        );

        writeClimatePrevailingWind(
                world,
                directory.resolve(
                        "climate-prevailing-wind.png"
                )
        );

        writeClimateOrographicLift(
                world,
                directory.resolve(
                        "climate-orographic-lift.png"
                )
        );

        writeClimatePrecipitation(
                world,
                directory.resolve(
                        "climate-precipitation.png"
                )
        );

        writeClimatePostOrographicMoisture(
                world,
                directory.resolve(
                        "climate-post-orographic-moisture.png"
                )
        );

        writeClimateRainShadow(
                world,
                directory.resolve(
                        "climate-rain-shadow.png"
                )
        );

        writeClimateRefinedPrecipitation(
                world,
                directory.resolve(
                        "climate-refined-precipitation.png"
                )
        );

        writeClimateRefinedMoisture(
                world,
                directory.resolve(
                        "climate-refined-moisture.png"
                )
        );

        RunoffDischargePreviewWriter.writeAll(
                world,
                directory
        );

        BioclimaticRegionPreviewWriter.writeAll(
                world,
                directory
        );

        BiomeMappingPreviewWriter.writeAll(
                world,
                directory
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


    private static void writeHydrologyFlowAccumulation(
            WorldBlueprint world,
            Path path,
            boolean logarithmic
    ) throws IOException {

        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        long maximum =
                Math.max(
                        1L,
                        hydrology.maximumFlowAccumulation()
                );

        double logMaximum =
                Math.log1p(maximum);

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (
                        hydrology.routedFlowDirection(x, z)
                                == FlowDirection.OCEAN
                ) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                long accumulation =
                        hydrology.flowAccumulation(
                                x,
                                z
                        );

                double normalized =
                        logarithmic
                                ? Math.log1p(accumulation) / logMaximum
                                : accumulation / (double) maximum;

                int gray =
                        clamp255(
                                normalized * 255.0
                        );

                image.setRGB(
                        x,
                        z,
                        (gray << 16)
                                | (gray << 8)
                                | gray
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyStreamNetwork(
            WorldBlueprint world,
            Path path,
            boolean reliefBackground
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
                int background =
                        rgb(0, 0, 0);

                if (reliefBackground && world.landMask(x, z) >= 0.5f) {
                    int west = Math.max(0, x - 1);
                    int east = Math.min(size - 1, x + 1);
                    int north = Math.max(0, z - 1);
                    int south = Math.min(size - 1, z + 1);

                    double dx =
                            world.elevation(east, z)
                                    - world.elevation(west, z);

                    double dz =
                            world.elevation(x, south)
                                    - world.elevation(x, north);

                    int shade =
                            clamp255(
                                    (0.33
                                            - dx * 0.0032
                                            - dz * 0.0032)
                                            * 255.0
                            );

                    background =
                            rgb(
                                    shade,
                                    shade,
                                    shade
                            );
                }

                StreamClass streamClass =
                        hydrology.streamClass(
                                x,
                                z
                        );

                int color =
                        streamClass == StreamClass.NONE
                                ? background
                                : streamClassColor(streamClass);

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


    private static void writeHydrologyStreamInitiationThreshold(
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
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(
                            x,
                            z,
                            rgb(0, 0, 0)
                    );
                    continue;
                }

                int threshold =
                        hydrology.streamInitiationThreshold(
                                x,
                                z
                        );

                /*
                 * Lower initiation thresholds are shown brighter because they
                 * represent terrain where channels can begin with smaller
                 * contributing areas.
                 */
                double normalized =
                        1.0
                                - (threshold - 16.0)
                                / (96.0 - 16.0);

                int gray =
                        clamp255(
                                Math.max(
                                        0.0,
                                        Math.min(
                                                1.0,
                                                normalized
                                        )
                                )
                                        * 255.0
                        );

                image.setRGB(
                        x,
                        z,
                        (gray << 16)
                                | (gray << 8)
                                | gray
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }




    private static void writeHydrologyRiverGraph(
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
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                int west = Math.max(0, x - 1);
                int east = Math.min(size - 1, x + 1);
                int north = Math.max(0, z - 1);
                int south = Math.min(size - 1, z + 1);

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                int shade =
                        clamp255(
                                (0.24
                                        - dx * 0.0024
                                        - dz * 0.0024)
                                        * 255.0
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(shade, shade, shade)
                );
            }
        }

        for (RiverSegment segment : hydrology.riverSegments()) {
            int color =
                    segment.type() == RiverSegmentType.LAKE_PASSAGE
                            ? rgb(70, 95, 255)
                            : streamClassColor(
                            segment.maximumStreamClass()
                    );

            for (int cell : segment.cellPath()) {
                int x = cell % size;
                int z = cell / size;

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


    private static void writeHydrologyRiverNodes(
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
                        world.landMask(x, z) >= 0.5f
                                ? rgb(22, 22, 22)
                                : rgb(0, 0, 0)
                );
            }
        }

        for (RiverNode node : hydrology.riverNodes()) {
            drawMarker(
                    image,
                    node.x(),
                    node.z(),
                    riverNodeColor(node.primaryType()),
                    node.primaryType() == RiverNodeType.CONFLUENCE
                            || node.primaryType() == RiverNodeType.MOUTH
                            ? 1
                            : 0
            );
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverStrahlerOrder(
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
                        world.landMask(x, z) >= 0.5f
                                ? rgb(18, 18, 18)
                                : rgb(0, 0, 0)
                );
            }
        }

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentHierarchy hierarchy =
                    hydrology.riverSegmentHierarchy(
                            segment.id()
                    );

            if (hierarchy == null) {
                continue;
            }

            int color =
                    strahlerColor(
                            hierarchy.strahlerOrder()
                    );

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverSegmentHierarchy(
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
                        world.landMask(x, z) >= 0.5f
                                ? rgb(12, 12, 12)
                                : rgb(0, 0, 0)
                );
            }
        }

        int maximumUpstreamSegments =
                0;

        for (RiverSegmentHierarchy hierarchy : hydrology.riverSegmentHierarchy()) {
            maximumUpstreamSegments =
                    Math.max(
                            maximumUpstreamSegments,
                            hierarchy.upstreamSegmentCount()
                    );
        }

        double logarithmicMaximum =
                Math.log1p(
                        Math.max(
                                1,
                                maximumUpstreamSegments
                        )
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentHierarchy hierarchy =
                    hydrology.riverSegmentHierarchy(
                            segment.id()
                    );

            if (hierarchy == null) {
                continue;
            }

            double normalized =
                    Math.log1p(
                            hierarchy.upstreamSegmentCount()
                    )
                            / logarithmicMaximum;

            int red =
                    clamp255(
                            35.0 + normalized * 220.0
                    );

            int green =
                    clamp255(
                            115.0 + normalized * 105.0
                    );

            int blue =
                    clamp255(
                            220.0 - normalized * 180.0
                    );

            int color =
                    rgb(
                            red,
                            green,
                            blue
                    );

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverMagnitude(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        12
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentMagnitude magnitude =
                    hydrology.riverSegmentMagnitude(
                            segment.id()
                    );

            if (magnitude == null) {
                continue;
            }

            double value =
                    clamp01(
                            magnitude.potentialMagnitude()
                    );

            int color =
                    heatColor(value);

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverWidth(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        12
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentMagnitude magnitude =
                    hydrology.riverSegmentMagnitude(
                            segment.id()
                    );

            if (magnitude == null) {
                continue;
            }

            int color =
                    riverScaleColor(
                            magnitude.riverScale()
                    );

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverSlope(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        12
                );

        double maximumSlope =
                0.0;

        for (RiverSegmentMagnitude magnitude : hydrology.riverSegmentMagnitudes()) {
            maximumSlope =
                    Math.max(
                            maximumSlope,
                            magnitude.channelSlope()
                    );
        }

        double logarithmicMaximum =
                Math.log1p(
                        maximumSlope / 0.001
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentMagnitude magnitude =
                    hydrology.riverSegmentMagnitude(
                            segment.id()
                    );

            if (magnitude == null) {
                continue;
            }

            double normalized =
                    logarithmicMaximum > 0.0
                            ? Math.log1p(
                            magnitude.channelSlope() / 0.001
                    ) / logarithmicMaximum
                            : 0.0;

            int color =
                    heatColor(
                            clamp01(normalized)
                    );

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverSmoothedWidth(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        12
                );

        double maximumWidth =
                Math.max(
                        1.0,
                        hydrology.maximumSmoothedRiverWidthBlocks()
                );

        double logarithmicMaximum =
                Math.log1p(maximumWidth);

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentProfile profile =
                    hydrology.riverSegmentProfile(
                            segment.id()
                    );

            if (profile == null) {
                continue;
            }

            double normalized =
                    Math.log1p(
                            profile.averageSmoothedWidthBlocks()
                    ) / logarithmicMaximum;

            normalized =
                    clamp01(normalized);

            int color =
                    rgb(
                            clamp255(18.0 + 30.0 * normalized),
                            clamp255(72.0 + 112.0 * normalized),
                            clamp255(120.0 + 135.0 * normalized)
                    );

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverContinuity(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        12
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentProfile profile =
                    hydrology.riverSegmentProfile(
                            segment.id()
                    );

            if (profile == null) {
                continue;
            }

            /*
             * 0.50 means the 2F solver moved at least one end of the segment
             * by fifty percent relative to the raw 2E width. Values above
             * that remain clamped red so unusual corrections stand out.
             */
            double normalized =
                    clamp01(
                            profile.widthAdjustmentFraction()
                                    / 0.50
                    );

            int color =
                    profile.endElevation()
                            > profile.startElevation() + 1.0e-6
                            ? rgb(230, 60, 210)
                            : continuityColor(normalized);

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
                        color
                );
            }

            double rawRatio =
                    profile.rawStartWidthRatio();

            if (rawRatio > 1.80 || rawRatio < 0.55) {
                RiverNode startNode =
                        hydrology.riverNodes()
                                .get(segment.startNodeId());

                if (
                        startNode.x() >= 0
                                && startNode.x() < size
                                && startNode.z() >= 0
                                && startNode.z() < size
                ) {
                    image.setRGB(
                            startNode.x(),
                            startNode.z(),
                            0xFFFFFF
                    );
                }
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverLongitudinalProfiles(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        HydrologyGrid hydrology =
                world.hydrology();

        int width = 1200;
        int height = 720;

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setColor(
                new Color(12, 14, 18)
        );

        graphics.fillRect(
                0,
                0,
                width,
                height
        );

        List<ProfileSeries> series =
                buildLargestRiverProfileSeries(
                        hydrology,
                        8
                );

        if (series.isEmpty()) {
            graphics.dispose();

            ImageIO.write(
                    image,
                    "PNG",
                    path.toFile()
            );
            return;
        }

        double maximumDistance =
                1.0;

        double minimumElevation =
                Double.POSITIVE_INFINITY;

        double maximumElevation =
                Double.NEGATIVE_INFINITY;

        for (ProfileSeries profileSeries : series) {
            if (!profileSeries.points().isEmpty()) {
                maximumDistance =
                        Math.max(
                                maximumDistance,
                                profileSeries.points()
                                        .getLast()
                                        .distanceBlocks()
                        );
            }

            for (ProfilePoint point : profileSeries.points()) {
                minimumElevation =
                        Math.min(
                                minimumElevation,
                                point.elevation()
                        );

                maximumElevation =
                        Math.max(
                                maximumElevation,
                                point.elevation()
                        );
            }
        }

        if (!Double.isFinite(minimumElevation)) {
            minimumElevation = 0.0;
            maximumElevation = 1.0;
        }

        if (maximumElevation - minimumElevation < 1.0) {
            maximumElevation =
                    minimumElevation + 1.0;
        }

        int left = 78;
        int right = 28;
        int top = 48;
        int bottom = 64;

        int plotWidth =
                width - left - right;

        int plotHeight =
                height - top - bottom;

        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );

        graphics.setStroke(
                new BasicStroke(1.0f)
        );

        for (int i = 0; i <= 5; i++) {
            double t =
                    i / 5.0;

            int y =
                    top
                            + (int) Math.round(
                            plotHeight * t
                    );

            graphics.setColor(
                    new Color(42, 46, 54)
            );

            graphics.drawLine(
                    left,
                    y,
                    left + plotWidth,
                    y
            );

            double elevation =
                    maximumElevation
                            - t
                            * (maximumElevation - minimumElevation);

            graphics.setColor(
                    new Color(190, 196, 205)
            );

            graphics.drawString(
                    String.format("%.0f", elevation),
                    18,
                    y + 5
            );
        }

        for (int i = 0; i <= 5; i++) {
            double t =
                    i / 5.0;

            int x =
                    left
                            + (int) Math.round(
                            plotWidth * t
                    );

            graphics.setColor(
                    new Color(34, 38, 45)
            );

            graphics.drawLine(
                    x,
                    top,
                    x,
                    top + plotHeight
            );

            graphics.setColor(
                    new Color(190, 196, 205)
            );

            graphics.drawString(
                    String.format("%.0f km", maximumDistance * t / 1000.0),
                    x - 18,
                    top + plotHeight + 26
            );
        }

        Color[] colors =
                new Color[]{
                        new Color(86, 180, 233),
                        new Color(230, 159, 0),
                        new Color(0, 158, 115),
                        new Color(204, 121, 167),
                        new Color(240, 228, 66),
                        new Color(0, 114, 178),
                        new Color(213, 94, 0),
                        new Color(120, 120, 230)
                };

        for (int seriesIndex = 0;
             seriesIndex < series.size();
             seriesIndex++) {

            ProfileSeries profileSeries =
                    series.get(seriesIndex);

            List<ProfilePoint> points =
                    profileSeries.points();

            if (points.size() < 2) {
                continue;
            }

            graphics.setColor(
                    colors[seriesIndex % colors.length]
            );

            graphics.setStroke(
                    new BasicStroke(2.2f)
            );

            int previousX = -1;
            int previousY = -1;

            for (ProfilePoint point : points) {
                int x =
                        left
                                + (int) Math.round(
                                point.distanceBlocks()
                                        / maximumDistance
                                        * plotWidth
                        );

                int y =
                        top
                                + (int) Math.round(
                                (
                                        maximumElevation
                                                - point.elevation()
                                )
                                        / (
                                        maximumElevation
                                                - minimumElevation
                                )
                                        * plotHeight
                        );

                if (previousX >= 0) {
                    graphics.drawLine(
                            previousX,
                            previousY,
                            x,
                            y
                    );
                }

                previousX = x;
                previousY = y;
            }

            graphics.drawString(
                    "A="
                            + profileSeries.mouthAccumulation(),
                    Math.min(
                            width - 110,
                            previousX + 5
                    ),
                    Math.max(
                            top + 14,
                            previousY - 4
                    )
            );
        }

        graphics.setColor(
                new Color(220, 224, 230)
        );

        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        18
                )
        );

        graphics.drawString(
                "Largest river main-stem longitudinal profiles",
                left,
                28
        );

        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );

        graphics.drawString(
                "distance from selected main-stem source",
                left + plotWidth / 2 - 110,
                height - 16
        );

        graphics.rotate(
                -Math.PI / 2.0
        );

        graphics.drawString(
                "elevation",
                -top - plotHeight / 2 - 28,
                18
        );

        graphics.rotate(
                Math.PI / 2.0
        );

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverPlannedGrade(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        12
                );

        double maximumGrade =
                Math.max(
                        1.0e-6,
                        hydrology.maximumPlannedRiverGrade()
                );

        double logarithmicMaximum =
                Math.log1p(
                        maximumGrade * 1000.0
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentGradePlan plan =
                    hydrology.riverSegmentGradePlan(
                            segment.id()
                    );

            if (plan == null) {
                continue;
            }

            if (plan.lakePassage()) {
                for (int cell : segment.cellPath()) {
                    image.setRGB(
                            cell % size,
                            cell / size,
                            rgb(35, 82, 150)
                    );
                }
                continue;
            }

            double normalized =
                    logarithmicMaximum > 0.0
                            ? Math.log1p(
                            plan.averagePlannedGrade()
                                    * 1000.0
                    ) / logarithmicMaximum
                            : 0.0;

            normalized =
                    clamp01(normalized);

            int color;

            if (normalized < 0.5) {
                double t =
                        normalized / 0.5;

                color =
                        rgb(
                                clamp255(25.0 + 20.0 * t),
                                clamp255(95.0 + 120.0 * t),
                                clamp255(180.0 + 50.0 * t)
                        );
            } else {
                double t =
                        (normalized - 0.5) / 0.5;

                color =
                        rgb(
                                clamp255(45.0 + 210.0 * t),
                                clamp255(215.0 - 60.0 * t),
                                clamp255(230.0 - 205.0 * t)
                        );
            }

            for (int cell : segment.cellPath()) {
                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverGradeCorrection(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size =
                world.resolution();

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                hydrologyDiagnosticBase(
                        world,
                        10
                );

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverSegmentGradePlan plan =
                    hydrology.riverSegmentGradePlan(
                            segment.id()
                    );

            if (
                    plan == null
                            || plan.lakePassage()
            ) {
                continue;
            }

            int sampleCount =
                    Math.min(
                            plan.sampleCount(),
                            segment.cellPath().size()
                    );

            for (int i = 0; i < sampleCount; i++) {
                double correction =
                        plan.gradeCorrectionAt(i);

                int color;

                if (correction < 0.05) {
                    color =
                            rgb(28, 120, 70);
                } else if (correction < 4.0) {
                    double t =
                            correction / 4.0;

                    color =
                            rgb(
                                    clamp255(45.0 + 185.0 * t),
                                    clamp255(155.0 + 65.0 * t),
                                    45
                            );
                } else if (correction < 16.0) {
                    double t =
                            (correction - 4.0) / 12.0;

                    color =
                            rgb(
                                    240,
                                    clamp255(220.0 - 175.0 * t),
                                    clamp255(45.0 - 20.0 * t)
                            );
                } else {
                    color =
                            0xFFFFFF;
                }

                int cell =
                        segment.cellPath().get(i);

                image.setRGB(
                        cell % size,
                        cell / size,
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


    private static void writeHydrologyRiverLongitudinalProfilesCorrected(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        HydrologyGrid hydrology =
                world.hydrology();

        int width = 1200;
        int height = 720;

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setColor(
                new Color(12, 14, 18)
        );

        graphics.fillRect(
                0,
                0,
                width,
                height
        );

        List<CorrectedProfileSeries> series =
                buildLargestCorrectedRiverProfileSeries(
                        hydrology,
                        6
                );

        if (series.isEmpty()) {
            graphics.dispose();
            ImageIO.write(image, "PNG", path.toFile());
            return;
        }

        double maximumDistance =
                1.0;

        double minimumElevation =
                Double.POSITIVE_INFINITY;

        double maximumElevation =
                Double.NEGATIVE_INFINITY;

        for (CorrectedProfileSeries profileSeries : series) {
            for (CorrectedProfilePoint point : profileSeries.points()) {
                maximumDistance =
                        Math.max(
                                maximumDistance,
                                point.distanceBlocks()
                        );

                minimumElevation =
                        Math.min(
                                minimumElevation,
                                Math.min(
                                        point.terrainElevation(),
                                        point.plannedBedElevation()
                                )
                        );

                maximumElevation =
                        Math.max(
                                maximumElevation,
                                Math.max(
                                        point.terrainElevation(),
                                        point.plannedBedElevation()
                                )
                        );
            }
        }

        if (!Double.isFinite(minimumElevation)) {
            minimumElevation = 0.0;
            maximumElevation = 1.0;
        }

        if (maximumElevation - minimumElevation < 1.0) {
            maximumElevation =
                    minimumElevation + 1.0;
        }

        int left = 78;
        int right = 28;
        int top = 48;
        int bottom = 64;

        int plotWidth =
                width - left - right;

        int plotHeight =
                height - top - bottom;

        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );

        for (int i = 0; i <= 5; i++) {
            double t =
                    i / 5.0;

            int y =
                    top
                            + (int) Math.round(
                            plotHeight * t
                    );

            graphics.setColor(
                    new Color(42, 46, 54)
            );

            graphics.setStroke(
                    new BasicStroke(1.0f)
            );

            graphics.drawLine(
                    left,
                    y,
                    left + plotWidth,
                    y
            );

            double elevation =
                    maximumElevation
                            - t
                            * (maximumElevation - minimumElevation);

            graphics.setColor(
                    new Color(190, 196, 205)
            );

            graphics.drawString(
                    String.format("%.0f", elevation),
                    18,
                    y + 5
            );
        }

        for (int i = 0; i <= 5; i++) {
            double t =
                    i / 5.0;

            int x =
                    left
                            + (int) Math.round(
                            plotWidth * t
                    );

            graphics.setColor(
                    new Color(34, 38, 45)
            );

            graphics.drawLine(
                    x,
                    top,
                    x,
                    top + plotHeight
            );

            graphics.setColor(
                    new Color(190, 196, 205)
            );

            graphics.drawString(
                    String.format("%.0f km", maximumDistance * t / 1000.0),
                    x - 18,
                    top + plotHeight + 26
            );
        }

        Color[] colors =
                new Color[]{
                        new Color(86, 180, 233),
                        new Color(230, 159, 0),
                        new Color(0, 158, 115),
                        new Color(204, 121, 167),
                        new Color(240, 228, 66),
                        new Color(213, 94, 0)
                };

        for (int seriesIndex = 0;
             seriesIndex < series.size();
             seriesIndex++) {

            CorrectedProfileSeries profileSeries =
                    series.get(seriesIndex);

            List<CorrectedProfilePoint> points =
                    profileSeries.points();

            if (points.size() < 2) {
                continue;
            }

            Color color =
                    colors[seriesIndex % colors.length];

            drawCorrectedProfileLine(
                    graphics,
                    points,
                    maximumDistance,
                    minimumElevation,
                    maximumElevation,
                    left,
                    top,
                    plotWidth,
                    plotHeight,
                    new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            75
                    ),
                    1.2f,
                    true
            );

            drawCorrectedProfileLine(
                    graphics,
                    points,
                    maximumDistance,
                    minimumElevation,
                    maximumElevation,
                    left,
                    top,
                    plotWidth,
                    plotHeight,
                    color,
                    2.4f,
                    false
            );
        }

        graphics.setColor(
                new Color(220, 224, 230)
        );

        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        18
                )
        );

        graphics.drawString(
                "Largest river profiles: terrain (faint) vs planned bed",
                left,
                28
        );

        graphics.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );

        graphics.drawString(
                "distance from selected main-stem source",
                left + plotWidth / 2 - 110,
                height - 16
        );

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void drawCorrectedProfileLine(
            Graphics2D graphics,
            List<CorrectedProfilePoint> points,
            double maximumDistance,
            double minimumElevation,
            double maximumElevation,
            int left,
            int top,
            int plotWidth,
            int plotHeight,
            Color color,
            float strokeWidth,
            boolean terrainLine
    ) {
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(strokeWidth));

        int previousX = -1;
        int previousY = -1;

        for (CorrectedProfilePoint point : points) {
            if (point.breakBefore()) {
                previousX = -1;
                previousY = -1;
            }

            double elevation =
                    terrainLine
                            ? point.terrainElevation()
                            : point.plannedBedElevation();

            int x =
                    left
                            + (int) Math.round(
                            point.distanceBlocks()
                                    / maximumDistance
                                    * plotWidth
                    );

            int y =
                    top
                            + (int) Math.round(
                            (
                                    maximumElevation
                                            - elevation
                            )
                                    / (
                                    maximumElevation
                                            - minimumElevation
                            )
                                    * plotHeight
                    );

            if (previousX >= 0) {
                graphics.drawLine(
                        previousX,
                        previousY,
                        x,
                        y
                );
            }

            previousX = x;
            previousY = y;
        }
    }


    private static List<CorrectedProfileSeries> buildLargestCorrectedRiverProfileSeries(
            HydrologyGrid hydrology,
            int maximumSeries
    ) {
        List<RiverSegment> mouthSegments =
                new ArrayList<>();

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverNode endNode =
                    hydrology.riverNodes()
                            .get(segment.endNodeId());

            if (endNode.mouth()) {
                mouthSegments.add(segment);
            }
        }

        mouthSegments.sort(
                Comparator.comparingLong(
                                RiverSegment::endAccumulation
                        )
                        .reversed()
        );

        List<CorrectedProfileSeries> result =
                new ArrayList<>();

        for (RiverSegment mouthSegment : mouthSegments) {
            if (result.size() >= maximumSeries) {
                break;
            }

            List<Integer> chain =
                    traceMainStemUpstream(
                            hydrology,
                            mouthSegment.id()
                    );

            if (chain.isEmpty()) {
                continue;
            }

            List<CorrectedProfilePoint> points =
                    new ArrayList<>();

            double distance =
                    0.0;

            boolean breakBeforeNext =
                    false;

            for (int segmentId : chain) {
                RiverSegment segment =
                        hydrology.riverSegments()
                                .get(segmentId);

                RiverSegmentGradePlan plan =
                        hydrology.riverSegmentGradePlan(
                                segmentId
                        );

                if (
                        plan == null
                                || plan.lakePassage()
                                || plan.sampleCount() <= 0
                ) {
                    distance +=
                            segment.lengthBlocks();

                    breakBeforeNext =
                            true;
                    continue;
                }

                int sampleCount =
                        plan.sampleCount();

                for (int i = 0; i < sampleCount; i++) {
                    if (
                            i == 0
                                    && !points.isEmpty()
                                    && !breakBeforeNext
                    ) {
                        continue;
                    }

                    double localDistance =
                            sampleCount <= 1
                                    ? 0.0
                                    : segment.lengthBlocks()
                                    * i
                                    / (double) (sampleCount - 1);

                    points.add(
                            new CorrectedProfilePoint(
                                    distance + localDistance,
                                    plan.terrainElevationAt(i),
                                    plan.plannedBedElevationAt(i),
                                    breakBeforeNext && i == 0
                            )
                    );
                }

                distance +=
                        segment.lengthBlocks();

                breakBeforeNext =
                        false;
            }

            if (points.size() >= 2) {
                result.add(
                        new CorrectedProfileSeries(
                                List.copyOf(points),
                                mouthSegment.endAccumulation()
                        )
                );
            }
        }

        return List.copyOf(result);
    }


    private static List<ProfileSeries> buildLargestRiverProfileSeries(
            HydrologyGrid hydrology,
            int maximumSeries
    ) {
        List<RiverSegment> mouthSegments =
                new ArrayList<>();

        for (RiverSegment segment : hydrology.riverSegments()) {
            RiverNode endNode =
                    hydrology.riverNodes()
                            .get(segment.endNodeId());

            if (endNode.mouth()) {
                mouthSegments.add(segment);
            }
        }

        mouthSegments.sort(
                Comparator.comparingLong(
                                RiverSegment::endAccumulation
                        )
                        .reversed()
        );

        List<ProfileSeries> result =
                new ArrayList<>();

        for (int mouthIndex = 0;
             mouthIndex < mouthSegments.size()
                     && result.size() < maximumSeries;
             mouthIndex++) {

            RiverSegment mouthSegment =
                    mouthSegments.get(mouthIndex);

            List<Integer> chain =
                    traceMainStemUpstream(
                            hydrology,
                            mouthSegment.id()
                    );

            if (chain.isEmpty()) {
                continue;
            }

            List<ProfilePoint> points =
                    new ArrayList<>();

            double distance =
                    0.0;

            RiverSegment first =
                    hydrology.riverSegments()
                            .get(chain.getFirst());

            RiverNode firstStart =
                    hydrology.riverNodes()
                            .get(first.startNodeId());

            points.add(
                    new ProfilePoint(
                            0.0,
                            firstStart.elevation()
                    )
            );

            for (int segmentId : chain) {
                RiverSegment segment =
                        hydrology.riverSegments()
                                .get(segmentId);

                distance +=
                        segment.lengthBlocks();

                RiverNode endNode =
                        hydrology.riverNodes()
                                .get(segment.endNodeId());

                points.add(
                        new ProfilePoint(
                                distance,
                                endNode.elevation()
                        )
                );
            }

            result.add(
                    new ProfileSeries(
                            List.copyOf(points),
                            mouthSegment.endAccumulation()
                    )
            );
        }

        return List.copyOf(result);
    }


    private static List<Integer> traceMainStemUpstream(
            HydrologyGrid hydrology,
            int mouthSegmentId
    ) {
        List<Integer> reverse =
                new ArrayList<>();

        int currentSegmentId =
                mouthSegmentId;

        while (currentSegmentId >= 0) {
            reverse.add(currentSegmentId);

            RiverSegmentHierarchy hierarchy =
                    hydrology.riverSegmentHierarchy(
                            currentSegmentId
                    );

            if (
                    hierarchy == null
                            || hierarchy.upstreamSegmentIds().isEmpty()
            ) {
                break;
            }

            int bestUpstream =
                    -1;

            double bestDistance =
                    -1.0;

            long bestAccumulation =
                    -1L;

            for (int upstreamId : hierarchy.upstreamSegmentIds()) {
                RiverSegmentHierarchy upstreamHierarchy =
                        hydrology.riverSegmentHierarchy(
                                upstreamId
                        );

                RiverSegment upstreamSegment =
                        hydrology.riverSegments()
                                .get(upstreamId);

                double sourceDistance =
                        upstreamHierarchy != null
                                ? upstreamHierarchy.longestSourceDistanceBlocks()
                                : 0.0;

                long accumulation =
                        upstreamSegment.endAccumulation();

                if (
                        sourceDistance > bestDistance
                                || (
                                sourceDistance == bestDistance
                                        && accumulation > bestAccumulation
                        )
                ) {
                    bestUpstream =
                            upstreamId;

                    bestDistance =
                            sourceDistance;

                    bestAccumulation =
                            accumulation;
                }
            }

            currentSegmentId =
                    bestUpstream;
        }

        Collections.reverse(reverse);

        return reverse;
    }


    private static void writeHydrologyRiverCenterlines(
            WorldBlueprint world,
            Path path,
            boolean reliefBackground
    ) throws IOException {
        final int previewSize =
                2048;

        BufferedImage image =
                new BufferedImage(
                        previewSize,
                        previewSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        if (reliefBackground) {
            BufferedImage base =
                    hydrologyReliefBase(world);

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.drawImage(
                    base,
                    0,
                    0,
                    previewSize,
                    previewSize,
                    null
            );
        } else {
            graphics.setColor(
                    new Color(5, 7, 10)
            );

            graphics.fillRect(
                    0,
                    0,
                    previewSize,
                    previewSize
            );
        }

        HydrologyGrid hydrology =
                world.hydrology();

        for (RiverSegmentCenterline centerline : hydrology.riverSegmentCenterlines()) {
            if (
                    centerline.lakePassage()
                            || centerline.points().size() < 2
            ) {
                continue;
            }

            RiverSegmentMagnitude magnitude =
                    hydrology.riverSegmentMagnitude(
                            centerline.segmentId()
                    );

            RiverSegmentProfile profile =
                    hydrology.riverSegmentProfile(
                            centerline.segmentId()
                    );

            int rgb =
                    magnitude != null
                            ? riverScaleColor(
                            magnitude.riverScale()
                    )
                            : rgb(55, 185, 235);

            Color color =
                    new Color(
                            (rgb >> 16) & 0xFF,
                            (rgb >> 8) & 0xFF,
                            rgb & 0xFF,
                            reliefBackground
                                    ? 230
                                    : 255
                    );

            double averageWidth =
                    profile != null
                            ? profile.averageSmoothedWidthBlocks()
                            : 3.0;

            float strokeWidth =
                    (float) Math.max(
                            0.75,
                            Math.min(
                                    4.0,
                                    averageWidth
                                            / world.config().worldSizeBlocks()
                                            * previewSize
                                            * 0.95
                            )
                    );

            graphics.setStroke(
                    new BasicStroke(
                            strokeWidth,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            graphics.setColor(color);

            Path2D.Double riverPath =
                    new Path2D.Double();

            RiverCenterlinePoint first =
                    centerline.points().getFirst();

            riverPath.moveTo(
                    blockToPreviewPixel(
                            world,
                            first.blockX(),
                            previewSize
                    ),
                    blockToPreviewPixel(
                            world,
                            first.blockZ(),
                            previewSize
                    )
            );

            for (int i = 1; i < centerline.points().size(); i++) {
                RiverCenterlinePoint point =
                        centerline.points().get(i);

                riverPath.lineTo(
                        blockToPreviewPixel(
                                world,
                                point.blockX(),
                                previewSize
                        ),
                        blockToPreviewPixel(
                                world,
                                point.blockZ(),
                                previewSize
                        )
                );
            }

            graphics.draw(riverPath);
        }

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverCenterlineOffset(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize =
                2048;

        HydrologyGrid hydrology =
                world.hydrology();

        BufferedImage image =
                new BufferedImage(
                        previewSize,
                        previewSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setColor(
                new Color(4, 5, 8)
        );

        graphics.fillRect(
                0,
                0,
                previewSize,
                previewSize
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        /*
         * Draw the original D8 macro graph underneath so this diagnostic
         * directly shows how Pass 2G departs from the 64-block grid route.
         */
        graphics.setStroke(
                new BasicStroke(
                        0.8f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        graphics.setColor(
                new Color(58, 58, 64)
        );

        for (RiverSegment segment : hydrology.riverSegments()) {
            if (
                    segment.type() == RiverSegmentType.LAKE_PASSAGE
                            || segment.cellPath().size() < 2
            ) {
                continue;
            }

            Path2D.Double macroPath =
                    new Path2D.Double();

            boolean started =
                    false;

            for (int cell : segment.cellPath()) {
                int x =
                        cell % world.resolution();

                int z =
                        cell / world.resolution();

                double blockX =
                        macroGridBlockCoordinate(
                                world,
                                x
                        );

                double blockZ =
                        macroGridBlockCoordinate(
                                world,
                                z
                        );

                double px =
                        blockToPreviewPixel(
                                world,
                                blockX,
                                previewSize
                        );

                double pz =
                        blockToPreviewPixel(
                                world,
                                blockZ,
                                previewSize
                        );

                if (!started) {
                    macroPath.moveTo(
                            px,
                            pz
                    );
                    started = true;
                } else {
                    macroPath.lineTo(
                            px,
                            pz
                    );
                }
            }

            graphics.draw(macroPath);
        }

        double normalization =
                Math.max(
                        1.0,
                        hydrology.blocksPerCell()
                                * 0.62
                );

        graphics.setStroke(
                new BasicStroke(
                        1.15f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        for (RiverSegmentCenterline centerline : hydrology.riverSegmentCenterlines()) {
            if (
                    centerline.lakePassage()
                            || centerline.points().size() < 2
            ) {
                continue;
            }

            List<RiverCenterlinePoint> points =
                    centerline.points();

            for (int i = 1; i < points.size(); i++) {
                RiverCenterlinePoint a =
                        points.get(i - 1);

                RiverCenterlinePoint b =
                        points.get(i);

                double normalizedOffset =
                        Math.max(
                                Math.abs(
                                        a.lateralOffsetBlocks()
                                ),
                                Math.abs(
                                        b.lateralOffsetBlocks()
                                )
                        ) / normalization;

                graphics.setColor(
                        new Color(
                                centerlineOffsetColor(
                                        normalizedOffset
                                )
                        )
                );

                graphics.drawLine(
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        a.blockX(),
                                        previewSize
                                )
                        ),
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        a.blockZ(),
                                        previewSize
                                )
                        ),
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        b.blockX(),
                                        previewSize
                                )
                        ),
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        b.blockZ(),
                                        previewSize
                                )
                        )
                );
            }
        }

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverCenterlineDetail(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize =
                1024;

        HydrologyGrid hydrology =
                world.hydrology();

        RiverSegmentCenterline selected =
                null;

        double bestScore =
                -1.0;

        for (RiverSegmentCenterline centerline : hydrology.riverSegmentCenterlines()) {
            if (
                    centerline.lakePassage()
                            || centerline.points().size() < 3
            ) {
                continue;
            }

            RiverSegmentProfile profile =
                    hydrology.riverSegmentProfile(
                            centerline.segmentId()
                    );

            double width =
                    profile != null
                            ? profile.averageSmoothedWidthBlocks()
                            : 1.0;

            /*
             * Pick a segment where the synthesized geometry is visually
             * informative rather than simply selecting the longest straight
             * trunk in the world.
             */
            double score =
                    2_500.0
                            * Math.max(
                            0.0,
                            centerline.sinuosityRatio() - 1.0
                    )
                            + 8.0
                            * centerline.maximumLateralOffsetBlocks()
                            + 0.05
                            * centerline.centerlineLengthBlocks()
                            + 4.0
                            * Math.sqrt(
                            Math.max(
                                    1.0,
                                    width
                            )
                    );

            if (score > bestScore) {
                bestScore = score;
                selected = centerline;
            }
        }

        BufferedImage image =
                new BufferedImage(
                        previewSize,
                        previewSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setColor(
                new Color(7, 9, 13)
        );

        graphics.fillRect(
                0,
                0,
                previewSize,
                previewSize
        );

        if (selected == null) {
            graphics.dispose();
            ImageIO.write(
                    image,
                    "PNG",
                    path.toFile()
            );
            return;
        }

        RiverSegment segment =
                hydrology.riverSegments()
                        .get(selected.segmentId());

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (RiverCenterlinePoint point : selected.points()) {
            minX = Math.min(minX, point.blockX());
            maxX = Math.max(maxX, point.blockX());
            minZ = Math.min(minZ, point.blockZ());
            maxZ = Math.max(maxZ, point.blockZ());
        }

        double widthBlocks =
                Math.max(
                        64.0,
                        maxX - minX
                );

        double heightBlocks =
                Math.max(
                        64.0,
                        maxZ - minZ
                );

        double span =
                Math.max(
                        widthBlocks,
                        heightBlocks
                )
                        * 1.24;

        span =
                Math.max(
                        span,
                        320.0
                );

        double centerX =
                0.5 * (minX + maxX);

        double centerZ =
                0.5 * (minZ + maxZ);

        double cropMinX =
                centerX - span * 0.5;

        double cropMinZ =
                centerZ - span * 0.5;

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setStroke(
                new BasicStroke(
                        4.0f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        graphics.setColor(
                new Color(85, 85, 90)
        );

        Path2D.Double macroPath =
                new Path2D.Double();

        boolean macroStarted =
                false;

        for (int cell : segment.cellPath()) {
            int x = cell % world.resolution();
            int z = cell / world.resolution();

            double bx = macroGridBlockCoordinate(world, x);
            double bz = macroGridBlockCoordinate(world, z);

            double px = (bx - cropMinX) / span * (previewSize - 1);
            double pz = (bz - cropMinZ) / span * (previewSize - 1);

            if (!macroStarted) {
                macroPath.moveTo(px, pz);
                macroStarted = true;
            } else {
                macroPath.lineTo(px, pz);
            }
        }

        graphics.draw(macroPath);

        graphics.setStroke(
                new BasicStroke(
                        2.4f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        Path2D.Double smoothPath =
                new Path2D.Double();

        RiverCenterlinePoint first =
                selected.points().getFirst();

        smoothPath.moveTo(
                (first.blockX() - cropMinX) / span * (previewSize - 1),
                (first.blockZ() - cropMinZ) / span * (previewSize - 1)
        );

        for (int i = 1; i < selected.points().size(); i++) {
            RiverCenterlinePoint point =
                    selected.points().get(i);

            smoothPath.lineTo(
                    (point.blockX() - cropMinX) / span * (previewSize - 1),
                    (point.blockZ() - cropMinZ) / span * (previewSize - 1)
            );
        }

        graphics.setColor(
                new Color(65, 225, 235)
        );

        graphics.draw(smoothPath);

        graphics.setColor(
                new Color(80, 235, 110)
        );

        graphics.fillOval(
                (int) Math.round(
                        (first.blockX() - cropMinX)
                                / span
                                * (previewSize - 1)
                                - 5.0
                ),
                (int) Math.round(
                        (first.blockZ() - cropMinZ)
                                / span
                                * (previewSize - 1)
                                - 5.0
                ),
                10,
                10
        );

        RiverCenterlinePoint last =
                selected.points().getLast();

        graphics.setColor(
                new Color(245, 90, 75)
        );

        graphics.fillOval(
                (int) Math.round(
                        (last.blockX() - cropMinX)
                                / span
                                * (previewSize - 1)
                                - 5.0
                ),
                (int) Math.round(
                        (last.blockZ() - cropMinZ)
                                / span
                                * (previewSize - 1)
                                - 5.0
                ),
                10,
                10
        );

        graphics.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        20
                )
        );

        graphics.setColor(Color.WHITE);

        graphics.drawString(
                String.format(
                        "segment %d  length %.0f  sinuosity %.3f  freedom %.2f  max offset %.1f",
                        selected.segmentId(),
                        selected.centerlineLengthBlocks(),
                        selected.sinuosityRatio(),
                        selected.lateralFreedom(),
                        selected.maximumLateralOffsetBlocks()
                ),
                24,
                34
        );

        graphics.setColor(
                new Color(150, 150, 155)
        );

        graphics.drawString(
                "gray = macro D8 route   cyan = Pass 2G centerline   green/red = endpoints",
                24,
                62
        );

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverChannelCorridor(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize =
                2048;

        BufferedImage image =
                new BufferedImage(
                        previewSize,
                        previewSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        configureHydrologyGraphics(
                graphics
        );

        graphics.setColor(
                new Color(4, 7, 10)
        );

        graphics.fillRect(
                0,
                0,
                previewSize,
                previewSize
        );

        HydrologyGrid hydrology =
                world.hydrology();

        for (RiverSegmentValleyCorridor corridor : hydrology.riverSegmentValleyCorridors()) {
            if (
                    corridor.lakePassage()
                            || corridor.points().size() < 2
            ) {
                continue;
            }

            drawValleyCorridorLayer(
                    graphics,
                    world,
                    corridor,
                    previewSize,
                    2,
                    new Color(32, 176, 236, 220),
                    0.80f,
                    8.0f
            );
        }

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverValleyCorridor(
            WorldBlueprint world,
            Path path,
            boolean reliefBackground
    ) throws IOException {
        final int previewSize =
                2048;

        BufferedImage image =
                new BufferedImage(
                        previewSize,
                        previewSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        configureHydrologyGraphics(
                graphics
        );

        if (reliefBackground) {
            BufferedImage base =
                    hydrologyReliefBase(world);

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.drawImage(
                    base,
                    0,
                    0,
                    previewSize,
                    previewSize,
                    null
            );
        } else {
            graphics.setColor(
                    new Color(5, 7, 9)
            );

            graphics.fillRect(
                    0,
                    0,
                    previewSize,
                    previewSize
            );
        }

        HydrologyGrid hydrology =
                world.hydrology();

        for (RiverSegmentValleyCorridor corridor : hydrology.riverSegmentValleyCorridors()) {
            if (
                    corridor.lakePassage()
                            || corridor.points().size() < 2
            ) {
                continue;
            }

            drawValleyCorridorLayer(
                    graphics,
                    world,
                    corridor,
                    previewSize,
                    0,
                    reliefBackground
                            ? new Color(215, 171, 78, 74)
                            : new Color(122, 95, 46, 170),
                    1.0f,
                    84.0f
            );
        }

        for (RiverSegmentValleyCorridor corridor : hydrology.riverSegmentValleyCorridors()) {
            if (
                    corridor.lakePassage()
                            || corridor.points().size() < 2
            ) {
                continue;
            }

            drawValleyCorridorLayer(
                    graphics,
                    world,
                    corridor,
                    previewSize,
                    1,
                    reliefBackground
                            ? new Color(70, 206, 150, 100)
                            : new Color(35, 145, 105, 185),
                    0.90f,
                    70.0f
            );
        }

        for (RiverSegmentValleyCorridor corridor : hydrology.riverSegmentValleyCorridors()) {
            if (
                    corridor.lakePassage()
                            || corridor.points().size() < 2
            ) {
                continue;
            }

            drawValleyCorridorLayer(
                    graphics,
                    world,
                    corridor,
                    previewSize,
                    2,
                    reliefBackground
                            ? new Color(36, 182, 238, 225)
                            : new Color(35, 185, 242, 235),
                    0.85f,
                    8.0f
            );
        }

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeHydrologyRiverConfinement(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize =
                2048;

        BufferedImage image =
                new BufferedImage(
                        previewSize,
                        previewSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        configureHydrologyGraphics(
                graphics
        );

        BufferedImage base =
                hydrologyReliefBase(world);

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.drawImage(
                base,
                0,
                0,
                previewSize,
                previewSize,
                null
        );

        graphics.setStroke(
                new BasicStroke(
                        1.65f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        for (RiverSegmentValleyCorridor corridor : world.hydrology().riverSegmentValleyCorridors()) {
            if (
                    corridor.lakePassage()
                            || corridor.points().size() < 2
            ) {
                continue;
            }

            List<RiverValleyCorridorPoint> points =
                    corridor.points();

            for (int i = 1; i < points.size(); i++) {
                RiverValleyCorridorPoint a =
                        points.get(i - 1);

                RiverValleyCorridorPoint b =
                        points.get(i);

                double confinement =
                        0.5
                                * (
                                a.confinement()
                                        + b.confinement()
                        );

                graphics.setColor(
                        confinementColor(
                                confinement
                        )
                );

                graphics.drawLine(
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        a.blockX(),
                                        previewSize
                                )
                        ),
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        a.blockZ(),
                                        previewSize
                                )
                        ),
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        b.blockX(),
                                        previewSize
                                )
                        ),
                        (int) Math.round(
                                blockToPreviewPixel(
                                        world,
                                        b.blockZ(),
                                        previewSize
                                )
                        )
                );
            }
        }

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void drawValleyCorridorLayer(
            Graphics2D graphics,
            WorldBlueprint world,
            RiverSegmentValleyCorridor corridor,
            int previewSize,
            int layer,
            Color color,
            float minimumStroke,
            float maximumStroke
    ) {
        List<RiverValleyCorridorPoint> points =
                corridor.points();

        graphics.setColor(color);

        float previousStroke =
                -1.0f;

        for (int i = 1; i < points.size(); i++) {
            RiverValleyCorridorPoint a =
                    points.get(i - 1);

            RiverValleyCorridorPoint b =
                    points.get(i);

            double widthBlocks =
                    0.5
                            * (
                            corridorFullWidth(
                                    a,
                                    layer
                            )
                                    + corridorFullWidth(
                                    b,
                                    layer
                            )
                    );

            float stroke =
                    (float) Math.max(
                            minimumStroke,
                            Math.min(
                                    maximumStroke,
                                    widthBlocks
                                            / world.config().worldSizeBlocks()
                                            * previewSize
                            )
                    );

            if (
                    previousStroke < 0.0f
                            || Math.abs(
                            stroke - previousStroke
                    ) > 0.20f
            ) {
                graphics.setStroke(
                        new BasicStroke(
                                stroke,
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND
                        )
                );

                previousStroke =
                        stroke;
            }

            graphics.drawLine(
                    (int) Math.round(
                            blockToPreviewPixel(
                                    world,
                                    a.blockX(),
                                    previewSize
                            )
                    ),
                    (int) Math.round(
                            blockToPreviewPixel(
                                    world,
                                    a.blockZ(),
                                    previewSize
                            )
                    ),
                    (int) Math.round(
                            blockToPreviewPixel(
                                    world,
                                    b.blockX(),
                                    previewSize
                            )
                    ),
                    (int) Math.round(
                            blockToPreviewPixel(
                                    world,
                                    b.blockZ(),
                                    previewSize
                            )
                    )
            );
        }
    }


    private static double corridorFullWidth(
            RiverValleyCorridorPoint point,
            int layer
    ) {
        double halfWidth =
                switch (layer) {
                    case 0 -> point.valleyHalfWidthBlocks();
                    case 1 -> point.floodplainHalfWidthBlocks();
                    default -> point.channelHalfWidthBlocks();
                };

        return halfWidth * 2.0;
    }


    private static Color confinementColor(
            double confinement
    ) {
        double t =
                clamp01(
                        confinement
                );

        if (t < 0.5) {
            double local =
                    t / 0.5;

            return new Color(
                    (int) Math.round(
                            35
                                    + local
                                    * (245 - 35)
                    ),
                    (int) Math.round(
                            205
                                    + local
                                    * (214 - 205)
                    ),
                    (int) Math.round(
                            164
                                    + local
                                    * (66 - 164)
                    ),
                    235
            );
        }

        double local =
                (t - 0.5) / 0.5;

        return new Color(
                (int) Math.round(
                        245
                                + local
                                * (238 - 245)
                ),
                (int) Math.round(
                        214
                                + local
                                * (65 - 214)
                ),
                (int) Math.round(
                        66
                                + local
                                * (54 - 66)
                ),
                235
        );
    }


    private static void configureHydrologyGraphics(
            Graphics2D graphics
    ) {
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
    }

    private static void writeHydrologyRiverBankfullChannel(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize = 2048;

        BufferedImage image = hydrologyReliefBase(world);
        BufferedImage scaled = new BufferedImage(
                previewSize,
                previewSize,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = scaled.createGraphics();
        configureHydrologyGraphics(graphics);
        graphics.drawImage(image, 0, 0, previewSize, previewSize, null);

        for (RiverSegmentCrossSection crossSection : world.hydrology().riverSegmentCrossSections()) {
            if (crossSection.lakePassage() || crossSection.points().size() < 2) {
                continue;
            }

            List<RiverCrossSectionPoint> points = crossSection.points();
            float previousStroke = -1.0f;
            graphics.setColor(new Color(32, 185, 240, 235));

            for (int i = 1; i < points.size(); i++) {
                RiverCrossSectionPoint a = points.get(i - 1);
                RiverCrossSectionPoint b = points.get(i);

                double widthBlocks =
                        a.bankfullHalfWidthBlocks()
                                + b.bankfullHalfWidthBlocks();

                float stroke = (float) Math.max(
                        0.85,
                        Math.min(
                                18.0,
                                widthBlocks
                                        / world.config().worldSizeBlocks()
                                        * previewSize
                        )
                );

                if (previousStroke < 0.0f || Math.abs(stroke - previousStroke) > 0.15f) {
                    graphics.setStroke(new BasicStroke(
                            stroke,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    ));
                    previousStroke = stroke;
                }

                graphics.drawLine(
                        (int) Math.round(blockToPreviewPixel(world, a.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, a.blockZ(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockZ(), previewSize))
                );
            }
        }

        graphics.dispose();
        ImageIO.write(scaled, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverCrossSectionType(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize = 2048;

        BufferedImage base = hydrologyReliefBase(world);
        BufferedImage image = new BufferedImage(
                previewSize,
                previewSize,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = image.createGraphics();
        configureHydrologyGraphics(graphics);
        graphics.drawImage(base, 0, 0, previewSize, previewSize, null);
        graphics.setStroke(new BasicStroke(
                1.70f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        for (RiverSegmentCrossSection crossSection : world.hydrology().riverSegmentCrossSections()) {
            if (crossSection.lakePassage() || crossSection.points().size() < 2) {
                continue;
            }

            List<RiverCrossSectionPoint> points = crossSection.points();
            for (int i = 1; i < points.size(); i++) {
                RiverCrossSectionPoint a = points.get(i - 1);
                RiverCrossSectionPoint b = points.get(i);

                graphics.setColor(reachTypeColor(b.reachType()));
                graphics.drawLine(
                        (int) Math.round(blockToPreviewPixel(world, a.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, a.blockZ(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockZ(), previewSize))
                );
            }
        }

        graphics.dispose();
        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverCrossSectionAsymmetry(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        final int previewSize = 2048;

        BufferedImage base = hydrologyReliefBase(world);
        BufferedImage image = new BufferedImage(
                previewSize,
                previewSize,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = image.createGraphics();
        configureHydrologyGraphics(graphics);
        graphics.drawImage(base, 0, 0, previewSize, previewSize, null);
        graphics.setStroke(new BasicStroke(
                1.70f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        for (RiverSegmentCrossSection crossSection : world.hydrology().riverSegmentCrossSections()) {
            if (crossSection.lakePassage() || crossSection.points().size() < 2) {
                continue;
            }

            List<RiverCrossSectionPoint> points = crossSection.points();
            for (int i = 1; i < points.size(); i++) {
                RiverCrossSectionPoint a = points.get(i - 1);
                RiverCrossSectionPoint b = points.get(i);
                double bias = 0.5 * (a.lateralBias() + b.lateralBias());

                graphics.setColor(asymmetryColor(bias));
                graphics.drawLine(
                        (int) Math.round(blockToPreviewPixel(world, a.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, a.blockZ(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockZ(), previewSize))
                );
            }
        }

        graphics.dispose();
        ImageIO.write(image, "PNG", path.toFile());
    }


    private static Color reachTypeColor(
            RiverReachType type
    ) {
        return switch (type) {
            case MOUNTAIN_CONFINED -> new Color(222, 75, 62, 235);
            case V_VALLEY -> new Color(232, 147, 58, 235);
            case FOOTHILL -> new Color(220, 211, 72, 235);
            case ALLUVIAL -> new Color(72, 190, 126, 235);
            case LOWLAND_FLOODPLAIN -> new Color(45, 170, 232, 235);
        };
    }


    private static Color asymmetryColor(
            double bias
    ) {
        double t = clamp01(0.5 + bias);

        if (t < 0.5) {
            double local = t / 0.5;
            return new Color(
                    (int) Math.round(55 + local * 180),
                    (int) Math.round(115 + local * 120),
                    (int) Math.round(235),
                    235
            );
        }

        double local = (t - 0.5) / 0.5;
        return new Color(
                235,
                (int) Math.round(235 - local * 145),
                (int) Math.round(235 - local * 165),
                235
        );
    }


    private static void writeHydrologyRiverCarvingStrength(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        writeHydrologyRiverConstraintScalar(
                world,
                path,
                0
        );
    }


    private static void writeHydrologyRiverCutBudget(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        writeHydrologyRiverConstraintScalar(
                world,
                path,
                1
        );
    }


    private static void writeHydrologyRiverFloodplainTarget(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        writeHydrologyRiverConstraintScalar(
                world,
                path,
                2
        );
    }


    private static void writeHydrologyRiverConstraintScalar(
            WorldBlueprint world,
            Path path,
            int mode
    ) throws IOException {
        final int previewSize = 2048;

        BufferedImage base = hydrologyReliefBase(world);
        BufferedImage image = new BufferedImage(
                previewSize,
                previewSize,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = image.createGraphics();
        configureHydrologyGraphics(graphics);
        graphics.drawImage(base, 0, 0, previewSize, previewSize, null);
        graphics.setStroke(new BasicStroke(
                1.75f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        for (RiverSegmentCarvingConstraints constraints : world.hydrology().riverSegmentCarvingConstraints()) {
            if (constraints.lakePassage() || constraints.points().size() < 2) {
                continue;
            }

            List<RiverCarvingConstraintPoint> points = constraints.points();

            for (int i = 1; i < points.size(); i++) {
                RiverCarvingConstraintPoint a = points.get(i - 1);
                RiverCarvingConstraintPoint b = points.get(i);

                double scalar = switch (mode) {
                    case 1 -> 0.5 * (a.maximumCutDepthBlocks() + b.maximumCutDepthBlocks()) / 64.0;
                    case 2 -> 0.5 * (
                            (a.targetFloodplainElevation() - a.targetBedElevation())
                                    + (b.targetFloodplainElevation() - b.targetBedElevation())
                    ) / 20.0;
                    default -> 0.5 * (a.cutStrength() + b.cutStrength());
                };

                graphics.setColor(constraintScalarColor(scalar));
                graphics.drawLine(
                        (int) Math.round(blockToPreviewPixel(world, a.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, a.blockZ(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockX(), previewSize)),
                        (int) Math.round(blockToPreviewPixel(world, b.blockZ(), previewSize))
                );
            }
        }

        graphics.dispose();
        ImageIO.write(image, "PNG", path.toFile());
    }


    private static Color constraintScalarColor(
            double value
    ) {
        double t = clamp01(value);

        if (t < 0.5) {
            double local = t / 0.5;
            return new Color(
                    (int) Math.round(45 + local * 190),
                    (int) Math.round(165 + local * 65),
                    (int) Math.round(225 - local * 145),
                    235
            );
        }

        double local = (t - 0.5) / 0.5;
        return new Color(
                235,
                (int) Math.round(230 - local * 170),
                (int) Math.round(80 - local * 35),
                235
        );
    }

    private static void writeHydrologyRiverTerrainDelta(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size = world.resolution();
        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_RGB
        );

        double maximum = Math.max(
                1.0,
                world.hydrology()
                        .maximumAbsoluteRiverTerrainDeltaBlocks()
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                double delta =
                        world.hydrology()
                                .riverTerrainDelta(x, z);

                double normalized = clamp01(
                        Math.abs(delta) / maximum
                );

                int color;

                if (Math.abs(delta) < 1.0e-4) {
                    int gray = 36;
                    color = rgb(gray, gray, gray);
                } else if (delta < 0.0) {
                    color = rgb(
                            clamp255(30.0 + 35.0 * normalized),
                            clamp255(75.0 + 120.0 * normalized),
                            clamp255(120.0 + 135.0 * normalized)
                    );
                } else {
                    color = rgb(
                            clamp255(120.0 + 135.0 * normalized),
                            clamp255(95.0 + 110.0 * normalized),
                            clamp255(35.0 + 25.0 * normalized)
                    );
                }

                image.setRGB(x, z, color);
            }
        }

        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverIntegratedRelief(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size = world.resolution();
        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_RGB
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                int west = Math.max(0, x - 1);
                int east = Math.min(size - 1, x + 1);
                int north = Math.max(0, z - 1);
                int south = Math.min(size - 1, z + 1);

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                int shade = clamp255(
                        (0.48
                                - dx * 0.0034
                                - dz * 0.0034)
                                * 255.0
                );

                double floor =
                        clamp01(
                                world.hydrology()
                                        .riverValleyFloorStrength(x, z)
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(shade * (1.0 - 0.14 * floor)),
                                clamp255(shade * (1.0 + 0.06 * floor)),
                                clamp255(shade * (1.0 + 0.10 * floor))
                        )
                );
            }
        }

        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverValleyFloor(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size = world.resolution();
        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_RGB
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                double strength = clamp01(
                        world.hydrology()
                                .riverValleyFloorStrength(x, z)
                );

                if (strength <= 1.0e-5) {
                    image.setRGB(x, z, rgb(24, 27, 30));
                    continue;
                }

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(30.0 + 50.0 * strength),
                                clamp255(80.0 + 155.0 * strength),
                                clamp255(70.0 + 95.0 * strength)
                        )
                );
            }
        }

        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverChannelIncision(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size = world.resolution();
        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_RGB
        );

        double maximum = Math.max(
                1.0,
                world.hydrology()
                        .maximumRiverChannelIncisionBlocks()
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                double delta =
                        world.hydrology()
                                .riverChannelDelta(x, z);

                if (delta >= -1.0e-4) {
                    image.setRGB(
                            x,
                            z,
                            rgb(30, 32, 35)
                    );
                    continue;
                }

                double normalized =
                        clamp01(
                                -delta / maximum
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(20.0 + 30.0 * normalized),
                                clamp255(90.0 + 120.0 * normalized),
                                clamp255(145.0 + 110.0 * normalized)
                        )
                );
            }
        }

        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverWaterSurface(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size = world.resolution();
        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_RGB
        );

        double minimum =
                world.hydrology()
                        .minimumRiverWaterSurfaceElevation();

        double maximum =
                world.hydrology()
                        .maximumRiverWaterSurfaceElevation();

        double range =
                Math.max(
                        1.0,
                        maximum - minimum
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                double strength =
                        clamp01(
                                world.hydrology()
                                        .riverChannelStrength(x, z)
                        );

                if (strength <= 1.0e-5) {
                    image.setRGB(
                            x,
                            z,
                            rgb(28, 31, 34)
                    );
                    continue;
                }

                double elevation =
                        world.hydrology()
                                .riverWaterSurfaceElevation(x, z);

                double normalized =
                        clamp01(
                                (elevation - minimum) / range
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(25.0 + 60.0 * normalized),
                                clamp255(120.0 + 90.0 * normalized),
                                clamp255(230.0 - 55.0 * normalized)
                        )
                );
            }
        }

        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeHydrologyRiverFinalRelief(
            WorldBlueprint world,
            Path path
    ) throws IOException {
        int size = world.resolution();
        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_RGB
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                if (world.landMask(x, z) < 0.5f) {
                    image.setRGB(x, z, rgb(0, 0, 0));
                    continue;
                }

                int west = Math.max(0, x - 1);
                int east = Math.min(size - 1, x + 1);
                int north = Math.max(0, z - 1);
                int south = Math.min(size - 1, z + 1);

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                int shade =
                        clamp255(
                                (0.48
                                        - dx * 0.0034
                                        - dz * 0.0034)
                                        * 255.0
                        );

                double channel =
                        clamp01(
                                world.hydrology()
                                        .riverChannelStrength(x, z)
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(shade * (1.0 - 0.16 * channel)),
                                clamp255(shade * (1.0 + 0.03 * channel)),
                                clamp255(shade * (1.0 + 0.14 * channel))
                        )
                );
            }
        }

        ImageIO.write(image, "PNG", path.toFile());
    }


    private static void writeClimateTemperature(
            WorldBlueprint world,
            Path path,
            boolean reliefBackground
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
                double temperature =
                        world.climate()
                                .temperatureCelsius(x, z);

                int climateColor =
                        temperatureColor(
                                temperature
                        );

                if (
                        !reliefBackground
                                || world.landMask(x, z) < 0.5f
                ) {
                    image.setRGB(
                            x,
                            z,
                            climateColor
                    );

                    continue;
                }

                int west =
                        Math.max(0, x - 1);

                int east =
                        Math.min(size - 1, x + 1);

                int north =
                        Math.max(0, z - 1);

                int south =
                        Math.min(size - 1, z + 1);

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                double relief =
                        clamp01(
                                0.52
                                        - dx * 0.0032
                                        - dz * 0.0032
                        );

                int red =
                        (climateColor >> 16) & 0xFF;

                int green =
                        (climateColor >> 8) & 0xFF;

                int blue =
                        climateColor & 0xFF;

                double shade =
                        0.62
                                + 0.58 * relief;

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


    private static int temperatureColor(
            double temperatureCelsius
    ) {
        /*
         * Fixed physical scale rather than per-seed normalization. This makes
         * the same color directly comparable across all generated seeds.
         */
        double normalized =
                clamp01(
                        (temperatureCelsius + 18.0)
                                / 36.0
                );

        if (normalized < 0.25) {
            double t =
                    normalized / 0.25;

            return rgb(
                    clamp255(28.0 + 22.0 * t),
                    clamp255(48.0 + 112.0 * t),
                    clamp255(138.0 + 105.0 * t)
            );
        }

        if (normalized < 0.50) {
            double t =
                    (normalized - 0.25) / 0.25;

            return rgb(
                    clamp255(50.0 + 70.0 * t),
                    clamp255(160.0 + 62.0 * t),
                    clamp255(243.0 - 83.0 * t)
            );
        }

        if (normalized < 0.75) {
            double t =
                    (normalized - 0.50) / 0.25;

            return rgb(
                    clamp255(120.0 + 118.0 * t),
                    clamp255(222.0 + 12.0 * t),
                    clamp255(160.0 - 105.0 * t)
            );
        }

        double t =
                (normalized - 0.75) / 0.25;

        return rgb(
                clamp255(238.0 + 17.0 * t),
                clamp255(234.0 - 154.0 * t),
                clamp255(55.0 - 25.0 * t)
        );
    }


    private static void writeClimateMoistureSource(
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
                double source =
                        clamp01(
                                world.climate()
                                        .moistureSource(x, z)
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(10.0 + 35.0 * source),
                                clamp255(18.0 + 150.0 * source),
                                clamp255(28.0 + 220.0 * source)
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


    private static void writeClimateMoistureTransport(
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
                double moisture =
                        clamp01(
                                world.climate()
                                        .transportedMoisture(x, z)
                        );

                int west =
                        Math.max(0, x - 1);

                int east =
                        Math.min(size - 1, x + 1);

                int north =
                        Math.max(0, z - 1);

                int south =
                        Math.min(size - 1, z + 1);

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                double shade =
                        Math.max(
                                0.48,
                                Math.min(
                                        1.08,
                                        0.76
                                                - dx * 0.0018
                                                - dz * 0.0018
                                )
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255((18.0 + 45.0 * moisture) * shade),
                                clamp255((30.0 + 155.0 * moisture) * shade),
                                clamp255((42.0 + 205.0 * moisture) * shade)
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


    private static void writeClimatePrevailingWind(
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

        Graphics2D graphics =
                image.createGraphics();

        graphics.setColor(
                new Color(
                        22,
                        27,
                        32
                )
        );

        graphics.fillRect(
                0,
                0,
                size,
                size
        );

        int spacing =
                Math.max(
                        28,
                        size / 24
                );

        int arrowLength =
                Math.max(
                        14,
                        spacing / 2
                );

        graphics.setStroke(
                new BasicStroke(
                        Math.max(
                                1.25f,
                                size / 900.0f
                        )
                )
        );

        graphics.setColor(
                new Color(
                        90,
                        205,
                        245,
                        225
                )
        );

        for (int z = spacing / 2; z < size; z += spacing) {
            for (int x = spacing / 2; x < size; x += spacing) {
                double windX =
                        world.climate()
                                .windX(x, z);

                double windZ =
                        world.climate()
                                .windZ(x, z);

                double magnitude =
                        Math.hypot(
                                windX,
                                windZ
                        );

                if (magnitude < 1.0e-6) {
                    continue;
                }

                double ux =
                        windX / magnitude;

                double uz =
                        windZ / magnitude;

                int endX =
                        (int) Math.round(
                                x + ux * arrowLength
                        );

                int endZ =
                        (int) Math.round(
                                z + uz * arrowLength
                        );

                graphics.drawLine(
                        x,
                        z,
                        endX,
                        endZ
                );

                double leftX =
                        endX
                                - ux * 5.0
                                + uz * 3.5;

                double leftZ =
                        endZ
                                - uz * 5.0
                                - ux * 3.5;

                double rightX =
                        endX
                                - ux * 5.0
                                - uz * 3.5;

                double rightZ =
                        endZ
                                - uz * 5.0
                                + ux * 3.5;

                graphics.drawLine(
                        endX,
                        endZ,
                        (int) Math.round(leftX),
                        (int) Math.round(leftZ)
                );

                graphics.drawLine(
                        endX,
                        endZ,
                        (int) Math.round(rightX),
                        (int) Math.round(rightZ)
                );
            }
        }

        graphics.dispose();

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }

    private static void writeClimateOrographicLift(
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
                double lift =
                        clamp01(
                                world.climate()
                                        .orographicLift(x, z)
                        );

                int gray =
                        clamp255(
                                18.0 + lift * 237.0
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(gray, gray, gray)
                );
            }
        }

        ImageIO.write(
                image,
                "PNG",
                path.toFile()
        );
    }


    private static void writeClimatePrecipitation(
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
                            rgb(20, 45, 72)
                    );
                    continue;
                }

                double precipitation =
                        clamp01(
                                world.climate()
                                        .orographicPrecipitation(x, z)
                        );

                double wet =
                        Math.pow(
                                precipitation,
                                0.72
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(128.0 - 88.0 * wet),
                                clamp255(92.0 + 132.0 * wet),
                                clamp255(48.0 + 166.0 * wet)
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


    private static void writeClimatePostOrographicMoisture(
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
                double moisture =
                        clamp01(
                                world.climate()
                                        .postOrographicMoisture(x, z)
                        );

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

                double relief =
                        clamp01(
                                0.52
                                        - dx * 0.0032
                                        - dz * 0.0032
                        );

                double shade =
                        0.62
                                + 0.58 * relief;

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        (18.0 + 45.0 * moisture)
                                                * shade
                                ),
                                clamp255(
                                        (30.0 + 155.0 * moisture)
                                                * shade
                                ),
                                clamp255(
                                        (42.0 + 205.0 * moisture)
                                                * shade
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

    private static void writeClimateRainShadow(
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
                            rgb(14, 25, 38)
                    );
                    continue;
                }

                double shadow =
                        clamp01(
                                world.climate()
                                        .rainShadowStrength(x, z)
                        );

                int value =
                        clamp255(
                                20.0
                                        + 235.0 * shadow
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                value,
                                value,
                                value
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


    private static void writeClimateRefinedPrecipitation(
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
                            rgb(20, 45, 72)
                    );
                    continue;
                }

                double precipitation =
                        clamp01(
                                world.climate()
                                        .refinedPrecipitation(x, z)
                        );

                double wet =
                        Math.pow(
                                precipitation,
                                0.72
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        128.0
                                                - 88.0 * wet
                                ),
                                clamp255(
                                        92.0
                                                + 132.0 * wet
                                ),
                                clamp255(
                                        48.0
                                                + 166.0 * wet
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


    private static void writeClimateRefinedMoisture(
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
                double moisture =
                        clamp01(
                                world.climate()
                                        .refinedMoisture(x, z)
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                clamp255(
                                        18.0
                                                + 45.0 * moisture
                                ),
                                clamp255(
                                        25.0
                                                + 160.0 * moisture
                                ),
                                clamp255(
                                        32.0
                                                + 215.0 * moisture
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

    private static BufferedImage hydrologyReliefBase(
            WorldBlueprint world
    ) {
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

                int west = Math.max(0, x - 1);
                int east = Math.min(size - 1, x + 1);
                int north = Math.max(0, z - 1);
                int south = Math.min(size - 1, z + 1);

                double dx =
                        world.elevation(east, z)
                                - world.elevation(west, z);

                double dz =
                        world.elevation(x, south)
                                - world.elevation(x, north);

                int shade =
                        clamp255(
                                (0.26
                                        - dx * 0.0025
                                        - dz * 0.0025)
                                        * 255.0
                        );

                image.setRGB(
                        x,
                        z,
                        rgb(
                                shade,
                                shade,
                                shade
                        )
                );
            }
        }

        return image;
    }


    private static double blockToPreviewPixel(
            WorldBlueprint world,
            double coordinate,
            int previewSize
    ) {
        double halfWorld =
                world.config().worldSizeBlocks()
                        * 0.5;

        return (
                coordinate + halfWorld
        )
                / world.config().worldSizeBlocks()
                * (previewSize - 1);
    }


    private static double macroGridBlockCoordinate(
            WorldBlueprint world,
            int gridCoordinate
    ) {
        double halfWorld =
                world.config().worldSizeBlocks()
                        * 0.5;

        double spacing =
                world.config().worldSizeBlocks()
                        / Math.max(
                        1.0,
                        world.resolution() - 1.0
                );

        return -halfWorld
                + gridCoordinate * spacing;
    }


    private static int centerlineOffsetColor(
            double normalized
    ) {
        double t =
                clamp01(normalized);

        if (t < 0.5) {
            double local =
                    t * 2.0;

            return rgb(
                    clamp255(40.0 + 190.0 * local),
                    clamp255(220.0 + 25.0 * local),
                    clamp255(100.0 - 55.0 * local)
            );
        }

        double local =
                (t - 0.5) * 2.0;

        return rgb(
                245,
                clamp255(245.0 - 195.0 * local),
                clamp255(45.0 - 20.0 * local)
        );
    }


    private static int continuityColor(
            double normalized
    ) {
        normalized =
                clamp01(normalized);

        if (normalized < 0.5) {
            double t =
                    normalized / 0.5;

            return rgb(
                    clamp255(30.0 + 210.0 * t),
                    clamp255(180.0 + 40.0 * t),
                    45
            );
        }

        double t =
                (normalized - 0.5) / 0.5;

        return rgb(
                240,
                clamp255(220.0 - 185.0 * t),
                45
        );
    }


    private record CorrectedProfilePoint(
            double distanceBlocks,
            double terrainElevation,
            double plannedBedElevation,
            boolean breakBefore
    ) {
    }


    private record CorrectedProfileSeries(
            List<CorrectedProfilePoint> points,
            long mouthAccumulation
    ) {
    }


    private record ProfilePoint(
            double distanceBlocks,
            double elevation
    ) {
    }


    private record ProfileSeries(
            List<ProfilePoint> points,
            long mouthAccumulation
    ) {
    }


    private static BufferedImage hydrologyDiagnosticBase(
            WorldBlueprint world,
            int landBrightness
    ) {
        int size =
                world.resolution();

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        int landColor =
                rgb(
                        landBrightness,
                        landBrightness,
                        landBrightness
                );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(
                        x,
                        z,
                        world.landMask(x, z) >= 0.5f
                                ? landColor
                                : rgb(0, 0, 0)
                );
            }
        }

        return image;
    }


    private static int heatColor(
            double normalized
    ) {
        double t =
                clamp01(normalized);

        if (t < 0.5) {
            double local =
                    t * 2.0;

            return rgb(
                    clamp255(35.0 + 80.0 * local),
                    clamp255(105.0 + 140.0 * local),
                    clamp255(225.0 - 120.0 * local)
            );
        }

        double local =
                (t - 0.5) * 2.0;

        return rgb(
                clamp255(115.0 + 140.0 * local),
                clamp255(245.0 - 170.0 * local),
                clamp255(105.0 - 55.0 * local)
        );
    }


    private static int riverScaleColor(
            RiverScale scale
    ) {
        return switch (scale) {
            case CREEK -> rgb(65, 105, 180);
            case STREAM -> rgb(45, 150, 220);
            case SMALL_RIVER -> rgb(40, 210, 230);
            case RIVER -> rgb(75, 225, 145);
            case LARGE_RIVER -> rgb(245, 205, 55);
            case MAJOR_RIVER -> rgb(255, 80, 60);
        };
    }


    private static int strahlerColor(
            int order
    ) {
        return switch (order) {
            case 1 -> rgb(55, 105, 190);
            case 2 -> rgb(35, 175, 225);
            case 3 -> rgb(55, 220, 155);
            case 4 -> rgb(220, 225, 60);
            case 5 -> rgb(255, 155, 45);
            case 6 -> rgb(255, 75, 55);
            default -> order >= 7
                    ? rgb(235, 70, 235)
                    : rgb(45, 45, 45);
        };
    }


    private static int riverNodeColor(
            RiverNodeType type
    ) {
        return switch (type) {
            case SOURCE -> rgb(85, 235, 95);
            case CONFLUENCE -> rgb(255, 225, 70);
            case LAKE_INLET -> rgb(40, 235, 235);
            case LAKE_OUTLET -> rgb(70, 115, 255);
            case MOUTH -> rgb(255, 75, 65);
        };
    }


    private static void drawMarker(
            BufferedImage image,
            int x,
            int z,
            int color,
            int radius
    ) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int pz = z + dz;

                if (
                        px >= 0
                                && px < image.getWidth()
                                && pz >= 0
                                && pz < image.getHeight()
                ) {
                    image.setRGB(px, pz, color);
                }
            }
        }
    }


    private static int streamClassColor(
            StreamClass streamClass
    ) {
        return switch (streamClass) {
            case NONE -> rgb(0, 0, 0);
            case HEADWATER -> rgb(55, 105, 180);
            case TRIBUTARY -> rgb(35, 160, 220);
            case RIVER -> rgb(35, 215, 235);
            case MAJOR_RIVER -> rgb(255, 205, 55);
            case TRUNK_RIVER -> rgb(255, 80, 60);
        };
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

