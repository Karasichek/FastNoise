package org.codeberg.zenxarch.matcomp.compiler.cw;

import static org.objectweb.asm.Opcodes.*;

import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.compiler.MaterialRuleCompilerCacheHeightContextSpecific;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public abstract class BaseMaterialRuleClassWriter extends ExtendedClassWriter {
  protected final int[] instructions;
  protected final FlattenedMaterialRule flattenedRule;
  protected final String flattenedClassname;

  public BaseMaterialRuleClassWriter(
      String classname, int[] instructions, FlattenedMaterialRule flattenedRule) {
    super(classname);
    this.instructions = instructions;
    this.flattenedRule = flattenedRule;
    this.flattenedClassname = Type.getInternalName(flattenedRule.clazz);
  }

  protected static final String BLOCK_STATE_INTERNAL_NAME = Type.getInternalName(BlockState.class);
  protected static final String BOOLEAN_SUPPLIER_INTERNAL_NAME =
      Type.getInternalName(BooleanSupplier.class);
  protected static final String BLOCK_STATE_RULE_INTERNAL_NAME =
      Type.getInternalName(BlockStateRule.class);
  protected static final String MATERIAL_RULE_CONTEXT_INTERNAL_NAME =
      Type.getInternalName(MaterialRuleContext.class);

  protected static final String BASE_MATERIAL_RULE_CW_CLASSNAME =
      Type.getInternalName(BaseMaterialRuleClassWriter.class);

  static int invertOpcode(int og) {
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

  protected abstract InstructionAdapter newMethod(int index);

  protected abstract int commonVariableCount();

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

  public void emitNullReturn() {
    var mv = newMethod(0);
    var l0 = startMethodVisit(mv);

    mv.aconst(null);
    mv.visitInsn(ARETURN);

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

    mv.visitVarInsn(ASTORE, this.commonVariableCount() + 1);

    var l1 = new Label();
    mv.visitLabel(l1);
    this.loadObjVar(mv, this.commonVariableCount() + 1);
    var l2 = new Label();
    mv.ifnull(l2);
    this.loadObjVar(mv, this.commonVariableCount() + 1);
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

    mv.visitVarInsn(ASTORE, this.commonVariableCount() + 1);

    var l1 = new Label();
    mv.visitLabel(l1);
    this.loadObjVar(mv, this.commonVariableCount() + 1);
    var l2 = new Label();
    mv.ifnull(l2);
    this.loadObjVar(mv, this.commonVariableCount() + 1);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l2);

    emitTailCall(mv, otherMethodIdx);

    var l3 = visitFunctionParams(mv, l0);
    mv.visitLocalVariable(
        "res", "L" + BLOCK_STATE_INTERNAL_NAME + ";", null, l1, l3, commonVariableCount());

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

  public static boolean callCondition(BooleanSupplier condition) {
    return condition.get();
  }

  protected int emitConditionCall(InstructionAdapter mv, int conditionIdx) {
    loadConditions(mv);
    mv.iconst(conditionIdx);
    mv.visitInsn(AALOAD);
    mv.invokestatic(
        BASE_MATERIAL_RULE_CW_CLASSNAME,
        "callCondition",
        "(L" + BOOLEAN_SUPPLIER_INTERNAL_NAME + ";)Z",
        false);
    return IFEQ;
  }

  protected abstract Label visitFunctionParams(InstructionAdapter mv, Label l0);

  public static BlockState callBlockStateRule(BlockStateRule rule, int x, int y, int z) {
    return rule.tryApply(x, y, z);
  }

  protected void callBlockStateRule(InstructionAdapter mv, int index) {
    loadRules(mv);
    mv.iconst(index);
    mv.visitInsn(AALOAD);
    loadx(mv);
    loady(mv);
    loadz(mv);
    mv.invokestatic(
        BASE_MATERIAL_RULE_CW_CLASSNAME,
        "callBlockStateRule",
        "(L" + BLOCK_STATE_RULE_INTERNAL_NAME + ";III)L" + BLOCK_STATE_INTERNAL_NAME + ";",
        false);
  }

  protected abstract void callBlockStateRuleMethodInClass(InstructionAdapter mv, int index);

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
