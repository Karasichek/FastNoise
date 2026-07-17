package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.lang.invoke.MethodHandle;

public final class MaterialRuleCompilerCache {
  private MaterialRuleCompilerCache() {}

  private static final Object2ObjectOpenCustomHashMap<int[], MethodHandle> cache =
      new Object2ObjectOpenCustomHashMap<>(
          16, Object2ObjectOpenCustomHashMap.FAST_LOAD_FACTOR, IntArrays.HASH_STRATEGY);

  public static MethodHandle get(int[] program) {
    synchronized (cache) {
      return cache.computeIfAbsent(program, MaterialRuleCompiler::compile);
    }
  }
}
