package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
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
  public BlockStateRule apply(MaterialRules.MaterialRuleContext context) {
    var condition = ifTrue.apply(context);
    if (condition == BiomePredicate.ALWAYS_FALSE) return BiomePredicate.ALWAYS_NULL;
    var rule = thenRun.apply(context);
    if (condition == BiomePredicate.ALWAYS_TRUE) return rule;
    return new MaterialRules.ConditionalBlockStateRule(condition, rule);
  }
}
