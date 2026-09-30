package dev.lemma.finiteworlds.worldgen;

import dev.lemma.finiteworlds.core.biome.BiomeIntent;
import dev.lemma.finiteworlds.core.biome.BiomeTargetResolver;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import java.util.HashSet;
import java.util.Set;

/** Registered warm companions retain optional-mod vegetation, spawns and visual effects. */
public final class TemperateBiomeVariants {
    private TemperateBiomeVariants() {}
    public static String warmId(String source) {
        return "finite-worlds:temperate/"+source.replace(':','/');
    }
    /** The companions also inherit structure, vegetation and creature biome tags. */
    @SuppressWarnings("unchecked")
    public static void inheritTags(MutableRegistry<?> loaded) {
        if(!loaded.getKey().equals(RegistryKeys.BIOME))return;
        MutableRegistry<Biome> biomes=(MutableRegistry<Biome>)loaded;
        var rawTags=((dev.lemma.finiteworlds.mixin.SimpleRegistryTagAccessor)biomes).finiteWorldsInitialTags();
        for(var rawTag:java.util.List.copyOf(rawTags.values())) {
            var tag=(net.minecraft.registry.entry.RegistryEntryList.Named<Biome>)(Object)rawTag;
            if(!tag.isBound())continue;
            var entries=new java.util.ArrayList<RegistryEntry<Biome>>(tag.stream().toList());
            for(var original:tag.stream().toList()) {
                String id=original.getKey().orElseThrow().getValue().toString();
                if(!id.startsWith("terralith:"))continue;
                biomes.getOptional(RegistryKey.of(RegistryKeys.BIOME,Identifier.of(warmId(id))))
                    .filter(entry -> !entries.contains(entry)).ifPresent(entries::add);
            }
            if(entries.size()!=tag.size())biomes.setEntries(tag.getTag(),entries);
        }
    }
    public static void register() {
        Set<String> candidates=new HashSet<>();
        for(BiomeIntent intent:BiomeIntent.values()) {
            candidates.add(BiomeTargetResolver.target(intent).preferredId());
            candidates.addAll(BiomeTargetResolver.variants(intent));
        }
        DynamicRegistrySetupCallback.EVENT.register(view -> view.getOptional(RegistryKeys.BIOME).ifPresent(registry ->
            RegistryEntryAddedCallback.event(registry).register((rawId,id,biome) -> {
                if(!id.getNamespace().equals("terralith")||!candidates.contains(id.toString()))return;
                Identifier warm=Identifier.of(warmId(id.toString()));
                if(registry.containsId(warm))return;
                Biome companion=new Biome.Builder().precipitation(biome.hasPrecipitation())
                    .temperature(1.0f).downfall(.8f).effects(biome.getEffects())
                    .spawnSettings(biome.getSpawnSettings()).generationSettings(biome.getGenerationSettings()).build();
                Registry.register(registry,warm,companion);
            })));
    }
}
