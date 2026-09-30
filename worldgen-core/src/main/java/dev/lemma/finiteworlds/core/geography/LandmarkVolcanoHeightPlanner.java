package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/** Gives the landmark edifice its own height budget, leaving other terrain unchanged. */
public final class LandmarkVolcanoHeightPlanner {
    // The dimension ends at Y=1535. Leave room for interpolation error and surface blocks.
    public static final double TARGET_SUMMIT_Y = 1520.0;

    private LandmarkVolcanoHeightPlanner() {
    }

    public static void apply(WorldBlueprint blueprint, float[] volcanicUplift) {
        int size = blueprint.resolution();
        if (volcanicUplift.length != size * size) {
            throw new IllegalArgumentException("Volcanic uplift must match blueprint resolution");
        }
        double maximum = 0.0;
        for (float uplift : volcanicUplift) {
            maximum = Math.max(maximum, uplift);
        }
        if (maximum <= 0.0) {
            return;
        }

        // Catmull-Rom interpolation can peak between cells. Calibrate against that
        // same continuous surface so a coarse macro grid cannot flatten the summit.
        double scale = Double.POSITIVE_INFINITY;
        double summitX = 0.0;
        double summitZ = 0.0;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double uplift = volcanicUplift[z * size + x];
                if (uplift <= 0.0) {
                    continue;
                }
                double background = blueprint.elevation(x, z) - uplift;
                double candidate = (TARGET_SUMMIT_Y - background) / uplift;
                if (candidate < scale) {
                    scale = candidate;
                    summitX = x;
                    summitZ = z;
                }
            }
        }
        for (int z = 0; z < size - 1; z++) {
            for (int x = 0; x < size - 1; x++) {
                if (Math.max(Math.max(volcanicUplift[z * size + x], volcanicUplift[z * size + x + 1]),
                        Math.max(volcanicUplift[(z + 1) * size + x], volcanicUplift[(z + 1) * size + x + 1]))
                        < maximum * 0.75) {
                    continue;
                }
                for (int dz = 0; dz <= 4; dz++) {
                    for (int dx = 0; dx <= 4; dx++) {
                        double px = x + dx / 4.0;
                        double pz = z + dz / 4.0;
                        double candidate = scaleAt(blueprint, volcanicUplift, px, pz);
                        if (candidate < scale) {
                            scale = candidate;
                            summitX = px;
                            summitZ = pz;
                        }
                    }
                }
            }
        }
        // Refine the selected summit to ~2-block spacing in the production grid.
        double centerX = summitX;
        double centerZ = summitZ;
        for (int dz = -8; dz <= 8; dz++) {
            for (int dx = -8; dx <= 8; dx++) {
                scale = Math.min(scale, scaleAt(blueprint, volcanicUplift,
                        centerX + dx / 32.0, centerZ + dz / 32.0));
            }
        }

        scale = Math.max(0.0, scale);
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                double addedUplift = volcanicUplift[z * size + x] * (scale - 1.0);
                if (addedUplift != 0.0) {
                    blueprint.setElevation(x, z, (float) (blueprint.elevation(x, z) + addedUplift));
                    blueprint.setCascadeUplift(x, z, (float) (blueprint.cascadeUplift(x, z) + addedUplift));
                }
            }
        }
    }

    private static double scaleAt(WorldBlueprint blueprint, float[] volcanicUplift, double x, double z) {
        double uplift = interpolate(blueprint, volcanicUplift, x, z, false);
        if (uplift <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double background = interpolate(blueprint, volcanicUplift, x, z, true);
        return (TARGET_SUMMIT_Y - background) / uplift;
    }

    private static double interpolate(WorldBlueprint blueprint, float[] field, double x, double z,
                                      boolean background) {
        int size = blueprint.resolution();
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        double[] rows = new double[4];
        for (int dz = -1; dz <= 2; dz++) {
            double[] values = new double[4];
            for (int dx = -1; dx <= 2; dx++) {
                int cx = Math.max(0, Math.min(size - 1, bx + dx));
                int cz = Math.max(0, Math.min(size - 1, bz + dz));
                double uplift = field[cz * size + cx];
                values[dx + 1] = background ? blueprint.elevation(cx, cz) - uplift : uplift;
            }
            rows[dz + 1] = catmullRom(values[0], values[1], values[2], values[3], x - bx);
        }
        return catmullRom(rows[0], rows[1], rows[2], rows[3], z - bz);
    }

    private static double catmullRom(double a, double b, double c, double d, double t) {
        return 0.5 * (2.0 * b + (-a + c) * t + (2.0 * a - 5.0 * b + 4.0 * c - d) * t * t
                + (-a + 3.0 * b - 3.0 * c + d) * t * t * t);
    }
}
