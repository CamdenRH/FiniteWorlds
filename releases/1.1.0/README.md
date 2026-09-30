# Finite Worlds 1.1.0

Minecraft **1.21.8**, Java **21**, Fabric Loader **0.19.5 or newer**, Fabric API (tested **0.136.1+1.21.8**). Optional Terralith **2.5.13** was tested with the Finite generator retained. See [the official Terralith release](https://modrinth.com/datapack/terralith/version/JKg71Gq0).

Local JAR: `C:/Dev/FiniteWorlds/releases/1.1.0/finite-worlds-1.1.0.jar`. Install this JAR as the only Finite Worlds version in your matching Minecraft instance. Prism installations were not modified.

Create a **new world**, select **Cascadia**, and leave the seed field blank for random terrain. Explicit seeds still work and survive save/reload. Existing generated chunks retain their old terrain; generating new chunks with this version can create boundaries at old/new chunk edges.

The source and IntelliJ project are current in `C:/Dev/FiniteWorlds`, branch `codex/north-cascades-realism`. Refresh Gradle in IDEA after opening the project. The previous release remains on main.

[Phase notes](../../docs/patches/north-cascades-realism.md), [terrain and biome evidence](../../docs/terrain-review/phase-4/README.md), [packaged client validation](../../docs/terrain-review/phase-5/README.md).

## Artifact

SHA-256: `2e3efb492cd872126f09addf29b15906a3b903dc8df843c1b1e70b24b6ea96c6`

Bundled core SHA-256: `2dff87c57cbbd20b00b33f896d06fc0c074f15c73b04192244095cc03b8c7e88`

The JAR includes its terrain core. Client tests and Terralith assets are excluded. Binaries are local build exports; Git tracks source, review images and this manifest. Rebuild with Java 21 using `./gradlew.bat build` from the repository root, then copy `build/libs/finite-worlds-1.1.0.jar` here. Rebuilt archive bytes may differ; the checksum above identifies the tested export.
