package org.codeberg.zenxarch.fastnoise.surface.rules;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public record XoroshiroVerticalGradientPredicate(
    int trueAtAndBelow, int falseAtAndAbove, long seedLo, long seedHi, MaterialRuleContext context)
    implements BooleanSupplier {

  @Override
  public boolean get() {
    final int blockY = this.context.blockY;
    if (blockY <= trueAtAndBelow) return true;
    if (blockY >= falseAtAndAbove) return false;

    var probability = map(blockY, trueAtAndBelow, falseAtAndAbove);
    return nextFloat(seedLo, seedHi, context.blockX, blockY, context.blockZ) < probability;
  }

  private static float nextFloat(long seedLo, long seedHi, int x, int y, int z) {
    seedLo = MathHelper.hashCode(x, y, z) ^ seedLo;

    if ((seedLo | seedHi) == 0L) {
      seedLo = -7046029254386353131L;
      seedHi = 7640891576956012809L;
    }

    return (float) (next(seedLo, seedHi) >>> (64 - 24)) * 5.9604645E-8F;
  }

  private static long next(long s0, long s1) {
    return Long.rotateLeft(s0 + s1, 17) + s0;
  }

  private static double map(double value, double start, double end) {
    double delta = (value - start) / (end - start);
    return 1.0 - delta;
  }
}
