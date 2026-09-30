package dev.lemma.finiteworlds.worldgen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.world.dimension.DimensionOptions;
import java.util.Map;

/** Preserve an explicitly selected/saved Finite overworld when a datapack also defines the overworld. */
public final class FiniteDimensionCompatibility {
    private FiniteDimensionCompatibility() {}
    public static Registry<DimensionOptions> preserveSelectedOverworld(
            Map<RegistryKey<DimensionOptions>, DimensionOptions> selected, Registry<DimensionOptions> loaded) {
        DimensionOptions overworld=selected.get(DimensionOptions.OVERWORLD);
        if(overworld==null || !(overworld.chunkGenerator() instanceof FiniteChunkGenerator))return loaded;
        if(loaded.getOptionalValue(DimensionOptions.OVERWORLD).orElse(null)==overworld)return loaded;
        SimpleRegistry<DimensionOptions> compatible=new SimpleRegistry<>(RegistryKeys.DIMENSION,Lifecycle.experimental());
        for(var entry:loaded.getEntrySet()){
            if(!entry.getKey().equals(DimensionOptions.OVERWORLD))compatible.add(entry.getKey(),entry.getValue(),
                loaded.getEntryInfo(entry.getKey()).orElse(RegistryEntryInfo.DEFAULT));
        }
        compatible.add(DimensionOptions.OVERWORLD,overworld,RegistryEntryInfo.DEFAULT);
        return compatible.freeze();
    }
}
