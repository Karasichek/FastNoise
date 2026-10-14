package org.codeberg.zenxarch.matcomp.compiler.cw;

import static org.objectweb.asm.Opcodes.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.BitSet;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class PreCompileMaterialRuleClassWriter extends BaseMaterialRuleClassWriter {
  private static final String BLOCKSTATE_RULE_METHOD_TYPE =
      Type.getMethodDescriptor(
          Type.getType(BlockState.class),
          Type.getType(BooleanSupplier[].class),
          Type.getType(BlockStateRule[].class),
          Type.getType(int.class),
          Type.getType(int.class),
          Type.getType(int.class),
          Type.getType(MaterialRuleContext.class),
          Type.getType(Object[].class));

  @SuppressWarnings("unchecked")
  public static boolean objToBoolean(Object obj, MaterialRuleContext ctx) {
    return switch (obj) {
      case Boolean bool -> bool;
      case RegistryEntryList<?> entryList ->
          ((RegistryEntryList<Biome>) entryList)
              .contains(((MaterialRuleContextExtension) ctx).zenxarch$getCurrentBiome());
      case RegistryEntry<?> entry ->
          ((RegistryEntry<Biome>) entry)
              == ((MaterialRuleContextExtension) ctx).zenxarch$getCurrentBiome();
      default -> throw new IllegalStateException("Unknown type passed to objToBoolean");
    };
  }

  public PreCompileMaterialRuleClassWriter(String classname, FlattenedMaterialRule flattenedRule) {
    super(classname, flattenedRule.program, flattenedRule);
  }

  public static MethodHandle compile(FlattenedMaterialRule flattenedRule, String classname) {
    var cw = new PreCompileMaterialRuleClassWriter(classname, flattenedRule);
    cw.start();

    if (cw.instructions.length == 0) {
      cw.emitNullReturn();
    } else {
      var compiled = new BitSet(cw.instructions.length);
      cw.compile(0, compiled);
    }

    var clazz = cw.finish(false);

    try {
      return MethodHandles.lookup()
          .findStatic(
              clazz,
              "method$0",
              MethodType.methodType(
                  BlockState.class,
                  BooleanSupplier[].class,
                  BlockStateRule[].class,
                  int.class,
                  int.class,
                  int.class,
                  MaterialRuleContext.class,
                  Object[].class));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  protected int commonVariableCount() {
    return 7;
  }

  private void compile(int index, BitSet compiled) {
    if (index >= instructions.length) return;
    if (compiled.get(index)) return;

    if (instructions[index] == FlattenedMaterialRule.STATE_RULE && index > 0) return;

    switch (instructions[index]) {
      case FlattenedMaterialRule.MATERIAL_RULE -> {
        emitBlockStateRule(index, instructions[index + 1], index + 2);
        compiled.set(index);
        compile(index + 2, compiled);
      }
      case FlattenedMaterialRule.CONDITION -> {
        emitIfElse(
            index,
            mv -> emitConditionCall(mv, instructions[index + 1]),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled);
        compile(instructions[index + 2], compiled);
      }
      case FlattenedMaterialRule.NOT_CONDITION -> {
        emitIfElse(
            index,
            mv -> invertOpcode(emitConditionCall(mv, instructions[index + 1])),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled);
        compile(instructions[index + 2], compiled);
      }
      case FlattenedMaterialRule.STATE_RULE -> {
        emitReturn(index, instructions[index + 1]);
        compiled.set(index);
      }
      case FlattenedMaterialRule.BIOME_CONDITION -> {
        emitIfElse(
            index,
            mv -> emitBiomeCondition(mv, instructions[index + 1]),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled);
        compile(instructions[index + 2], compiled);
      }
      case FlattenedMaterialRule.NOT_BIOME_CONDITION -> {
        emitIfElse(
            index,
            mv -> invertOpcode(emitBiomeCondition(mv, instructions[index + 1])),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled);
        compile(instructions[index + 2], compiled);
      }
      case FlattenedMaterialRule.STATIC_CONDITION -> {
        emitIfElse(
            index,
            mv -> {
              loadCtx(mv);
              mv.invokestatic(
                  this.flattenedClassname,
                  "condition$" + instructions[index + 1],
                  FlattenedMaterialRuleClassWriter.CONDITION_DESC,
                  false);
              return IFEQ;
            },
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled);
        compile(instructions[index + 2], compiled);
      }
      case FlattenedMaterialRule.NOT_STATIC_CONDITION -> {
        emitIfElse(
            index,
            mv -> {
              loadCtx(mv);
              mv.invokestatic(
                  this.flattenedClassname,
                  "condition$" + instructions[index + 1],
                  FlattenedMaterialRuleClassWriter.CONDITION_DESC,
                  false);
              return IFNE;
            },
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled);
        compile(instructions[index + 2], compiled);
      }
      case FlattenedMaterialRule.STATIC_RULE -> {
        emitStaticRule(index, instructions[index + 1], index + 2);
        compiled.set(index);
        compile(index + 2, compiled);
      }
      default -> throw new IllegalStateException("Invalid instruction");
    }
  }

  private static final String MATERIAL_RULE_CW_INTERNAL_NAME =
      Type.getInternalName(PreCompileMaterialRuleClassWriter.class);

  @Override
  protected InstructionAdapter newMethod(int index) {
    return this.newMethod("method$" + index, BLOCKSTATE_RULE_METHOD_TYPE, null);
  }

  private int emitBiomeCondition(InstructionAdapter mv, int conditionIdx) {
    loadBiomeConditions(mv);
    mv.iconst(conditionIdx);
    mv.aload(InstructionAdapter.OBJECT_TYPE);
    loadCtx(mv);
    mv.invokestatic(
        MATERIAL_RULE_CW_INTERNAL_NAME,
        "objToBoolean",
        "(" + Type.getDescriptor(Object.class) + "L" + MATERIAL_RULE_CONTEXT_INTERNAL_NAME + ";)Z",
        false);
    return IFEQ;
  }

  @Override
  protected Label visitFunctionParams(InstructionAdapter mv, Label l0) {
    Label ll = new Label();
    mv.visitLabel(ll);
    mv.visitLocalVariable(
        "conditions", "[L" + BOOLEAN_SUPPLIER_INTERNAL_NAME + ";", null, l0, ll, 0);
    mv.visitLocalVariable("rules", "[L" + BLOCK_STATE_RULE_INTERNAL_NAME + ";", null, l0, ll, 1);
    mv.visitLocalVariable("x", "I", null, l0, ll, 2);
    mv.visitLocalVariable("y", "I", null, l0, ll, 3);
    mv.visitLocalVariable("z", "I", null, l0, ll, 4);
    mv.visitLocalVariable(
        "context", "L" + MATERIAL_RULE_CONTEXT_INTERNAL_NAME + ";", null, l0, ll, 5);
    mv.visitLocalVariable("biomeConditions", Type.getDescriptor(Object[].class), null, l0, ll, 6);
    return ll;
  }

  protected void callBlockStateRuleMethodInClass(InstructionAdapter mv, int index) {
    callBlockStateRuleMethodInClass(mv, classname, "method$" + index, BLOCKSTATE_RULE_METHOD_TYPE);
  }

  public void callBlockStateRuleMethodInClass(
      InstructionAdapter mv, String className, String methodName, String descriptor) {
    loadConditions(mv);
    loadRules(mv);
    loadx(mv);
    loady(mv);
    loadz(mv);
    loadCtx(mv);
    loadBiomeConditions(mv);
    mv.invokestatic(className, methodName, descriptor, false);
  }

  public void loadBiomeConditions(InstructionAdapter mv) {
    this.loadObjVar(mv, 6);
  }
}
