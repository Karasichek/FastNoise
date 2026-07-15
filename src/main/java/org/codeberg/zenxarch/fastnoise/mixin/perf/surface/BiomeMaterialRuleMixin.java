package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Set;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BiomeMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.rules.BiomePredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.VanillaBiomePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeMaterialRule.class)
public abstract class BiomeMaterialRuleMixin {
  @Shadow
  abstract boolean isDisjoint(final Set<RegistryEntry<Biome>> contextBiomes);

  @Shadow
  abstract boolean isSuperset(final Set<RegistryEntry<Biome>> contextBiomes);

  @WrapMethod(
      method =
          "apply(Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRuleContext;)Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$BooleanSupplier;")
  public BooleanSupplier zenxarch$apply(
      final MaterialRules.MaterialRuleContext ruleContext, Operation<BooleanSupplier> op) {
    if (ruleContext instanceof MaterialRuleContext ctx) {
      return BiomePredicate.getSupplier(((BiomeMaterialRule) (Object) this).biomes(), ctx);
    }
    if (this.isDisjoint(ruleContext.includedBiomes)) return BiomePredicate.ALWAYS_FALSE;
    if (this.isSuperset(ruleContext.includedBiomes)) return BiomePredicate.ALWAYS_TRUE;
    return new VanillaBiomePredicate(((BiomeMaterialRule) (Object) this).biomes(), ruleContext);
  }
}
