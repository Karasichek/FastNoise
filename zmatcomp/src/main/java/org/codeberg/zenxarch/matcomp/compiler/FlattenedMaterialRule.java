package org.codeberg.zenxarch.matcomp.compiler;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import java.util.function.IntFunction;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceMaterialRule;

public class FlattenedMaterialRule {

  public final MaterialRule[] rules;
  public final MaterialCondition[] conditions;
  public final BlockState[] states;
  public final int[] program;

  public static final int MATERIAL_RULE = 0;
  public static final int CONDITION_RULE = 1;
  public static final int STATE_RULE = 2;

  public FlattenedMaterialRule(
      MaterialRule[] rules, MaterialCondition[] conditions, BlockState[] states, int[] program) {
    this.rules = rules;
    this.conditions = conditions;
    this.states = states;
    this.program = program;
  }

  public static FlattenedMaterialRule flatten(MaterialRule rule) {
    var program = new IntArrayList(64);

    var rules = new IndexedList<MaterialRule>();
    var conditions = new IndexedList<MaterialCondition>();
    var states = new IndexedList<BlockState>();

    push(program, rules, conditions, states, rule);

    return new FlattenedMaterialRule(
        rules.build(MaterialRule[]::new),
        conditions.build(MaterialCondition[]::new),
        states.build(BlockState[]::new),
        program.toIntArray());
  }

  private static void push(
      IntArrayList program,
      IndexedList<MaterialRule> rules,
      IndexedList<MaterialCondition> conditions,
      IndexedList<BlockState> states,
      MaterialRule rule) {
    switch (rule) {
      case BlockMaterialRule block -> {
        program.add(STATE_RULE);
        program.add(states.add(block.resultState()));
      }
      case ConditionMaterialRule cond -> {
        program.add(CONDITION_RULE);
        program.add(addCondition(conditions, cond.ifTrue()));

        var idx = program.size();
        program.add(0);

        push(program, rules, conditions, states, cond.thenRun());
        program.set(idx, program.size());
      }
      case SequenceMaterialRule seq -> {
        for (var rulex : seq.sequence()) {
          var curIdx = program.size();
          push(program, rules, conditions, states, rulex);
          if (curIdx + 2 == program.size() && program.getInt(program.size() - 2) == STATE_RULE) {
            break; // break since if there is simple block state rule present it would always
            // discontinue from here
          }
        }
      }
      default -> {
        program.add(MATERIAL_RULE);
        program.add(rules.add(rule));
      }
    }
  }

  private static int addCondition(
      IndexedList<MaterialCondition> conditions, MaterialCondition condition) {
    return conditions.add(condition);
  }

  private static final class IndexedList<T> {
    private ObjectArrayList<T> states = new ObjectArrayList<>();
    private Reference2IntArrayMap<T> index = new Reference2IntArrayMap<>();

    public int add(T state) {
      return index.computeIfAbsent(state, k -> addImpl(state));
    }

    private int addImpl(T state) {
      var res = states.size();
      states.add(state);
      return res;
    }

    public T[] build(IntFunction<T[]> generator) {
      return states.toArray(generator);
    }
  }
}
