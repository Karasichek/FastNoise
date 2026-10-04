package org.codeberg.zenxarch.matcomp.compiler.ir;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.BiomeMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.BlockMaterialRule;
import net.minecraft.world.gen.surfacebuilder.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition.RegisteredCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule.RegisteredRule;
import net.minecraft.world.gen.surfacebuilder.NotMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.SequenceMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.StaticMaterialCondition;
import org.codeberg.zenxarch.matcomp.compiler.StaticMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.cw.FlattenedMaterialRuleClassWriter;

public class FlattenedMaterialRule {

  public final MaterialRule[] rules;
  public final MaterialCondition[] conditions;
  public final RegistryEntryList<Biome>[] biomeConditions;
  public final RegistryEntry<Biome>[] biomes;
  public final StaticMaterialCondition[] staticConditions;
  public final StaticMaterialRule[] staticRules;
  public final BlockState[] states;
  public final int[] program;
  public final Class<?> clazz;

  public static final int MATERIAL_RULE = 0;
  public static final int STATE_RULE = 1;
  public static final int STATIC_RULE = 2;
  public static final int CONDITION = 3;
  public static final int BIOME_CONDITION = 4;
  public static final int STATIC_CONDITION = 5;
  public static final int NOT_CONDITION = 6;
  public static final int NOT_BIOME_CONDITION = 7;
  public static final int NOT_STATIC_CONDITION = 8;

  public static int invertOpcode(int opcode) {
    return switch (opcode) {
      case CONDITION -> NOT_CONDITION;
      case BIOME_CONDITION -> NOT_BIOME_CONDITION;
      case STATIC_CONDITION -> NOT_STATIC_CONDITION;
      case NOT_CONDITION -> CONDITION;
      case NOT_BIOME_CONDITION -> BIOME_CONDITION;
      case NOT_STATIC_CONDITION -> STATIC_CONDITION;
      default -> throw new IllegalStateException("Not a jump opcode");
    };
  }

  private static final AtomicInteger classNum = new AtomicInteger(0);

  public FlattenedMaterialRule(
      MaterialRule[] rules,
      MaterialCondition[] conditions,
      RegistryEntryList<Biome>[] biomeConditions,
      RegistryEntry<Biome>[] biomes,
      StaticMaterialCondition[] staticConditions,
      StaticMaterialRule[] staticRules,
      BlockState[] states,
      int[] program) {
    this.rules = rules;
    this.conditions = conditions;
    this.biomeConditions = biomeConditions;
    this.biomes = biomes;
    this.staticConditions = staticConditions;
    this.staticRules = staticRules;
    this.states = states;
    this.program = program;

    this.clazz =
        FlattenedMaterialRuleClassWriter.compile(
            "FlattenedRule$" + classNum.incrementAndGet(), this);
  }

  @SuppressWarnings("unchecked")
  public static FlattenedMaterialRule flatten(
      MaterialRule rule, HeightContext ctx, NoiseConfig config) {
    var program = new IntArrayList(64);

    var rules = new ObjectArrayList<MaterialRule>();
    var conditions = new ObjectArrayList<MaterialCondition>();
    var states = new ReferenceArrayList<BlockState>();
    var biomeConditions = new ReferenceArrayList<RegistryEntryList<Biome>>();
    var biomes = new ReferenceArrayList<RegistryEntry<Biome>>();

    var staticConditions = new ObjectArrayList<StaticMaterialCondition>();
    var staticRules = new ObjectArrayList<StaticMaterialRule>();

    push(
        program,
        rules,
        conditions,
        states,
        biomeConditions,
        staticConditions,
        staticRules,
        ctx,
        config,
        rule);

    for (var cond : biomeConditions) for (var biome : cond) add(biomes, biome);

    return new FlattenedMaterialRule(
        rules.toArray(MaterialRule[]::new),
        conditions.toArray(MaterialCondition[]::new),
        biomeConditions.toArray(RegistryEntryList[]::new),
        biomes.toArray(RegistryEntry[]::new),
        staticConditions.toArray(StaticMaterialCondition[]::new),
        staticRules.toArray(StaticMaterialRule[]::new),
        states.toArray(BlockState[]::new),
        program.toIntArray());
  }

  private static void push(
      IntArrayList program,
      List<MaterialRule> rules,
      List<MaterialCondition> conditions,
      List<BlockState> states,
      List<RegistryEntryList<Biome>> biomeConditions,
      List<StaticMaterialCondition> staticConditions,
      List<StaticMaterialRule> staticRules,
      HeightContext ctx,
      NoiseConfig config,
      MaterialRule ruleValue) {

    final var rule = dereference(ruleValue);

    switch (rule) {
      case BlockMaterialRule block -> {
        program.add(STATE_RULE);
        program.add(add(states, block.resultState()));
      }
      case ConditionMaterialRule cond -> {
        var ifTrue = dereference(cond.ifTrue());
        var reversed = false;
        while (ifTrue instanceof NotMaterialCondition notCond) {
          ifTrue = dereference(notCond.target());
          reversed = !reversed;
        }
        pushCond(
            program,
            rules,
            conditions,
            states,
            biomeConditions,
            staticConditions,
            staticRules,
            ctx,
            config,
            ifTrue,
            cond.thenRun(),
            reversed);
      }
      case SequenceMaterialRule seq -> {
        for (var rulex : seq.sequence()) {
          var curIdx = program.size();
          push(
              program,
              rules,
              conditions,
              states,
              biomeConditions,
              staticConditions,
              staticRules,
              ctx,
              config,
              rulex);
          if (curIdx + 2 == program.size() && program.getInt(program.size() - 2) == STATE_RULE) {
            break; // break since if there is simple block state rule present it would always
            // discontinue from here
          }
        }
      }
      default -> {
        var staticRule = StaticMaterialRule.getRule(rule);
        if (staticRule != null) {
          program.add(STATIC_RULE);
          program.add(add(staticRules, staticRule));
        } else {
          program.add(MATERIAL_RULE);
          program.add(add(rules, rule));
        }
      }
    }
  }

  private static void pushCond(
      IntArrayList program,
      List<MaterialRule> rules,
      List<MaterialCondition> conditions,
      List<BlockState> states,
      List<RegistryEntryList<Biome>> biomeConditions,
      List<StaticMaterialCondition> staticConditions,
      List<StaticMaterialRule> staticRules,
      HeightContext ctx,
      NoiseConfig config,
      MaterialCondition ifTrue,
      MaterialRule thenRun,
      boolean reversed) {

    if (ifTrue instanceof BiomeMaterialCondition biomeCond) {
      program.add(BIOME_CONDITION);
      program.add(add(biomeConditions, biomeCond.biomes()));
    } else {
      var staticCond = StaticMaterialCondition.tryCreate(ifTrue, ctx, config);
      if (staticCond != null) {
        program.add(STATIC_CONDITION);
        program.add(add(staticConditions, staticCond));
      } else {
        program.add(CONDITION);
        program.add(add(conditions, ifTrue));
      }
    }

    if (reversed) {
      var opcodeIdx = program.size() - 2;
      program.set(opcodeIdx, invertOpcode(program.getInt(opcodeIdx)));
    }

    var idx = program.size();
    program.add(0);
    push(
        program,
        rules,
        conditions,
        states,
        biomeConditions,
        staticConditions,
        staticRules,
        ctx,
        config,
        thenRun);
    program.set(idx, program.size());
  }

  private static MaterialCondition dereference(MaterialCondition condition) {
    if (condition instanceof RegisteredCondition reg) return dereference(reg.holder().value());
    return condition;
  }

  private static MaterialRule dereference(MaterialRule rule) {
    if (rule instanceof RegisteredRule reg) return dereference(reg.holder().value());
    return rule;
  }

  private static <T> int add(List<T> array, T value) {
    var index = array.indexOf(value);
    if (index == -1) {
      index = array.size();
      array.add(value);
    }
    return index;
  }
}
