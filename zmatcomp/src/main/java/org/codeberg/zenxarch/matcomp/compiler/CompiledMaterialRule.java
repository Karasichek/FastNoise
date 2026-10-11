package org.codeberg.zenxarch.matcomp.compiler;

import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;

public final record CompiledMaterialRule(MaterialRule rule) implements MaterialRule {

  @Override
  public BlockStateRule apply(MaterialRuleContext context) {
    var cache =
        ((MaterialRuleCompilerCacheHolder) context.surfaceBuilder)
            .zenxarch$getCache()
            .get(
                context.heightContext,
                context.noiseConfig,
                rule,
                ((MaterialRuleContextExtension) context).zenxarch$getRegistry());

    return cache.get(context);
  }

  @Override
  public CodecHolder<? extends MaterialRule> codec() {
    throw new IllegalStateException("Tried to save to disk, intermediary rule");
  }
}
