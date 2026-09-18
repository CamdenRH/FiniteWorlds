package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/**
 * First global hydrology pass.
 *
 * Pass 1B preserves the raw D8 behavior from Pass 1A but corrects one
 * important open-boundary case: a shoreline land cell that touches ocean is
 * allowed to discharge to the sea even when the shoreline fade leaves the
 * ocean neighbor at exactly the same elevation. Those cells become explicit
 * OUTLET cells instead of false SINKs.
 *
 * No depression filling, breaching, accumulation, or terrain mutation occurs
 * here. Interior cells with no lower neighbor remain explicit SINK cells so a
 * later pass can diagnose and resolve them intentionally.
 */
public final class HydrologyPlanner {

    private static final double MINIMUM_DROP =
            1.0e-6;

    private HydrologyPlanner() {
    }

    public static void populateRawFlow(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        int size =
                world.resolution();

        copySurface(
                world,
                hydrology,
                size
        );

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                assignDirection(
                        world,
                        hydrology,
                        x,
                        z,
                        size,
                        false
                );
            }
        }
    }


    /**
     * Recomputes D8 flow against the conditioned hydrology surface. The raw
     * terrain elevation and raw flow field remain unchanged for comparison.
     */
    public static void populateConditionedFlow(
            WorldBlueprint world
    ) {
        HydrologyGrid hydrology =
                world.hydrology();

        int size =
                world.resolution();

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                assignDirection(
                        world,
                        hydrology,
                        x,
                        z,
                        size,
                        true
                );
            }
        }
    }

    private static void copySurface(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            int size
    ) {
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                hydrology.setSurfaceElevation(
                        x,
                        z,
                        world.elevation(x, z)
                );
            }
        }
    }

    private static void assignDirection(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            int x,
            int z,
            int size,
            boolean conditioned
    ) {
        if (!isLand(world, x, z)) {
            setDirection(
                    hydrology,
                    x,
                    z,
                    FlowDirection.OCEAN,
                    conditioned
            );
            return;
        }

        double sourceElevation =
                elevation(
                        hydrology,
                        x,
                        z,
                        conditioned
                );

        FlowDirection bestDirection =
                null;

        double bestSlope =
                0.0;

        boolean touchesOcean =
                false;

        for (FlowDirection direction : FlowDirection.values()) {
            if (!direction.routesToNeighbor()) {
                continue;
            }

            int nx =
                    x + direction.dx();

            int nz =
                    z + direction.dz();

            if (
                    nx < 0
                            || nx >= size
                            || nz < 0
                            || nz >= size
            ) {
                continue;
            }

            if (!isLand(world, nx, nz)) {
                touchesOcean = true;
            }

            double drop =
                    sourceElevation
                            - elevation(
                            hydrology,
                            nx,
                            nz,
                            conditioned
                    );

            if (drop <= MINIMUM_DROP) {
                continue;
            }

            double slope =
                    drop
                            / direction.gridDistance();

            if (slope > bestSlope) {
                bestSlope = slope;
                bestDirection = direction;
            }
        }

        if (bestDirection != null) {
            setDirection(
                    hydrology,
                    x,
                    z,
                    bestDirection,
                    conditioned
            );
            return;
        }

        /*
         * The coastline generator deliberately fades land toward sea level.
         * A shoreline land cell can therefore be level with its ocean
         * neighbor even though water has an unobstructed escape to the sea.
         * Treat that open boundary as a terminal outlet rather than an
         * interior depression.
         */
        if (touchesOcean) {
            setDirection(
                    hydrology,
                    x,
                    z,
                    FlowDirection.OUTLET,
                    conditioned
            );
            return;
        }

        if (isBoundaryCell(x, z, size)) {
            setDirection(
                    hydrology,
                    x,
                    z,
                    FlowDirection.OUTLET,
                    conditioned
            );
            return;
        }

        setDirection(
                hydrology,
                x,
                z,
                FlowDirection.SINK,
                conditioned
        );
    }


    private static double elevation(
            HydrologyGrid hydrology,
            int x,
            int z,
            boolean conditioned
    ) {
        return conditioned
                ? hydrology.conditionedElevation(x, z)
                : hydrology.surfaceElevation(x, z);
    }

    private static void setDirection(
            HydrologyGrid hydrology,
            int x,
            int z,
            FlowDirection direction,
            boolean conditioned
    ) {
        if (conditioned) {
            hydrology.setConditionedFlowDirection(
                    x,
                    z,
                    direction
            );
        } else {
            hydrology.setFlowDirection(
                    x,
                    z,
                    direction
            );
        }
    }

    private static boolean isLand(
            WorldBlueprint world,
            int x,
            int z
    ) {
        return world.landMask(x, z) >= 0.5f;
    }

    private static boolean isBoundaryCell(
            int x,
            int z,
            int size
    ) {
        return x == 0
                || z == 0
                || x == size - 1
                || z == size - 1;
    }
}
