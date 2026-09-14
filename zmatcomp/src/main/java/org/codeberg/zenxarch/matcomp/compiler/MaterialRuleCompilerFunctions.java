package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.VerticalGradientPredicate;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;

public final class MaterialRuleCompilerFunctions {
  private MaterialRuleCompilerFunctions() {}

  public static boolean aboveY(
      int testY,
      int surfaceDepthMultiplier,
      boolean addStoneDepth,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0)
        >= testY + context.runDepth * surfaceDepthMultiplier;
  }

  public static boolean stoneDepth(
      int offset,
      boolean addSurfaceDepth,
      int secondaryDepthRange,
      boolean ceiling,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    var stoneDepth = ceiling ? context.stoneDepthBelow : context.stoneDepthAbove;
    if (addSurfaceDepth) stoneDepth -= context.runDepth;
    if (secondaryDepthRange != 0)
      stoneDepth -= (int) ((context.getSecondaryDepth() + 1.0) * 0.5 * secondaryDepthRange);

    return stoneDepth <= offset;
  }

  public static boolean xoroVerticalGradient(
      int trueAtAndBelow,
      int falseAtAndAbove,
      long seedLo,
      long seedHi,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    final int blockY = context.blockY;
    if (blockY <= trueAtAndBelow) return true;
    if (blockY >= falseAtAndAbove) return false;

    var probability = VerticalGradientPredicate.map(blockY, trueAtAndBelow, falseAtAndAbove);
    return AllocFreeRandom.nextFloatXoroshiro(
            seedLo, seedHi, context.blockX, blockY, context.blockZ)
        < probability;
  }

  public static boolean checkedVerticalGradient(
      int trueAtAndBelow,
      int falseAtAndAbove,
      long seed,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    final int blockY = context.blockY;
    if (blockY <= trueAtAndBelow) return true;
    if (blockY >= falseAtAndAbove) return false;

    var probability = VerticalGradientPredicate.map(blockY, trueAtAndBelow, falseAtAndAbove);
    return AllocFreeRandom.nextFloatChecked(seed, context.blockX, blockY, context.blockZ)
        < probability;
  }

  public static boolean negativeRunDepth(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.runDepth <= 0;
  }

  public static boolean surfacePredicate(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.blockY >= context.estimateSurfaceHeight();
  }

  public static boolean biomeTemperaturePredicate(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context
        .getCurrentBiome()
        .value()
        .isCold(
            context.pos.set(context.blockX, context.blockY, context.blockZ), context.getSeaLevel());
  }

  public static boolean steepSlopePredicate(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.steepSlopePredicate.get();
  }

  public static boolean water(
      int offset,
      int surfaceDepthMultiplier,
      boolean addStoneDepth,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    final var fluidHeight = context.fluidHeight;
    if (fluidHeight == Integer.MIN_VALUE) return true;
    return (context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0))
        >= fluidHeight + offset + context.runDepth * surfaceDepthMultiplier;
  }
}
