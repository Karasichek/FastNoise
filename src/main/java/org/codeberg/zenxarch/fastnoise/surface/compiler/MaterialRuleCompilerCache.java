package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.lang.invoke.MethodHandle;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;

public final class MaterialRuleCompilerCache {
  private MaterialRuleCompilerCache() {}

  public static MaterialRuleCompilerCache create() {
    return new MaterialRuleCompilerCache();
  }

  private final Object2ObjectLinkedOpenHashMap<MaterialRuleProgram, MethodHandle> cache =
      new Object2ObjectLinkedOpenHashMap<>(
          FastNoiseConfig.MAX_CACHE_SIZE, Object2ObjectOpenCustomHashMap.FAST_LOAD_FACTOR);

  public synchronized MethodHandle get(MaterialRuleProgram program) {
    var res = cache.getAndMoveToFirst(program);
    if (res != null) return res;
    res = MaterialRuleCompiler.compile(program);
    cache.put(program, res);
    while (cache.size() > FastNoiseConfig.MAX_CACHE_SIZE) cache.removeLast();
    return res;
  }
}
