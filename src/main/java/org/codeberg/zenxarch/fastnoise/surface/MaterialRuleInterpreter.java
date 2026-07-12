package org.codeberg.zenxarch.fastnoise.surface;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
  // 0..3 instr id 4..17 a 18..31 b
  private final int[] instructions;
  private final MethodHandle compiled;

  private static final int JUMP_IF_FALSE = 0; // if (!conditions[a]) jmp(b);
  private static final int RETURN = 1; // return states[a];
  private static final int TRY_RETURN = 2; // if (rules[a] != null) return rules[a];
  private static final int RETURN_IF = 3; // if (conditions[a]) return states[b];

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
      var opcode = inst & 0xF;
      var a = (inst >>> 4) & 0x3FFF;
      var b = (inst >>> 18) & 0x3FFF;
      switch (opcode) {
        case JUMP_IF_FALSE -> {
          if (conditions[a].get()) instPointer++;
          else instPointer = b;
        }
        case RETURN -> {
          return states[a];
        }
        case TRY_RETURN -> {
          var res = this.rules[a].tryApply(x, y, z);
          if (res != null) return res;
          instPointer++;
        }
        case RETURN_IF -> {
          if (conditions[a].get()) return states[b];
          instPointer++;
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
    for (int i = 0; i < instructions.length; i++) {
      var inst = instructions[i];
      var opcode = inst & 0xF;
      var a = (inst >>> 4) & 0x3FFF;
      var b = (inst >>> 18) & 0x3FFF;
      res.append(i).append(" ");
      appendInstForToString(res, opcode, a, b);
    }
    res.append("]");
    return res.toString();
  }

  private void appendInstForToString(StringBuilder res, int opcode, int a, int b) {
    switch (opcode) {
      case JUMP_IF_FALSE ->
          res.append("jmpiffalse ").append(conditions[a]).append(" ").append(b).append(",");
      case RETURN -> res.append("ret ").append(states[a]).append(",");
      case TRY_RETURN -> res.append("tryret ").append(rules[a]).append(",");
      case RETURN_IF ->
          res.append("retif ").append(conditions[a]).append(" ").append(states[b]).append(",");
      default ->
          res.append("noop ")
              .append("{")
              .append(opcode)
              .append(" ")
              .append(a)
              .append(" ")
              .append(b)
              .append("},");
    }
  }

  private static class Builder {
    private final ObjectArrayList<BooleanSupplier> conditions;
    private final ObjectArrayList<BlockStateRule> rules;
    private final ObjectArrayList<BlockState> states;
    private final IntArrayList instructions;

    public Builder() {
      this.conditions = new ObjectArrayList<>();
      this.rules = new ObjectArrayList<>();
      this.states = new ObjectArrayList<>();
      this.instructions = new IntArrayList();
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
        var stateIndex = this.states.size();
        this.states.add(simple.state());
        this.addInst(RETURN_IF, index, stateIndex);
        return;
      }

      var instIndex = this.instructions.size();
      this.addInst(JUMP_IF_FALSE, index, 0);

      push(rule.followup());

      this.instructions.set(instIndex, combineInst(JUMP_IF_FALSE, index, this.instructions.size()));
    }

    private void push(BlockStateRule rule) {
      if (rule instanceof SequenceBlockStateRule seq) push(seq);
      else if (rule instanceof ConditionalBlockStateRule cond) push(cond);
      else if (rule instanceof SimpleBlockStateRule simple) {
        var index = this.states.size();
        this.states.add(simple.state());
        addInst(RETURN, index, 0);
      } else {
        var index = this.rules.size();
        this.rules.add(rule);
        addInst(TRY_RETURN, index, 0);
      }
    }

    private void addInst(int opcode, int a, int b) {
      this.instructions.add(opcode | (a << 4) | (b << 18));
    }

    private int combineInst(int opcode, int a, int b) {
      return opcode | (a << 4) | (b << 18);
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
