package dev.lemma.finiteworlds.core.biome;

/**
 * Registry-level realization of one semantic biome intent.
 *
 * preferredId may reference Terralith. vanillaFallbackId must always reference
 * a vanilla Minecraft biome so FiniteWorlds never requires Terralith merely to
 * interpret the 3G plan.
 */
public record BiomeTarget(
        String preferredId,
        String vanillaFallbackId
) {
    public boolean prefersTerralith() {
        return preferredId.startsWith(
                "terralith:"
        );
    }

    public String resolve(
            boolean terralithAvailable
    ) {
        if (
                terralithAvailable
                        && prefersTerralith()
        ) {
            return preferredId;
        }

        return vanillaFallbackId;
    }
}
