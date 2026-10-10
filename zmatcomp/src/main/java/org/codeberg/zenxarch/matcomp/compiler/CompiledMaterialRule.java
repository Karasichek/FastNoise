package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public final record CompiledMaterialRule(MaterialRule rule) implements MaterialRule {

  @Override
  public BlockStateRule apply(MaterialRuleContext context) {
    return new CompiledBlockStateRule(context, context.heightContext, context.noiseConfig, rule);
  }

  @Override
  public CodecHolder<? extends MaterialRule> codec() {
    throw new IllegalStateException("Tried to save to disk, intermediary rule");
  }
}
