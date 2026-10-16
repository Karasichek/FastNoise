package org.codeberg.zenxarch.matcomp.rules;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import org.jetbrains.annotations.Nullable;

public record ConditionalBlockStateRule(BooleanSupplier condition, BlockStateRule rule)
    implements BlockStateRule {
  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    if (condition.get()) return rule.tryApply(x, y, z);
    return null;
  }
}
