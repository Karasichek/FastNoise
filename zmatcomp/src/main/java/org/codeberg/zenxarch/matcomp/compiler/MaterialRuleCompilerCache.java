package org.codeberg.zenxarch.matcomp.compiler;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;

public final class MaterialRuleCompilerCache {
  private MaterialRuleCompilerCache() {}

  public static MaterialRuleCompilerCache create() {
    return new MaterialRuleCompilerCache();
  }

  private static final Hash.Strategy<CacheKey> HEIGHT_CONTEXT_STRATEGY =
      new Hash.Strategy<CacheKey>() {
        public boolean equals(CacheKey a, CacheKey b) {
          if (a == b) return true;
          if (a == null || b == null) return false;

          var oa = a.context;
          var ob = b.context;
          return a.config == b.config
              && a.rule == b.rule
              && oa.getHeight() == ob.getHeight()
              && oa.getMinY() == ob.getMinY();
        }

        public int hashCode(CacheKey key) {
          var o = key.context;
          return HashCommon.mix(o.getHeight()) ^ HashCommon.mix(o.getMinY());
        }
      };

  private final Object2ObjectMap<CacheKey, MaterialRuleCompilerCacheHeightContextSpecific> cache =
      new Object2ObjectOpenCustomHashMap<>(HEIGHT_CONTEXT_STRATEGY);

  public synchronized MaterialRuleCompilerCacheHeightContextSpecific get(
      HeightContext context, NoiseConfig config, MaterialRule rule) {
    return this.cache.computeIfAbsent(
        new CacheKey(context, config, rule),
        (ctx) -> new MaterialRuleCompilerCacheHeightContextSpecific(context, config, rule));
  }

  private static record CacheKey(HeightContext context, NoiseConfig config, MaterialRule rule) {}
}
