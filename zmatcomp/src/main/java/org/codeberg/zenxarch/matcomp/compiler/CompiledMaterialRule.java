package org.codeberg.zenxarch.matcomp.compiler;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;

public final record CompiledMaterialRule(MaterialRule rule) implements MaterialRule {

  @Override
  public BlockStateRule getBlockStateRule(MaterialRuleContext context) {
    return new CompiledBlockStateRule(context, context.heightContext, context.noiseConfig, rule);
  }

  @Override
  public MapCodec<? extends MaterialRule> codec() {
    throw new IllegalStateException("Tried to save to disk, intermediary rule");
  }
}
