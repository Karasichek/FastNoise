package org.codeberg.zenxarch.matcomp.mixin.perf.surface;

import net.minecraft.world.gen.surfacebuilder.MaterialRules.BiomeMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BiomeMaterialRule.class)
public abstract class BiomeMaterialConditionMixin {
  @Overwrite
  public BooleanSupplier apply(final MaterialRuleContext context) {
    return BiomePredicate.getSupplier(((BiomeMaterialRule) (Object) this).biomes(), context);
  }
}
