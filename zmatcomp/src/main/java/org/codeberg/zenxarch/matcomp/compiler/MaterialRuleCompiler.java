package org.codeberg.zenxarch.matcomp.compiler;

import static org.codeberg.zenxarch.matcomp.compiler.MaterialRuleInterpreter.*;

import java.lang.invoke.MethodHandle;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.VerticalGradientPredicate;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;

public final class MaterialRuleCompiler {
  private MaterialRuleCompiler() {}

  public static boolean condition(
      int index,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return conditions[index].get();
  }

  public static BlockState ret(
      int index,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return states[index];
  }

  public static BlockState tryRet(
      int index,
      MethodHandle orElse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    var state = rules[index].tryApply(x, y, z);
    if (state != null) return state;
    return (BlockState) orElse.invokeExact(conditions, rules, states, x, y, z, context);
  }

  public static BlockState retNull(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return null;
  }

  public static boolean aboveY(
      int testY,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.blockY >= testY;
  }

  public static boolean aboveYWithRunDepth(
      int testY,
      int runDepthMultiplier,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.blockY >= (testY + context.runDepth * runDepthMultiplier);
  }

  public static boolean aboveYWithStoneDepth(
      int testY,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return (context.blockY + context.stoneDepthAbove) >= testY;
  }

  public static boolean aboveYWithRunDepthAndStoneDepth(
      int testY,
      int runDepthMultiplier,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return (context.blockY + context.stoneDepthAbove)
        >= (testY + context.runDepth * runDepthMultiplier);
  }

  public static boolean stoneDepthTest(
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

  public static boolean verticalGradientTest(
      int trueAtAndBelow,
      int falseAtAndAbove,
      RandomSplitter splitter,
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
    return splitter.split(context.blockX, blockY, context.blockZ).nextFloat() < probability;
  }

  public static boolean xoroVerticalGradientTest(
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

  public static boolean checkedVerticalGradientTest(
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

  public static boolean invert(
      MethodHandle original,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    return !(boolean) original.invokeExact(conditions, rules, states, x, y, z, context);
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
    return context.getCurrentBiome().value().isCold(context.getBlockPos(), context.getSeaLevel());
  }

  public static boolean steepSlopePredicate(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    return context.getSteepnessX() <= 4 || context.getSteepnessZ() >= 4;
  }
}
