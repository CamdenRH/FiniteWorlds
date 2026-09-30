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
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.EnumMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import dev.lemma.finiteworlds.core.SeedUtil;
import dev.lemma.finiteworlds.core.noise.ValueNoise;
import java.util.stream.Stream;

/** Places the locked 3G intent palette with continuous boundaries and physical river footprints. */
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
    private final Map<BiomeIntent, List<RegistryEntry<Biome>>> regionalPalette;
    private final Map<String, RegistryEntry<Biome>> temperateCompanions;
    private final Map<BiomeIntent, RegistryEntry<Biome>> belowSnowlinePalette;
    private final ThreadLocal<HorizontalBiomeCache> horizontalCache =
            ThreadLocal.withInitial(HorizontalBiomeCache::new);
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
        Map<BiomeIntent,List<RegistryEntry<Biome>>> regional=new EnumMap<>(BiomeIntent.class);
        Map<String,RegistryEntry<Biome>> companions=new java.util.HashMap<>();
        for(BiomeIntent intent:BiomeIntent.values()) {
            var entries=BiomeTargetResolver.variants(intent).stream().map(id -> registry.getOptional(key(id)))
                .flatMap(Optional::stream).map(entry -> (RegistryEntry<Biome>)entry).toList();
            boolean optionalPresent=entries.stream().anyMatch(entry -> entry.getKey().orElseThrow()
                .getValue().getNamespace().equals("terralith"));
            if(optionalPresent)entries=entries.stream().filter(entry -> entry.getKey().orElseThrow()
                .getValue().getNamespace().equals("terralith")).toList();
            regional.put(intent,entries.isEmpty()?List.of(resolved.get(intent)):entries);
            for(var entry:regional.get(intent)) {
                String id=entry.getKey().orElseThrow().getValue().toString();
                registry.getOptional(key(TemperateBiomeVariants.warmId(id))).ifPresent(warm -> companions.put(id,warm));
            }
        }
        this.regionalPalette=Map.copyOf(regional);
        this.temperateCompanions=Map.copyOf(companions);
        // The vanilla taiga/alpine substitutes have cold base temperatures.
        // Minecraft cools biomes again with altitude, making those substitutes
        // snow below the authored belt. Warm native variants keep lower forest
        // and steppe terrain free of permanent snow while retaining their flora.
        RegistryEntry<Biome> lowerForest = registry.getOptional(key("finite-worlds:montane_forest"))
                .orElseGet(() -> registry.getOrThrow(key("minecraft:old_growth_pine_taiga")));
        RegistryEntry<Biome> lowerTemperate = registry.getOptional(key("finite-worlds:temperate_forest"))
                .orElseGet(() -> registry.getOrThrow(key("minecraft:forest")));
        RegistryEntry<Biome> lowerRainforest = registry.getOptional(key("finite-worlds:temperate_rainforest"))
                .orElseGet(() -> registry.getOrThrow(key("minecraft:old_growth_spruce_taiga")));
        RegistryEntry<Biome> lowerSteppe = registry.getOptional(key("finite-worlds:temperate_steppe"))
                .orElseGet(() -> registry.getOrThrow(key("minecraft:plains")));
        RegistryEntry<Biome> lowerRiver = registry.getOptional(key("finite-worlds:temperate_river"))
                .orElseGet(() -> registry.getOrThrow(key("minecraft:river")));
        RegistryEntry<Biome> lowerRock = registry.getOrThrow(key("minecraft:stony_peaks"));
        Map<BiomeIntent, RegistryEntry<Biome>> lower = new EnumMap<>(BiomeIntent.class);
        for (BiomeIntent intent : new BiomeIntent[]{BiomeIntent.SUBALPINE_GROVE,
                BiomeIntent.MONTANE_FOREST, BiomeIntent.DRY_FOREST}) {
            lower.put(intent, lowerForest);
        }
        lower.put(BiomeIntent.WET_HIGHLAND_FOREST, lowerRainforest);
        lower.put(BiomeIntent.TEMPERATE_RAINFOREST, lowerRainforest);
        lower.put(BiomeIntent.TEMPERATE_FOREST, lowerTemperate);
        lower.put(BiomeIntent.ALPINE_HIGHLANDS, lowerTemperate);
        lower.put(BiomeIntent.PERMANENT_SNOWFIELD, lowerTemperate);
        lower.put(BiomeIntent.SHRUB_STEPPE, lowerSteppe);
        lower.put(BiomeIntent.COLD_STEPPE, lowerSteppe);
        lower.put(BiomeIntent.ROCKY_ALPINE, lowerRock);
        lower.put(BiomeIntent.FROZEN_CLIFFS, lowerRock);
        lower.put(BiomeIntent.RIVER, lowerRiver);
        lower.put(BiomeIntent.ESTUARY, lowerRiver);
        this.belowSnowlinePalette = Map.copyOf(lower);
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

    public TerrainSampler terrainSampler() {
        return plannedWorld().terrain();
    }

    /** Surface materials retain distinct sandy, gravel, and rocky coast intents. */
    public BiomeIntent intentAtBlock(int blockX, int blockZ) {
        return plannedWorld().sampler().intentAtBlock(blockX, blockZ);
    }

    public double snowlineAtBlock(int x,int z) {
        return plannedWorld().sampler().snowlineAtBlock(x,z);
    }
    public double permanentSnowlineAtBlock(int x,int z) {
        return plannedWorld().sampler().permanentSnowlineAtBlock(x,z);
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
                            BiomeIntentPlanner.plan(blueprint), blueprint.config(), seed),
                            new TerrainSampler(blueprint, seed),
                            new ValueNoise(SeedUtil.derive(seed, "regional-biome-variants")));
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
        return Stream.of(palette.values().stream(),belowSnowlinePalette.values().stream(),
                regionalPalette.values().stream().flatMap(List::stream),temperateCompanions.values().stream())
                .flatMap(stream -> stream).distinct();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z,
                                        MultiNoiseUtil.MultiNoiseSampler noise) {
        // A 2048-high chunk repeats each of its 16 horizontal quart positions
        // 512 times. Biome columns are height-independent, so retain only the
        // current chunk's entries per worker to avoid repeated terrain queries.
        HorizontalBiomeCache cache = horizontalCache.get();
        int chunkX = x >> 2;
        int chunkZ = z >> 2;
        if (cache.chunkX != chunkX || cache.chunkZ != chunkZ) {
            cache.chunkX = chunkX;
            cache.chunkZ = chunkZ;
            Arrays.fill(cache.biomes, null);
        }
        int index = ((z & 3) << 2) | (x & 3);
        RegistryEntry<Biome> biome = cache.biomes[index];
        if (biome == null) {
            biome = resolvedBiomeAtBlock(BiomeCoords.toBlock(x), BiomeCoords.toBlock(z));
            cache.biomes[index] = biome;
        }
        return biome;
    }

    private RegistryEntry<Biome> resolvedBiomeAtBlock(int blockX, int blockZ) {
        // Minecraft supplies quart coordinates; the engine uses block coordinates.
        PlannedWorld world = plannedWorld();
        var column = world.terrain().sampleColumn(blockX, blockZ);
        BiomeIntent intent = column.river()
                ? (world.sampler().intentAtBlock(blockX, blockZ) == BiomeIntent.ESTUARY
                    ? BiomeIntent.ESTUARY : BiomeIntent.RIVER)
                : world.sampler().landIntentAtBlock(blockX, blockZ);
        double surface = column.terrainElevation();
        RegistryEntry<Biome> selected=regionalBiome(intent,world,blockX,blockZ);
        if (surface < world.sampler().snowlineAtBlock(blockX, blockZ)) {
            RegistryEntry<Biome> warmer = belowSnowlinePalette.get(intent);
            boolean snowBearingIntent = switch (intent) {
                case SUBALPINE_GROVE, ALPINE_HIGHLANDS, ROCKY_ALPINE,
                        PERMANENT_SNOWFIELD, FROZEN_CLIFFS, COLD_STEPPE -> true;
                default -> false;
            };
            if (warmer != null && (snowBearingIntent || selected.value().isCold(
                    new BlockPos(blockX, (int) Math.floor(surface + 1.0), blockZ),
                    world.blueprint().config().seaLevel()))) {
                var companion=temperateCompanions.get(selected.getKey().orElseThrow().getValue().toString());
                return companion!=null?companion:warmer;
            }
        }
        if (surface < world.sampler().permanentSnowlineAtBlock(blockX, blockZ)) {
            intent = switch (intent) {
                case PERMANENT_SNOWFIELD -> BiomeIntent.ALPINE_HIGHLANDS;
                case FROZEN_CLIFFS -> BiomeIntent.ROCKY_ALPINE;
                default -> intent;
            };
        }
        return regionalBiome(intent,world,blockX,blockZ);
    }

    private RegistryEntry<Biome> regionalBiome(BiomeIntent intent,PlannedWorld world,int x,int z) {
        var choices=regionalPalette.get(intent);
        double region=world.variants().fbm(x/1200.0,z/1200.0,3,2,.5)*1.6;
        int index=Math.max(0,Math.min(choices.size()-1,(int)((region+1)*.5*choices.size())));
        return choices.get(index);
    }

    private record PlannedWorld(WorldBlueprint blueprint, BiomeIntentSampler sampler, TerrainSampler terrain, ValueNoise variants) {
    }

    private static final class HorizontalBiomeCache {
        private int chunkX = Integer.MIN_VALUE;
        private int chunkZ = Integer.MIN_VALUE;
        private final RegistryEntry<Biome>[] biomes;

        @SuppressWarnings("unchecked")
        private HorizontalBiomeCache() {
            biomes = (RegistryEntry<Biome>[]) new RegistryEntry<?>[16];
        }
    }
}
