package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.lang.invoke.MethodHandle;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceMaterialRule;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;

public class MaterialRuleCompilerCacheHeightContextSpecific {
  private final Reference2IntOpenHashMap<MaterialCondition> staticConditions =
      new Reference2IntOpenHashMap<>();

  private final MethodHandle[] compiledStaticConditions;

  private final Object2ObjectLinkedOpenCustomHashMap<int[], MethodHandle> cache =
      new Object2ObjectLinkedOpenCustomHashMap<>(
          FastNoiseConfig.MAX_CACHE_SIZE,
          Object2ObjectOpenCustomHashMap.FAST_LOAD_FACTOR,
          IntArrays.HASH_STRATEGY);

  public MaterialRuleCompilerCacheHeightContextSpecific(
      HeightContext context, NoiseConfig config, MaterialRule rule) {
    var handles = new ObjectArrayList<MethodHandle>();
    createCache(context, config, rule, handles);
    this.compiledStaticConditions = handles.toArray(MethodHandle[]::new);
  }

  private void createCache(
      HeightContext context,
      NoiseConfig config,
      MaterialRule rule,
      ObjectArrayList<MethodHandle> handles) {
    switch (rule) {
      case SequenceMaterialRule seq -> {
        var sequence = seq.sequence();
        var size = sequence.size();
        for (int i = 0; i < size; i++) {
          createCache(context, config, sequence.get(i), handles);
        }
      }
      case ConditionMaterialRule cond -> {
        tryAddStaticRule(context, config, cond.ifTrue(), handles);
        createCache(context, config, cond.thenRun(), handles);
      }
      default -> {}
    }
  }

  private void tryAddStaticRule(
      HeightContext context,
      NoiseConfig config,
      MaterialCondition condition,
      ObjectArrayList<MethodHandle> handles) {
    if (staticConditions.containsKey(condition)) return;
    var staticCond = StaticMaterialCondition.tryCreate(condition, context, config);
    if (staticCond != null) {
      var idx = handles.size();
      staticConditions.put(condition, idx);
      handles.add(staticCond.compiled());
    }
  }

  public int getIdx(MaterialCondition condition) {
    return staticConditions.getOrDefault(condition, -1);
  }

  public MethodHandle getHandle(int idx) {
    return compiledStaticConditions[idx];
  }

  public synchronized MethodHandle get(int[] program) {
    var res = cache.getAndMoveToFirst(program);
    if (res != null) return res;
    res = MaterialRuleCompiler.compile(program, this);
    cache.put(program, res);
    while (cache.size() > FastNoiseConfig.MAX_CACHE_SIZE) cache.removeLast();
    return res;
  }
}
