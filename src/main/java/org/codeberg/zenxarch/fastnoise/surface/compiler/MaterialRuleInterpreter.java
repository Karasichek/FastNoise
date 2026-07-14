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
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.rules.AboveYPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.BiomePredicate;
import org.jspecify.annotations.Nullable;

public final class MaterialRuleInterpreter implements BlockStateRule {

  private final BooleanSupplier[] conditions;
  private final BlockStateRule[] rules;
  private final BlockState[] states;
  private final int[] instructions;
  private final MethodHandle compiled;
  private final MaterialRuleContext context;

  public static final int JUMP_IF_FALSE = 0; // if (!conditions[a]) jmp(b);
  public static final int RETURN = 1; // return states[a];
  public static final int TRY_RETURN = 2; // if (rules[a] != null) return rules[a];
  public static final int RETURN_IF = 3; // if (conditions[a]) return states[b];
  public static final int BIOME_TEST =
      4; // if (!ctx.getCurrentBiomeIndex() matches a(mask)) jmp(b);
  public static final int ABOVE_Y = 5; // if (!(context.blockY >= a + context.runDepth * b)) jmp(c);
  public static final int ABOVE_Y_WITH_STONE_DEPTH =
      6; // if (!(context.blockY + stoneDepthAbove >= a + context.runDepth * b)) jmp(c);

  // private static final int NO_OP = 4;

  public MaterialRuleInterpreter(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      int[] instructions,
      MaterialRuleContext context) {
    this.conditions = conditions;
    this.rules = rules;
    this.states = states;
    this.instructions = instructions;
    this.compiled = MaterialRuleCompilerCache.get(instructions);
    this.context = context;
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    try {
      return (BlockState) compiled.invokeExact(conditions, rules, states, x, y, z, context);
    } catch (Throwable e) {
    }
    var instPointer = 0;
    var max = instructions.length;
    while (instPointer < max) {
      var inst = this.instructions[instPointer];
      switch (inst) {
        case JUMP_IF_FALSE -> {
          instPointer +=
              (conditions[instructions[instPointer + 1]].get()) ? 3 : instructions[instPointer + 2];
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
        case BIOME_TEST -> {
          var idx = context.getCurrentBiomeIdx();
          instPointer +=
              ((getLong(instructions, instPointer + 1) & (0x1L << idx)) == 0)
                  ? instructions[instPointer + 3]
                  : 4;
        }
        case ABOVE_Y -> {
          instPointer +=
              (context.blockY
                      < (instructions[instPointer + 1]
                          + context.runDepth * instructions[instPointer + 2]))
                  ? instructions[instPointer + 3]
                  : 4;
        }
        case ABOVE_Y_WITH_STONE_DEPTH -> {
          instPointer +=
              ((context.blockY + context.stoneDepthAbove)
                      < (instructions[instPointer + 1]
                          + context.runDepth * instructions[instPointer + 2]))
                  ? instructions[instPointer + 3]
                  : 4;
        }
        default -> {
          // no op
          instPointer++;
        }
      }
    }
    return null;
  }

  public static long getLong(int[] data, int index) {
    var a = data[index];
    var b = data[index + 1];
    return (Integer.toUnsignedLong(a) << 32) | Integer.toUnsignedLong(b);
  }

  public static MaterialRuleInterpreter of(
      SequenceBlockStateRule root, MaterialRuleContext context) {
    var builder = new Builder(context);
    builder.push(root);

    return builder.build();
  }

  @Override
  public String toString() {
    var res = new StringBuilder();
    res.append("MaterialRuleInterpreter[");
    toString0(res, instructions);
    res.append("]");
    return res.toString();
  }

  public static void toString0(StringBuilder builder, int[] instructions) {
    builder.append("long idx; BlockState state; ");
    toString1(builder, instructions, 0);
  }

  public static void toString1(StringBuilder builder, int[] instructions, int index) {
    if (index >= instructions.length) {
      builder.append("return null; /* end of program */");
      return;
    }
    switch (instructions[index]) {
      case JUMP_IF_FALSE -> {
        builder.append("if (conditions[").append(instructions[index + 1]).append("].get()) {");
        toString1(builder, instructions, index + 3);
        builder.append("} else {");
        toString1(builder, instructions, index + instructions[index + 2]);
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
        toString1(builder, instructions, index + 2);
        builder.append("}");
      }
      case RETURN_IF -> {
        builder
            .append("if (conditions[")
            .append(instructions[index + 1])
            .append("].get()) { return states[")
            .append(instructions[index + 2])
            .append("]; } else {");
        toString1(builder, instructions, index + 3);
        builder.append("}");
      }
      case BIOME_TEST -> {
        builder.append("idx = context.getCurrentBiomeIdx(); if ((");
        builder.append(String.format("0x%xL", getLong(instructions, index + 1)));
        builder.append(" & (0x1L << idx)) != 0) { ");
        toString1(builder, instructions, index + 4);
        builder.append(" } else { ");
        toString1(builder, instructions, index + instructions[index + 3]);
        builder.append(" }");
      }
      case ABOVE_Y -> {
        builder
            .append("if (context.blockY >= (")
            .append(instructions[index + 1])
            .append(" context.runDepth * ")
            .append(instructions[index + 2])
            .append(") { ");
        toString1(builder, instructions, index + 4);
        builder.append(" } else { ");
        toString1(builder, instructions, index + instructions[index + 3]);
        builder.append(" }");
      }
      case ABOVE_Y_WITH_STONE_DEPTH -> {
        builder
            .append("if ((context.blockY + context.stoneDepthAbove) >= (")
            .append(instructions[index + 1])
            .append(" context.runDepth * ")
            .append(instructions[index + 2])
            .append(") { ");
        toString1(builder, instructions, index + 4);
        builder.append(" } else { ");
        toString1(builder, instructions, index + instructions[index + 3]);
        builder.append(" }");
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
    private final MaterialRuleContext context;

    public Builder(MaterialRuleContext context) {
      this.conditions = new ObjectArrayList<>();
      this.rules = new ReferenceArrayList<>();
      this.states = new ReferenceArrayList<>();
      this.instructions = new IntArrayList();
      this.context = context;
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
      var startIndex = instructions.size();
      if (rule.condition() instanceof BiomePredicate biome) {
        this.addInst(BIOME_TEST, (int) (biome.mask >>> 32), (int) (biome.mask), 0);
        var instIndex = instructions.size() - 1;

        push(rule.followup());

        this.instructions.set(instIndex, instructions.size() - startIndex);
        return;
      }

      if (rule.condition() instanceof AboveYPredicate above) {
        this.addInst(
            above.addStoneDepth() ? ABOVE_Y_WITH_STONE_DEPTH : ABOVE_Y,
            above.y(),
            above.surfaceDepthMultiplier(),
            0);
        var instIndex = instructions.size() - 1;

        push(rule.followup());

        this.instructions.set(instIndex, instructions.size() - startIndex);
        return;
      }

      var index = this.conditions.size();
      this.conditions.add(rule.condition());

      if (rule.followup() instanceof SimpleBlockStateRule simple) {
        this.addInst(RETURN_IF, index, indexOf(simple.state()));
        return;
      }

      this.addInst(JUMP_IF_FALSE, index, 0);
      var instIndex = instructions.size() - 1;

      push(rule.followup());

      this.instructions.set(instIndex, instructions.size() - startIndex);
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
          this.instructions.toIntArray(),
          context);
    }
  }
}
