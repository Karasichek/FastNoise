package org.codeberg.zenxarch.fastnoise.surface.rules;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.VerticalGradientMaterialCondition;

public record VerticalGradientPredicate(
    int trueAtAndBelow, int falseAtAndAbove, RandomSplitter splitter, MaterialRuleContext context)
    implements BooleanSupplier {

  public static BooleanSupplier create(
      VerticalGradientMaterialCondition condition, MaterialRuleContext context) {
    var split = context.noiseConfig.getOrCreateRandomDeriver(condition.randomName());
    var trueAtAndBelow = condition.trueAtAndBelow().getY(context.heightContext);
    var falseAtAndAbove = condition.falseAtAndAbove().getY(context.heightContext);
    if (split instanceof Xoroshiro128PlusPlusRandom.Splitter xoroshiro)
      return new XoroshiroVerticalGradientPredicate(
          trueAtAndBelow, falseAtAndAbove, xoroshiro.seedLo, xoroshiro.seedHi, context);
    if (split instanceof CheckedRandom.Splitter checked)
      return new CheckedVerticalGradientPredicate(
          trueAtAndBelow, falseAtAndAbove, checked.seed, context);

    return new VerticalGradientPredicate(trueAtAndBelow, falseAtAndAbove, split, context);
  }

  @Override
  public boolean get() {
    final int blockY = this.context.blockY;
    if (blockY <= trueAtAndBelow) return true;
    if (blockY >= falseAtAndAbove) return false;

    var probability = map(blockY, trueAtAndBelow, falseAtAndAbove);
    return splitter.split(context.blockX, blockY, context.blockZ).nextFloat() < probability;
  }

  private static double map(double value, double start, double end) {
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
      return nextFloat(seed, context.blockX, blockY, context.blockZ) < probability;
    }

    private static float nextFloat(long seed, int x, int y, int z) {
      seed = MathHelper.hashCode(x, y, z) ^ seed;
      seed = (seed ^ 25214903917L) & 281474976710655L;

      return next(seed, 24) * 5.9604645E-8F;
    }

    private static int next(long seed, final int bits) {
      seed = seed * 25214903917L + 11L & 281474976710655L;
      return (int) (seed >> 48 - bits);
    }
  }
}
