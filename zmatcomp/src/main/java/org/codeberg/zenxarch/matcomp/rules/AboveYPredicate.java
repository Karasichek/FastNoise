package org.codeberg.zenxarch.matcomp.rules;

import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public record AboveYPredicate(
    int y, int surfaceDepthMultiplier, boolean addStoneDepth, MaterialRuleContext context)
    implements BooleanSupplier {

  @Override
  public boolean get() {
    return (context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0))
        >= (y + context.runDepth * surfaceDepthMultiplier);
  }
}
