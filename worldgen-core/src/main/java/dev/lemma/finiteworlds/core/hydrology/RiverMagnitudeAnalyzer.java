package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrology Pass 2E: derives provisional river magnitude and channel scale
 * from the explicit graph and Pass-2D hierarchy metadata.
 *
 * This pass assumes spatially uniform runoff. It therefore estimates potential
 * drainage capacity rather than real discharge. Climate/precipitation can
 * later rescale the same topology without rebuilding the river graph.
 *
 * No terrain or channel geometry is modified here.
 */
public final class RiverMagnitudeAnalyzer {

    private static final double MINIMUM_WIDTH_BLOCKS = 1.5;
    private static final double MAXIMUM_WIDTH_BLOCKS = 64.0;

    private static final double MINIMUM_DEPTH_BLOCKS = 1.0;
    private static final double MAXIMUM_DEPTH_BLOCKS = 12.0;

    private RiverMagnitudeAnalyzer() {
    }

    public static void analyze(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        List<RiverSegment> segments =
                hydrology.riverSegments();

        List<RiverSegmentHierarchy> hierarchy =
                hydrology.riverSegmentHierarchy();

        if (segments.isEmpty()) {
            hydrology.setRiverSegmentMagnitudes(
                    List.of()
            );
            return;
        }

        if (hierarchy.size() != segments.size()) {
            throw new IllegalStateException(
                    "River magnitude analysis requires complete Pass-2D hierarchy metadata"
            );
        }

        long maximumAccumulation =
                Math.max(
                        1L,
                        hydrology.maximumFlowAccumulation()
                );

        int maximumStrahlerOrder =
                Math.max(
                        1,
                        hydrology.maximumStrahlerOrder()
                );

        double maximumNetworkLength =
                1.0;

        for (RiverSegment segment : segments) {
            RiverSegmentHierarchy segmentHierarchy =
                    hierarchy.get(segment.id());

            maximumNetworkLength =
                    Math.max(
                            maximumNetworkLength,
                            segmentHierarchy.totalUpstreamNetworkLengthBlocks()
                                    + segment.lengthBlocks()
                    );
        }

        double accumulationLogMaximum =
                Math.log1p(maximumAccumulation);

        double networkLogMaximum =
                Math.log1p(maximumNetworkLength);

        List<RiverSegmentMagnitude> result =
                new ArrayList<>(segments.size());

        for (RiverSegment segment : segments) {
            RiverSegmentHierarchy segmentHierarchy =
                    hierarchy.get(segment.id());

            long effectiveAccumulation =
                    Math.max(
                            1L,
                            Math.max(
                                    segment.startAccumulation(),
                                    segment.endAccumulation()
                            )
                    );

            double channelSlope =
                    Math.max(
                            0.0,
                            segment.elevationDrop()
                    )
                            / Math.max(
                            1.0,
                            segment.lengthBlocks()
                    );

            double accumulationFactor =
                    Math.log1p(effectiveAccumulation)
                            / accumulationLogMaximum;

            double orderFactor =
                    segmentHierarchy.strahlerOrder()
                            / (double) maximumStrahlerOrder;

            double networkFactor =
                    Math.log1p(
                            segmentHierarchy.totalUpstreamNetworkLengthBlocks()
                                    + segment.lengthBlocks()
                    )
                            / networkLogMaximum;

            /*
             * Slope has only a small influence on potential magnitude. Real
             * discharge should ultimately be governed by catchment runoff,
             * not by channel gradient. The gradient term mainly represents
             * additional hydraulic energy in the current uniform-runoff
             * approximation.
             */
            double slopeEnergy =
                    channelSlope
                            / (channelSlope + 0.020);

            double potentialMagnitude =
                    clamp01(
                            0.68 * accumulationFactor
                                    + 0.20 * orderFactor
                                    + 0.10 * networkFactor
                                    + 0.02 * slopeEnergy
                    );

            /*
             * Width begins with a square-root catchment relationship. This
             * gives approximately 2-3 block headwaters, ~10-20 block ordinary
             * rivers, and several-dozen-block major trunks at the current
             * production-world scale.
             */
            double baseWidth =
                    1.25
                            + 0.20
                            * Math.sqrt(effectiveAccumulation);

            double hierarchyMultiplier =
                    1.0
                            + 0.05
                            * Math.max(
                            0,
                            segmentHierarchy.strahlerOrder() - 1
                    );

            double networkMultiplier =
                    0.90
                            + 0.20
                            * networkFactor;

            /*
             * Steep mountain channels remain narrower for the same catchment;
             * low-gradient channels are allowed to spread laterally.
             */
            double slopeNarrowing =
                    1.18
                            - 0.36
                            * slopeEnergy;

            double provisionalWidth =
                    clamp(
                            baseWidth
                                    * hierarchyMultiplier
                                    * networkMultiplier
                                    * slopeNarrowing,
                            MINIMUM_WIDTH_BLOCKS,
                            MAXIMUM_WIDTH_BLOCKS
                    );

            /*
             * Depth grows much more slowly than width. Steeper channels get a
             * modest relative deepening so mountain rivers can remain narrow
             * and incised without implying enormous discharges.
             */
            double provisionalDepth =
                    clamp(
                            (0.55
                                    + 0.48
                                    * Math.sqrt(provisionalWidth))
                                    * (0.90 + 0.25 * slopeEnergy),
                            MINIMUM_DEPTH_BLOCKS,
                            MAXIMUM_DEPTH_BLOCKS
                    );

            RiverScale riverScale =
                    classifyScale(
                            provisionalWidth
                    );

            result.add(
                    new RiverSegmentMagnitude(
                            segment.id(),
                            riverScale,
                            potentialMagnitude,
                            channelSlope,
                            provisionalWidth,
                            provisionalDepth
                    )
            );
        }

        hydrology.setRiverSegmentMagnitudes(
                result
        );
    }

    private static RiverScale classifyScale(
            double widthBlocks
    ) {
        if (widthBlocks < 3.0) {
            return RiverScale.CREEK;
        }

        if (widthBlocks < 5.0) {
            return RiverScale.STREAM;
        }

        if (widthBlocks < 9.0) {
            return RiverScale.SMALL_RIVER;
        }

        if (widthBlocks < 16.0) {
            return RiverScale.RIVER;
        }

        if (widthBlocks < 28.0) {
            return RiverScale.LARGE_RIVER;
        }

        return RiverScale.MAJOR_RIVER;
    }

    private static double clamp01(
            double value
    ) {
        return clamp(
                value,
                0.0,
                1.0
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
