package dev.lemma.finiteworlds.core.hydrology;

/**
 * Explicit topological landmark in the macro river graph.
 *
 * A node may satisfy more than one role at once (for example, a confluence at
 * a lake inlet). The boolean role flags preserve that information while
 * primaryType() provides a stable diagnostic label.
 */
public record RiverNode(
        int id,
        int x,
        int z,
        boolean source,
        boolean confluence,
        boolean lakeInlet,
        boolean lakeOutlet,
        boolean mouth,
        int inletLakeId,
        int outletLakeId,
        StreamClass streamClass,
        long flowAccumulation,
        float elevation
) {

    public RiverNodeType primaryType() {
        if (mouth) {
            return RiverNodeType.MOUTH;
        }

        if (lakeInlet) {
            return RiverNodeType.LAKE_INLET;
        }

        if (lakeOutlet) {
            return RiverNodeType.LAKE_OUTLET;
        }

        if (confluence) {
            return RiverNodeType.CONFLUENCE;
        }

        return RiverNodeType.SOURCE;
    }
}
