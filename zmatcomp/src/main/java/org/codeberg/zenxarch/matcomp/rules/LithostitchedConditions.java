package org.codeberg.zenxarch.matcomp.rules;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.util.dynamic.Range;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public final class LithostitchedConditions {
  public static interface SlopeCondition extends MaterialCondition {
    public Range<Integer> threshold();
  }

  public static interface AnyOfCondition extends MaterialCondition {
    public List<MaterialCondition> conditions();
  }

  public static interface AllOfCondition extends MaterialCondition {
    public List<MaterialCondition> conditions();
  }

  public static BooleanSupplier compileAllofCondition(
      List<MaterialCondition> conditions, MaterialRuleContext ctx) {
    var size = conditions.size();

    var resConditions = new ObjectArrayList<BooleanSupplier>(size);
    for (int i = 0; i < size; i++) {
      var next = conditions.get(i).apply(ctx);
      if (next == BiomePredicate.ALWAYS_FALSE) return BiomePredicate.ALWAYS_FALSE;
      if (next == BiomePredicate.ALWAYS_TRUE) continue;
      resConditions.add(next);
    }

    var resSize = resConditions.size();
    if (resSize == 1) return resConditions.getFirst();
    if (resSize == 0) return BiomePredicate.ALWAYS_TRUE;
    return new AllOfConditionImpl(resConditions.toArray(BooleanSupplier[]::new));
  }

  public static BooleanSupplier compileAnyofCondition(
      List<MaterialCondition> conditions, MaterialRuleContext ctx) {
    var size = conditions.size();

    var resConditions = new ObjectArrayList<BooleanSupplier>(size);
    for (int i = 0; i < size; i++) {
      var next = conditions.get(i).apply(ctx);
      if (next == BiomePredicate.ALWAYS_FALSE) continue;
      if (next == BiomePredicate.ALWAYS_TRUE) return BiomePredicate.ALWAYS_TRUE;
      resConditions.add(next);
    }

    var resSize = resConditions.size();
    if (resSize == 1) return resConditions.getFirst();
    if (resSize == 0) return BiomePredicate.ALWAYS_FALSE;
    return new AnyOfConditionImpl(resConditions.toArray(BooleanSupplier[]::new));
  }

  public static record AllOfConditionImpl(BooleanSupplier[] conditions) implements BooleanSupplier {
    @Override
    public boolean get() {
      for (int i = 0; i < conditions.length; i++) {
        if (!conditions[i].get()) return false;
      }
      return true;
    }
  }

  public static record AnyOfConditionImpl(BooleanSupplier[] conditions) implements BooleanSupplier {
    @Override
    public boolean get() {
      for (int i = 0; i < conditions.length; i++) {
        if (conditions[i].get()) return true;
      }
      return false;
    }
  }

  public static BooleanSupplier compileSlopeCondition(
      Range<Integer> range, MaterialRuleContext ctx) {
    return new SlopeConditionImpl(ctx, range.minInclusive(), range.maxInclusive());
  }

  public static record SlopeConditionImpl(MaterialRuleContext ctx, int min, int max, XZCacheKey key)
      implements BooleanSupplier {

    public SlopeConditionImpl(MaterialRuleContext ctx, int min, int max) {
      this(ctx, min, max, new XZCacheKey(ctx.uniqueHorizontalPosValue - 1L));
    }

    boolean compute() {
      var heightmap = ctx.chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
      int x = ctx.blockX & 0xF;
      int z = ctx.blockZ & 0xF;

      int north = heightmap.get(x, Math.max(z - 1, 0));
      int south = heightmap.get(x, Math.min(z + 1, 15));
      int west = heightmap.get(Math.max(x - 1, 0), z);
      int east = heightmap.get(Math.min(x + 1, 15), z);

      var value =
          Math.max(Math.max(north, south), Math.max(east, west))
              - Math.min(Math.min(north, south), Math.min(east, west));

      return value >= min && value <= max;
    }

    @Override
    public boolean get() {
      if (key.key != ctx.uniqueHorizontalPosValue) {
        key.value = compute();
        key.key = ctx.uniqueHorizontalPosValue;
      }

      return key.value;
    }
  }

  private static class XZCacheKey {
    boolean value;
    long key;

    public XZCacheKey(long key) {
      this.key = key;
    }
  }
}
