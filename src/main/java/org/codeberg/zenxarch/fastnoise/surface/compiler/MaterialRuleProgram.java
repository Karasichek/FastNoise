package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.codeberg.zenxarch.fastnoise.surface.rules.AboveYPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.StoneDepthPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.VerticalGradientPredicate.CheckedVerticalGradientPredicate;
import org.codeberg.zenxarch.fastnoise.surface.rules.VerticalGradientPredicate.XoroshiroVerticalGradientPredicate;

public record MaterialRuleProgram(
    byte[] opcode, // opcode
    int[] data, // data, 1 for each instruction
    JumpData[] jumpDataArr,
    long[] biomeMasks,
    AboveYCondition[] aboveYConditions,
    StoneDepthCondition[] stoneDepthConditions,
    XoroVerticalGradientCondition[] xoroVerticalGradientConditions,
    CheckedVerticalGradientCondition[] checkedVerticalGradientConditions) {

  public int size() {
    return opcode.length;
  }

  public static final byte OPCODE_RETURN = 0;
  public static final byte OPCODE_TRY_RETURN = 1;
  public static final byte OPCODE_JUMP_IF = 2;
  public static final byte OPCODE_JUMP_IF_NOT = 3;

  public static record JumpData(byte type, int idx, int offset) {}

  public static final byte JUMP_TYPE_CONDITION = 0; // conditions[idx]
  public static final byte JUMP_TYPE_BIOME = 1;
  public static final byte JUMP_TYPE_ABOVE_Y = 2;
  public static final byte JUMP_TYPE_STONE_DEPTH = 3;
  public static final byte JUMP_TYPE_XORO_VERTICAL_GRADIENT = 4;
  public static final byte JUMP_TYPE_CHECKED_VERTICAL_GRADIENT = 5;

  public static record AboveYCondition(boolean addStoneDepth, int y, int surfaceDepthMultiplier) {}

  public static record StoneDepthCondition(
      int offset, boolean addSurfaceDepth, int secondaryDepthRange, boolean ceiling) {}

  public static record XoroVerticalGradientCondition(
      int trueAtAndBelow, int falseAtAndAbove, long seedLo, long seedHi) {}

  public static record CheckedVerticalGradientCondition(
      int trueAtAndBelow, int falseAtAndAbove, long seed) {}

  public static final class Builder {
    private ByteArrayList opcodes = new ByteArrayList();
    private IntArrayList data = new IntArrayList();
    private ResizingPalette<JumpData> jumpDatas = new ResizingPalette<>();
    private ResizingPalette<Long> biomeMasks = new ResizingPalette<>();
    private ResizingPalette<AboveYCondition> aboveYConditions = new ResizingPalette<>();
    private ResizingPalette<StoneDepthCondition> stoneDepthConditions = new ResizingPalette<>();
    private ResizingPalette<XoroVerticalGradientCondition> xoroVerticalGradientConditions =
        new ResizingPalette<>();
    private ResizingPalette<CheckedVerticalGradientCondition> checkedVerticalGradientConditions =
        new ResizingPalette<>();

    public void addInst(byte opcode, int data) {
      this.opcodes.add(opcode);
      this.data.add(data);
    }

    public void updateInst(int idx, byte opcode, int data) {
      this.opcodes.set(idx, opcode);
      this.data.set(idx, data);
    }

    public int size() {
      return opcodes.size();
    }

    public int addJumpData(JumpData jumpData) {
      return jumpDatas.indexOf(jumpData);
    }

    public int addBiomeMask(long mask) {
      return this.biomeMasks.indexOf(mask);
    }

    public int addAboveY(AboveYPredicate predicate) {
      return this.aboveYConditions.indexOf(
          new AboveYCondition(
              predicate.addStoneDepth(), predicate.y(), predicate.surfaceDepthMultiplier()));
    }

    public int addStoneDepth(StoneDepthPredicate predicate) {
      return this.stoneDepthConditions.indexOf(
          new StoneDepthCondition(
              predicate.offset(),
              predicate.addSurfaceDepth(),
              predicate.secondaryDepthRange(),
              predicate.ceiling()));
    }

    public int addXoro(XoroshiroVerticalGradientPredicate predicate) {
      return this.xoroVerticalGradientConditions.indexOf(
          new XoroVerticalGradientCondition(
              predicate.trueAtAndBelow(),
              predicate.falseAtAndAbove(),
              predicate.seedLo(),
              predicate.seedHi()));
    }

    public int addChecked(CheckedVerticalGradientPredicate predicate) {
      return this.checkedVerticalGradientConditions.indexOf(
          new CheckedVerticalGradientCondition(
              predicate.trueAtAndBelow(), predicate.falseAtAndAbove(), predicate.seed()));
    }

    public MaterialRuleProgram build() {
      return new MaterialRuleProgram(
          opcodes.toByteArray(),
          data.toIntArray(),
          jumpDatas.build(JumpData[]::new),
          ArrayUtils.toPrimitive(biomeMasks.build(Long[]::new)),
          aboveYConditions.build(AboveYCondition[]::new),
          stoneDepthConditions.build(StoneDepthCondition[]::new),
          xoroVerticalGradientConditions.build(XoroVerticalGradientCondition[]::new),
          checkedVerticalGradientConditions.build(CheckedVerticalGradientCondition[]::new));
    }
  }
}
