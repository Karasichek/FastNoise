package org.codeberg.zenxarch.fastnoise.surface.compiler;

import static org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleProgram.*;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.lang.invoke.MethodHandle;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.ConditionalBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.InvertedBooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SimpleBlockStateRule;
import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;
import org.codeberg.zenxarch.fastnoise.surface.AllocFreeRandom;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;
import org.codeberg.zenxarch.fastnoise.surface.compiler.MaterialRuleProgram.JumpData;
import org.codeberg.zenxarch.fastnoise.surface.rules.AboveYPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.BiomePredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.StoneDepthPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.VerticalGradientPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.VerticalGradientPredicate.CheckedVerticalGradientPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.VerticalGradientPredicate.XoroshiroVerticalGradientPredicate;
import org.jspecify.annotations.Nullable;

public final class MaterialRuleInterpreter implements BlockStateRule {

  private final BooleanSupplier[] conditions;
  public final BlockStateRule[] rules;
  public final BlockState[] states;
  private final MaterialRuleProgram program;
  private final MethodHandle compiled;
  private final MaterialRuleContext context;

  private boolean hasNotCrashed = true;

  public MaterialRuleInterpreter(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      MaterialRuleProgram program,
      MaterialRuleContext context) {
    this.conditions = conditions;
    this.rules = rules;
    this.states = states;
    this.program = program;
    this.compiled =
        ((MaterialRuleCompilerCacheHolder) context.surfaceBuilder).zenxarch$getCache().get(program);
    this.context = context;
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    if (hasNotCrashed) {
      try {
        return (BlockState) compiled.invokeExact(conditions, rules, states, x, y, z, context);
      } catch (Throwable e) {
        // TODO: handle crashes
        FastNoiseConstants.LOGGER.error("Compiled surface rule failed", e);
        hasNotCrashed = false;
      }
    }
    var instPointer = 0;
    final var opcodes = program.opcode();
    final var data = program.data();
    final var max = opcodes.length;
    while (instPointer < max) {
      var inst = opcodes[instPointer];
      switch (inst) {
        case OPCODE_RETURN -> {
          return states[data[instPointer]];
        }
        case OPCODE_TRY_RETURN -> {
          var res = rules[data[instPointer]].tryApply(x, y, z);
          if (res != null) return res;
          instPointer++;
        }
        case OPCODE_JUMP_IF -> {
          var jumpData = program.jumpDataArr()[data[instPointer]];
          instPointer +=
              evalCondition(jumpData.type(), jumpData.idx(), x, y, z) ? jumpData.offset() : 1;
        }
        case OPCODE_JUMP_IF_NOT -> {
          var jumpData = program.jumpDataArr()[data[instPointer]];
          instPointer +=
              evalCondition(jumpData.type(), jumpData.idx(), x, y, z) ? 1 : jumpData.offset();
        }
        default -> {
          // no op
          instPointer++;
        }
      }
    }
    return null;
  }

  private boolean evalCondition(byte type, int idx, int x, int y, int z) {
    return switch (type) {
      case JUMP_TYPE_CONDITION -> conditions[idx].get();
      case JUMP_TYPE_BIOME -> {
        var biomeIdx = context.getCurrentBiomeIdx();
        yield (program.biomeMasks()[idx] & (0x1L << biomeIdx)) != 0;
      }
      case JUMP_TYPE_ABOVE_Y -> {
        var aboveY = program.aboveYConditions()[idx];
        yield (context.blockY + (aboveY.addStoneDepth() ? context.stoneDepthAbove : 0))
            >= (aboveY.y() + context.runDepth * aboveY.surfaceDepthMultiplier());
      }
      case JUMP_TYPE_STONE_DEPTH -> {
        var stoneDepthCondition = program.stoneDepthConditions()[idx];
        var stoneDepth =
            stoneDepthCondition.ceiling() ? context.stoneDepthBelow : context.stoneDepthAbove;
        if (stoneDepthCondition.addSurfaceDepth()) stoneDepth -= context.runDepth;
        if (stoneDepthCondition.secondaryDepthRange() != 0)
          stoneDepth -=
              (int)
                  ((context.getSecondaryDepth() + 1.0)
                      * 0.5
                      * stoneDepthCondition.secondaryDepthRange());

        yield stoneDepth <= stoneDepthCondition.offset();
      }
      case JUMP_TYPE_XORO_VERTICAL_GRADIENT -> {
        var xoro = program.xoroVerticalGradientConditions()[idx];

        final int blockY = this.context.blockY;
        if (blockY <= xoro.trueAtAndBelow()) yield true;
        if (blockY >= xoro.falseAtAndAbove()) yield false;

        var probability =
            VerticalGradientPredicate.map(blockY, xoro.trueAtAndBelow(), xoro.falseAtAndAbove());
        yield AllocFreeRandom.nextFloatXoroshiro(
                xoro.seedLo(), xoro.seedHi(), context.blockX, blockY, context.blockZ)
            < probability;
      }
      case JUMP_TYPE_CHECKED_VERTICAL_GRADIENT -> {
        var checked = program.checkedVerticalGradientConditions()[idx];

        final int blockY = this.context.blockY;
        if (blockY <= checked.trueAtAndBelow()) yield true;
        if (blockY >= checked.falseAtAndAbove()) yield false;

        var probability =
            VerticalGradientPredicate.map(
                blockY, checked.trueAtAndBelow(), checked.falseAtAndAbove());
        yield AllocFreeRandom.nextDoubleChecked(
                checked.seed(), context.blockX, blockY, context.blockZ)
            < probability;
      }
      default -> true;
    };
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
    toString0(res, program);
    res.append("]");
    return res.toString();
  }

  public static void debugPrint(MaterialRuleProgram program) {
    var builder = new StringBuilder();
    toString0(builder, program);
    FastNoiseConstants.LOGGER.info("Program : {}", builder.toString());
  }

  public static void toString0(StringBuilder builder, MaterialRuleProgram program) {
    builder.append("long idx; BlockState state; ");
    toString1(builder, program, 0);
  }

  private static void toString1(StringBuilder builder, MaterialRuleProgram program, int index) {
    if (index >= program.opcode().length) {
      builder.append("return null; /* end of program */");
      return;
    }
    final var opcode = program.opcode()[index];
    final var data = program.data()[index];
    switch (opcode) {
      case OPCODE_RETURN -> {
        builder.append("return states[").append(data).append("];");
      }
      case OPCODE_TRY_RETURN -> {
        builder
            .append("state = rules[")
            .append(data)
            .append("].tryApply(x,y,z); if (state != null) {return state;} else {");
        toString1(builder, program, index + 1);
        builder.append("}");
      }
      case OPCODE_JUMP_IF -> {
        var jumpData = program.jumpDataArr()[data];
        builder.append("if (");
        toString2(builder, program, jumpData.type(), jumpData.idx());
        builder.append(") {");
        toString1(builder, program, index + jumpData.offset());
        builder.append("} else {");
        toString1(builder, program, index + 1);
        builder.append("}");
      }
      case OPCODE_JUMP_IF_NOT -> {
        var jumpData = program.jumpDataArr()[data];
        builder.append("if (");
        toString2(builder, program, jumpData.type(), jumpData.idx());
        builder.append(") {");
        toString1(builder, program, index + 1);
        builder.append("} else {");
        toString1(builder, program, index + jumpData.offset());
        builder.append("}");
      }
      default -> {
        builder.append("return null; /* invalid */");
      }
    }
  }

  private static void toString2(
      StringBuilder builder, MaterialRuleProgram program, int type, int idx) {
    switch (type) {
      case JUMP_TYPE_CONDITION -> {
        builder.append("conditions[").append(idx).append("].get()");
      }
      case JUMP_TYPE_BIOME -> {
        builder.append("biomeTest(context, ").append(program.biomeMasks()[idx]).append(")");
      }
      case JUMP_TYPE_ABOVE_Y -> {
        var cond = program.aboveYConditions()[idx];
        builder
            .append("aboveYTest(context, y, ")
            .append(cond.addStoneDepth())
            .append(",")
            .append(cond.y())
            .append(",")
            .append(cond.surfaceDepthMultiplier())
            .append(")");
      }
      case JUMP_TYPE_STONE_DEPTH -> {
        var cond = program.stoneDepthConditions()[idx];
        builder
            .append("stoneDepthTest(context, ")
            .append(cond.offset())
            .append(",")
            .append(cond.addSurfaceDepth())
            .append(",")
            .append(cond.secondaryDepthRange())
            .append(",")
            .append(cond.ceiling())
            .append(")");
      }
      case JUMP_TYPE_XORO_VERTICAL_GRADIENT -> {
        var cond = program.xoroVerticalGradientConditions()[idx];
        builder
            .append("xoroVerticalGradientTest(context, x, y, z, ")
            .append(cond.trueAtAndBelow())
            .append(",")
            .append(cond.falseAtAndAbove())
            .append(",")
            .append(cond.seedLo())
            .append(",")
            .append(cond.seedHi())
            .append(")");
      }
      case JUMP_TYPE_CHECKED_VERTICAL_GRADIENT -> {
        var cond = program.checkedVerticalGradientConditions()[idx];
        builder
            .append("checkedVerticalGradientTest(context, x, y, z, ")
            .append(cond.trueAtAndBelow())
            .append(",")
            .append(cond.falseAtAndAbove())
            .append(",")
            .append(cond.seed())
            .append(")");
      }
      default -> {
        builder.append("unknown");
      }
    }
  }

  private static class Builder {
    private final ReferenceArrayList<BooleanSupplier> conditions;
    private final ReferenceArrayList<BlockStateRule> rules;
    private final ReferenceArrayList<BlockState> states;
    private final MaterialRuleProgram.Builder program;
    private final MaterialRuleContext context;

    public Builder(MaterialRuleContext context) {
      this.conditions = new ReferenceArrayList<>();
      this.rules = new ReferenceArrayList<>();
      this.states = new ReferenceArrayList<>();
      this.program = new MaterialRuleProgram.Builder();
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

    private int indexOf(BooleanSupplier supplier) {
      var res = conditions.indexOf(supplier);
      if (res == -1) {
        res = conditions.size();
        conditions.add(supplier);
      }
      return res;
    }

    public void push(SequenceBlockStateRule rules) {
      var rles = rules.rules();
      var size = rles.size();
      for (int i = 0; i < size; i++) push(rles.get(i));
    }

    public void push(ConditionalBlockStateRule rule) {
      var startIndex = program.size();

      byte jumpType = -1;
      var jumpIdx = -1;

      var condition = rule.condition();
      var invert = false;

      while (condition instanceof InvertedBooleanSupplier inverted) {
        invert = !invert;
        condition = inverted.target();
      }

      switch (condition) {
        case BiomePredicate biome -> {
          jumpType = JUMP_TYPE_BIOME;
          jumpIdx = program.addBiomeMask(biome.mask);
        }
        case AboveYPredicate aboveY -> {
          jumpType = JUMP_TYPE_ABOVE_Y;
          jumpIdx = program.addAboveY(aboveY);
        }
        case StoneDepthPredicate stoneDepth -> {
          jumpType = JUMP_TYPE_STONE_DEPTH;
          jumpIdx = program.addStoneDepth(stoneDepth);
        }
        case XoroshiroVerticalGradientPredicate xoro -> {
          jumpType = JUMP_TYPE_XORO_VERTICAL_GRADIENT;
          jumpIdx = program.addXoro(xoro);
        }
        case CheckedVerticalGradientPredicate checked -> {
          jumpType = JUMP_TYPE_CHECKED_VERTICAL_GRADIENT;
          jumpIdx = program.addChecked(checked);
        }
        default -> {
          jumpType = JUMP_TYPE_CONDITION;
          jumpIdx = indexOf(condition);
        }
      }

      program.addInst(invert ? OPCODE_JUMP_IF : OPCODE_JUMP_IF_NOT, 0);
      push(rule.followup());
      var jumpOffset = program.size() - startIndex;

      var jumpDataIdx = program.addJumpData(new JumpData(jumpType, jumpIdx, jumpOffset));

      program.updateInst(startIndex, invert ? OPCODE_JUMP_IF : OPCODE_JUMP_IF_NOT, jumpDataIdx);
    }

    private void push(BlockStateRule rule) {
      switch (rule) {
        case SequenceBlockStateRule seq -> push(seq);
        case ConditionalBlockStateRule cond -> push(cond);
        case SimpleBlockStateRule simple -> {
          program.addInst(OPCODE_RETURN, indexOf(simple.state()));
        }
        default -> {
          program.addInst(OPCODE_TRY_RETURN, indexOf(rule));
        }
      }
    }

    public MaterialRuleInterpreter build() {
      return new MaterialRuleInterpreter(
          this.conditions.toArray(BooleanSupplier[]::new),
          this.rules.toArray(BlockStateRule[]::new),
          this.states.toArray(BlockState[]::new),
          this.program.build(),
          context);
    }
  }
}
