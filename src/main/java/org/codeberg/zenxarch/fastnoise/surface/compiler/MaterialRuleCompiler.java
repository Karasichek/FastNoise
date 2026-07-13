package org.codeberg.zenxarch.fastnoise.surface.compiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;

public final class MaterialRuleCompiler {
  private MaterialRuleCompiler() {}

  private static final MethodHandle condition;
  private static final MethodHandle ret;
  private static final MethodHandle tryRet;
  private static final MethodHandle retNull;

  static {
    MethodHandle temp$condition;
    MethodHandle temp$ret;
    MethodHandle temp$tryRet;
    MethodHandle temp$retNull;

    var lookup = MethodHandles.lookup();

    try {
      temp$condition =
          lookup.findStatic(
              MaterialRuleCompiler.class,
              "condition",
              MethodType.methodType(
                  BlockState.class,
                  int.class,
                  MethodHandle.class,
                  MethodHandle.class,
                  BooleanSupplier[].class,
                  BlockStateRule[].class,
                  BlockState[].class,
                  int.class,
                  int.class,
                  int.class));

      temp$ret =
          lookup.findStatic(
              MaterialRuleCompiler.class,
              "ret",
              MethodType.methodType(
                  BlockState.class,
                  int.class,
                  BooleanSupplier[].class,
                  BlockStateRule[].class,
                  BlockState[].class,
                  int.class,
                  int.class,
                  int.class));

      temp$tryRet =
          lookup.findStatic(
              MaterialRuleCompiler.class,
              "tryRet",
              MethodType.methodType(
                  BlockState.class,
                  int.class,
                  MethodHandle.class,
                  BooleanSupplier[].class,
                  BlockStateRule[].class,
                  BlockState[].class,
                  int.class,
                  int.class,
                  int.class));

      temp$retNull =
          lookup.findStatic(
              MaterialRuleCompiler.class,
              "retNull",
              MethodType.methodType(
                  BlockState.class,
                  BooleanSupplier[].class,
                  BlockStateRule[].class,
                  BlockState[].class,
                  int.class,
                  int.class,
                  int.class));

    } catch (Exception e) {
      temp$condition = null;
      temp$ret = null;
      temp$tryRet = null;
      temp$retNull = null;
    }
    condition = temp$condition;
    ret = temp$ret;
    tryRet = temp$tryRet;
    retNull = temp$retNull;
  }

  private static final int JUMP_IF_FALSE = 0; // if (!conditions[a]) jmp(b);
  private static final int RETURN = 1; // return states[a];
  private static final int TRY_RETURN = 2; // if (rules[a] != null) return rules[a];
  private static final int RETURN_IF = 3; // if (conditions[a]) return states[b];

  public static MethodHandle compile(int[] program) {
    return compile0(program, 0);
  }

  private static MethodHandle compile0(int[] program, int index) {
    if (index >= program.length) return retNull;
    var inst = program[index];
    var opcode = inst & 0xF;
    var a = (inst >>> 4) & 0x3FFF;
    var b = (inst >>> 18) & 0x3FFF;

    return switch (opcode) {
      case JUMP_IF_FALSE -> createCondition(a, compile0(program, index + 1), compile0(program, b));
      case RETURN -> createRet(a);
      case TRY_RETURN -> createTryRet(a, compile0(program, index + 1));
      case RETURN_IF -> createCondition(a, createRet(b), compile0(program, index + 1));
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

  public static BlockState condition(
      int index,
      MethodHandle ifTrue,
      MethodHandle ifFalse,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z)
      throws Throwable {
    if (conditions[index].get()) {
      return (BlockState) ifTrue.invokeExact(conditions, rules, states, x, y, z);
    }
    return (BlockState) ifFalse.invokeExact(conditions, rules, states, x, y, z);
  }

  public static BlockState ret(
      int index,
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z) {
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
      int z)
      throws Throwable {
    var state = rules[index].tryApply(x, y, z);
    if (state != null) return state;
    return (BlockState) orElse.invokeExact(conditions, rules, states, x, y, z);
  }

  public static BlockState retNull(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int x,
      int y,
      int z) {
    return null;
  }
}
