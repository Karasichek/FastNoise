package org.codeberg.zenxarch.fastnoise.surface.compiler;

import static org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleInterpreter.*;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;

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

  private static final MethodHandle condition =
      lookup("condition", int.class, MethodHandle.class, MethodHandle.class);
  private static final MethodHandle ret = lookup("ret", int.class);
  private static final MethodHandle tryRet = lookup("tryRet", int.class, MethodHandle.class);
  private static final MethodHandle retNull = lookup("retNull");
  private static final MethodHandle biomeTest =
      lookup("biomeTest", long.class, MethodHandle.class, MethodHandle.class);

  private static final MethodHandle aboveY =
      lookup("aboveY", int.class, MethodHandle.class, MethodHandle.class);
  private static final MethodHandle aboveYWithRunDepth =
      lookup("aboveYWithRunDepth", int.class, int.class, MethodHandle.class, MethodHandle.class);
  private static final MethodHandle aboveYWithStoneDepth =
      lookup("aboveYWithStoneDepth", int.class, MethodHandle.class, MethodHandle.class);
  private static final MethodHandle aboveYWithRunDepthAndStoneDepth =
      lookup(
          "aboveYWithRunDepthAndStoneDepth",
          int.class,
          int.class,
          MethodHandle.class,
          MethodHandle.class);

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

  public static MethodHandle compile(int[] program) {
    return compile0(program, 0);
  }

  private static MethodHandle compile0(int[] program, int index) {
    if (index >= program.length) return retNull;
    var inst = program[index];

    return switch (inst) {
      case JUMP_IF_FALSE ->
          createCondition(
              program[index + 1],
              compile0(program, index + 3),
              compile0(program, index + program[index + 2]));
      case RETURN -> createRet(program[index + 1]);
      case TRY_RETURN -> createTryRet(program[index + 1], compile0(program, index + 2));
      case RETURN_IF ->
          createCondition(
              program[index + 1], createRet(program[index + 2]), compile0(program, index + 3));
      case BIOME_TEST ->
          createBiomeTest(
              getLong(program, index + 1),
              compile0(program, index + 4),
              compile0(program, index + program[index + 3]));
      case ABOVE_Y ->
          createAboveY(
              program[index + 1],
              program[index + 2],
              false,
              compile0(program, index + 4),
              compile0(program, index + program[index + 3]));
      case ABOVE_Y_WITH_STONE_DEPTH ->
          createAboveY(
              program[index + 1],
              program[index + 2],
              true,
              compile0(program, index + 4),
              compile0(program, index + program[index + 3]));
      default -> retNull;
    };
  }

  private static record ConditionCache(int index, MethodHandle ifTrue, MethodHandle ifFalse) {}

  private static final Object2ObjectOpenHashMap<ConditionCache, MethodHandle> conditionCache =
      new Object2ObjectOpenHashMap<>();

  private static MethodHandle createCondition(
      int index, MethodHandle ifTrue, MethodHandle ifFalse) {
    return conditionCache.computeIfAbsent(
        new ConditionCache(index, ifTrue, ifFalse), MaterialRuleCompiler::createCondition0);
  }

  private static MethodHandle createCondition0(ConditionCache entry) {
    return createCondition1(entry.index, entry.ifTrue, entry.ifFalse);
  }

  private static MethodHandle createCondition1(
      int index, MethodHandle ifTrue, MethodHandle ifFalse) {
    return MethodHandles.insertArguments(condition, 0, index, ifTrue, ifFalse);
  }

  private static final ObjectArrayList<MethodHandle> retCache = new ObjectArrayList<>();

  private static MethodHandle createRet(int index) {
    if (index >= retCache.size()) {
      retCache.size(index + 1);
    }
    var res = retCache.get(index);
    if (res == null) {
      res = createRet0(index);
      retCache.set(index, res);
    }
    return res;
  }

  private static MethodHandle createRet0(int index) {
    return MethodHandles.insertArguments(ret, 0, index);
  }

  private static record TryRetCacheEntry(int index, MethodHandle orElse) {}

  private static final Object2ObjectOpenHashMap<TryRetCacheEntry, MethodHandle> tryRetCache =
      new Object2ObjectOpenHashMap<>();

  private static MethodHandle createTryRet(int index, MethodHandle orElse) {
    return tryRetCache.computeIfAbsent(
        new TryRetCacheEntry(index, orElse), MaterialRuleCompiler::createTryRet0);
  }

  private static MethodHandle createTryRet0(TryRetCacheEntry entry) {
    return createTryRet1(entry.index, entry.orElse);
  }

  private static MethodHandle createTryRet1(int index, MethodHandle orElse) {
    return MethodHandles.insertArguments(tryRet, 0, index, orElse);
  }

  private static record BiomeTestCache(long mask, MethodHandle ifTrue, MethodHandle ifFalse) {}

  private static final Object2ObjectOpenHashMap<BiomeTestCache, MethodHandle> biomeTestCache =
      new Object2ObjectOpenHashMap<>();

  private static MethodHandle createBiomeTest(
      long mask, MethodHandle ifTrue, MethodHandle ifFalse) {
    return biomeTestCache.computeIfAbsent(
        new BiomeTestCache(mask, ifTrue, ifFalse), MaterialRuleCompiler::createBiomeTest0);
  }

  private static MethodHandle createBiomeTest0(BiomeTestCache cache) {
    return createBiomeTest1(cache.mask, cache.ifTrue, cache.ifFalse);
  }

  private static MethodHandle createBiomeTest1(
      long mask, MethodHandle ifTrue, MethodHandle ifFalse) {
    return MethodHandles.insertArguments(biomeTest, 0, mask, ifTrue, ifFalse);
  }

  private static record AboveYCache(
      int y, int runDepthMultiplier, MethodHandle ifTrue, MethodHandle ifFalse) {}

  private static final Object2ObjectOpenHashMap<AboveYCache, MethodHandle> aboveYCache =
      new Object2ObjectOpenHashMap<>();

  private static final Object2ObjectOpenHashMap<AboveYCache, MethodHandle>
      aboveYWithStoneDepthCache = new Object2ObjectOpenHashMap<>();

  private static MethodHandle createAboveY(
      int y,
      int runDepthMultiplier,
      boolean addStoneDepth,
      MethodHandle ifTrue,
      MethodHandle ifFalse) {
    var entry = new AboveYCache(y, runDepthMultiplier, ifTrue, ifFalse);
    return (addStoneDepth
        ? aboveYWithStoneDepthCache.computeIfAbsent(entry, MaterialRuleCompiler::createAboveY1)
        : aboveYCache.computeIfAbsent(entry, MaterialRuleCompiler::createAboveY0));
  }

  private static MethodHandle createAboveY0(AboveYCache cache) {
    return createAboveY2(cache.y, cache.runDepthMultiplier, false, cache.ifTrue, cache.ifFalse);
  }

  private static MethodHandle createAboveY1(AboveYCache cache) {
    return createAboveY2(cache.y, cache.runDepthMultiplier, true, cache.ifTrue, cache.ifFalse);
  }

  private static MethodHandle createAboveY2(
      int y,
      int runDepthMultiplier,
      boolean addStoneDepth,
      MethodHandle ifTrue,
      MethodHandle ifFalse) {
    if (addStoneDepth) {
      if (runDepthMultiplier == 0) {
        return MethodHandles.insertArguments(aboveYWithStoneDepth, 0, y, ifTrue, ifFalse);
      } else {
        return MethodHandles.insertArguments(
            aboveYWithRunDepthAndStoneDepth, 0, y, runDepthMultiplier, ifTrue, ifFalse);
      }
    }

    if (runDepthMultiplier == 0) {
      return MethodHandles.insertArguments(aboveY, 0, y, ifTrue, ifFalse);
    } else {
      return MethodHandles.insertArguments(
          aboveYWithRunDepth, 0, y, runDepthMultiplier, ifTrue, ifFalse);
    }
  }

  public static BlockState condition(
      int index,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    if (conditions[index].get()) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z, context);
    }
    return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z, context);
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

  public static BlockState biomeTest(
      long mask,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    var idx = context.getCurrentBiomeIdx();
    if ((mask & (0x1L << idx)) != 0) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z, context);
    }
    return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z, context);
  }

  public static BlockState aboveY(
      int testY,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    if (context.blockY >= testY) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z, context);
    } else {
      return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z, context);
    }
  }

  public static BlockState aboveYWithRunDepth(
      int testY,
      int runDepthMultiplier,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    if (context.blockY >= (testY + context.runDepth * runDepthMultiplier)) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z, context);
    } else {
      return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z, context);
    }
  }

  public static BlockState aboveYWithStoneDepth(
      int testY,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    if ((context.blockY + context.stoneDepthAbove) >= testY) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z, context);
    } else {
      return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z, context);
    }
  }

  public static BlockState aboveYWithRunDepthAndStoneDepth(
      int testY,
      int runDepthMultiplier,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
    if ((context.blockY + context.stoneDepthAbove)
        >= (testY + context.runDepth * runDepthMultiplier)) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z, context);
    } else {
      return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z, context);
    }
  }
}
