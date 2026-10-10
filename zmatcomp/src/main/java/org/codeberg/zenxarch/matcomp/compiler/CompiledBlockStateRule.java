package org.codeberg.zenxarch.matcomp.compiler;

import java.lang.invoke.MethodHandle;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.MaterialRuleCompilerConstants;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.jspecify.annotations.Nullable;

public final class CompiledBlockStateRule implements BlockStateRule {

  private final MaterialRuleContext ctx;
  private final BooleanSupplier[] conditions;
  private final BlockStateRule[] rules;
  private final MethodHandle handle;

  public CompiledBlockStateRule(
      MaterialRuleContext ctx, HeightContext heightContext, NoiseConfig config, MaterialRule rule) {
    var cache =
        ((MaterialRuleCompilerCacheHolder) ctx.surfaceBuilder)
            .zenxarch$getCache()
            .get(
                heightContext,
                config,
                rule,
                ((MaterialRuleContextExtension) ctx).zenxarch$getRegistry());

    var flattenedRule = cache.get(ctx);

    this.handle = flattenedRule.handle;
    this.conditions = new BooleanSupplier[flattenedRule.condIdx.length];

    for (int i = 0; i < this.conditions.length; i++) {
      this.conditions[i] = cache.flattened.conditions[flattenedRule.condIdx[i]].apply(ctx);
    }

    this.rules = new BlockStateRule[flattenedRule.ruleIdx.length];

    for (int i = 0; i < this.rules.length; i++) {
      this.rules[i] = cache.flattened.rules[flattenedRule.ruleIdx[i]].apply(ctx);
    }

    this.ctx = ctx;
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    try {
      return (BlockState) this.handle.invokeExact(conditions, rules, x, y, z, ctx);
    } catch (Throwable e) {
      MaterialRuleCompilerConstants.LOGGER.error("Compiled surface rule failed", e);
      throw new IllegalStateException(e);
    }
  }
}
