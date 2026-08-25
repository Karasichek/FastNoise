package org.codeberg.zenxarch.matcomp.compiler;

import static org.codeberg.zenxarch.matcomp.compiler.MaterialRuleInterpreter.*;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.VerticalGradientPredicate;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;

public final class MaterialRuleCompiler {
  private MaterialRuleCompiler() {}

  private static final ObjectList<Class<?>> commonTypes =
      ObjectList.of(
          BooleanSupplier[].class,
          BlockStateRule[].class,
          BlockState[].class,
          int.class,
          int.class,
          int.class,
          MaterialRuleContext.class);

  private static final MethodHandle condition = lookupCondition("condition", int.class);
  private static final MethodHandle ret = lookup("ret", int.class);
  private static final MethodHandle tryRet = lookup("tryRet", int.class, MethodHandle.class);
  private static final MethodHandle retNull = lookup("retNull");
  // private static final MethodHandle biomeTest = lookupCondition("biomeTest", long.class);

  private static final MethodHandle aboveY = lookupCondition("aboveY", int.class);
  private static final MethodHandle aboveYWithRunDepth =
      lookupCondition("aboveYWithRunDepth", int.class, int.class);
  private static final MethodHandle aboveYWithStoneDepth =
      lookupCondition("aboveYWithStoneDepth", int.class);
  private static final MethodHandle aboveYWithRunDepthAndStoneDepth =
      lookupCondition("aboveYWithRunDepthAndStoneDepth", int.class, int.class);
  private static final MethodHandle stoneDepthTest =
      lookupCondition("stoneDepthTest", int.class, boolean.class, int.class, boolean.class);
  private static final MethodHandle verticalGradientTest =
      lookupCondition("verticalGradientTest", int.class, int.class, RandomSplitter.class);
  private static final MethodHandle xoroVerticalGradientTest =
      lookupCondition("xoroVerticalGradientTest", int.class, int.class, long.class, long.class);
  private static final MethodHandle checkedVerticalGradientTest =
      lookupCondition("checkedVerticalGradientTest", int.class, int.class, long.class);
  private static final MethodHandle invert = lookupCondition("invert", MethodHandle.class);
  public static final MethodHandle negativeRunDepth = lookupCondition("negativeRunDepth");
  public static final MethodHandle surfacePredicate = lookupCondition("surfacePredicate");
  public static final MethodHandle biomeTemperaturePredicate =
      lookupCondition("biomeTemperaturePredicate");
  public static final MethodHandle steepSlopePredicate = lookupCondition("steepSlopePredicate");

  private static MethodHandle lookupCondition(String name, Class<?>... classes) {
    var list = ObjectArrayList.of(classes);
    list.addAll(commonTypes);
    try {
      return MethodHandles.lookup()
          .findStatic(
              MaterialRuleCompiler.class,
              name,
              MethodType.methodType(boolean.class, list.toArray(Class<?>[]::new)));
    } catch (Exception e) {
      return null;
    }
  }

  private static MethodHandle lookup(String name, Class<?>... classes) {
    var list = ObjectArrayList.of(classes);
    list.addAll(commonTypes);
    try {
      return MethodHandles.lookup()
          .findStatic(
              MaterialRuleCompiler.class,
              name,
              MethodType.methodType(BlockState.class, list.toArray(Class<?>[]::new)));
    } catch (Exception e) {
      return null;
    }
  }

  public static MethodHandle compile(
      int[] program, MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    var baseCache = new Int2ObjectOpenHashMap<MethodHandle>();
    return compile0(program, 0, baseCache, contextSpecificCache);
  }

  private static MethodHandle compile0(
      int[] program,
      int index,
      Int2ObjectOpenHashMap<MethodHandle> cache,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    if (index >= program.length) return retNull;
    if (cache.containsKey(index)) return cache.get(index);
    var res = compile1(program, index, cache, contextSpecificCache);
    cache.put(index, res);
    return res;
  }

  private static MethodHandle compile1(
      int[] program,
      int index,
      Int2ObjectOpenHashMap<MethodHandle> cache,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    if (index >= program.length) return retNull;
    var inst = program[index];

    return switch (inst) {
      case JUMP_IF_FALSE ->
          createTernary(
              createCondition(program[index + 1]),
              program,
              index + 3,
              index + program[index + 2],
              cache,
              contextSpecificCache);
      case RETURN -> createRet(program[index + 1]);
      case TRY_RETURN ->
          createTryRet(
              program[index + 1], compile0(program, index + 2, cache, contextSpecificCache));
      case RETURN_IF ->
          createTernary(
              createCondition(program[index + 1]),
              createRet(program[index + 2]),
              compile0(program, index + 3, cache, contextSpecificCache));
      // case BIOME_TEST ->
      //     createTernary(
      //         createBiomeTest(getLong(program, index + 1)),
      //         program,
      //         index + 4,
      //         index + program[index + 3],
      //         cache,
      //         contextSpecificCache);
      case JUMP_IF_FALSE_STATIC ->
          createTernary(
              contextSpecificCache.getHandle(program[index + 1]),
              program,
              index + 3,
              index + program[index + 2],
              cache,
              contextSpecificCache);
      default -> throw new IllegalStateException("Invalid instruction");
    };
  }

  private static MethodHandle createTernary(
      MethodHandle guard,
      int[] program,
      int index0,
      int index1,
      Int2ObjectOpenHashMap<MethodHandle> cache,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    return createTernary(
        guard,
        compile0(program, index0, cache, contextSpecificCache),
        compile0(program, index1, cache, contextSpecificCache));
  }

  private static MethodHandle createTernary(
      MethodHandle guard, MethodHandle ifTrue, MethodHandle ifFalse) {
    return MethodHandles.guardWithTest(guard, ifTrue, ifFalse);
  }

  private static MethodHandle createCondition(int index) {
    return MethodHandles.insertArguments(condition, 0, index);
  }

  private static MethodHandle createRet(int index) {
    return MethodHandles.insertArguments(ret, 0, index);
  }

  private static MethodHandle createTryRet(int index, MethodHandle orElse) {
    return MethodHandles.insertArguments(tryRet, 0, index, orElse);
  }

  // private static MethodHandle createBiomeTest(long mask) {
  //   return MethodHandles.insertArguments(biomeTest, 0, mask);
  // }

  public static MethodHandle createAboveY(int y, int runDepthMultiplier, boolean addStoneDepth) {
    if (addStoneDepth) {
      if (runDepthMultiplier == 0) {
        return MethodHandles.insertArguments(aboveYWithStoneDepth, 0, y);
      } else {
        return MethodHandles.insertArguments(
            aboveYWithRunDepthAndStoneDepth, 0, y, runDepthMultiplier);
      }
    }

    if (runDepthMultiplier == 0) {
      return MethodHandles.insertArguments(aboveY, 0, y);
    } else {
      return MethodHandles.insertArguments(aboveYWithRunDepth, 0, y, runDepthMultiplier);
    }
  }

  public static MethodHandle createStoneDepthTest(
      int offset, boolean addSurfaceDepth, int secondaryDepthRange, boolean ceiling) {
    return MethodHandles.insertArguments(
        stoneDepthTest, 0, offset, addSurfaceDepth, secondaryDepthRange, ceiling);
  }

  public static MethodHandle createVerticalGradientTest(
      int trueAtAndBelow, int falseAtAndAbove, RandomSplitter splitter) {
    return switch (splitter) {
      case Xoroshiro128PlusPlusRandom.Splitter xoro ->
          createXoroVerticalGradientTest(trueAtAndBelow, falseAtAndAbove, xoro.seedLo, xoro.seedHi);
      case CheckedRandom.Splitter checked ->
          createCheckedVerticalGradientTest(trueAtAndBelow, falseAtAndAbove, checked.seed);
      default ->
          MethodHandles.insertArguments(
              verticalGradientTest, 0, trueAtAndBelow, falseAtAndAbove, splitter);
    };
  }

  public static MethodHandle createXoroVerticalGradientTest(
      int trueAtAndBelow, int falseAtAndAbove, long seedLo, long seedHi) {
    return MethodHandles.insertArguments(
        xoroVerticalGradientTest, 0, trueAtAndBelow, falseAtAndAbove, seedLo, seedHi);
  }

  public static MethodHandle createCheckedVerticalGradientTest(
      int trueAtAndBelow, int falseAtAndAbove, long seed) {
    return MethodHandles.insertArguments(
        checkedVerticalGradientTest, 0, trueAtAndBelow, falseAtAndAbove, seed);
  }

  public static MethodHandle createInverted(MethodHandle original) {
    return MethodHandles.insertArguments(invert, 0, original);
  }

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

  // public static boolean biomeTest(
  //     long mask,
  //     BooleanSupplier[] conditions,
  //     BlockStateRule[] rules,
  //     BlockState[] states,
  //     int x,
  //     int y,
  //     int z,
  //     MaterialRuleContext context) {
  //   var idx = context.getCurrentBiomeIdx();
  //   return (mask & (0x1L << idx)) != 0;
  // }

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
    return context.biomeTemperaturePredicate.get();
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
}
