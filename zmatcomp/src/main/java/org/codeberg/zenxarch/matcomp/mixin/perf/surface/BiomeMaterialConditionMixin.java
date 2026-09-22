package org.codeberg.zenxarch.matcomp.mixin.perf.surface;

import net.minecraft.world.gen.surfacebuilder.BiomeMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BiomeMaterialCondition.class)
public abstract class BiomeMaterialConditionMixin {
  @Overwrite
  public BooleanSupplier getPredicate(final MaterialRuleContext context) {
    return BiomePredicate.getSupplier(((BiomeMaterialCondition) (Object) this).biomes(), context);
  }
}
