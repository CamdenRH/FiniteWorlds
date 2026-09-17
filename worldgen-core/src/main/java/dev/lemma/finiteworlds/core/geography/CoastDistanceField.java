package dev.lemma.finiteworlds.core.geography;

import dev.lemma.finiteworlds.core.WorldBlueprint;

public final class CoastDistanceField {

    /*
     * A very large finite number.
     *
     * Avoid Double.POSITIVE_INFINITY because
     * the distance-transform math performs
     * subtraction on these values.
     */
    private static final double INF =
            1.0e20;

    private CoastDistanceField() {
    }

    public static void populate(
            WorldBlueprint blueprint
    ) {

        int size =
                blueprint.resolution();

        /*
         * Source image:
         *
         * coastline cell = 0
         * everything else = INF
         */
        double[][] source =
                new double[size][size];

        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {

                source[z][x] =
                        isCoastCell(
                                blueprint,
                                x,
                                z
                        )
                                ? 0.0
                                : INF;
            }
        }


        /*
         * =====================================================
         * FIRST PASS
         *
         * Distance-transform every row.
         * =====================================================
         */

        double[][] rowPass =
                new double[size][size];

        double[] input =
                new double[size];

        double[] output =
                new double[size];

        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {
                input[x] =
                        source[z][x];
            }

            distanceTransform1D(
                    input,
                    output,
                    size
            );

            for (int x = 0; x < size; x++) {
                rowPass[z][x] =
                        output[x];
            }
        }


        /*
         * =====================================================
         * SECOND PASS
         *
         * Transform each column using the results
         * of the horizontal pass.
         *
         * Result is exact squared Euclidean distance.
         * =====================================================
         */

        double[] squaredDistance =
                new double[
                        size * size
                        ];

        for (int x = 0; x < size; x++) {

            for (int z = 0; z < size; z++) {
                input[z] =
                        rowPass[z][x];
            }

            distanceTransform1D(
                    input,
                    output,
                    size
            );

            for (int z = 0; z < size; z++) {

                squaredDistance[
                        index(
                                x,
                                z,
                                size
                        )
                        ] =
                        output[z];
            }
        }


        /*
         * =====================================================
         * CONVERT TO BLOCK DISTANCE
         *
         * positive = inland
         * negative = offshore
         * =====================================================
         */

        double blocksPerCell =
                blueprint.config()
                        .blocksPerCell();

        for (int z = 0; z < size; z++) {

            for (int x = 0; x < size; x++) {

                double distanceCells =
                        Math.sqrt(
                                squaredDistance[
                                        index(
                                                x,
                                                z,
                                                size
                                        )
                                        ]
                        );

                float distanceBlocks =
                        (float) (
                                distanceCells
                                        * blocksPerCell
                        );

                if (!isLand(
                        blueprint,
                        x,
                        z
                )) {
                    distanceBlocks =
                            -distanceBlocks;
                }

                blueprint.setCoastDistance(
                        x,
                        z,
                        distanceBlocks
                );
            }
        }
    }


    /*
     * =========================================================
     * EXACT 1D SQUARED EUCLIDEAN DISTANCE TRANSFORM
     *
     * Based on the lower envelope of parabolas.
     *
     * Applying this once horizontally and once vertically
     * produces the exact 2D Euclidean distance transform.
     * =========================================================
     */

    private static void distanceTransform1D(
            double[] f,
            double[] d,
            int length
    ) {

        int[] v =
                new int[length];

        double[] z =
                new double[
                        length + 1
                        ];

        int k = 0;

        v[0] = 0;

        z[0] =
                Double.NEGATIVE_INFINITY;

        z[1] =
                Double.POSITIVE_INFINITY;


        for (int q = 1; q < length; q++) {

            double s =
                    intersection(
                            f,
                            q,
                            v[k]
                    );

            while (
                    s <= z[k]
                            && k > 0
            ) {

                k--;

                s =
                        intersection(
                                f,
                                q,
                                v[k]
                        );
            }

            k++;

            v[k] =
                    q;

            z[k] =
                    s;

            z[k + 1] =
                    Double.POSITIVE_INFINITY;
        }


        k = 0;

        for (int q = 0; q < length; q++) {

            while (
                    z[k + 1] < q
            ) {
                k++;
            }

            double delta =
                    q - v[k];

            d[q] =
                    delta * delta
                            + f[v[k]];
        }
    }


    private static double intersection(
            double[] f,
            int q,
            int vk
    ) {

        return (
                (
                        f[q]
                                + q * (double) q
                )
                        -
                        (
                                f[vk]
                                        + vk
                                        * (double) vk
                        )
        )
                /
                (
                        2.0
                                * (q - vk)
                );
    }


    /*
     * =========================================================
     * COASTLINE DETECTION
     * =========================================================
     */

    private static boolean isCoastCell(
            WorldBlueprint blueprint,
            int x,
            int z
    ) {

        boolean land =
                isLand(
                        blueprint,
                        x,
                        z
                );

        int size =
                blueprint.resolution();


        if (
                x > 0
                        && isLand(
                        blueprint,
                        x - 1,
                        z
                ) != land
        ) {
            return true;
        }


        if (
                x < size - 1
                        && isLand(
                        blueprint,
                        x + 1,
                        z
                ) != land
        ) {
            return true;
        }


        if (
                z > 0
                        && isLand(
                        blueprint,
                        x,
                        z - 1
                ) != land
        ) {
            return true;
        }


        if (
                z < size - 1
                        && isLand(
                        blueprint,
                        x,
                        z + 1
                ) != land
        ) {
            return true;
        }


        return false;
    }


    private static boolean isLand(
            WorldBlueprint blueprint,
            int x,
            int z
    ) {

        return blueprint.landMask(
                x,
                z
        ) >= 0.5f;
    }


    private static int index(
            int x,
            int z,
            int size
    ) {

        return z
                * size
                + x;
    }
}