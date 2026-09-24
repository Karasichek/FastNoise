package org.codeberg.zenxarch.matcomp.compiler.cw;

import static org.objectweb.asm.Opcodes.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.BitSet;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.compiler.MaterialRuleCompilerCacheHeightContextSpecific;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public final class MaterialRuleClassWriter extends ExtendedClassWriter {
  private final int[] instructions;
  private final FlattenedMaterialRule flattenedRule;
  private final String flattenedClassname;

  private static final String BLOCK_STATE_INTERNAL_NAME = Type.getInternalName(BlockState.class);
  private static final String BOOLEAN_SUPPLIER_INTERNAL_NAME =
      Type.getInternalName(BooleanSupplier.class);
  private static final String BLOCK_STATE_RULE_INTERNAL_NAME =
      Type.getInternalName(BlockStateRule.class);
  private static final String MATERIAL_RULE_CONTEXT_INTERNAL_NAME =
      Type.getInternalName(MaterialRuleContext.class);
  private static final String BLOCK_INTERNAL_NAME = Type.getInternalName(Block.class);

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
    this.instructions = instructions;
    this.flattenedRule = flattenedRule;
    this.flattenedClassname = Type.getInternalName(flattenedRule.clazz);
    super(classname);
  }

  public static MethodHandle compile(
      FlattenedMaterialRule flattenedRule,
      String classname,
      int[] instructions,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    var cw = new MaterialRuleClassWriter(classname, instructions, flattenedRule);
    cw.start();

    var compiled = new BitSet(instructions.length);
    cw.compile(0, compiled, contextSpecificCache);

    var clazz = cw.finish();

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

  private static int invertOpcode(int og) {
    return switch (og) {
      case IFEQ -> IFNE;
      case IFNE -> IFEQ;
      case IFGE -> IFLT;
      case IFLT -> IFGE;
      case IFGT -> IFLE;
      case IFLE -> IFGT;
      case IF_ICMPGE -> IF_ICMPLT;
      case IF_ICMPLT -> IF_ICMPGE;
      case IF_ICMPGT -> IF_ICMPLE;
      case IF_ICMPLE -> IF_ICMPGT;
      case IF_ICMPEQ -> IF_ICMPNE;
      case IF_ACMPEQ -> IF_ACMPNE;
      case IF_ACMPNE -> IF_ACMPEQ;
      case IFNULL -> IFNONNULL;
      case IFNONNULL -> IFNULL;
      default -> throw new IllegalStateException("Unsupported jump insn");
    };
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

  private InstructionAdapter newMethod(int index) {
    return this.newMethod("method$" + index, BLOCKSTATE_RULE_METHOD_TYPE, null);
  }

  public void emitIfElse(
      int methodIdx,
      Function<InstructionAdapter, Integer> conditionEmitter,
      int ifTrue,
      int ifFalse) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    var inst = conditionEmitter.apply(mv);

    var l1 = new Label();
    mv.visitJumpInsn(inst, l1);

    emitTailCall(mv, ifTrue);
    mv.visitLabel(l1);

    emitTailCall(mv, ifFalse);

    visitFunctionParams(mv, l0);
    endMethodVisit(mv);
  }

  public void emitReturn(int methodIdx, int stateIdx) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    mv.getstatic(flattenedClassname, "block$" + stateIdx, "L" + BLOCK_STATE_INTERNAL_NAME + ";");
    mv.visitInsn(ARETURN);

    visitFunctionParams(mv, l0);
    endMethodVisit(mv);
  }

  public void emitBlockStateRule(int methodIdx, int ruleIndex, int otherMethodIdx) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    callBlockStateRule(mv, ruleIndex);

    mv.visitVarInsn(ASTORE, 6);

    var l1 = new Label();
    mv.visitLabel(l1);
    this.loadObjVar(mv, 6);
    var l2 = new Label();
    mv.ifnull(l2);
    this.loadObjVar(mv, 6);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l2);

    emitTailCall(mv, otherMethodIdx);

    var l3 = visitFunctionParams(mv, l0);
    mv.visitLocalVariable("res", "L" + BLOCK_STATE_INTERNAL_NAME + ";", null, l1, l3, 6);

    endMethodVisit(mv);
  }

  public void emitStaticRule(int methodIdx, int ruleIndex, int otherMethodIdx) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    flattenedRule.staticRules[ruleIndex].emitBytecode(this, mv);

    mv.visitVarInsn(ASTORE, 6);

    var l1 = new Label();
    mv.visitLabel(l1);
    this.loadObjVar(mv, 6);
    var l2 = new Label();
    mv.ifnull(l2);
    this.loadObjVar(mv, 6);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l2);

    emitTailCall(mv, otherMethodIdx);

    var l3 = visitFunctionParams(mv, l0);
    mv.visitLocalVariable("res", "L" + BLOCK_STATE_INTERNAL_NAME + ";", null, l1, l3, 6);

    endMethodVisit(mv);
  }

  private void emitTailCall(InstructionAdapter mv, int methodIdx) {
    if (methodIdx >= this.instructions.length) {
      mv.aconst(null);
    } else {
      if (instructions[methodIdx] == MaterialRuleCompilerCacheHeightContextSpecific.STATE_RULE) {
        mv.getstatic(
            flattenedClassname,
            "block$" + instructions[methodIdx + 1],
            "L" + BLOCK_STATE_INTERNAL_NAME + ";");
      } else {
        callBlockStateRuleMethodInClass(mv, methodIdx);
      }
    }

    mv.visitInsn(ARETURN);
  }

  public static boolean zclasswriter$callCondition(BooleanSupplier condition) {
    return condition.get();
  }

  private int emitConditionCall(InstructionAdapter mv, int conditionIdx) {
    loadConditions(mv);
    mv.iconst(conditionIdx);
    mv.visitInsn(AALOAD);
    mv.invokestatic(
        MATERIAL_RULE_CW_INTERNAL_NAME,
        "zclasswriter$callCondition",
        "(L" + BOOLEAN_SUPPLIER_INTERNAL_NAME + ";)Z",
        false);
    return IFEQ;
  }

  public static RegistryEntry<Biome> getCurrentBiome(MaterialRuleContext ctx) {
    return ctx.getCurrentBiome();
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

  private Label visitFunctionParams(InstructionAdapter mv, Label l0) {
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

  public static BlockState callBlockStateRule(BlockStateRule rule, int x, int y, int z) {
    return rule.tryApply(x, y, z);
  }

  private void callBlockStateRule(InstructionAdapter mv, int index) {
    loadRules(mv);
    mv.iconst(index);
    mv.visitInsn(AALOAD);
    loadx(mv);
    loady(mv);
    loadz(mv);
    mv.invokestatic(
        MATERIAL_RULE_CW_INTERNAL_NAME,
        "callBlockStateRule",
        "(L" + BLOCK_STATE_RULE_INTERNAL_NAME + ";III)L" + BLOCK_STATE_INTERNAL_NAME + ";",
        false);
  }

  private void callBlockStateRuleMethodInClass(InstructionAdapter mv, int index) {
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
    mv.invokestatic(className, methodName, descriptor, false);
  }

  public void loadConditions(InstructionAdapter mv) {
    this.loadObjVar(mv, 0);
  }

  public void loadRules(InstructionAdapter mv) {
    this.loadObjVar(mv, 1);
  }

  public void loadx(InstructionAdapter mv) {
    this.loadIntVar(mv, 2);
  }

  public void loady(InstructionAdapter mv) {
    this.loadIntVar(mv, 3);
  }

  public void loadz(InstructionAdapter mv) {
    this.loadIntVar(mv, 4);
  }

  public void loadCtx(InstructionAdapter mv) {
    this.loadObjVar(mv, 5);
  }
}
