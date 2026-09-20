package org.codeberg.zenxarch.matcomp.compiler.cw;

import static org.objectweb.asm.Opcodes.*;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
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

    return cw.finish();
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
}
