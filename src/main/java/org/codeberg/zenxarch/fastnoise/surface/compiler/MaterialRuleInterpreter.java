package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.lang.invoke.MethodHandle;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionalBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SimpleBlockStateRule;
import org.jspecify.annotations.Nullable;

public final class MaterialRuleInterpreter implements BlockStateRule {

  private final BooleanSupplier[] conditions;
  private final BlockStateRule[] rules;
  private final BlockState[] states;
  private final int[] instructions;
  private final MethodHandle compiled;

  public static final int JUMP_IF_FALSE = 0; // if (!conditions[a]) jmp(b);
  public static final int RETURN = 1; // return states[a];
  public static final int TRY_RETURN = 2; // if (rules[a] != null) return rules[a];
  public static final int RETURN_IF = 3; // if (conditions[a]) return states[b];

  // private static final int NO_OP = 4;

  public MaterialRuleInterpreter(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int[] instructions) {
    this.conditions = conditions;
    this.rules = rules;
    this.states = states;
    this.instructions = instructions;
    this.compiled = MaterialRuleCompilerCache.get(instructions);
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    try {
      return (BlockState) compiled.invoke(conditions, rules, states, x, y, z);
    } catch (Throwable e) {
    }
    var instPointer = 0;
    var max = instructions.length;
    while (instPointer < max) {
      var inst = this.instructions[instPointer];
      switch (inst) {
        case JUMP_IF_FALSE -> {
          instPointer =
              (conditions[instructions[instPointer + 1]].get())
                  ? instPointer + 3
                  : instructions[instPointer + 2];
        }
        case RETURN -> {
          return states[instructions[instPointer + 1]];
        }
        case TRY_RETURN -> {
          var res = this.rules[instructions[instPointer + 1]].tryApply(x, y, z);
          if (res != null) return res;
          instPointer += 2;
        }
        case RETURN_IF -> {
          if (conditions[instructions[instPointer + 1]].get())
            return states[instructions[instPointer + 2]];
          instPointer += 3;
        }
        default -> {
          // no op
          instPointer++;
        }
      }
    }
    return null;
  }

  public static MaterialRuleInterpreter of(SequenceBlockStateRule root) {
    var builder = new Builder();
    builder.push(root);

    return builder.build();
  }

  @Override
  public String toString() {
    var res = new StringBuilder();
    res.append("MaterialRuleInterpreter[");
    toString0(res, instructions, 0);
    res.append("]");
    return res.toString();
  }

  public static void toString0(StringBuilder builder, int[] instructions, int index) {
    if (index >= instructions.length) {
      builder.append("return null; /* end of program */");
      return;
    }
    switch (instructions[index]) {
      case JUMP_IF_FALSE -> {
        builder.append("if (conditions[").append(instructions[index + 1]).append("].get()) {");
        toString0(builder, instructions, index + 3);
        builder.append("} else {");
        toString0(builder, instructions, instructions[index + 2]);
        builder.append("}");
      }
      case RETURN -> {
        builder.append("return states[").append(instructions[index + 1]).append("];");
      }
      case TRY_RETURN -> {
        builder
            .append("state = rules[")
            .append(instructions[index + 1])
            .append("].tryApply(x,y,z); if (state != null) {return state;} else {");
        toString0(builder, instructions, index + 2);
        builder.append("}");
      }
      case RETURN_IF -> {
        builder
            .append("if (conditions[")
            .append(instructions[index + 1])
            .append("].get()) { return states[")
            .append(instructions[index + 2])
            .append("]; } else {");
        toString0(builder, instructions, index + 3);
        builder.append("}");
      }
      default -> {
        builder.append("return null; /* invalid */");
      }
    }
  }

  private static class Builder {
    private final ObjectArrayList<BooleanSupplier> conditions;
    private final ReferenceArrayList<BlockStateRule> rules;
    private final ReferenceArrayList<BlockState> states;
    private final IntArrayList instructions;

    public Builder() {
      this.conditions = new ObjectArrayList<>();
      this.rules = new ReferenceArrayList<>();
      this.states = new ReferenceArrayList<>();
      this.instructions = new IntArrayList();
    }

    private int indexOf(BlockState state) {
      var res = states.indexOf(state);
      if (res == -1) {
        res = states.size();
        states.add(state);
      }
      return res;
    }

    private int indexOf(BlockStateRule state) {
      var res = rules.indexOf(state);
      if (res == -1) {
        res = rules.size();
        rules.add(state);
      }
      return res;
    }

    public void push(SequenceBlockStateRule rules) {
      var rles = rules.rules();
      var size = rles.size();
      for (int i = 0; i < size; i++) push(rles.get(i));
    }

    public void push(ConditionalBlockStateRule rule) {
      var index = this.conditions.size();
      this.conditions.add(rule.condition());

      if (rule.followup() instanceof SimpleBlockStateRule simple) {
        this.addInst(RETURN_IF, index, indexOf(simple.state()));
        return;
      }

      this.addInst(JUMP_IF_FALSE, index, 0);
      var instIndex = instructions.size() - 1;

      push(rule.followup());

      this.instructions.set(instIndex, instructions.size());
    }

    private void push(BlockStateRule rule) {
      switch (rule) {
        case SequenceBlockStateRule seq -> push(seq);
        case ConditionalBlockStateRule cond -> push(cond);
        case SimpleBlockStateRule simple -> addInst(RETURN, indexOf(simple.state()));
        default -> addInst(TRY_RETURN, indexOf(rule));
      }
    }

    private void addInst(int opcode, int... data) {
      this.instructions.add(opcode);
      for (int i = 0; i < data.length; i++) this.instructions.add(data[i]);
    }

    public MaterialRuleInterpreter build() {
      return new MaterialRuleInterpreter(
          this.conditions.toArray(BooleanSupplier[]::new),
          this.rules.toArray(BlockStateRule[]::new),
          this.states.toArray(BlockState[]::new),
          this.instructions.toIntArray());
    }
  }
}
