package dev.lemma.finiteworlds.core.biome;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable 3G semantic-biome grid aligned 1:1 with WorldBlueprint.
 */
public final class BiomeIntentPlan {

    private final int resolution;
    private final byte[] intent;
    private final int[] counts;

    BiomeIntentPlan(
            int resolution,
            byte[] intent,
            int[] counts
    ) {
        this.resolution =
                resolution;

        this.intent =
                intent.clone();

        this.counts =
                counts.clone();
    }

    public int resolution() {
        return resolution;
    }

    public BiomeIntent intent(
            int x,
            int z
    ) {
        return BiomeIntent.fromOrdinal(
                Byte.toUnsignedInt(
                        intent[index(x, z)]
                )
        );
    }

    public int count(
            BiomeIntent biomeIntent
    ) {
        return counts[
                biomeIntent.ordinal()
                ];
    }

    public Map<BiomeIntent, Integer> counts() {
        Map<BiomeIntent, Integer> result =
                new EnumMap<>(
                        BiomeIntent.class
                );

        for (BiomeIntent biomeIntent : BiomeIntent.values()) {
            result.put(
                    biomeIntent,
                    count(biomeIntent)
            );
        }

        return Map.copyOf(
                result
        );
    }

    private int index(
            int x,
            int z
    ) {
        return z * resolution + x;
    }
}
