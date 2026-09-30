package dev.lemma.finiteworlds.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lemma.finiteworlds.FiniteWorlds;
import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import dev.lemma.finiteworlds.core.biome.BiomeIntentPlanner;
import dev.lemma.finiteworlds.core.biome.BiomeTargetResolver;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Registry and codec checks that do not require launching a client. */
public final class FiniteBiomeSourceChecks {
    @Test
    public void registryAndCodecChecks() throws Exception {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        new FiniteWorlds().onInitialize();
        var registries = BuiltinRegistries.createWrapperLookup();
        TerralithCompatibilityChecks.run(registries);
        var vanilla = registries.getOrThrow(RegistryKeys.BIOME);
        var source = new FiniteBiomeSource(vanilla, 1L);
        var expected = java.util.Arrays.stream(BiomeIntent.values())
                .map(intent -> vanilla.getOrThrow(key(BiomeTargetResolver.target(intent).vanillaFallbackId())))
                .collect(Collectors.toSet());
        expected.add(vanilla.getOrThrow(BiomeKeys.STONY_PEAKS));
        for(BiomeIntent intent:BiomeIntent.values())for(String id:BiomeTargetResolver.variants(intent))
            vanilla.getOptional(key(id)).ifPresent(expected::add);
        require(source.getBiomes().equals(expected), "Vanilla palette must cover every fallback");
        require(source.withSeed(1L) == source, "Same-seed source must be reused");
        require(source.withSeed(2L) != source, "A changed seed must get a fresh plan");

        // Simulate a partially available optional palette. Only gravel beach
        // is provided, using a distinguishable registry entry for this check.
        RegistryEntryLookup<Biome> partial = new RegistryEntryLookup<>() {
            @Override
            public Optional<RegistryEntry.Reference<Biome>> getOptional(RegistryKey<Biome> biome) {
                return biome.equals(key("terralith:gravel_beach"))
                        ? Optional.of(vanilla.getOrThrow(BiomeKeys.DESERT))
                        : vanilla.getOptional(biome);
            }

            @Override
            public Optional<RegistryEntryList.Named<Biome>> getOptional(TagKey<Biome> tag) {
                return vanilla.getOptional(tag);
            }
        };
        var preferred = new FiniteBiomeSource(partial, 1L);
        require(preferred.getBiomes().contains(vanilla.getOrThrow(BiomeKeys.DESERT)),
                "Available optional biome must be selected");
        require(preferred.getBiomes().contains(vanilla.getOrThrow(BiomeKeys.STONY_SHORE)),
                "Other missing optional biomes must still fall back");

        var ops = RegistryOps.of(JsonOps.INSTANCE, registries);
        var renderedRegistry = mutableBiomes(vanilla);
        for (String variant : new String[]{"montane_forest", "temperate_forest",
                "temperate_rainforest", "temperate_steppe", "temperate_river"}) {
            try (var stream = FiniteBiomeSourceChecks.class.getResourceAsStream(
                    "/data/finite-worlds/worldgen/biome/" + variant + ".json")) {
                require(stream != null, "Missing temperate biome resource: " + variant);
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                var biome = Biome.CODEC.parse(ops, json).getOrThrow();
                require(biome.getPrecipitation(new BlockPos(0, 700, 0), 64) == Biome.Precipitation.RAIN,
                        "Lower mountain variant must resist premature altitude snow: " + variant);
                renderedRegistry.add(key("finite-worlds:" + variant), biome, RegistryEntryInfo.DEFAULT);
            }
        }
        var renderedOps = mutableOps(renderedRegistry);
        renderedRegistry.freeze();
        var encoded = BiomeSource.CODEC.encodeStart(ops, source).getOrThrow();
        require(encoded.getAsJsonObject().get("type").getAsString().equals("finite-worlds:finite"),
                "Source type must survive encoding");
        var decoded = BiomeSource.CODEC.parse(ops, encoded).getOrThrow();
        require(decoded instanceof FiniteBiomeSource, "Source codec round trip failed");
        require(decoded.getBiomes().equals(expected), "Palette changed after reload");
        require(BiomeSource.CODEC.encodeStart(ops, decoded).getOrThrow().getAsJsonObject()
                .get("seed").getAsLong() == 1L, "Seed changed after reload");
        require(BiomeSource.CODEC.encodeStart(ops, source.withSeed(2L)).getOrThrow().getAsJsonObject()
                .get("seed").getAsLong() == 2L, "Generator seed override was not serialized");
        JsonObject defaults = new JsonObject();
        defaults.addProperty("type", "finite-worlds:finite");
        require(BiomeSource.CODEC.parse(ops, defaults).getOrThrow() instanceof FiniteBiomeSource,
                "Preset without explicit source seed must decode");

        // World creation decodes with forward-reference lookups, unlike the
        // frozen built-in lookup above. Optional probes must not add entries.
        checkOriginalCodecFailure(vanilla);
        checkMutableRegistryCodec(vanilla, false);
        checkMutableRegistryCodec(vanilla, true);

        JsonObject generatorJson = new JsonObject();
        generatorJson.addProperty("type", "finite-worlds:finite");
        generatorJson.addProperty("seed", 12345L);
        JsonObject mismatchedSource = defaults.deepCopy();
        mismatchedSource.addProperty("seed", 99L);
        generatorJson.add("biome_source", mismatchedSource);
        var generator = (FiniteChunkGenerator) ChunkGenerator.CODEC.parse(renderedOps, generatorJson).getOrThrow();
        var placed = (FiniteBiomeSource) generator.getBiomeSource();
        var placedJson = BiomeSource.CODEC.encodeStart(renderedOps, placed).getOrThrow().getAsJsonObject();
        require((placedJson.has("seed") ? placedJson.get("seed").getAsLong() : 12345L) == 12345L,
                "Terrain and biome seeds must agree, including the omitted default seed");
        var blueprint = placed.blueprint();
        require(placed.blueprint() == blueprint, "Queries must reuse the planned world");
        var plan = BiomeIntentPlanner.plan(blueprint);
        var checked = java.util.EnumSet.noneOf(BiomeIntent.class);
        for (int z = 0; z < plan.resolution(); z++) {
            for (int x = 0; x < plan.resolution(); x++) {
                BiomeIntent intent = plan.intent(x, z);
                if (!checked.add(intent)) {
                    continue;
                }
                int quartX = (int) Math.round((-32768.0 + x * 65536.0 / (plan.resolution() - 1)) / 4.0);
                int quartZ = (int) Math.round((-32768.0 + z * 65536.0 / (plan.resolution() - 1)) / 4.0);
                var resolvedBiome = placed.getBiome(quartX, 0, quartZ, null);
                require(placed.getBiomes().contains(resolvedBiome),
                        "Rendered biome must belong to the advertised palette for " + intent);
                require(placed.getBiome(quartX, 300, quartZ, null).equals(resolvedBiome),
                        "Surface biome column changed with height for " + intent);
            }
        }
        require(checked.size() > 10, "Production integration did not exercise a varied palette");
        require(placed.getBiome(8193, 0, 0, null).matchesKey(BiomeKeys.OCEAN),
                "Outside the finite world must remain ocean");
        checkRenderedTerrain(generator, placed, renderedRegistry);
        System.out.println("Production preset seed-12345 placement matched " + checked.size() + " biome intents.");
        System.out.println("Biome registry fallback, optional palette, and codec checks passed.");
    }

    private static void checkRenderedTerrain(FiniteChunkGenerator generator, FiniteBiomeSource source,
                                              Registry<Biome> biomes) {
        var world = source.blueprint();
        var terrain = source.terrainSampler();
        int peakX = 0;
        int peakZ = 0;
        double peak = Double.NEGATIVE_INFINITY;
        for (int z = 0; z < world.resolution(); z++) {
            for (int x = 0; x < world.resolution(); x++) {
                if (world.elevation(x, z) > peak) {
                    peak = world.elevation(x, z);
                    peakX = x;
                    peakZ = z;
                }
            }
        }
        int centerX = (int) Math.round(-32768.0 + peakX * 65536.0 / (world.resolution() - 1));
        int centerZ = (int) Math.round(-32768.0 + peakZ * 65536.0 / (world.resolution() - 1));
        double surfacePeak = Double.NEGATIVE_INFINITY;
        int summitX = centerX;
        int summitZ = centerZ;
        for (int dz = -128; dz <= 128; dz += 8) {
            for (int dx = -128; dx <= 128; dx += 8) {
                double elevation = terrain.surfaceElevationAt(centerX + dx, centerZ + dz);
                if (elevation > surfacePeak) {
                    surfacePeak = elevation;
                    summitX = centerX + dx;
                    summitZ = centerZ + dz;
                }
            }
        }
        require(surfacePeak >= 1400.0 && surfacePeak <= 1435.0,
                "Landmark summit must approach the dimension ceiling, got " + surfacePeak);

        int riverX = 0;
        int riverZ = 0;
        int checkedRivers = 0;
        int wetRivers = 0;
        for (var section : world.hydrology().riverSegmentCrossSections()) {
            for (int i = 0; i < section.points().size(); i += 20) {
                var point = section.points().get(i);
                int x = (int) Math.round(point.blockX());
                int z = (int) Math.round(point.blockZ());
                var column = terrain.sampleColumn(x, z);
                checkedRivers++;
                if (column.river() && column.hasWater()) {
                    wetRivers++;
                    if (column.waterSurfaceElevation() > 90.0) {
                        riverX = x;
                        riverZ = z;
                    }
                }
            }
        }
        require(checkedRivers > 100 && wetRivers > checkedRivers * 0.8,
                "Planned river centerlines must materialize as wet channels: " + wetRivers + "/" + checkedRivers);
        require(terrain.sampleColumn(riverX, riverZ).waterSurfaceElevation() > 90.0,
                "Test must include a river above ocean level");
        checkActualChunk(generator, source, biomes, riverX, riverZ, true);
        checkActualChunk(generator, source, biomes, summitX, summitZ, false);
        System.out.println("Rendered summit Y=" + surfacePeak + "; wet river samples="
                + wetRivers + "/" + checkedRivers);
    }

    private static void checkActualChunk(FiniteChunkGenerator generator, FiniteBiomeSource source,
                                          Registry<Biome> biomes, int x, int z, boolean wet) {
        var limits = HeightLimitView.create(FiniteChunkGenerator.MIN_Y, FiniteChunkGenerator.WORLD_HEIGHT);
        var chunk = new ProtoChunk(new ChunkPos(x >> 4, z >> 4), UpgradeData.NO_UPGRADE_DATA,
                limits, biomes, null);
        generator.populateNoise(Blender.getNoBlending(), null, null, chunk).join();
        chunk.populateBiomes(source, null);
        var sampled = generator.getColumnSample(x, z, limits, null);
        int solidTop = generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, limits, null) - 1;
        int visibleTop = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, limits, null) - 1;
        for (int y = FiniteChunkGenerator.DEVELOPMENT_TERRAIN_MIN_Y; y <= visibleTop + 1; y++) {
            require(chunk.getBlockState(new BlockPos(x, y, z)).equals(sampled.getState(y)),
                    "Chunk and column sample differ at " + x + "," + y + "," + z);
        }
        if (wet) {
            require(visibleTop > solidTop && sampled.getState(visibleTop).isOf(Blocks.WATER),
                    "Generated river must contain water above its bed");
        } else {
            require(visibleTop == solidTop && (sampled.getState(visibleTop).isOf(Blocks.SNOW_BLOCK) || sampled.getState(visibleTop).isOf(Blocks.STONE)),
                    "Dry summit must remain solid and within the height limit");
        }
        require(sampled.getState(visibleTop + 1).isAir(), "Column must end above its visible surface");
    }

    private static void checkMutableRegistryCodec(RegistryWrapper.Impl<Biome> vanilla,
                                                 boolean optionalBiomeAvailable) {
        var registry = mutableBiomes(vanilla);
        RegistryKey<Biome> optionalKey = key("terralith:gravel_beach");
        if (optionalBiomeAvailable) {
            Biome original = vanilla.getOrThrow(BiomeKeys.STONY_SHORE).value();
            Biome optionalBiome = new Biome.Builder()
                    .precipitation(original.hasPrecipitation())
                    .temperature(original.getTemperature())
                    .downfall(0.3F)
                    .effects(original.getEffects())
                    .generationSettings(original.getGenerationSettings())
                    .spawnSettings(original.getSpawnSettings())
                    .build();
            registry.add(optionalKey, optionalBiome, RegistryEntryInfo.DEFAULT);
        }
        int initialSize = registry.size();
        var ops = mutableOps(registry);
        JsonObject sourceJson = new JsonObject();
        sourceJson.addProperty("type", "finite-worlds:finite");
        sourceJson.addProperty("seed", 99L);
        JsonObject generatorJson = new JsonObject();
        generatorJson.addProperty("type", "finite-worlds:finite");
        generatorJson.addProperty("seed", 1L);
        generatorJson.add("biome_source", sourceJson);

        var generator = ChunkGenerator.CODEC.parse(ops, generatorJson).getOrThrow();
        var source = (FiniteBiomeSource) generator.getBiomeSource();
        // Freeze is where the reported create-world crash occurred. It throws
        // for any missing Terralith entry accidentally created by the codec.
        registry.freeze();
        require(registry.size() == initialSize, "Decoding must not register optional biome IDs");
        var expected = java.util.Arrays.stream(BiomeIntent.values())
                .map(intent -> {
                    var target = BiomeTargetResolver.target(intent);
                    return registry.getOptional(key(target.preferredId()))
                            .orElseGet(() -> registry.getOrThrow(key(target.vanillaFallbackId())));
                })
                .collect(Collectors.toSet());
        expected.add(registry.getOrThrow(BiomeKeys.STONY_PEAKS));
        require(source.getBiomes().containsAll(expected),
                "Mutable registry palette must select present optional biomes and vanilla fallbacks");
        require(source.getBiomes().stream().anyMatch(entry -> entry.matchesKey(optionalKey))
                        == optionalBiomeAvailable,
                "Real optional registry entry must be selected only when present");
        for (BiomeIntent intent : BiomeIntent.values()) {
            RegistryKey<Biome> preferred = key(BiomeTargetResolver.target(intent).preferredId());
            if (preferred.getValue().getNamespace().equals("terralith")
                    && !(optionalBiomeAvailable && preferred.equals(optionalKey))) {
                require(registry.getOptional(preferred).isEmpty(),
                        "Missing optional probe created a registry entry: " + preferred);
            }
        }
        require(BiomeSource.CODEC.encodeStart(ops, source).getOrThrow().getAsJsonObject()
                .get("seed").getAsLong() == 1L,
                "Mutable registry decoding must retain the generator's authoritative seed");
    }

    private static void checkOriginalCodecFailure(RegistryWrapper.Impl<Biome> vanilla) {
        var registry = mutableBiomes(vanilla);
        // Keep the previous codec as a control: frozen-registry tests could not
        // expose its mutating lookup, so verify this fixture reproduces the crash.
        MapCodec<FiniteBiomeSource> originalCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                RegistryOps.<Biome, FiniteBiomeSource>getEntryLookupCodec(RegistryKeys.BIOME),
                Codec.LONG.optionalFieldOf("seed", 12345L).forGetter(source -> 12345L)
        ).apply(instance, FiniteBiomeSource::new));
        originalCodec.codec().parse(mutableOps(registry), new JsonObject()).getOrThrow();
        IllegalStateException error = assertThrows(IllegalStateException.class, registry::freeze);
        require(error.getMessage().contains("Unbound values"),
                "Original codec control must reproduce the reported registry freeze failure");
        for (BiomeIntent intent : BiomeIntent.values()) {
            String preferred = BiomeTargetResolver.target(intent).preferredId();
            if (preferred.startsWith("terralith:")) {
                require(error.getMessage().contains(preferred),
                        "Original codec must reproduce the missing Terralith reference: " + preferred);
            }
        }
    }

    private static SimpleRegistry<Biome> mutableBiomes(RegistryWrapper.Impl<Biome> vanilla) {
        var registry = new SimpleRegistry<Biome>(RegistryKeys.BIOME, Lifecycle.stable());
        vanilla.streamEntries().forEach(entry -> registry.add(
                entry.registryKey(), entry.value(), RegistryEntryInfo.DEFAULT));
        return registry;
    }

    private static RegistryOps<JsonElement> mutableOps(SimpleRegistry<Biome> registry) {
        var mutableLookup = registry.createMutableRegistryLookup();
        var infoGetter = new RegistryOps.RegistryInfoGetter() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<RegistryOps.RegistryInfo<T>> getRegistryInfo(
                    RegistryKey<? extends Registry<? extends T>> registryKey) {
                if (!registryKey.equals(RegistryKeys.BIOME)) {
                    return Optional.empty();
                }
                // This is the exact owner/lookup pairing used by RegistryLoader:
                // the owner only reads entries; the lookup creates forward refs.
                RegistryOps.RegistryInfo<Biome> info = new RegistryOps.RegistryInfo<>(
                        registry, mutableLookup, registry.getLifecycle());
                return Optional.of((RegistryOps.RegistryInfo<T>) (Object) info);
            }
        };
        return RegistryOps.of(JsonOps.INSTANCE, infoGetter);
    }

    private static RegistryKey<Biome> key(String id) {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of(id));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
