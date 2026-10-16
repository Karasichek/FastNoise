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
import org.codeberg.zenxarch.matcomp.compiler.MaterialRuleCompilerCacheHeightContextSpecific;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public final class MaterialRuleClassWriter extends BaseMaterialRuleClassWriter {
  private static final String BLOCKSTATE_RULE_METHOD_TYPE =
      Type.getMethodDescriptor(
          Type.getType(BlockState.class),
          Type.getType(BooleanSupplier[].class),
          Type.getType(BlockStateRule[].class),
          Type.getType(int.class),
          Type.getType(int.class),
          Type.getType(int.class),
          Type.getType(MaterialRuleContext.class));

  public MaterialRuleClassWriter(
      String classname, int[] instructions, FlattenedMaterialRule flattenedRule) {
    super(classname, instructions, flattenedRule);
  }

  public static MethodHandle compile(
      FlattenedMaterialRule flattenedRule,
      String classname,
      int[] instructions,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    var cw = new MaterialRuleClassWriter(classname, instructions, flattenedRule);
    cw.start();

    if (instructions.length == 0) {
      cw.emitNullReturn();
    } else {
      var compiled = new BitSet(instructions.length);
      cw.compile(0, compiled, contextSpecificCache);
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
                  MaterialRuleContext.class));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  protected int commonVariableCount() {
    return 6;
  }

  private void compile(
      int index,
      BitSet compiled,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    if (index >= instructions.length) return;
    if (compiled.get(index)) return;

    if (instructions[index] == MaterialRuleCompilerCacheHeightContextSpecific.STATE_RULE
        && index > 0) return;

    switch (instructions[index]) {
      case MaterialRuleCompilerCacheHeightContextSpecific.MATERIAL_RULE -> {
        emitBlockStateRule(index, instructions[index + 1], index + 2);
        compiled.set(index);
        compile(index + 2, compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.CONDITION -> {
        emitIfElse(
            index,
            mv -> emitConditionCall(mv, instructions[index + 1]),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.NOT_CONDITION -> {
        emitIfElse(
            index,
            mv -> invertOpcode(emitConditionCall(mv, instructions[index + 1])),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.STATE_RULE -> {
        emitReturn(index, instructions[index + 1]);
        compiled.set(index);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.BIOME_CONDITION -> {
        emitIfElse(
            index,
            mv -> emitBiomeListContains(mv, instructions[index + 1]),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.NOT_BIOME_CONDITION -> {
        emitIfElse(
            index,
            mv -> invertOpcode(emitBiomeListContains(mv, instructions[index + 1])),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.SINGLE_BIOME_CONDITION -> {
        emitIfElse(
            index,
            mv -> emitSingleBiomeEquals(mv, instructions[index + 1]),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.NOT_SINGLE_BIOME_CONDITION -> {
        emitIfElse(
            index,
            mv -> invertOpcode(emitSingleBiomeEquals(mv, instructions[index + 1])),
            index + 3,
            instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.STATIC_CONDITION -> {
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
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.NOT_STATIC_CONDITION -> {
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
        compile(index + 3, compiled, contextSpecificCache);
        compile(instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleCompilerCacheHeightContextSpecific.STATIC_RULE -> {
        emitStaticRule(index, instructions[index + 1], index + 2);
        compiled.set(index);
        compile(index + 2, compiled, contextSpecificCache);
      }
      default -> throw new IllegalStateException("Invalid instruction");
    }
  }

  private static final String MATERIAL_RULE_CW_INTERNAL_NAME =
      Type.getInternalName(MaterialRuleClassWriter.class);

  @Override
  protected InstructionAdapter newMethod(int index) {
    return this.newMethod("method$" + index, BLOCKSTATE_RULE_METHOD_TYPE, null);
  }

  public static RegistryEntry<Biome> getCurrentBiome(MaterialRuleContext ctx) {
    return ((MaterialRuleContextExtension) ctx).zenxarch$getCurrentBiome();
  }

  public static boolean contains(RegistryEntryList<Biome> biomeList, RegistryEntry<Biome> biome) {
    return biomeList.contains(biome);
  }

  private int emitBiomeListContains(InstructionAdapter mv, int conditionIdx) {
    mv.getstatic(
        flattenedClassname,
        "biomeList$" + conditionIdx,
        FlattenedMaterialRuleClassWriter.REGISTRY_ENTRY_LIST_DESCRIPTOR);
    loadCtx(mv);
    mv.invokestatic(
        MATERIAL_RULE_CW_INTERNAL_NAME,
        "getCurrentBiome",
        "(L"
            + MATERIAL_RULE_CONTEXT_INTERNAL_NAME
            + ";)"
            + FlattenedMaterialRuleClassWriter.REGISTRY_ENTRY_DESCRIPTOR,
        false);
    mv.invokestatic(
        MATERIAL_RULE_CW_INTERNAL_NAME,
        "contains",
        "("
            + FlattenedMaterialRuleClassWriter.REGISTRY_ENTRY_LIST_DESCRIPTOR
            + FlattenedMaterialRuleClassWriter.REGISTRY_ENTRY_DESCRIPTOR
            + ")Z",
        false);
    return IFEQ;
  }

  private int emitSingleBiomeEquals(InstructionAdapter mv, int conditionIdx) {
    mv.getstatic(
        flattenedClassname,
        "biome$" + conditionIdx,
        FlattenedMaterialRuleClassWriter.REGISTRY_ENTRY_DESCRIPTOR);
    loadCtx(mv);
    mv.invokestatic(
        MATERIAL_RULE_CW_INTERNAL_NAME,
        "getCurrentBiome",
        "(L"
            + MATERIAL_RULE_CONTEXT_INTERNAL_NAME
            + ";)"
            + FlattenedMaterialRuleClassWriter.REGISTRY_ENTRY_DESCRIPTOR,
        false);
    return IF_ACMPNE;
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
    return ll;
  }

  @Override
  protected void callBlockStateRuleMethodInClass(InstructionAdapter mv, int index) {
    callBlockStateRuleMethodInClass(mv, classname, "method$" + index, BLOCKSTATE_RULE_METHOD_TYPE);
  }
}
