package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.lang.invoke.MethodHandle;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;

public final class MaterialRuleCompilerCache {
  private MaterialRuleCompilerCache() {}

  public static MaterialRuleCompilerCache create() {
    return new MaterialRuleCompilerCache();
  }

  private final Object2ObjectLinkedOpenCustomHashMap<int[], MethodHandle> cache =
      new Object2ObjectLinkedOpenCustomHashMap<>(
          FastNoiseConfig.MAX_CACHE_SIZE,
          Object2ObjectOpenCustomHashMap.FAST_LOAD_FACTOR,
          IntArrays.HASH_STRATEGY);

  public synchronized MethodHandle get(int[] program) {
    var res = cache.getAndMoveToFirst(program);
    if (res != null) return res;
    res = MaterialRuleCompiler.compile(program);
    cache.put(program, res);
    while (cache.size() > FastNoiseConfig.MAX_CACHE_SIZE) cache.removeLast();
    return res;
  }
}
