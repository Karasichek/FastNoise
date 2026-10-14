package org.codeberg.zenxarch.matcomp.mixin.perf.compiler;

import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.matcomp.compiler.MaterialRuleCompilerCache;
import org.codeberg.zenxarch.matcomp.compiler.MaterialRuleCompilerCacheHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin implements MaterialRuleCompilerCacheHolder {
  @Unique
  private final MaterialRuleCompilerCache zenxarch$cache = MaterialRuleCompilerCache.create();

  @Override
  public MaterialRuleCompilerCache zenxarch$getCache() {
    return zenxarch$cache;
  }
}
