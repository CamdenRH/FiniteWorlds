# Patch 4C: coastal surfaces (version 1.0.1)

This release includes patch 4B's world-seed fix and adds coastal materials.
Beach biomes previously received the same grass surface as inland terrain.
They now render according to the existing authored coastal classification.

| Coastal intent | Surface | Underlying material |
| --- | --- | --- |
| Sandy or cold beach | Sand | Sandstone, then stone |
| Gravel beach | Gravel | Stone |
| Rocky coast | Exposed stone | Stone |

Gravel beaches and rocky coasts keep distinct materials even when their
biomes both fall back to `minecraft:stony_shore`. This release preserves the
existing biome map, boundary shape, and biome registry palette. It does not
add Terralith biomes when Terralith is absent.

The renderer and column query now agree on these surface layers, including
the existing dirt layers inland. Sea water still uses the existing prototype
sea level of Y=64. Terrain elevations are unchanged in this patch.

## Validation

The Minecraft runtime regression suite checks generated chunks for dry sand,
gravel, rocky coast, and ordinary forest. It compares actual blocks against
column queries, verifies stored biome placement and adjacent chunk borders,
and checks shallow sandy shore water. A direct cold-beach check covers sand
and sandstone because the production test seed has no cold beach.

The previous random-seed, save/reload, recreation, and registry-freeze tests
remain part of the build. Visual client inspection is the next check.

## Client check

Replace the prior Finite Worlds JAR with this version. Create a fresh Cascadia
world, or explore newly generated chunks; already-generated chunks keep their
saved blocks. Look for sand and gravel beaches and exposed stone at rocky
shores. F3 should continue to show the chosen world seed and coastal biome.

For a quick comparison, create a fresh Cascadia world with seed `1`, enable
commands, and use these locations from the runtime checks:

| Surface | Teleport command |
| --- | --- |
| Sandy beach | `/tp @s 2852 68 -24184` |
| Gravel beach | `/tp @s 3556 67 -23992` |
| Rocky coast | `/tp @s -14316 68 -21620` |

These coordinates are for seed `1`; other seeds place the coast differently.

## Next patch

Patch 4D will address terrain height and mountain relief, including ordinary
mountain ranges and foothills as well as the landmark volcano. This is next
in response to the reported overall flatness. Biome boundary smoothing,
physical rivers, and the snow belt remain separate later patches.
