package org.codeberg.zenxarch.matcomp.compiler;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;

public final class MaterialRuleCompilerCache {
  private MaterialRuleCompilerCache() {}

  public static MaterialRuleCompilerCache create() {
    return new MaterialRuleCompilerCache();
  }

  private static final Hash.Strategy<HeightContext> HEIGHT_CONTEXT_STRATEGY =
      new Hash.Strategy<HeightContext>() {
        public boolean equals(HeightContext a, HeightContext b) {
          if (a == null || b == null) return a == b;
          if (a == b) return true;
          return a.getHeight() == b.getHeight()
              && a.getMinY() == b.getMinY()
              && a.getSeaLevel() == b.getSeaLevel();
        }

        public int hashCode(HeightContext o) {
          return HashCommon.mix(o.getHeight())
              ^ HashCommon.mix(o.getMinY())
              ^ HashCommon.mix(o.getSeaLevel());
        }
      };

  private final Object2ObjectOpenCustomHashMap<
          HeightContext, MaterialRuleCompilerCacheHeightContextSpecific>
      cache = new Object2ObjectOpenCustomHashMap<>(HEIGHT_CONTEXT_STRATEGY);

  public synchronized MaterialRuleCompilerCacheHeightContextSpecific get(
      HeightContext context, NoiseConfig config, MaterialRule rule) {
    return this.cache.computeIfAbsent(
        context,
        (ctx) -> new MaterialRuleCompilerCacheHeightContextSpecific(context, config, rule));
  }
}
