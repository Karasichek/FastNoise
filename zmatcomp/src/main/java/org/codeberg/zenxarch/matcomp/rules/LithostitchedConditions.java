package org.codeberg.zenxarch.matcomp.rules;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.util.dynamic.Range;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;

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
      var next = conditions.get(i).getPredicate(ctx);
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
      var next = conditions.get(i).getPredicate(ctx);
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

  public static record SlopeConditionImpl(MaterialRuleContext ctx, int min, int max)
      implements BooleanSupplier {
    @Override
    public boolean get() {
      var x = Math.abs(ctx.steepnessX);
      if (x >= min && x <= max) return true;
      var z = Math.abs(ctx.steepnessZ);
      return (z >= min && z <= max);
    }
  }
}
