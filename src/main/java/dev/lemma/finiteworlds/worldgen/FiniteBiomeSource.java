package dev.lemma.finiteworlds.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.WorldConfig;
import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import dev.lemma.finiteworlds.core.biome.BiomeIntentPlanner;
import dev.lemma.finiteworlds.core.biome.BiomeIntentSampler;
import dev.lemma.finiteworlds.core.biome.BiomeTarget;
import dev.lemma.finiteworlds.core.biome.BiomeTargetResolver;
import dev.lemma.finiteworlds.core.generator.CascadiaGenerator;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Pass 4A: places the locked 3G intent palette into Minecraft's biome grid. */
public final class FiniteBiomeSource extends BiomeSource {
    private static final MapCodec<RegistryEntryLookup<Biome>> BIOME_LOOKUP_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<RegistryEntryLookup<Biome>> decode(DynamicOps<T> ops, MapLike<T> input) {
            if (ops instanceof RegistryOps<T> registryOps) {
                var owner = registryOps.getOwner(RegistryKeys.BIOME).orElse(null);
                if (owner instanceof RegistryWrapper.Impl<?> wrapper) {
                    // RegistryLoader's entryLookup creates forward references even
                    // for getOptional. Its owner is the registry itself, whose
                    // wrapper lookup can check absent optional IDs without mutation.
                    @SuppressWarnings("unchecked")
                    RegistryEntryLookup<Biome> lookup = (RegistryWrapper<Biome>) wrapper;
                    return DataResult.success(lookup, wrapper.getLifecycle());
                }
            }
            return DataResult.error(() -> "Finite biome source requires a readable biome registry");
        }

        @Override
        public <T> RecordBuilder<T> encode(RegistryEntryLookup<Biome> input, DynamicOps<T> ops,
                                            RecordBuilder<T> prefix) {
            return prefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }
    };

    public static final MapCodec<FiniteBiomeSource> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BIOME_LOOKUP_CODEC.forGetter(source -> source.registry),
                    Codec.LONG.optionalFieldOf("seed")
                            .forGetter(source -> source.seedBound ? Optional.of(source.seed) : Optional.empty())
            ).apply(instance, FiniteBiomeSource::new));

    private final RegistryEntryLookup<Biome> registry;
    private volatile long seed;
    private volatile boolean seedBound;
    private final Map<BiomeIntent, RegistryEntry<Biome>> palette;
    private volatile PlannedWorld plannedWorld;

    public FiniteBiomeSource(RegistryEntryLookup<Biome> registry, long seed) {
        this(registry, Optional.of(seed));
    }

    private FiniteBiomeSource(RegistryEntryLookup<Biome> registry, Optional<Long> configuredSeed) {
        this.registry = registry;
        this.seed = configuredSeed.orElse(0L);
        this.seedBound = configuredSeed.isPresent();
        Map<BiomeIntent, RegistryEntry<Biome>> resolved = new EnumMap<>(BiomeIntent.class);
        for (BiomeIntent intent : BiomeIntent.values()) {
            BiomeTarget target = BiomeTargetResolver.target(intent);
            RegistryEntry<Biome> biome = registry.getOptional(key(target.preferredId()))
                    .orElseGet(() -> registry.getOrThrow(key(target.vanillaFallbackId())));
            resolved.put(intent, biome);
        }
        this.palette = Map.copyOf(resolved);
    }

    private static RegistryKey<Biome> key(String id) {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of(id));
    }

    /** The chunk generator's configured seed is authoritative for both layers. */
    public FiniteBiomeSource withSeed(long seed) {
        return seedBound && this.seed == seed ? this : new FiniteBiomeSource(registry, seed);
    }

    /** Bind the shared source before Minecraft starts sampling biome coordinates. */
    synchronized void bindSeed(long seed) {
        if (seedBound && this.seed == seed) {
            return;
        }
        if (plannedWorld != null) {
            throw new IllegalStateException("Cannot change the seed after planning Finite Worlds terrain");
        }
        this.seed = seed;
        this.seedBound = true;
    }

    public WorldBlueprint blueprint() {
        return plannedWorld().blueprint();
    }

    private PlannedWorld plannedWorld() {
        if (!seedBound) {
            throw new IllegalStateException("Finite Worlds biomes have not received the saved world seed yet");
        }
        PlannedWorld result = plannedWorld;
        if (result == null) {
            synchronized (this) {
                result = plannedWorld;
                if (result == null) {
                    WorldBlueprint blueprint = new CascadiaGenerator().generate(
                            seed, WorldConfig.production());
                    result = new PlannedWorld(blueprint, new BiomeIntentSampler(
                            BiomeIntentPlanner.plan(blueprint), blueprint.config()));
                    plannedWorld = result;
                }
            }
        }
        return result;
    }

    @Override
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return palette.values().stream().distinct();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z,
                                        MultiNoiseUtil.MultiNoiseSampler noise) {
        // Minecraft supplies quart coordinates; the engine uses block coordinates.
        BiomeIntent intent = intentAtBlock(
                BiomeCoords.toBlock(x), BiomeCoords.toBlock(z));
        return palette.get(intent);
    }

    /** Surface materials retain distinct sandy, gravel, and rocky coast intents. */
    public BiomeIntent intentAtBlock(int blockX, int blockZ) {
        return plannedWorld().sampler().intentAtBlock(blockX, blockZ);
    }

    private record PlannedWorld(WorldBlueprint blueprint, BiomeIntentSampler sampler) {
    }
}
