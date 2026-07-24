package org.codeberg.zenxarch.fastnoise.surface.compiler;

import static org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleInterpreter.*;
import static org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleProgram.*;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import org.codeberg.zenxarch.fastnoise.surface.AllocFreeRandom;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.rules.VerticalGradientPredicate;

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
  private static final MethodHandle biomeTest = lookupCondition("biomeTest", long.class);

  private static final MethodHandle aboveY = lookupCondition("aboveY", int.class);
  private static final MethodHandle aboveYWithRunDepth =
      lookupCondition("aboveYWithRunDepth", int.class, int.class);
  private static final MethodHandle aboveYWithStoneDepth =
      lookupCondition("aboveYWithStoneDepth", int.class);
  private static final MethodHandle aboveYWithRunDepthAndStoneDepth =
      lookupCondition("aboveYWithRunDepthAndStoneDepth", int.class, int.class);
  private static final MethodHandle stoneDepthTest =
      lookupCondition("stoneDepthTest", int.class, boolean.class, int.class, boolean.class);
  private static final MethodHandle xoroVerticalGradientTest =
      lookupCondition("xoroVerticalGradientTest", int.class, int.class, long.class, long.class);
  private static final MethodHandle checkedVerticalGradientTest =
      lookupCondition("checkedVerticalGradientTest", int.class, int.class, long.class);

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

  public static MethodHandle compile(MaterialRuleProgram program) {
    var baseCache = new Int2ObjectOpenHashMap<MethodHandle>();
    return compile0(program, 0, baseCache);
  }

  private static MethodHandle compile0(
      MaterialRuleProgram program, int index, Int2ObjectOpenHashMap<MethodHandle> cache) {
    if (index >= program.size()) return retNull;
    if (cache.containsKey(index)) return cache.get(index);
    var res = compile1(program, index, cache);
    cache.put(index, res);
    return res;
  }

  private static MethodHandle compile1(
      MaterialRuleProgram program, int index, Int2ObjectOpenHashMap<MethodHandle> cache) {
    if (index >= program.size()) return retNull;
    var inst = program.opcode()[index];

    return switch (inst) {
      case OPCODE_RETURN -> createRet(program.data()[index]);
      case OPCODE_TRY_RETURN ->
          createTryRet(program.data()[index], compile0(program, index + 1, cache));
      case OPCODE_JUMP_IF -> {
        var jumpData = program.jumpDataArr()[program.data()[index]];
        yield createTernary(
            createConditionFromProgram(program, jumpData.type(), jumpData.idx()),
            program,
            index + jumpData.offset(),
            index + 1,
            cache);
      }
      case OPCODE_JUMP_IF_NOT -> {
        var jumpData = program.jumpDataArr()[program.data()[index]];
        yield createTernary(
            createConditionFromProgram(program, jumpData.type(), jumpData.idx()),
            program,
            index + 1,
            index + jumpData.offset(),
            cache);
      }
      default -> retNull;
    };
  }

  private static MethodHandle createTernary(
      MethodHandle guard,
      MaterialRuleProgram program,
      int index0,
      int index1,
      Int2ObjectOpenHashMap<MethodHandle> cache) {
    return createTernary(guard, compile0(program, index0, cache), compile0(program, index1, cache));
  }

  private static MethodHandle createConditionFromProgram(
      MaterialRuleProgram program, byte type, int idx) {
    return switch (type) {
      case JUMP_TYPE_CONDITION -> createCondition(idx);
      case JUMP_TYPE_BIOME -> createBiomeTest(program.biomeMasks()[idx]);
      case JUMP_TYPE_ABOVE_Y -> {
        var aboveY = program.aboveYConditions()[idx];
        yield createAboveY(aboveY.y(), aboveY.surfaceDepthMultiplier(), aboveY.addStoneDepth());
      }
      case JUMP_TYPE_STONE_DEPTH -> {
        var stoneDepth = program.stoneDepthConditions()[idx];
        yield MethodHandles.insertArguments(
            stoneDepthTest,
            0,
            stoneDepth.offset(),
            stoneDepth.addSurfaceDepth(),
            stoneDepth.secondaryDepthRange(),
            stoneDepth.ceiling());
      }
      case JUMP_TYPE_XORO_VERTICAL_GRADIENT -> {
        var xoro = program.xoroVerticalGradientConditions()[idx];
        yield MethodHandles.insertArguments(
            xoroVerticalGradientTest,
            0,
            xoro.trueAtAndBelow(),
            xoro.falseAtAndAbove(),
            xoro.seedLo(),
            xoro.seedHi());
      }
      case JUMP_TYPE_CHECKED_VERTICAL_GRADIENT -> {
        var checked = program.checkedVerticalGradientConditions()[idx];
        yield MethodHandles.insertArguments(
            checkedVerticalGradientTest,
            0,
            checked.trueAtAndBelow(),
            checked.falseAtAndAbove(),
            checked.seed());
      }
      default -> {
        throw new IllegalStateException("undefined condition type " + type);
      }
    };
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

  private static MethodHandle createBiomeTest(long mask) {
    return MethodHandles.insertArguments(biomeTest, 0, mask);
  }

  private static MethodHandle createAboveY(int y, int runDepthMultiplier, boolean addStoneDepth) {
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

  public static boolean condition(
      int index,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
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

  public static boolean biomeTest(
      long mask,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context) {
    var idx = context.getCurrentBiomeIdx();
    return (mask & (0x1L << idx)) != 0;
  }

  public static boolean aboveY(
      int testY,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z,
      MaterialRuleContext context)
      throws Throwable {
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
      MaterialRuleContext context)
      throws Throwable {
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
      MaterialRuleContext context)
      throws Throwable {
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
}
