package dev.lemma.finiteworlds.worldgen;

import net.minecraft.resource.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.world.dimension.DimensionOptions;
import java.nio.file.Path;
import java.util.*;

final class TerralithCompatibilityChecks {
    @SuppressWarnings("unchecked")
    private static <T> void loadStaticTags(ResourceManager manager, Registry<T> registry) {
        net.minecraft.registry.tag.TagGroupLoader.loadInitial(manager,(MutableRegistry<T>)registry);
        registry.freeze();
    }
    static void run(RegistryWrapper.WrapperLookup vanilla)throws Exception{
        String jar=System.getProperty("finiteworlds.terralithJar");
        if(jar==null)return;
        ResourcePackInfo info=new ResourcePackInfo("terralith-test",Text.literal("Terralith compatibility"),
            ResourcePackSource.NONE,Optional.empty());
        Path resources=Path.of(System.getProperty("finiteworlds.projectDir"),"src/main/resources");
        try(var manager=new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA,List.of(
                VanillaDataPackProvider.createDefaultPack(),new ZipResourcePack.ZipBackedFactory(Path.of(jar)).open(info),
                new DirectoryResourcePack(new ResourcePackInfo("finite-worlds-test",Text.literal("Finite Worlds"),
                    ResourcePackSource.NONE,Optional.empty()),resources)))){
            List<RegistryWrapper.Impl<?>> staticRegistries=new ArrayList<>();
            Registries.REGISTRIES.stream().forEach(registry -> {
                loadStaticTags(manager,registry);
                staticRegistries.add(registry);
            });
            var dynamic=RegistryLoader.loadFromResource(manager,staticRegistries,RegistryLoader.DYNAMIC_REGISTRIES);
            var all=new ArrayList<RegistryWrapper.Impl<?>>(staticRegistries);
            dynamic.stream().forEach(all::add);
            var dimensions=RegistryLoader.loadFromResource(manager,all,RegistryLoader.DIMENSION_REGISTRIES);
            var presets=dynamic.getOrThrow(RegistryKeys.WORLD_PRESET);
            var selected=presets.getValueOrThrow(RegistryKey.of(RegistryKeys.WORLD_PRESET,
                net.minecraft.util.Identifier.of("finite-worlds:cascadia"))).createDimensionsRegistryHolder();
            var loaded=dimensions.getOrThrow(RegistryKeys.DIMENSION);
            if(loaded.getValueOrThrow(DimensionOptions.OVERWORLD).chunkGenerator() instanceof FiniteChunkGenerator)
                throw new AssertionError("Test must exercise Terralith's overworld dimension override");
            var configured=selected.toConfig(loaded).dimensions();
            var overworld=configured.getValueOrThrow(DimensionOptions.OVERWORLD);
            if(!(overworld.chunkGenerator() instanceof FiniteChunkGenerator generator))
                throw new AssertionError("Terralith replaced the explicitly selected Finite generator");
            var source=(FiniteBiomeSource)generator.getBiomeSource();
            BiomeReviewChecks.write(source);
            long terralithBiomes=source.getBiomes().stream().filter(b -> b.getKey().orElseThrow().getValue()
                .getNamespace().equals("terralith")).count();
            if(terralithBiomes<10)throw new AssertionError("Real Terralith palette was not loaded: "+terralithBiomes);
            var biomeRegistry=dynamic.getOrThrow(RegistryKeys.BIOME);
            var original=biomeRegistry.getValueOrThrow(RegistryKey.of(RegistryKeys.BIOME,
                net.minecraft.util.Identifier.of("terralith:cloud_forest")));
            var companion=biomeRegistry.getValueOrThrow(RegistryKey.of(RegistryKeys.BIOME,
                net.minecraft.util.Identifier.of(TemperateBiomeVariants.warmId("terralith:cloud_forest"))));
            if(companion.getPrecipitation(new net.minecraft.util.math.BlockPos(0,700,0),64)!=net.minecraft.world.biome.Biome.Precipitation.RAIN)
                throw new AssertionError("Terralith forest companion snows below the authored belt");
            if(companion.getGenerationSettings()!=original.getGenerationSettings() || companion.getSpawnSettings()!=original.getSpawnSettings())
                throw new AssertionError("Terralith vegetation/spawns were lost in the temperate companion");
            var originalEntry=biomeRegistry.getEntry(original);
            var companionEntry=biomeRegistry.getEntry(companion);
            for(var tag:biomeRegistry.streamTags().toList())if(tag.contains(originalEntry)&&!tag.contains(companionEntry))
                throw new AssertionError("Temperate companion lost biome tag "+tag.getTag());
            // Minecraft's feature-order graph can expose incompatibilities absent from codec-only checks.
            generator.initializeIndexedFeaturesList();
            var standard=presets.getValueOrThrow(net.minecraft.world.gen.WorldPresets.DEFAULT).createDimensionsRegistryHolder();
            if(standard.toConfig(loaded).dimensions().getValueOrThrow(DimensionOptions.OVERWORLD)
                    .chunkGenerator() instanceof FiniteChunkGenerator)
                throw new AssertionError("Compatibility hook changed an unrelated vanilla preset");
            System.out.println("Real Terralith registries loaded; Finite overworld retained; "+terralithBiomes+" Terralith palette entries.");
        }
    }
}
