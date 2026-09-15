package org.codeberg.zenxarch.matcomp.compiler;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.lang.invoke.MethodHandle;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import org.codeberg.zenxarch.matcomp.config.MaterialRuleCompilerConfig;

public class MaterialRuleCompilerCacheHeightContextSpecific {
  private final Reference2IntOpenHashMap<MaterialCondition> staticConditions =
      new Reference2IntOpenHashMap<>();

  private final StaticMaterialCondition[] compiledStaticConditions;
  public final FlattenedMaterialRule flattened;

  private final Object2ObjectLinkedOpenCustomHashMap<int[], MethodHandle> cache =
      new Object2ObjectLinkedOpenCustomHashMap<>(
          MaterialRuleCompilerConfig.MAX_CACHE_SIZE,
          Object2ObjectOpenCustomHashMap.FAST_LOAD_FACTOR,
          IntArrays.HASH_STRATEGY);

  public MaterialRuleCompilerCacheHeightContextSpecific(
      HeightContext context, NoiseConfig config, MaterialRule rule) {
    var handles = new ObjectArrayList<StaticMaterialCondition>();
    this.flattened = FlattenedMaterialRule.flatten(rule);
    for (var condition : flattened.conditions)
      tryAddStaticRule(context, config, condition, handles);
    this.compiledStaticConditions = handles.toArray(StaticMaterialCondition[]::new);
  }

  private void tryAddStaticRule(
      HeightContext context,
      NoiseConfig config,
      MaterialCondition condition,
      ObjectArrayList<StaticMaterialCondition> handles) {
    if (staticConditions.containsKey(condition)) return;
    var staticCond = StaticMaterialCondition.tryCreate(condition, context, config);
    if (staticCond != null) {
      var idx = handles.size();
      staticConditions.put(condition, idx);
      handles.add(staticCond);
    }
  }

  public int getIdx(MaterialCondition condition) {
    return staticConditions.getOrDefault(condition, -1);
  }

  public StaticMaterialCondition getHandle(int idx) {
    return compiledStaticConditions[idx];
  }

  private static AtomicInteger counter = new AtomicInteger(0);

  public synchronized MethodHandle get(int[] program, BitSet states) {
    var res = cache.getAndMoveToFirst(program);
    if (res != null) return res;
    int idx = counter.incrementAndGet();
    res = ZClassWriter.compile("ZClassWriter_Compiled_" + idx, program, states, this);
    cache.put(program, res);
    while (cache.size() > MaterialRuleCompilerConfig.MAX_CACHE_SIZE) cache.removeLast();
    return res;
  }
}
