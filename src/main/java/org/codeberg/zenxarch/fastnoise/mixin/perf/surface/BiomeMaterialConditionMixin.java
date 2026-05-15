package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.List;
import java.util.Set;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.BiomeConditionModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.gen.surfacebuilder.MaterialRules$BiomeMaterialCondition")
public abstract class BiomeMaterialConditionMixin {

  @Final private Set<RegistryKey<Biome>> zenxarch$biomes;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void zenxarch$init(final List<RegistryKey<Biome>> biomes, CallbackInfo ci) {
    this.zenxarch$biomes = ObjectArraySet.of();
    biomes.forEach(this.zenxarch$biomes::add);
  }

  @WrapMethod(
      method =
          "apply(Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRuleContext;)Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$BooleanSupplier;")
  public MaterialRules.BooleanSupplier zenxarch$apply(
      MaterialRuleContext context, Operation<MaterialRules.BooleanSupplier> op) {
    if (context instanceof org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext ctx) {
      var modified = BiomeConditionModifier.getSupplier(ctx, zenxarch$biomes);
      if (modified != null) return modified;
    }
    return op.call(context);
  }
}
