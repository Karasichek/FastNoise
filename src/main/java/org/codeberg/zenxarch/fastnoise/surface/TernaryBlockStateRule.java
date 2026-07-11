package org.codeberg.zenxarch.fastnoise.surface;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionalBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SimpleBlockStateRule;
import org.jspecify.annotations.Nullable;

public record TernaryBlockStateRule(
    BooleanSupplier condition, BlockState ifTrue, BlockState ifFalse) implements BlockStateRule {
  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    return condition.get() ? ifTrue : ifFalse;
  }

  @Override
  public final String toString() {
    return "TernaryBlockStateRule["
        + condition.toString()
        + " ? "
        + ifTrue.toString()
        + " : "
        + ifFalse.toString()
        + "]";
  }

  public static @Nullable TernaryBlockStateRule tryApply(List<BlockStateRule> rules) {
    if (rules.size() != 2) return null;
    if (rules.get(0) instanceof ConditionalBlockStateRule conditionalBlockStateRule
        && conditionalBlockStateRule.followup() instanceof SimpleBlockStateRule rulea
        && rules.get(1) instanceof SimpleBlockStateRule ruleb) {
      return new TernaryBlockStateRule(
          conditionalBlockStateRule.condition(), rulea.state(), ruleb.state());
    }

    return null;
  }
}
