package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.TerracottaBandsMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.cw.MaterialRuleClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public interface StaticMaterialRule {
  public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv);

  public static BlockState callTerracottaRule(MaterialRuleContext context, int x, int y, int z) {
    return context.surfaceBuilder.getTerracottaBlock(x, y, z);
  }

  public static final String STATIC_MATERIAL_RULE_INTERNAL_NAME =
      Type.getInternalName(StaticMaterialRule.class);
  public static final String MATERIALRULECTX = Type.getDescriptor(MaterialRuleContext.class);
  public static final String BLOCKSTATE = Type.getDescriptor(BlockState.class);

  public static StaticMaterialRule getRule(MaterialRule rule) {
    return switch (rule) {
      case TerracottaBandsMaterialRule _ -> TerracottaBandsRule.INSTANCE;
      default -> null;
    };
  }

  public static record TerracottaBandsRule() implements StaticMaterialRule {
    public static final TerracottaBandsRule INSTANCE = new TerracottaBandsRule();

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      cw.loadx(mv);
      cw.loady(mv);
      cw.loadz(mv);
      mv.invokestatic(
          STATIC_MATERIAL_RULE_INTERNAL_NAME,
          "callTerracottaRule",
          "(" + MATERIALRULECTX + "III)" + BLOCKSTATE,
          true);
    }
  }
}
