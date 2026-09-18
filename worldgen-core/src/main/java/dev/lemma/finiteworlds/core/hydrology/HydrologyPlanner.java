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
                assignRawDirection(
                        world,
                        hydrology,
                        x,
                        z,
                        size
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

    private static void assignRawDirection(
            WorldBlueprint world,
            HydrologyGrid hydrology,
            int x,
            int z,
            int size
    ) {
        if (!isLand(world, x, z)) {
            hydrology.setFlowDirection(
                    x,
                    z,
                    FlowDirection.OCEAN
            );
            return;
        }

        double sourceElevation =
                hydrology.surfaceElevation(
                        x,
                        z
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
                            - hydrology.surfaceElevation(
                            nx,
                            nz
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
            hydrology.setFlowDirection(
                    x,
                    z,
                    bestDirection
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
            hydrology.setFlowDirection(
                    x,
                    z,
                    FlowDirection.OUTLET
            );
            return;
        }

        if (isBoundaryCell(x, z, size)) {
            hydrology.setFlowDirection(
                    x,
                    z,
                    FlowDirection.OUTLET
            );
            return;
        }

        hydrology.setFlowDirection(
                x,
                z,
                FlowDirection.SINK
        );
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
