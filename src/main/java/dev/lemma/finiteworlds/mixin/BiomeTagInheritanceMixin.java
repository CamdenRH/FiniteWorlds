package dev.lemma.finiteworlds.mixin;

import dev.lemma.finiteworlds.worldgen.TemperateBiomeVariants;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TagGroupLoader.class)
public abstract class BiomeTagInheritanceMixin {
    @Inject(method="loadInitial",at=@At("RETURN"))
    private static <T> void finiteWorldsInheritBiomeTags(ResourceManager manager,MutableRegistry<T> registry,CallbackInfo ci) {
        TemperateBiomeVariants.inheritTags(registry);
    }
}
