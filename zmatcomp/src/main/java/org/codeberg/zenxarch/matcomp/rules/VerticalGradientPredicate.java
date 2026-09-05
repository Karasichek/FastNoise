package org.codeberg.zenxarch.matcomp.rules;

import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;

public record VerticalGradientPredicate(
    int trueAtAndBelow, int falseAtAndAbove, RandomSplitter splitter, MaterialRuleContext context)
    implements BooleanSupplier {

  @Override
  public boolean get() {
    final int blockY = this.context.blockY;
    if (blockY <= trueAtAndBelow) return true;
    if (blockY >= falseAtAndAbove) return false;

    var probability = map(blockY, trueAtAndBelow, falseAtAndAbove);
    return splitter.split(context.blockX, blockY, context.blockZ).nextFloat() < probability;
  }

  public static double map(double value, double start, double end) {
    double delta = (value - start) / (end - start);
    return 1.0 - delta;
  }

  public static record XoroshiroVerticalGradientPredicate(
      int trueAtAndBelow,
      int falseAtAndAbove,
      long seedLo,
      long seedHi,
      MaterialRuleContext context)
      implements BooleanSupplier {

    @Override
    public boolean get() {
      final int blockY = this.context.blockY;
      if (blockY <= trueAtAndBelow) return true;
      if (blockY >= falseAtAndAbove) return false;

      var probability = map(blockY, trueAtAndBelow, falseAtAndAbove);
      return AllocFreeRandom.nextFloatXoroshiro(
              seedLo, seedHi, context.blockX, blockY, context.blockZ)
          < probability;
    }
  }

  public static record CheckedVerticalGradientPredicate(
      int trueAtAndBelow, int falseAtAndAbove, long seed, MaterialRuleContext context)
      implements BooleanSupplier {

    @Override
    public boolean get() {
      final int blockY = this.context.blockY;
      if (blockY <= trueAtAndBelow) return true;
      if (blockY >= falseAtAndAbove) return false;

      var probability = map(blockY, trueAtAndBelow, falseAtAndAbove);
      return AllocFreeRandom.nextFloatChecked(seed, context.blockX, blockY, context.blockZ)
          < probability;
    }
  }
}
