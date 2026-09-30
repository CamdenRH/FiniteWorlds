package dev.lemma.finiteworlds.mixin;

import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.entry.RegistryEntryList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

/** Initial tags are populated before the public frozen tag lookup becomes available. */
@Mixin(SimpleRegistry.class)
public interface SimpleRegistryTagAccessor {
    @Accessor("tags")
    Map<TagKey<?>, RegistryEntryList.Named<?>> finiteWorldsInitialTags();
}
