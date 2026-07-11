package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionalBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.BiomePredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ConditionMaterialRule.class)
public abstract class ConditionMaterialRuleMixin {
  @Shadow @Final private MaterialCondition ifTrue;

  @Shadow @Final private MaterialRule thenRun;

  @Overwrite
  public BlockStateRule apply(MaterialRuleContext context) {
    var condition = ifTrue.apply(context);
    if (condition == BiomePredicate.ALWAYS_FALSE) return BiomePredicate.ALWAYS_NULL;
    var rule = thenRun.apply(context);
    if (condition == BiomePredicate.ALWAYS_TRUE) return rule;
    return new ConditionalBlockStateRule(condition, rule);
  }
}
