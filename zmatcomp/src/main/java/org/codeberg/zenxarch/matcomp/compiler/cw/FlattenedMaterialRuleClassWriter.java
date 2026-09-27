package org.codeberg.zenxarch.matcomp.compiler.cw;

import static org.objectweb.asm.Opcodes.*;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.compiler.StaticMaterialCondition;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public final class FlattenedMaterialRuleClassWriter extends ExtendedClassWriter {

  public static final String BLOCK_STATE_DESCRIPTOR = Type.getDescriptor(BlockState.class);
  public static final String REGISTRY_ENTRY_LIST_DESCRIPTOR =
      Type.getDescriptor(RegistryEntryList.class);
  public static final String REGISTRY_ENTRY_LIST_NAME =
      Type.getInternalName(RegistryEntryList.class);
  public static final String REGISTRY_ENTRY_DESCRIPTOR = Type.getDescriptor(RegistryEntry.class);
  public static final String REGISTRY_ENTRY_NAME = Type.getInternalName(RegistryEntry.class);
  public static final String BIOME_DESCRIPTOR = Type.getDescriptor(Biome.class);

  public static final String CONDITION_DESC =
      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(MaterialRuleContext.class));

  public static final String MATERIAL_RULE_CONTEXT_INTERNAL_NAME =
      Type.getInternalName(MaterialRuleContext.class);

  public static FlattenedMaterialRule currentRule;

  public static synchronized Class<?> compile(String classname, FlattenedMaterialRule rule) {
    currentRule = rule;

    var cw = new FlattenedMaterialRuleClassWriter(classname);
    cw.start();

    writeBlockFields(cw, rule.states.length, "block$", BLOCK_STATE_DESCRIPTOR, null);
    writeBlockFields(
        cw,
        rule.biomeConditions.length,
        "biomeList$",
        REGISTRY_ENTRY_LIST_DESCRIPTOR,
        "L" + REGISTRY_ENTRY_LIST_NAME + "<" + BIOME_DESCRIPTOR + ">;");
    writeBlockFields(
        cw,
        rule.biomes.length,
        "biome$",
        REGISTRY_ENTRY_DESCRIPTOR,
        "L" + REGISTRY_ENTRY_NAME + "<" + BIOME_DESCRIPTOR + ">;");

    var mv = cw.beginStaticInit();
    putfields(cw, mv, rule.states.length, "block$", BLOCK_STATE_DESCRIPTOR, "states");
    putfields(
        cw,
        mv,
        rule.biomeConditions.length,
        "biomeList$",
        REGISTRY_ENTRY_LIST_DESCRIPTOR,
        "biomeConditions");
    putfields(cw, mv, rule.biomes.length, "biome$", REGISTRY_ENTRY_DESCRIPTOR, "biomes");

    cw.finishStaticInit(mv);

    for (int i = 0; i < rule.staticConditions.length; i++) {
      cw.writeStaticCondition(rule.staticConditions[i], i);
    }

    return cw.finish(true);
  }

  public FlattenedMaterialRuleClassWriter(String classname) {
    super(classname);
  }

  private static void writeBlockFields(
      FlattenedMaterialRuleClassWriter cw,
      int numFields,
      String prefix,
      String descriptor,
      String signature) {
    for (int i = 0; i < numFields; i++) {
      cw.addStaticFinalField(prefix + i, descriptor, signature);
    }
  }

  private static final String FLATTENED_RULE_COMPILER_INTERNAL_NAME =
      Type.getInternalName(FlattenedMaterialRuleClassWriter.class);
  private static final String FLATTENED_RULE_DESC = Type.getDescriptor(FlattenedMaterialRule.class);
  private static final String FLATTENED_RULE_NAME =
      Type.getInternalName(FlattenedMaterialRule.class);

  private static void putfields(
      FlattenedMaterialRuleClassWriter cw,
      InstructionAdapter mv,
      int numFields,
      String prefix,
      String descriptor,
      String fieldName) {
    for (int i = 0; i < numFields; i++) {
      mv.getstatic(FLATTENED_RULE_COMPILER_INTERNAL_NAME, "currentRule", FLATTENED_RULE_DESC);
      mv.getfield(FLATTENED_RULE_NAME, fieldName, "[" + descriptor);
      mv.iconst(i);
      mv.visitInsn(AALOAD);
      cw.initStaticField(mv, prefix + i, descriptor);
    }
  }

  private void writeStaticCondition(StaticMaterialCondition condition, int index) {
    var mv = this.newMethod("condition$" + index, CONDITION_DESC, null);
    var l0 = startMethodVisit(mv);

    condition.emitBytecode(this, mv);

    visitFunctionParams(mv, l0);
    endMethodVisit(mv);
  }

  private Label visitFunctionParams(InstructionAdapter mv, Label l0) {
    Label ll = new Label();
    mv.visitLabel(ll);
    mv.visitLocalVariable(
        "context", "L" + MATERIAL_RULE_CONTEXT_INTERNAL_NAME + ";", null, l0, ll, 0);
    return ll;
  }

  public void loadCtx(InstructionAdapter mv) {
    this.loadObjVar(mv, 0);
  }
}
