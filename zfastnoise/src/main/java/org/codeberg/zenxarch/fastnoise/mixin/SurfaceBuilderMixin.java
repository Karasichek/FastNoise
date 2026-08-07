package org.codeberg.zenxarch.fastnoise.mixin;

import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleCompilerCache;
import org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleCompilerCacheHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin implements MaterialRuleCompilerCacheHolder {
  @Unique MaterialRuleCompilerCache zenxarch$cache = MaterialRuleCompilerCache.create();

  @Override
  public MaterialRuleCompilerCache zenxarch$getCache() {
    return zenxarch$cache;
  }
}
