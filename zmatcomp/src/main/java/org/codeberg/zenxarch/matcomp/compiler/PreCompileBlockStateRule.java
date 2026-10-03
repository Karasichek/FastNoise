package org.codeberg.zenxarch.matcomp.compiler;

import java.lang.invoke.MethodHandle;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.MaterialRuleCompilerConstants;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.jspecify.annotations.Nullable;

public class PreCompileBlockStateRule implements BlockStateRule {
  private final MaterialRuleContext ctx;
  private final BooleanSupplier[] conditions;
  private final BlockStateRule[] rules;
  private final MethodHandle handle;
  private final Object[] biomeObjs;

  public PreCompileBlockStateRule(
      FlattenedMaterialRule flattened,
      MaterialRuleContext ctx,
      MethodHandle handle,
      Object[] biomeObjs) {
    this.biomeObjs = biomeObjs;
    this.handle = handle;
    this.conditions = new BooleanSupplier[flattened.conditions.length];

    for (int i = 0; i < this.conditions.length; i++) {
      this.conditions[i] = flattened.conditions[i].getPredicate(ctx);
    }

    this.rules = new BlockStateRule[flattened.rules.length];

    for (int i = 0; i < this.rules.length; i++) {
      this.rules[i] = flattened.rules[i].getBlockStateRule(ctx);
    }

    this.ctx = ctx;
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    try {
      return (BlockState) this.handle.invokeExact(conditions, rules, x, y, z, ctx, this.biomeObjs);
    } catch (Throwable e) {
      MaterialRuleCompilerConstants.LOGGER.error("Compiled surface rule failed", e);
      throw new IllegalStateException(e);
    }
  }
}
