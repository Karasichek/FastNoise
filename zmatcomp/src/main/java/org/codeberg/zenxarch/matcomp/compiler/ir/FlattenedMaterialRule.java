package org.codeberg.zenxarch.matcomp.compiler.ir;

import static org.codeberg.zenxarch.matcomp.config.MaterialRuleCompilerConfig.MAX_DEPTH;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BiomeMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.NotMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.CompiledMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.StaticMaterialCondition;
import org.codeberg.zenxarch.matcomp.compiler.StaticMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.cw.FlattenedMaterialRuleClassWriter;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedConditions.LithostitchedBiomeCondition;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules;

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

  public static FlattenedMaterialRule flatten(
      MaterialRule rule, HeightContext ctx, NoiseConfig config, Registry<Biome> registry) {
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
        rule,
        0,
        registry);

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
      MaterialRule ruleValue,
      int depth,
      Registry<Biome> registry) {

    final var rule = dereference(ruleValue);

    if (depth >= MAX_DEPTH) {
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

          if (ifTrue instanceof BiomeMaterialCondition biomeCond) {
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
                biomeCond,
                cond.thenRun(),
                reversed,
                depth,
                registry);
          } else {
            program.add(MATERIAL_RULE);
            program.add(add(rules, new CompiledMaterialRule(rule)));
          }
        }
        default -> {
          program.add(MATERIAL_RULE);
          program.add(add(rules, new CompiledMaterialRule(rule)));
        }
      }
      return;
    }

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
            reversed,
            depth,
            registry);
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
              rulex,
              (depth + 1 == MAX_DEPTH) ? depth : depth + 1,
              registry);
          if (curIdx + 2 == program.size() && program.getInt(program.size() - 2) == STATE_RULE) {
            break; // break since if there is simple block state rule present it would always
            // discontinue from here
          }
        }
      }
      case LithostitchedRules.ReferenceRule ref -> {
        for (var rulex : ref.options()) {
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
              rulex.value(),
              (depth + 1 == MAX_DEPTH) ? depth : depth + 1,
              registry);
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
      boolean reversed,
      int depth,
      Registry<Biome> registry) {

    if (ifTrue instanceof BiomeMaterialCondition biomeCond) {
      program.add(BIOME_CONDITION);
      program.add(
          add(biomeConditions, RegistryEntryList.of(registry::getOrThrow, biomeCond.biomes)));
    } else if (ifTrue instanceof LithostitchedBiomeCondition biomeCond) {
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
        thenRun,
        depth + 1,
        registry);
    program.set(idx, program.size());
  }

  private static MaterialCondition dereference(MaterialCondition condition) {
    return condition;
  }

  private static MaterialRule dereference(MaterialRule rule) {
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
