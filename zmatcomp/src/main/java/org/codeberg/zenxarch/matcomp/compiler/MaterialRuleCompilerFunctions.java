package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
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
    return ((MaterialRuleContextExtension) context)
        .zenxarch$getCurrentBiome()
        .value()
        .isCold(
            context.pos.set(context.blockX, context.blockY, context.blockZ), context.getSeaLevel());
  }

  public static boolean steepSlopePredicate(MaterialRuleContext context) {
    return context.steepSlopePredicate.get();
  }
}
