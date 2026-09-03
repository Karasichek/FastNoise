package org.codeberg.zenxarch.matcomp.mixin.perf.surface;

import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ConditionMaterialRule.class)
public abstract class ConditionMaterialRuleMixin {
  @Shadow @Final private MaterialCondition ifTrue;

  @Shadow @Final private MaterialRule thenRun;

  @Overwrite
  public BlockStateRule getBlockStateRule(final MaterialRuleContext context) {
    var condition = ifTrue.getPredicate(context);
    if (condition == BiomePredicate.ALWAYS_FALSE) return BiomePredicate.ALWAYS_NULL;
    var rule = thenRun.getBlockStateRule(context);
    if (rule == BiomePredicate.ALWAYS_NULL) return rule;
    if (condition == BiomePredicate.ALWAYS_TRUE) return rule;

    return (x, y, z) -> condition.get() ? rule.tryApply(x, y, z) : null;
  }
}
