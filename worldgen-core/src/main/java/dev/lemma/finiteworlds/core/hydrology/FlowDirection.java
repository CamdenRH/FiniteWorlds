package dev.lemma.finiteworlds.core.hydrology;

/**
 * Raw D8 drainage direction for one hydrology-grid cell.
 *
 * SINK is intentionally preserved during Hydrology Pass 1A. A later
 * depression-resolution pass will decide whether a sink should be filled,
 * breached, or retained as a lake basin.
 */
public enum FlowDirection {
    OCEAN(0, 0, false),
    SINK(0, 0, false),
    OUTLET(0, 0, false),

    NORTH(0, -1, true),
    NORTH_EAST(1, -1, true),
    EAST(1, 0, true),
    SOUTH_EAST(1, 1, true),
    SOUTH(0, 1, true),
    SOUTH_WEST(-1, 1, true),
    WEST(-1, 0, true),
    NORTH_WEST(-1, -1, true);

    private final int dx;
    private final int dz;
    private final boolean routesToNeighbor;

    FlowDirection(
            int dx,
            int dz,
            boolean routesToNeighbor
    ) {
        this.dx = dx;
        this.dz = dz;
        this.routesToNeighbor = routesToNeighbor;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public boolean routesToNeighbor() {
        return routesToNeighbor;
    }

    public double gridDistance() {
        if (!routesToNeighbor) {
            return 0.0;
        }

        return dx != 0 && dz != 0
                ? Math.sqrt(2.0)
                : 1.0;
    }

    public static FlowDirection fromOrdinal(
            int ordinal
    ) {
        FlowDirection[] values = values();

        if (ordinal < 0 || ordinal >= values.length) {
            return SINK;
        }

        return values[ordinal];
    }
}
