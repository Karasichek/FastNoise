package org.codeberg.zenxarch.matcomp.compiler;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.lang.invoke.MethodHandle;
import java.util.BitSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.BiomeMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.BlockMaterialRule;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.ConditionMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule.RegisteredRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SequenceMaterialRule;
import org.codeberg.zenxarch.matcomp.MaterialRuleCompilerConstants;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.codeberg.zenxarch.matcomp.rules.OreVeinMaterialRule;
import org.jspecify.annotations.Nullable;

public final class MaterialRuleInterpreter implements BlockStateRule {

  private final BooleanSupplier[] conditions;
  public final BlockStateRule[] rules;
  public final BlockState[] states;
  private final int[] instructions;
  private final MethodHandle compiled;
  private final MaterialRuleContext context;
  private final MaterialRuleCompilerCacheHeightContextSpecific cache;

  public static final int JUMP_IF_FALSE = 0; // if (!conditions[a]) jmp(b);
  public static final int RETURN = 1; // return states[a];
  public static final int TRY_RETURN = 2; // if (rules[a] != null) return rules[a];
  public static final int JUMP_IF_FALSE_STATIC = 3; // if (!staticConditions[a].get()) jmp(b);

  // private static final int NO_OP = 5;

  private boolean hasNotCrashed = true;

  public MaterialRuleInterpreter(
      BooleanSupplier[] conditions,
      BlockStateRule[] rules,
      BlockState[] states,
      BitSet stateSet,
      int[] instructions,
      MaterialRuleCompilerCacheHeightContextSpecific cache,
      MaterialRuleContext context) {
    this.conditions = conditions;
    this.rules = rules;
    this.states = states;
    this.instructions = instructions;
    this.cache = cache;
    this.compiled = cache.get(instructions, stateSet);
    this.context = context;
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    if (hasNotCrashed) {
      try {
        return (BlockState) compiled.invokeExact(conditions, rules, states, x, y, z, context);
      } catch (Throwable e) {
        MaterialRuleCompilerConstants.LOGGER.error("Compiled surface rule failed", e);
        hasNotCrashed = false;
      }
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
          return Block.getStateFromRawId(instructions[instPointer + 1]);
        }
        case TRY_RETURN -> {
          var res = this.rules[instructions[instPointer + 1]].tryApply(x, y, z);
          if (res != null) return res;
          instPointer += 2;
        }
        case JUMP_IF_FALSE_STATIC -> {
          var idx = instructions[instPointer + 1];
          var offset = instructions[instPointer + 2];
          try {
            instPointer +=
                ((boolean)
                        cache
                            .getHandle(idx)
                            .interpret(
                                this.conditions, this.rules, this.states, x, y, z, this.context))
                    ? 3
                    : offset;
          } catch (Throwable e) {
            instPointer += offset;
          }
        }
        default -> {
          throw new IllegalStateException("Invalid instruction");
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
      MaterialRuleContext context,
      HeightContext heightContext,
      NoiseConfig config,
      MaterialRule rule) {
    return new Builder(context, config, rule).build();
  }

  @Override
  public String toString() {
    var res = new StringBuilder();
    res.append("MaterialRuleInterpreter[");
    toString0(res, instructions);
    res.append("]");
    return res.toString();
  }

  public static void debugPrint(int[] instructions) {
    var builder = new StringBuilder();
    toString0(builder, instructions);
    MaterialRuleCompilerConstants.LOGGER.info("Program : {}", builder.toString());
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
        builder
            .append("return Block.getStateFromRawId(")
            .append(instructions[index + 1])
            .append(");");
      }
      case TRY_RETURN -> {
        builder
            .append("state = rules[")
            .append(instructions[index + 1])
            .append("].tryApply(x,y,z); if (state != null) {return state;} else {");
        toString1(builder, instructions, index + 2);
        builder.append("}");
      }
      case JUMP_IF_FALSE_STATIC -> {
        builder
            .append("if (specialCondition(")
            .append(instructions[index + 1])
            .append(",")
            .append("context)) { ");
        toString1(builder, instructions, index + 3);
        builder.append(" } else {");
        toString1(builder, instructions, index + instructions[index + 2]);
        builder.append(" }");
      }
      default -> {
        builder.append("return null; /* invalid */");
      }
    }
  }

  public BlockState[] getExpectedStates() {
    if (rules.length == 0) return states;
    for (int i = 0; i < rules.length; i++) {
      if (!(rules[i] instanceof OreVeinMaterialRule)) return null;
    }

    var result = new ObjectOpenHashSet<>(this.states, Hash.FAST_LOAD_FACTOR);

    for (int i = 0; i < rules.length; i++) {
      var ore = (OreVeinMaterialRule) rules[i];
      result.add(ore.oreBlock());
      result.add(ore.rawOreBlock());
      result.add(ore.fillerBlock());
    }
    return result.toArray(BlockState[]::new);
  }

  private static class Builder {
    private final ObjectArrayList<BooleanSupplier> conditions;
    private final ReferenceArrayList<BlockStateRule> rules;
    private final IntArrayList instructions;
    private final MaterialRuleContext context;
    private final BitSet statesSet;

    private final MaterialRuleCompilerCacheHeightContextSpecific cache;

    public Builder(MaterialRuleContext context, NoiseConfig config, MaterialRule rule) {
      this.conditions = new ObjectArrayList<>();
      this.rules = new ReferenceArrayList<>();
      this.instructions = new IntArrayList();
      this.statesSet = new BitSet();
      this.context = context;
      this.cache =
          ((MaterialRuleCompilerCacheHolder) context.surfaceBuilder)
              .zenxarch$getCache()
              .get(context.heightContext, config, rule);
      this.push(rule);
    }

    private int indexOf(BlockState state) {
      var res = Block.getRawIdFromState(state);
      statesSet.set(res);
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

    public void push(SequenceMaterialRule rule) {
      var rles = rule.sequence();
      var size = rles.size();
      for (int i = 0; i < size; i++) push(rles.get(i));
    }

    public void push(ConditionMaterialRule rule) {
      var startIndex = instructions.size();
      if (rule.ifTrue() instanceof BiomeMaterialCondition biome) {
        var cond = BiomePredicate.getSupplier(biome.biomes(), context);
        if (cond == BiomePredicate.ALWAYS_FALSE) return;
        if (cond == BiomePredicate.ALWAYS_TRUE) {
          push(rule.thenRun());
          return;
        }

        var index = this.conditions.size();
        this.conditions.add(cond);

        this.addInst(JUMP_IF_FALSE, index, 0);
        var instIndex = instructions.size() - 1;

        push(rule.thenRun());

        if (instIndex + 1 == instructions.size()) {
          this.removeInst(4);
          return;
        }

        this.instructions.set(instIndex, instructions.size() - startIndex);
        return;
      }

      var specialIdx = cache.getIdx(rule.ifTrue());

      if (specialIdx != -1) {
        this.addInst(JUMP_IF_FALSE_STATIC, specialIdx, 0);

        var instIndex = instructions.size() - 1;
        push(rule.thenRun());

        if (instIndex + 1 == instructions.size()) {
          this.removeInst(3);
          return;
        }

        this.instructions.set(instIndex, instructions.size() - startIndex);
        return;
      }

      var index = this.conditions.size();
      this.conditions.add(rule.ifTrue().getPredicate(context));

      this.addInst(JUMP_IF_FALSE, index, 0);
      var instIndex = instructions.size() - 1;

      push(rule.thenRun());

      if (instIndex + 1 == instructions.size()) {
        this.removeInst(3);
        return;
      }

      this.instructions.set(instIndex, instructions.size() - startIndex);
    }

    private void push(MaterialRule rule) {
      switch (rule) {
        case SequenceMaterialRule seq -> push(seq);
        case ConditionMaterialRule cond -> push(cond);
        case BlockMaterialRule simple -> addInst(RETURN, indexOf(simple.resultState()));
        case RegisteredRule registered -> push(registered.holder().value());
        default -> addInst(TRY_RETURN, indexOf(rule.getBlockStateRule(context)));
      }
    }

    private void addInst(int opcode, int... data) {
      this.instructions.add(opcode);
      for (int i = 0; i < data.length; i++) this.instructions.add(data[i]);
    }

    private void removeInst(int len) {
      this.instructions.size(this.instructions.size() - len);
    }

    public MaterialRuleInterpreter build() {
      var states =
          this.statesSet.stream()
              .mapToObj(k -> Block.getStateFromRawId(k))
              .toArray(BlockState[]::new);

      return new MaterialRuleInterpreter(
          this.conditions.toArray(BooleanSupplier[]::new),
          this.rules.toArray(BlockStateRule[]::new),
          states,
          statesSet,
          this.instructions.toIntArray(),
          cache,
          context);
    }
  }
}
