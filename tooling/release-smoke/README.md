# Packaged Minecraft client smoke test

Use Java 21. First build the release at the repository root, then run the separate consumer project:

```powershell
.\gradlew.bat build
.\gradlew.bat -p tooling/release-smoke runClientGameTest
.\gradlew.bat -p tooling/release-smoke runClientGameTest '-PterralithJar=../../build/compat-mods/Terralith_1.21.x_v2.5.13.jar'
```

Get the optional Terralith JAR from its [official 2.5.13 release](https://modrinth.com/datapack/terralith/version/JKg71Gq0); it is not checked in. Paths supplied as consumer Gradle properties resolve against `tooling/release-smoke`. An alternate release artifact can be supplied with `-PfiniteJar=<path>`. The default is `../../build/libs/finite-worlds-1.1.0.jar`.

The consumer loads the packaged release and extracts its exact nested terrain core because Loom strips nested dependency metadata while remapping development mod dependencies. No main Finite Worlds source set is included. The only shared source is the separate client test under `src/gametest`. The root project also offers `runClientGameTest` for source development.

Saves, options and screenshots live in `tooling/release-smoke/build/run/clientGameTest`; they do not touch a Prism instance. The test uses the real rendered Minecraft client and integrated server in the development namespace. It checks world selection, completed chunks/features, save/reload, random seeds and visible terrain rendering. It is not a production-namespace launcher test or a full performance benchmark. Review results and screenshots in [phase 5](../../docs/terrain-review/phase-5/README.md).
