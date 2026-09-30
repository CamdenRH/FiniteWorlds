# Patch 4B: use the world creation seed

This patch connects Finite Worlds to Minecraft's saved world seed and removes
the preset's fixed `12345`. Terrain and biomes use the same chosen seed.
Leaving the seed field blank uses Minecraft's normal random seed selection.
Entered seeds, including zero and negative values, work normally.

Terrain planning now waits until Minecraft's server initialization supplies
the world seed. Fresh worlds save no generator seed override, allowing
Re-create World to use a different seed. Existing worlds that already saved
an explicit generator seed retain it to avoid changing terrain at old chunk
borders. Use a fresh Cascadia world to test this patch.

## Changed files

- `FiniteChunkGenerator.java`: optional legacy seed, startup binding, lazy
  terrain planning, and F3 seed display.
- `FiniteBiomeSource.java`: optional seed and initialization of the shared
  biome source before coordinate queries.
- `cascadia.json`: removed the fixed preset seed.
- `FiniteBiomeSourceChecks.java`: startup, random selection, zero/negative
  seeds, distinct terrain, save/reload, recreation, and legacy compatibility.
- Documentation: this patch record and the seed description in biome placement.

## Client check

1. Create two fresh worlds with the Cascadia preset and different entered seeds.
2. Check F3's `Finite Worlds seed` line matches the chosen seed in each world.
3. Compare the terrain; the two worlds should differ.
4. Save and reopen one world. Its terrain must remain consistent.
5. Re-create it with a different seed and confirm F3 shows the new value.
6. Leave the seed blank in a fresh world to use a random seed.

## Patch queue

Only the seed change is included in this build. The next separate patch will
address visible coastal surfaces and verify biome placement. The current
renderer uses grass even for beach biomes. The old production seed's semantic
map contains sandy beaches, gravel beaches, and rocky coasts across 6.18% of
land, so coast placement and surface material must be distinguished.

Biome boundary smoothing, volcano height, physical rivers, mountain relief,
and the raised snow belt remain drafts in the Codex worktree. They have not
been applied to the IntelliJ project or included in this seed-only JAR. Each
will be delivered separately with its own change description and client check.
