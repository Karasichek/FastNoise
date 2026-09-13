package org.codeberg.zenxarch.matcomp.compiler;

import static org.objectweb.asm.Opcodes.*;

import com.google.common.io.Files;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.BitSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.MaterialRuleCompilerConstants;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public final class ZClassWriter {
  private final String classname;
  private final int[] instructions;
  private final ClassWriter cw;

  private static final String BLOCK_STATE_INTERNAL_NAME = Type.getInternalName(BlockState.class);
  private static final String BOOLEAN_SUPPLIER_INTERNAL_NAME =
      Type.getInternalName(BooleanSupplier.class);
  private static final String BLOCK_STATE_RULE_INTERNAL_NAME =
      Type.getInternalName(BlockStateRule.class);
  private static final String MATERIAL_RULE_CONTEXT_INTERNAL_NAME =
      Type.getInternalName(MaterialRuleContext.class);

  private static final String BLOCKSTATE_RULE_METHOD_TYPE =
      Type.getMethodDescriptor(
          Type.getType(BlockState.class),
          Type.getType(BooleanSupplier[].class),
          Type.getType(BlockStateRule[].class),
          Type.getType(BlockState[].class),
          Type.getType(int.class),
          Type.getType(int.class),
          Type.getType(int.class),
          Type.getType(MaterialRuleContext.class));

  public ZClassWriter(String classname, int[] instructions) {
    this.classname = classname;
    this.instructions = instructions;
    this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
  }

  private static final boolean dumpClasses =
      Boolean.getBoolean("org.codeberg.zenxarch.zmatcomp.dumpClasses");
  private static final Path dumpDir =
      FabricLoader.getInstance().getGameDir().resolve("./cache/zmatcomp/classes");

  static {
    if (dumpClasses) {
      try {
        for (var file : dumpDir.toFile().listFiles()) {
          if (file.getName().endsWith(".class")) file.delete();
        }
      } catch (Exception e) {
        MaterialRuleCompilerConstants.LOGGER.error("Failed to clean class dump", e);
      }
    }
  }

  private static void dumpClass(String classname, byte[] bytes) {
    var file = dumpDir.resolve(classname + ".class").toFile();
    file.getParentFile().mkdirs();
    try {
      Files.write(bytes, file);
    } catch (Exception e) {
      MaterialRuleCompilerConstants.LOGGER.error("Failed to dump " + classname, e);
    }
  }

  public static MethodHandle compile(
      String classname,
      int[] instructions,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    var cw = new ZClassWriter(classname, instructions);
    cw.startClass();

    var compiled = new BitSet(instructions.length);
    cw.compile(0, compiled, contextSpecificCache);

    var bytes = cw.finish();

    if (dumpClasses) dumpClass(classname, bytes);

    var clazz = defineClass(classname, bytes);

    try {
      return MethodHandles.lookup()
          .findStatic(
              clazz,
              "method$0",
              MethodType.methodType(
                  BlockState.class,
                  BooleanSupplier[].class,
                  BlockStateRule[].class,
                  BlockState[].class,
                  int.class,
                  int.class,
                  int.class,
                  MaterialRuleContext.class));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void compile(
      int index,
      BitSet compiled,
      MaterialRuleCompilerCacheHeightContextSpecific contextSpecificCache) {
    if (index >= instructions.length) return;
    if (compiled.get(index)) return;

    if (instructions[index] == MaterialRuleInterpreter.RETURN && index > 0) return;

    switch (instructions[index]) {
      case MaterialRuleInterpreter.JUMP_IF_FALSE -> {
        emitJmpIfFalse(index, instructions[index + 1], index + 3, index + instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(index + instructions[index + 2], compiled, contextSpecificCache);
      }
      case MaterialRuleInterpreter.RETURN -> {
        emitReturn(index, instructions[index + 1]);
        compiled.set(index);
      }
      case MaterialRuleInterpreter.TRY_RETURN -> {
        emitTryRet(index, instructions[index + 1], index + 2);
        compiled.set(index);
        compile(index + 2, compiled, contextSpecificCache);
      }
      case MaterialRuleInterpreter.JUMP_IF_FALSE_STATIC -> {
        emitJmpIfFalseStatic(
            index,
            contextSpecificCache.getHandle(instructions[index + 1]),
            index + 3,
            index + instructions[index + 2]);
        compiled.set(index);
        compile(index + 3, compiled, contextSpecificCache);
        compile(index + instructions[index + 2], compiled, contextSpecificCache);
      }
      default -> throw new IllegalStateException("Invalid instruction");
    }
  }

  private static Class<?> defineClass(String className, byte[] bytes) {
    ClassLoader classLoader =
        new ClassLoader(ZClassWriter.class.getClassLoader()) {
          @Override
          public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(className)) {
              return super.defineClass(name, bytes, 0, bytes.length);
            }

            return super.loadClass(name);
          }
        };

    try {
      return classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void startClass() {
    cw.visit(V25, ACC_PUBLIC | ACC_STATIC | ACC_SUPER, classname, null, "java/lang/Object", null);

    var mv = new InstructionAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null));
    mv.visitCode();
    var l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.invokespecial("java/lang/Object", "<init>", "()V", false);
    mv.visitInsn(RETURN);

    var l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "L" + classname + ";", null, l0, l1, 0);

    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private InstructionAdapter newMethod(int index) {
    return new InstructionAdapter(
        cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC, "method$" + index, BLOCKSTATE_RULE_METHOD_TYPE, null, null));
  }

  private Label startMethodVisit(InstructionAdapter mv) {
    mv.visitCode();
    var l0 = new Label();
    mv.visitLabel(l0);
    return l0;
  }

  private void endMethodVisit(InstructionAdapter mv) {
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public void emitJmpIfFalse(int methodIdx, int conditionIdx, int ifTrue, int ifFalse) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    emitConditionCall(mv, conditionIdx);

    var l1 = new Label();
    mv.ifeq(l1);

    emitTailCall(mv, ifTrue);
    mv.visitLabel(l1);

    emitTailCall(mv, ifFalse);

    visitFunctionParams(mv, l0);
    endMethodVisit(mv);
  }

  public void emitReturn(int methodIdx, int stateIdx) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    loadStates(mv);
    mv.iconst(stateIdx);
    mv.visitInsn(AALOAD);
    mv.visitInsn(ARETURN);

    visitFunctionParams(mv, l0);
    endMethodVisit(mv);
  }

  public void emitTryRet(int methodIdx, int ruleIndex, int otherMethodIdx) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    callBlockStateRule(mv, ruleIndex);

    mv.visitVarInsn(ASTORE, 7);

    var l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 7);
    var l2 = new Label();
    mv.ifnull(l2);
    mv.visitVarInsn(ALOAD, 7);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l2);

    emitTailCall(mv, otherMethodIdx);

    var l3 = visitFunctionParams(mv, l0);
    mv.visitLocalVariable("res", "L" + BLOCK_STATE_INTERNAL_NAME + ";", null, l1, l3, 7);

    endMethodVisit(mv);
  }

  public void emitJmpIfFalseStatic(
      int methodIdx, StaticMaterialCondition staticMaterialCondition, int ifTrue, int ifFalse) {
    var mv = newMethod(methodIdx);
    var l0 = startMethodVisit(mv);

    staticMaterialCondition.emitBytecode(this, mv);

    var l1 = new Label();
    staticMaterialCondition.emitJumpInst(this, mv, l1);

    emitTailCall(mv, ifTrue);

    mv.visitLabel(l1);

    emitTailCall(mv, ifFalse);

    visitFunctionParams(mv, l0);
    endMethodVisit(mv);
  }

  private void emitTailCall(InstructionAdapter mv, int methodIdx) {
    if (methodIdx >= this.instructions.length) {
      mv.aconst(null);
    } else {
      if (instructions[methodIdx] == MaterialRuleInterpreter.RETURN) {
        loadStates(mv);
        mv.iconst(instructions[methodIdx + 1]);
        mv.visitInsn(AALOAD);
      } else {
        callBlockStateRuleMethodInClass(mv, methodIdx);
      }
    }

    mv.visitInsn(ARETURN);
  }

  private void emitConditionCall(InstructionAdapter mv, int conditionIdx) {
    loadConditions(mv);
    mv.iconst(conditionIdx);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEINTERFACE, BOOLEAN_SUPPLIER_INTERNAL_NAME, "get", "()Z", true);
  }

  private Label visitFunctionParams(InstructionAdapter mv, Label l0) {
    Label ll = new Label();
    mv.visitLabel(ll);
    mv.visitLocalVariable(
        "conditions", "[L" + BOOLEAN_SUPPLIER_INTERNAL_NAME + ";", null, l0, ll, 0);
    mv.visitLocalVariable("rules", "[" + BLOCK_STATE_RULE_INTERNAL_NAME + ";", null, l0, ll, 1);
    mv.visitLocalVariable("states", "[L" + BLOCK_STATE_INTERNAL_NAME + ";", null, l0, ll, 2);
    mv.visitLocalVariable("x", "I", null, l0, ll, 3);
    mv.visitLocalVariable("y", "I", null, l0, ll, 4);
    mv.visitLocalVariable("z", "I", null, l0, ll, 5);
    mv.visitLocalVariable(
        "context", "L" + MATERIAL_RULE_CONTEXT_INTERNAL_NAME + ";", null, l0, ll, 6);
    return ll;
  }

  private void callBlockStateRule(InstructionAdapter mv, int index) {
    loadRules(mv);
    mv.iconst(index);
    mv.visitInsn(AALOAD);
    loadx(mv);
    loady(mv);
    loadz(mv);
    mv.invokeinterface(
        BLOCK_STATE_RULE_INTERNAL_NAME, "tryApply", "(III)L" + BLOCK_STATE_INTERNAL_NAME + ";");
  }

  private void callBlockStateRuleMethodInClass(InstructionAdapter mv, int index) {
    callBlockStateRuleMethodInClass(mv, classname, "method$" + index, BLOCKSTATE_RULE_METHOD_TYPE);
  }

  public void callBlockStateRuleMethodInClass(
      InstructionAdapter mv, String className, String methodName, String descriptor) {
    loadConditions(mv);
    loadRules(mv);
    loadStates(mv);
    loadx(mv);
    loady(mv);
    loadz(mv);
    loadCtx(mv);
    mv.invokestatic(className, methodName, descriptor, false);
  }

  public void loadConditions(InstructionAdapter mv) {
    mv.visitVarInsn(ALOAD, 0);
  }

  public void loadRules(InstructionAdapter mv) {
    mv.visitVarInsn(ALOAD, 1);
  }

  public void loadStates(InstructionAdapter mv) {
    mv.visitVarInsn(ALOAD, 2);
  }

  public void loadx(InstructionAdapter mv) {
    mv.visitVarInsn(ILOAD, 3);
  }

  public void loady(InstructionAdapter mv) {
    mv.visitVarInsn(ILOAD, 4);
  }

  public void loadz(InstructionAdapter mv) {
    mv.visitVarInsn(ILOAD, 5);
  }

  public void loadCtx(InstructionAdapter mv) {
    mv.visitVarInsn(ALOAD, 6);
  }

  public byte[] finish() {
    cw.visitEnd();
    return cw.toByteArray();
  }
}
