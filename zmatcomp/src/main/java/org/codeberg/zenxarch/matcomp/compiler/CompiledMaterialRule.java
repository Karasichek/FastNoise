package org.codeberg.zenxarch.matcomp.compiler;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public final record CompiledMaterialRule(MaterialRule rule) implements MaterialRule {

  @Override
  public BlockStateRule apply(MaterialRuleContext context) {
    var cache =
        ((MaterialRuleCompilerCacheHolder) context.surfaceBuilder)
            .zenxarch$getCache()
            .get(context.heightContext, context.noiseConfig, rule);

    return cache.get(context);
  }

  @Override
  public MapCodec<? extends MaterialRule> codec() {
    throw new IllegalStateException("Tried to save to disk, intermediary rule");
  }
}
