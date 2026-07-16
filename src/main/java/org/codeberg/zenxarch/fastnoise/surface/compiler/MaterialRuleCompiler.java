package org.codeberg.zenxarch.fastnoise.surface.compiler;

import static org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleInterpreter.*;

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

  public static MethodHandle compile(int[] program) {
    return compile0(program, 0);
  }

  private static MethodHandle compile0(int[] program, int index) {
    if (index >= program.length) return retNull;
    var inst = program[index];

    return switch (inst) {
      case JUMP_IF_FALSE ->
          createTernary(
              createCondition(program[index + 1]), program, index + 3, index + program[index + 2]);
      case RETURN -> createRet(program[index + 1]);
      case TRY_RETURN -> createTryRet(program[index + 1], compile0(program, index + 2));
      case RETURN_IF ->
          createTernary(
              createCondition(program[index + 1]),
              createRet(program[index + 2]),
              compile0(program, index + 3));
      case BIOME_TEST ->
          createTernary(
              createBiomeTest(getLong(program, index + 1)),
              program,
              index + 4,
              index + program[index + 3]);
      case ABOVE_Y ->
          createTernary(
              createAboveY(program[index + 1], program[index + 2], false),
              program,
              index + 4,
              index + program[index + 3]);
      case ABOVE_Y_WITH_STONE_DEPTH ->
          createTernary(
              createAboveY(program[index + 1], program[index + 2], true),
              program,
              index + 4,
              index + program[index + 3]);
      default -> retNull;
    };
  }

  private static MethodHandle createTernary(
      MethodHandle guard, int[] program, int index0, int index1) {
    return createTernary(guard, compile0(program, index0), compile0(program, index1));
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
      MaterialRuleContext context)
      throws Throwable {
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
      MaterialRuleContext context)
      throws Throwable {
    return (context.blockY + context.stoneDepthAbove)
        >= (testY + context.runDepth * runDepthMultiplier);
  }
}
