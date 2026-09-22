package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.VerticalGradientPredicate;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;

public final class MaterialRuleCompilerFunctions {
  private MaterialRuleCompilerFunctions() {}

  public static int loadX(MaterialRuleContext ctx) {
    return ctx.blockX;
  }

  public static int loadY(MaterialRuleContext ctx) {
    return ctx.blockY;
  }

  public static int loadZ(MaterialRuleContext ctx) {
    return ctx.blockZ;
  }

  public static int loadStoneDepthAbove(MaterialRuleContext ctx) {
    return ctx.stoneDepthAbove;
  }

  public static int loadStoneDepthBelow(MaterialRuleContext ctx) {
    return ctx.stoneDepthBelow;
  }

  public static int loadRunDepth(MaterialRuleContext ctx) {
    return ctx.runDepth;
  }

  public static int loadFluidHeight(MaterialRuleContext ctx) {
    return ctx.fluidHeight;
  }

  public static int getMultipliedSecondaryDepthRange(
      int secondaryDepthRange, MaterialRuleContext ctx) {
    return (int) ((ctx.getSecondaryDepth() + 1.0) * 0.5 * secondaryDepthRange);
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
    return context.steepnessX <= -4 || context.steepnessZ >= 4;
  }

  public static boolean slopeCondition(int min, int max, MaterialRuleContext ctx) {
    var x = Math.abs(ctx.steepnessX);
    if (x >= min && x <= max) return true;
    var z = Math.abs(ctx.steepnessZ);
    return (z >= min && z <= max);
  }

  public static boolean slopeConditionAbove(int min, MaterialRuleContext ctx) {
    return Math.abs(ctx.steepnessX) >= min || Math.abs(ctx.steepnessZ) >= min;
  }

  public static boolean slopeConditionBelow(int max, MaterialRuleContext ctx) {
    return Math.abs(ctx.steepnessX) <= max || Math.abs(ctx.steepnessZ) <= max;
  }

  public static boolean slopeConditionEquals(int value, MaterialRuleContext ctx) {
    return Math.abs(ctx.steepnessX) == value || Math.abs(ctx.steepnessZ) == value;
  }
}
