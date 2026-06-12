package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BiomeMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import org.codeberg.zenxarch.fastnoise.surface.BiomePredicate;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;

@Debug(export = true)
@Mixin(BiomeMaterialCondition.class)
public abstract class BiomeMaterialRuleMixin {
  @WrapMethod(
      method =
          "apply(Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRuleContext;)Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$BooleanSupplier;")
  public BooleanSupplier zenxarch$apply(
      final MaterialRules.MaterialRuleContext ruleContext, Operation<BooleanSupplier> op) {
    if (ruleContext instanceof MaterialRuleContext ctx) {
      return BiomePredicate.getSupplier(((BiomeMaterialCondition) (Object) this).biomes, ctx);
    }
    return op.call(ruleContext);
  }
}
