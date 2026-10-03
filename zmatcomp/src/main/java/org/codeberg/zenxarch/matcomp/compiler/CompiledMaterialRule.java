package org.codeberg.zenxarch.matcomp.compiler;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;

public final record CompiledMaterialRule(MaterialRule rule) implements MaterialRule {

  @Override
  public BlockStateRule getBlockStateRule(MaterialRuleContext context) {
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
