package dev.lemma.finiteworlds.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.Heightmap;
import net.minecraft.world.HeightLimitView;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Registry and codec checks that do not require launching a client. */
public final class FiniteBiomeSourceChecks {
    @Test
    public void registryAndCodecChecks() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        new FiniteWorlds().onInitialize();
        var registries = BuiltinRegistries.createWrapperLookup();
        var vanilla = registries.getOrThrow(RegistryKeys.BIOME);
        var source = new FiniteBiomeSource(vanilla, 1L);
        var expected = java.util.Arrays.stream(BiomeIntent.values())
                .map(intent -> vanilla.getOrThrow(key(BiomeTargetResolver.target(intent).vanillaFallbackId())))
                .collect(Collectors.toSet());
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
        generatorJson.addProperty("seed", 1L);
        JsonObject mismatchedSource = defaults.deepCopy();
        mismatchedSource.addProperty("seed", 99L);
        generatorJson.add("biome_source", mismatchedSource);
        var generator = ChunkGenerator.CODEC.parse(ops, generatorJson).getOrThrow();
        var placed = (FiniteBiomeSource) generator.getBiomeSource();
        require(BiomeSource.CODEC.encodeStart(ops, placed).getOrThrow().getAsJsonObject()
                .get("seed").getAsLong() == 1L, "Terrain and biome seeds must agree");
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
                var expectedBiome = vanilla.getOrThrow(key(BiomeTargetResolver.target(intent).vanillaFallbackId()));
                require(placed.getBiome(quartX, 0, quartZ, null).equals(expectedBiome),
                        "Quart-coordinate placement differs from the blueprint for " + intent);
                require(placed.getBiome(quartX, 300, quartZ, null).equals(expectedBiome),
                        "Surface biome column changed with height for " + intent);
            }
        }
        require(checked.size() > 10, "Production integration did not exercise a varied palette");
        require(placed.getBiome(8193, 0, 0, null).equals(vanilla.getOrThrow(BiomeKeys.OCEAN)),
                "Outside the finite world must remain ocean");
        CoastalSurfaceChecks.run((FiniteChunkGenerator) generator, placed, vanilla);
        checkWorldSeedBinding(registries, ops, (FiniteChunkGenerator) generator);
        System.out.println("Production seed-1 placement matched " + checked.size() + " biome intents.");
        System.out.println("Biome registry fallback, optional palette, and codec checks passed.");
    }

    private static void checkWorldSeedBinding(RegistryWrapper.WrapperLookup registries,
                                              RegistryOps<JsonElement> ops,
                                              FiniteChunkGenerator legacy) {
        JsonObject seedless = new JsonObject();
        seedless.addProperty("type", "finite-worlds:finite");
        JsonObject source = new JsonObject();
        source.addProperty("type", "finite-worlds:finite");
        seedless.add("biome_source", source);
        // BuiltinRegistries' bootstrap-only structure biome tags are unbound.
        // Seed initialization uses the real calculator with an empty structure registry.
        var structures = new SimpleRegistry<net.minecraft.structure.StructureSet>(
                RegistryKeys.STRUCTURE_SET, Lifecycle.stable());
        structures.freeze();
        var noise = NoiseConfig.create(ChunkGeneratorSettings.createMissingSettings(),
                registries.getOrThrow(RegistryKeys.NOISE_PARAMETERS), 0L);
        var fresh = (FiniteChunkGenerator) ChunkGenerator.CODEC.parse(ops, seedless).getOrThrow();
        require(!ChunkGenerator.CODEC.encodeStart(ops, fresh).getOrThrow().getAsJsonObject().has("seed"),
                "A preset must not acquire a hidden fixed seed while loading");
        assertThrows(IllegalStateException.class,
                () -> fresh.getHeight(0, 0, Heightmap.Type.WORLD_SURFACE_WG, null, noise));
        assertThrows(IllegalStateException.class,
                () -> fresh.getBiomeSource().getBiome(0, 0, 0, null));

        long selected = GeneratorOptions.parseSeed("2").orElseThrow();
        fresh.createStructurePlacementCalculator(structures, noise, selected);
        var freshSource = (FiniteBiomeSource) fresh.getBiomeSource();
        require(BiomeSource.CODEC.encodeStart(ops, freshSource).getOrThrow().getAsJsonObject()
                        .get("seed").getAsLong() == selected,
                "The Minecraft startup hook must bind the creation-screen seed to biomes");
        var saved = ChunkGenerator.CODEC.encodeStart(ops, fresh).getOrThrow();
        require(!saved.getAsJsonObject().has("seed"),
                "New worlds must remain editable through Re-create World's seed field");
        var reload = (FiniteChunkGenerator) ChunkGenerator.CODEC.parse(ops, saved).getOrThrow();
        reload.createStructurePlacementCalculator(structures, noise, selected);
        var limits = HeightLimitView.create(FiniteChunkGenerator.MIN_Y, FiniteChunkGenerator.WORLD_HEIGHT);
        boolean differentTerrain = false;
        for (int x : new int[]{-20000, -10000, 0, 10000, 20000}) {
            for (int z : new int[]{-12000, 0, 12000}) {
                int height = fresh.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, limits, noise);
                require(height == reload.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, limits, noise),
                        "A saved world must reproduce its terrain after reload");
                differentTerrain |= height != legacy.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, limits, noise);
            }
        }
        require(differentTerrain, "Different selected seeds must change actual terrain columns");

        // Re-create World retains dimension settings but must honor a new UI seed.
        var recreated = (FiniteChunkGenerator) ChunkGenerator.CODEC.parse(ops, saved).getOrThrow();
        recreated.createStructurePlacementCalculator(structures, noise, 0L);
        require(BiomeSource.CODEC.encodeStart(ops, recreated.getBiomeSource()).getOrThrow()
                        .getAsJsonObject().get("seed").getAsLong() == 0L,
                "Zero is a valid chosen seed, including when recreating a prior world");

        require(GeneratorOptions.parseSeed("").isEmpty(), "Blank UI seed must request vanilla randomness");
        long randomA = new GeneratorOptions(0L, true, false)
                .withSeed(GeneratorOptions.parseSeed("")).getSeed();
        long randomB = new GeneratorOptions(0L, true, false)
                .withSeed(GeneratorOptions.parseSeed("")).getSeed();
        require(randomA != randomB, "Blank UI seed requests must receive distinct random seeds");
        for (long seed : new long[]{randomA, randomB, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var randomWorld = (FiniteChunkGenerator) ChunkGenerator.CODEC.parse(ops, seedless).getOrThrow();
            randomWorld.createStructurePlacementCalculator(structures, noise, seed);
            require(BiomeSource.CODEC.encodeStart(ops, randomWorld.getBiomeSource()).getOrThrow()
                            .getAsJsonObject().get("seed").getAsLong() == seed,
                    "All saved world seeds must reach the finite source without replacement");
        }

        legacy.createStructurePlacementCalculator(structures, noise, -77L);
        require(ChunkGenerator.CODEC.encodeStart(ops, legacy).getOrThrow()
                        .getAsJsonObject().get("seed").getAsLong() == 1L,
                "Existing serialized seeds must remain stable to protect old chunks");
        require(BiomeSource.CODEC.encodeStart(ops, legacy.getBiomeSource()).getOrThrow()
                        .getAsJsonObject().get("seed").getAsLong() == 1L,
                "Existing world biome and terrain seeds must remain aligned");
        System.out.println("World seed binding, random requests, zero/negative seeds, reload, and recreation passed.");
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
        require(source.getBiomes().equals(expected),
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
