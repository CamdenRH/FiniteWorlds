package dev.lemma.finiteworlds.mixin;

import dev.lemma.finiteworlds.worldgen.FiniteDimensionCompatibility;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Map;

@Mixin(DimensionOptionsRegistryHolder.class)
public abstract class DimensionOptionsRegistryHolderMixin {
    @Shadow @Final private Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensions;
    @ModifyVariable(method="toConfig",at=@At("HEAD"),argsOnly=true)
    private Registry<DimensionOptions> finiteWorldsPreservePreset(Registry<DimensionOptions> loaded) {
        return FiniteDimensionCompatibility.preserveSelectedOverworld(dimensions,loaded);
    }
}
