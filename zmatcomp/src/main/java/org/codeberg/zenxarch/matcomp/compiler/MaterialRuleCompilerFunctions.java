package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.VerticalGradientPredicate;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;

public final class MaterialRuleCompilerFunctions {
  private MaterialRuleCompilerFunctions() {}

  public static boolean aboveY(
      int testY, int surfaceDepthMultiplier, boolean addStoneDepth, MaterialRuleContext context) {
    return context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0)
        >= testY + context.runDepth * surfaceDepthMultiplier;
  }

  public static boolean stoneDepth(
      int offset,
      boolean addSurfaceDepth,
      int secondaryDepthRange,
      boolean ceiling,
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
      int trueAtAndBelow, int falseAtAndAbove, long seed, MaterialRuleContext context) {
    final int blockY = context.blockY;
    if (blockY <= trueAtAndBelow) return true;
    if (blockY >= falseAtAndAbove) return false;

    var probability = VerticalGradientPredicate.map(blockY, trueAtAndBelow, falseAtAndAbove);
    return AllocFreeRandom.nextFloatChecked(seed, context.blockX, blockY, context.blockZ)
        < probability;
  }

  public static boolean negativeRunDepth(MaterialRuleContext context) {
    return context.runDepth <= 0;
  }

  public static boolean surfacePredicate(MaterialRuleContext context) {
    return context.blockY >= context.estimateSurfaceHeight();
  }

  public static boolean biomeTemperaturePredicate(MaterialRuleContext context) {
    return context.getCurrentBiome().value().isCold(context.getBlockPos(), context.getSeaLevel());
  }

  public static boolean steepSlopePredicate(MaterialRuleContext context) {
    return context.steepnessX <= 4 || context.steepnessZ >= 4;
  }

  public static boolean water(
      int offset, int surfaceDepthMultiplier, boolean addStoneDepth, MaterialRuleContext context) {
    final var fluidHeight = context.fluidHeight;
    if (fluidHeight == Integer.MIN_VALUE) return true;
    return (context.blockY + (addStoneDepth ? context.stoneDepthAbove : 0))
        >= fluidHeight + offset + context.runDepth * surfaceDepthMultiplier;
  }
}
