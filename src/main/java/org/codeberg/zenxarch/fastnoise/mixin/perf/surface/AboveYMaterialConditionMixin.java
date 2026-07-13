package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.AboveYMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.rules.AboveYPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AboveYMaterialCondition.class)
public abstract class AboveYMaterialConditionMixin {
  @Shadow @Final private YOffset anchor;
  @Shadow @Final private int surfaceDepthMultiplier;
  @Shadow @Final private boolean addStoneDepth;

  @Overwrite
  private BooleanSupplier apply(final MaterialRuleContext ruleContext) {
    return new AboveYPredicate(
        anchor.getY(ruleContext.heightContext), surfaceDepthMultiplier, addStoneDepth, ruleContext);
  }
}
