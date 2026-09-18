package dev.lemma.finiteworlds.core.geography;

import java.util.List;

/**
 * One large structural buttress descending from the landmark volcano.
 *
 * Coordinates are stored directly in Cascade-local space so the ridge can
 * continue beyond the analytic volcanic cone and merge into surrounding
 * Cascade terrain.  These are macro landforms; later hydrology and erosion
 * will carve valleys between and across them.
 */
public record CascadeVolcanicRidge(
        List<Node> nodes,
        double startWidth,
        double endWidth,
        double strength,
        boolean secondary,
        double minimumSignedDistance,
        double maximumSignedDistance,
        double minimumArc,
        double maximumArc,
        double maximumWidth
) {

    public CascadeVolcanicRidge {
        nodes = List.copyOf(nodes);
    }

    public record Node(
            double signedDistance,
            double arcPosition,
            double progress
    ) {
    }
}
