# Minecraft client validation — Finite Worlds 1.1.0

Both final packaged-JAR client runs passed on Minecraft 1.21.8, Java 21, Fabric Loader 0.19.5 and Fabric API 0.136.1+1.21.8. The optional run loaded the official Terralith 2.5.13 mod. [Machine-readable results and hashes](validation.json).

The separate [release consumer](../../../tooling/release-smoke/README.md) depends on the remapped release JAR and the exact terrain-core bytes extracted from inside that JAR. It compiles only the client test, not production Finite Worlds sources. Loom removes nested-JAR metadata from remapped development dependencies, so the consumer supplies the bundled core explicitly. This is an actual rendered Minecraft integrated-server test in Loom's development namespace; it is not a manual Prism launch or a production-namespace launcher test. Prism's installed files were only inspected and were not edited.

The test selects Cascadia through the actual WorldCreator settings, then asserts:

- Finite remains the overworld generator; the dimension ceiling remains Y1535.
- Completed terrain chunks exist at spawn, two other locations, and the seed-12345 volcano; the actual summit chunk reaches Y1400+.
- Terralith's registered palette remains available when installed.
- Actual forest features place tree logs in a 3×3 chunk neighborhood: **874** in the Terralith sample at (-5312,-22976), **300** in the vanilla sample at (-4544,-23232). Tick timing can alter these counts; nonzero placement is the assertion.
- Terrain in front of the camera is loaded and rendered before screenshots.
- Saving and reopening preserves the Finite generator and explicit seed 12345.
- Two independently created worlds with blank seed fields have different saved seeds and different terrain sample signatures, and retain Finite generation.

| Client run | Result | Runtime | Random-world terrain signatures |
|---|---|---|---|
| Terralith 2.5.13 | PASS | 54 seconds | 337.000143; 234.439992 |
| Vanilla fallback | PASS | 52 seconds | 201.287453; 229.499487 |

These durations describe this local test, not a general performance benchmark. View distance was 5. An earlier vanilla screenshot wait timed out on the global render queue; the final test checks the visible terrain section rather than requiring all background render activity to stop. This does not validate high render distances, all fluids after extended ticking, multiplayer, or other world-generation mod combinations.

## Screenshots

[Actual Terralith summit](terralith-client-volcano.png), [Terralith spawn](terralith-client-spawn.png), [vanilla summit](vanilla-client-volcano.png), [vanilla spawn](vanilla-client-spawn.png). The close summit view shows snow and Terralith ice features. For continent-scale terrain structure, use the [phase 4 sampled views](../phase-4/README.md), which are explicitly labeled terrain review renders.

## Build and package verification

The final root `build` passed: 17 core regression tests plus the Minecraft registry/codec bootstrap test, which loads real Terralith resources and validates dimension selection, biome tags, feature ordering and source fallback. The [fourteen-seed audit](../phase-4/seed-audit.csv) checked 951,341 physical center samples; all were wet, with zero measured downstream water rises.

Export: `C:/Dev/FiniteWorlds/releases/1.1.0/finite-worlds-1.1.0.jar` (425,707 bytes).
SHA-256: `2e3efb492cd872126f09addf29b15906a3b903dc8df843c1b1e70b24b6ea96c6`.
The bundled core matches the consumer's dependency byte for byte. The release contains no client-test classes and no copied Terralith datapack definitions.

Use a fresh Cascadia world to assess this release. Existing chunk terrain does not regenerate and can meet new terrain at visible seams. Blank seeds are now random; setting an explicit seed remains supported.
