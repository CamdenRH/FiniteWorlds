# Minecraft terrain and biome placement

**Released prototype:** patch 4B enables Minecraft's selected or random world
seed. Patch 4C (version 1.0.1) adds sand, sandstone, gravel, and stone coastal
surfaces to that prototype, preserving its existing elevations, biome map,
and sea-level water. See `docs/patches/4b-world-seeds.md` and
`docs/patches/4c-coastal-surfaces.md` for the individual releases.

**DRAFT terrain integration:** the terrain, biome boundary, river, and snow
changes described below remain staged in the Codex worktree. They have not
been applied to the IntelliJ project or included in the delivered patch 4C
JAR. Patch 4D is next and will address terrain height and mountain relief,
including ordinary ranges and foothills as well as the landmark volcano.
Biome boundary smoothing, physical rivers, and the snow belt remain separate
later patches, each with its own client check.

The Cascadia preset uses `finite-worlds:finite` for terrain and biomes. Both
share one production blueprint and one continuous terrain sampler. New
Cascadia worlds use the seed selected in Minecraft's world creation screen;
leaving that field blank uses Minecraft's normal random selection. Planning
waits until server initialization supplies the saved world seed. An existing
world's explicit generator `seed` remains authoritative for terrain and biomes
to preserve its old chunks. New worlds save no generator seed override, so
Re-create World can use a different selected seed.

## Terrain

The landmark volcano receives its own vertical scale, calibrated against the
interpolated surface to target a summit near Y=1520. The dimension runs from
Y=-512 through Y=1535. Local detail fades near the summit to preserve this
height budget without clipping a flat plateau into the edifice. Ordinary
mountains keep their original macro elevation.

Mountain uplift masks now enable coherent warped ridges, rock detail, and
erosion-style gullies through the foothills. These are deterministic landform
shapes, rather than a time-stepped hydraulic or glacial erosion simulation.

Planned river cross-sections carve continuous channels after local detail.
An immutable spatial index supplies beds, asymmetric banks, valley shoulders,
and downstream water levels at block resolution. Lakes use a constant water
surface with interpolated depths and eased shores. Minecraft places water up
to these local levels, including rivers above ocean level. Submerged beds use
gravel; coasts retain patch 4C's intent-specific sand, sandstone, gravel, and
stone layers above and below water. Other prototype surfaces use grass, dirt,
and stone. Chunk
filling, column samples, and height queries use the same column calculation.

## Biomes and snow

The accepted 3G.3 semantic map remains the source of biome intent. Continuous
class-membership interpolation and seeded domain warping replace the old
64-block nearest-cell squares with curved, irregular boundaries. Physical wet
river channels select river biomes; land outside those channels uses the
surrounding land intent. Minecraft still stores biomes at four-block
resolution, and each biome fills its vertical column. Outside the finite
blueprint, the biome resolves to ocean.

Available Terralith targets are preferred, with independent vanilla fallbacks
for missing targets. A raised seasonal snow belt is centered around Y=624,
varying with latitude and coherent noise. Permanent snow starts about 375
blocks higher. Warm forest, steppe, and river variants prevent Minecraft's
altitude cooling from producing snow below the planned belt, while retaining
their vanilla vegetation and features. Warm enough Terralith targets retain
their original selection. These variants are in
`src/main/resources/data/finite-worlds/worldgen/biome`.

Placing a Terralith biome does not run Terralith's terrain noise generator or
surface rules. Patch 4C supplies coastal surface materials from the authored
intent map. Inland biome-specific surface rules and underground biomes remain
future work.

The biome source codec reads the registry owner's wrapper when probing
optional biomes. Minecraft's registry-loading entry lookup creates forward
references even for `getOptional`; probing absent Terralith IDs through that
lookup would leave unbound entries and crash world creation.

## Validation and client inspection

Run with Java 21:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Core tests cover boundary continuity, coastal pocket rounding, river infill,
snowline variation, volcanic height calibration, local mountain relief,
physical river cross-sections, lakes, and ocean water. Fabric Loader JUnit
checks optional registry fallback and freeze safety, biome JSON loading and
precipitation, seed consistency, and actual Minecraft `ProtoChunk` block and
biome filling. The production preset test checks the rendered summit and wet
river centerlines, including a river above ocean level.

Create a fresh world using Cascadia for a consistent visual comparison.
Existing chunks retain their blocks and biomes; the new terrain appears only
in newly generated chunks. Visual client inspection and testing with the
actual Terralith datapack remain separate from the automated runtime checks.
