package org.codeberg.zenxarch.fastnoise.surface.rules;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.List;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionalBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SimpleBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.StoneDepthMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.TerracottaBandsMaterialRule;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;

public final class RuleExpansion {
  public static BlockStateRule expand(MaterialRule rule, MaterialRuleContext context) {
    return switch (rule) {
      case SequenceMaterialRule sequence -> expand(sequence, context);
      case ConditionMaterialRule condition ->
          new ConditionalBlockStateRule(
              expand(condition.ifTrue(), context), expand(condition.thenRun(), context));
      case BlockMaterialRule block -> new SimpleBlockStateRule(block.resultState());
      case TerracottaBandsMaterialRule terracotta -> terracotta.apply(context);
      default -> rule.apply(context);
    };
  }

  public static BlockStateRule expand(SequenceMaterialRule seq, MaterialRuleContext context) {
    var rules = seq.sequence();
    var size = rules.size();

    if (size == 1) return expand(rules.getFirst(), context);

    var builder = ObjectArrayList.<BlockStateRule>of();

    for (int i = 0; i < size; i++) {
      var rule = expand(rules.get(i), context);
      if (rule == BiomePredicate.ALWAYS_NULL) continue;
      if (rule instanceof SequenceBlockStateRule sequenceBlockStateRule) {
        var nsize = sequenceBlockStateRule.rules().size();
        for (int j = 0; j < nsize; j++) {
          var nrule = sequenceBlockStateRule.rules().get(j);
          if (nrule == BiomePredicate.ALWAYS_NULL) continue;
          builder.add(nrule);
        }
        continue;
      }
      builder.add(rule);
    }

    if (builder.isEmpty()) return BiomePredicate.ALWAYS_NULL;
    if (builder.size() == 1) return builder.getFirst();

    return new SequenceBlockStateRule(ObjectLists.unmodifiable(builder));
  }

  private static void addAll(
      List<MaterialRule> rules,
      ObjectArrayList<BlockStateRule> builder,
      MaterialRuleContext context) {
    var size = rules.size();

    for (int i = 0; i < size; i++) add(rules.get(i), builder, context);
  }

  private static void add(
      MaterialRule rule, ObjectArrayList<BlockStateRule> builder, MaterialRuleContext context) {
    switch (rule) {
      case SequenceMaterialRule seq -> addAll(seq.sequence(), builder, context);
      case ConditionMaterialRule cond -> {
        var condition = expand(cond.ifTrue(), context);
        if (condition == BiomePredicate.ALWAYS_FALSE) return;

        var nrule = expand(cond.thenRun(), context);
        if (nrule == BiomePredicate.ALWAYS_NULL) return;
        if (condition == BiomePredicate.ALWAYS_TRUE) add(nrule, builder, context);
        else add(new ConditionalBlockStateRule(condition, nrule), builder, context);
      }
      default -> {
        if (rule != BiomePredicate.ALWAYS_NULL) builder.add(expand(rule, context));
      }
    }
  }

  private static void add(
      BlockStateRule rule, ObjectArrayList<BlockStateRule> builder, MaterialRuleContext context) {
    switch (rule) {
      case SequenceBlockStateRule seq -> {
        var size = seq.rules().size();
        for (int i = 0; i < size; i++) add(seq.rules().get(i), builder, context);
      }
      default -> {
        if (rule != BiomePredicate.ALWAYS_NULL) builder.add(rule);
      }
    }
  }

  private static BooleanSupplier expand(MaterialCondition condition, MaterialRuleContext context) {
    return switch (condition) {
      case MaterialRules.BiomeMaterialRule biome ->
          BiomePredicate.getSupplier(biome.biomes(), context);
      case StoneDepthMaterialCondition stone -> StoneDepthPredicate.create(stone, context);
      default -> condition.apply(context);
    };
  }
}
