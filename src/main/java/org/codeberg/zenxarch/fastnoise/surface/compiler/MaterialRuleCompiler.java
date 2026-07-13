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

  private static final MethodHandle condition;
  private static final MethodHandle ret;
  private static final MethodHandle tryRet;
  private static final MethodHandle retNull;
  private static final MethodHandle biomeTest;

  private static MethodHandle lookup(String name, Class<?>... classes)
      throws NoSuchMethodException, IllegalAccessException {
    var list = ObjectArrayList.of(classes);
    list.addAll(commonTypes);
    return MethodHandles.lookup()
        .findStatic(
            MaterialRuleCompiler.class,
            name,
            MethodType.methodType(BlockState.class, list.toArray(Class<?>[]::new)));
  }

  static {
    MethodHandle temp$condition;
    MethodHandle temp$ret;
    MethodHandle temp$tryRet;
    MethodHandle temp$retNull;
    MethodHandle temp$biomeTest;

    try {
      temp$condition = lookup("condition", int.class, MethodHandle.class, MethodHandle.class);
      temp$ret = lookup("ret", int.class);
      temp$tryRet = lookup("tryRet", int.class, MethodHandle.class);
      temp$retNull = lookup("retNull");
      temp$biomeTest = lookup("biomeTest", long.class, MethodHandle.class, MethodHandle.class);
    } catch (Exception e) {
      temp$condition = null;
      temp$ret = null;
      temp$tryRet = null;
      temp$retNull = null;
      temp$biomeTest = null;
    }
    condition = temp$condition;
    ret = temp$ret;
    tryRet = temp$tryRet;
    retNull = temp$retNull;
    biomeTest = temp$biomeTest;
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
              compile0(program, program[index + 2]));
      case RETURN -> createRet(program[index + 1]);
      case TRY_RETURN -> createTryRet(program[index + 1], compile0(program, index + 2));
      case RETURN_IF ->
          createCondition(
              program[index + 1], createRet(program[index + 2]), compile0(program, index + 3));
      case BIOME_TEST ->
          createBiomeTest(
              getLong(program, index + 1),
              compile0(program, index + 4),
              compile0(program, program[index + 3]));
      default -> retNull;
    };
  }

  private static MethodHandle createCondition(
      int index, MethodHandle ifTrue, MethodHandle ifFalse) {
    return MethodHandles.insertArguments(condition, 0, index, ifTrue, ifFalse);
  }

  private static MethodHandle createRet(int index) {
    return MethodHandles.insertArguments(ret, 0, index);
  }

  private static MethodHandle createTryRet(int index, MethodHandle orElse) {
    return MethodHandles.insertArguments(tryRet, 0, index, orElse);
  }

  private static MethodHandle createBiomeTest(
      long mask, MethodHandle ifTrue, MethodHandle ifFalse) {
    return MethodHandles.insertArguments(biomeTest, 0, mask, ifTrue, ifFalse);
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
}
