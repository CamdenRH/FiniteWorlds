# Pass 4A: Minecraft biome placement

The Cascadia preset now uses `finite-worlds:finite` as its biome source. It
samples the accepted 3G.3 biome intent grid and resolves each intent through
Minecraft's loaded biome registry. Available Terralith targets are preferred;
each absent target independently uses its vanilla fallback.

Terrain and biome placement share one production blueprint. Since patch 4B,
fresh Cascadia worlds use Minecraft's saved world seed: an entered seed is
honored and a blank field uses Minecraft's normal random seed generation.
The generator binds that seed before planning terrain or sampling biomes.
Existing worlds with an explicit generator seed retain it for chunk continuity.
New worlds keep the generator override absent, including after saving, so
Re-create World can select a different seed. See `docs/patches/4b-world-seeds.md`.

Sampling uses nearest blueprint cells at Minecraft's four-block biome
coordinates. The same biome fills each vertical column, and coordinates
outside the finite blueprint resolve to ocean. This first bridge preserves
the categorical map; finer biome boundary blending and underground biomes can
be developed separately.

Since patch 4C, sandy and cold beaches use sand over sandstone, gravel beaches
use gravel, and rocky coasts expose stone. These materials follow the original
semantic intent, keeping gravel and rock distinct even when both resolve to
the vanilla stony shore biome. Other surfaces retain grass, dirt, and stone.
Chunk filling and column queries use the same surface rules. Placing a
Terralith biome does not run Terralith's terrain noise or surface rules.
See `docs/patches/4c-coastal-surfaces.md` for this release and its client checks.

## Validation

Run with Java 21:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

The core tests cover every semantic intent, negative coordinates, nearest-cell
transitions, all four finite-world edges, and mismatched grid resolutions.
Fabric Loader JUnit tests cover the vanilla fallback palette, a partially
available optional palette, source codec reloads, and a full production seed-1
blueprint. The integration check samples every intent present in that world,
checks vertical consistency and the outer ocean, and verifies that the chunk
generator overrides a mismatched biome source seed.

The biome source codec uses the registry owner's read-only wrapper when
testing optional biome availability. Minecraft's registry-loading lookup
creates forward references even for `getOptional`; probing absent Terralith
IDs through that lookup would leave unbound entries and crash the world
creation screen. Regression tests use the mutable registry-loading context
and check that the registry can freeze after decoding the source.

## In-game inspection

Create a new world using the Cascadia preset and check the F3 biome names as
you move through forest, coast, mountains, and the eastern interior. Repeat
with Terralith loaded to inspect preferred biome selection. Existing saved
worlds retain their serialized generator/biome source, and existing chunks
retain their saved biomes; changing the bundled preset does not migrate them.

Automated placement checks have passed. Visual client inspection and testing
with the actual Terralith datapack remain to be done.
