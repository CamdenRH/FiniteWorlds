package dev.lemma.finiteworlds.worldgen;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

/** Exercises the real create-world screen, integrated server, chunk features and reload. */
public final class TerrainClientSmokeTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    TestWorldSave save;
    try (var singleplayer =
        context
            .worldBuilder()
            .adjustSettings(
                settings -> {
                  var cascadian =
                      settings.getNormalWorldTypes().stream()
                          .filter(
                              type ->
                                  type.preset().matchesId(Identifier.of("finite-worlds:cascadia")))
                          .findFirst()
                          .orElseThrow(
                              () -> new AssertionError("Cascadia missing from world screen"));
                  settings.setWorldType(cascadian);
                  settings.setSeed("12345");
                  settings.setGameMode(WorldCreator.Mode.CREATIVE);
                })
            .create()) {
      save = singleplayer.getWorldSave();
      singleplayer
          .getServer()
          .runOnServer(
              server -> {
                var world = server.getOverworld();
                if (!(world.getChunkManager().getChunkGenerator() instanceof FiniteChunkGenerator))
                  throw new AssertionError("Selected Finite generator was replaced");
                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("terralith")) {
                  long count =
                      world
                          .getChunkManager()
                          .getChunkGenerator()
                          .getBiomeSource()
                          .getBiomes()
                          .stream()
                          .filter(
                              b ->
                                  b.getKey()
                                      .orElseThrow()
                                      .getValue()
                                      .getNamespace()
                                      .equals("terralith"))
                          .count();
                  if (count < 10) throw new AssertionError("Terralith biomes were not retained");
                }
                if (world.getTopYInclusive() != 1535)
                  throw new AssertionError("Tall Finite dimension was replaced");
                for (int[] coordinate :
                    new int[][] {{0, 0}, {512, 512}, {-512, -512}, {2466, -2082}}) {
                  var chunk = world.getChunk(coordinate[0] >> 4, coordinate[1] >> 4);
                  int surface =
                      chunk.sampleHeightmap(
                          Heightmap.Type.WORLD_SURFACE, coordinate[0] & 15, coordinate[1] & 15);
                  if (surface < 64) throw new AssertionError("Missing completed surface chunk");
                  if (coordinate[0] == 2466 && surface < 1400)
                    throw new AssertionError(
                        "Completed client summit did not reach its planned height: " + surface);
                }
                var source =
                    (FiniteBiomeSource)
                        world.getChunkManager().getChunkGenerator().getBiomeSource();
                int forestX = 0, forestZ = 0;
                boolean forest = false;
                search:
                for (int z = -24000; z <= 24000; z += 256)
                  for (int x = -24000; x <= 24000; x += 256) {
                    String id =
                        source
                            .getBiome(x >> 2, 0, z >> 2, null)
                            .getKey()
                            .orElseThrow()
                            .getValue()
                            .toString();
                    double height = source.terrainSampler().surfaceElevationAt(x, z);
                    if ((id.contains("cloud_forest")
                            || id.equals("minecraft:forest")
                            || id.contains("temperate_forest"))
                        && height > 90
                        && height < 500) {
                      forestX = x;
                      forestZ = z;
                      forest = true;
                      break search;
                    }
                  }
                if (!forest) throw new AssertionError("No temperate forest biome found");
                long logs = 0;
                for (int cz = (forestZ >> 4) - 1; cz <= (forestZ >> 4) + 1; cz++)
                  for (int cx = (forestX >> 4) - 1; cx <= (forestX >> 4) + 1; cx++) {
                    var chunk = world.getChunk(cx, cz);
                    for (int z = 0; z < 16; z++)
                      for (int x = 0; x < 16; x++) {
                        int top = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z);
                        for (int y = top - 25; y <= top; y++)
                          if (chunk
                              .getBlockState(new BlockPos(cx * 16 + x, y, cz * 16 + z))
                              .isIn(BlockTags.LOGS)) logs++;
                      }
                  }
                if (logs == 0)
                  throw new AssertionError("Forest biome placed no tree logs in completed chunks");
                System.out.printf(
                    "CLIENT FOREST: position=(%d,%d) tree logs=%d%n", forestX, forestZ, logs);
                server
                    .getPlayerManager()
                    .getPlayerList()
                    .getFirst()
                    .teleport(
                        world,
                        0,
                        world.getTopY(Heightmap.Type.WORLD_SURFACE, 0, 0) + 30,
                        0,
                        java.util.Set.of(),
                        0,
                        25,
                        true);
              });
      context.runOnClient(
          client -> {
            client.options.hudHidden = true;
            client.options.getViewDistance().setValue(5);
            client.player.getAbilities().flying = true;
            client.player.sendAbilitiesUpdate();
          });
      context.waitTicks(40);
      waitForVisibleTerrain(context);
      context.takeScreenshot(
          TestScreenshotOptions.of("finite-cascadia-client").withSize(1280, 720));
      singleplayer
          .getServer()
          .runOnServer(
              server -> {
                var world = server.getOverworld();
                server
                    .getPlayerManager()
                    .getPlayerList()
                    .getFirst()
                    .teleport(world, 2466, 1480, -2082, java.util.Set.of(), 30, 50, true);
              });
      context.waitFor(
          client -> client.world.getChunk(2466 >> 4, -2082 >> 4, ChunkStatus.FULL, false) != null,
          1200);
      context.waitTicks(40);
      waitForVisibleTerrain(context);
      context.takeScreenshot(TestScreenshotOptions.of("finite-volcano-client").withSize(1280, 720));
    }
    try (var reloaded = save.open()) {
      reloaded
          .getServer()
          .runOnServer(
              server -> {
                if (!(server.getOverworld().getChunkManager().getChunkGenerator()
                    instanceof FiniteChunkGenerator))
                  throw new AssertionError("Saved generator did not survive reload");
                if (server.getOverworld().getSeed() != 12345L)
                  throw new AssertionError("World seed did not survive reload");
              });
    }
    long previousSeed = 12345L;
    double previousTerrain = Double.NaN;
    for (int attempt = 0; attempt < 2; attempt++) {
      try (var random =
          context
              .worldBuilder()
              .setUseConsistentSettings(false)
              .adjustSettings(
                  settings -> {
                    settings.setWorldType(
                        settings.getNormalWorldTypes().stream()
                            .filter(
                                type ->
                                    type.preset()
                                        .matchesId(Identifier.of("finite-worlds:cascadia")))
                            .findFirst()
                            .orElseThrow());
                    settings.setSeed("");
                    settings.setGenerateStructures(false);
                    settings.setGameMode(WorldCreator.Mode.CREATIVE);
                  })
              .create()) {
        long generated =
            random.getServer().computeOnServer(server -> server.getOverworld().getSeed());
        double signature =
            random
                .getServer()
                .computeOnServer(
                    server -> {
                      var world = server.getOverworld();
                      var generator = world.getChunkManager().getChunkGenerator();
                      if (!(generator instanceof FiniteChunkGenerator))
                        throw new AssertionError("Random world replaced Finite");
                      var source = (FiniteBiomeSource) generator.getBiomeSource();
                      var terrain = source.terrainSampler();
                      return terrain.surfaceElevationAt(0, 0)
                          + terrain.surfaceElevationAt(1200, -800) * .731;
                    });
        if (generated == previousSeed || generated == 12345L)
          throw new AssertionError("Blank world seed was fixed");
        if (signature == previousTerrain)
          throw new AssertionError("Different saved seeds produced identical sampled terrain");
        System.out.printf("CLIENT RANDOM: seed=%d terrain signature=%.6f%n", generated, signature);
        previousSeed = generated;
        previousTerrain = signature;
      }
    }
  }

  private static void waitForVisibleTerrain(ClientGameTestContext context) {
    // Ticking leaves, fluids and lighting can keep the global render queue busy.
    // Require the actual terrain section in front of the camera to be rendered.
    context.waitFor(
        client -> {
          double yaw = Math.toRadians(client.player.getYaw());
          int x = client.player.getBlockX() - (int) Math.round(Math.sin(yaw) * 48);
          int z = client.player.getBlockZ() + (int) Math.round(Math.cos(yaw) * 48);
          var chunk = client.world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
          if (chunk == null) return false;
          int y = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15) - 1;
          return client.worldRenderer.isRenderingReady(new BlockPos(x, y, z));
        },
        1200);
    context.waitTicks(20);
    System.out.println(
        "CLIENT RENDER: "
            + context.computeOnClient(client -> client.worldRenderer.getChunksDebugString()));
  }
}
