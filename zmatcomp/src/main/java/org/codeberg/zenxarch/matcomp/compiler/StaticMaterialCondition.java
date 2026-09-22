package org.codeberg.zenxarch.matcomp.compiler;

import static org.objectweb.asm.Opcodes.*;

import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.AboveYMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.NegativeRunDepthPredicate;
import net.minecraft.world.gen.surfacebuilder.SteepSlopePredicate;
import net.minecraft.world.gen.surfacebuilder.StoneDepthMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.VerticalGradientMaterialCondition;
import org.codeberg.zenxarch.matcomp.compiler.cw.MaterialRuleClassWriter;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedConditions;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public interface StaticMaterialCondition {
  // return jump insn opcode
  public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv);

  static void callFuncWithCtxAtEnd(
      MaterialRuleClassWriter cw, InstructionAdapter mv, String funcName, String desc) {
    cw.loadCtx(mv);
    mv.invokestatic(MATERIALRULECOMPILER_NAME, funcName, desc, false);
  }

  static void loadX(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadX", loadIntDesc);
  }

  static void loadY(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadY", loadIntDesc);
  }

  static void loadZ(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadZ", loadIntDesc);
  }

  static void loadStoneDepthAbove(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadStoneDepthAbove", loadIntDesc);
  }

  static void loadStoneDepthBelow(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadStoneDepthBelow", loadIntDesc);
  }

  static void loadRunDepth(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadRunDepth", loadIntDesc);
  }

  static void loadFluidHeight(MaterialRuleClassWriter cw, InstructionAdapter mv) {
    callFuncWithCtxAtEnd(cw, mv, "loadFluidHeight", loadIntDesc);
  }

  static void getMultipliedSecondaryDepthRange(
      MaterialRuleClassWriter cw, InstructionAdapter mv, int secondaryDepthRange) {
    mv.iconst(secondaryDepthRange);
    callFuncWithCtxAtEnd(
        cw, mv, "getMultipliedSecondaryDepthRange", getMultipliedSecondaryDepthRange);
  }

  @Nullable
  public static StaticMaterialCondition tryCreate(
      MaterialCondition condition, HeightContext context, NoiseConfig config) {
    return switch (condition) {
      case VerticalGradientMaterialCondition vertical ->
          switch (config.getOrCreateRandomSplitter(vertical.randomName())) {
            case Xoroshiro128PlusPlusRandom.Splitter xoro ->
                new VerticalGradientConditionXoroshiro(
                    vertical.trueAtAndBelow().getY(context),
                    vertical.falseAtAndAbove().getY(context),
                    xoro.seedLo,
                    xoro.seedHi);
            case CheckedRandom.Splitter checked ->
                new VerticalGradientConditionChecked(
                    vertical.trueAtAndBelow().getY(context),
                    vertical.falseAtAndAbove().getY(context),
                    checked.seed);
            default -> null;
          };
      case AboveYMaterialCondition aboveY ->
          new AboveYCondition(
              aboveY.anchor().getY(context),
              aboveY.surfaceDepthMultiplier(),
              aboveY.addStoneDepth());
      case StoneDepthMaterialCondition stone ->
          new StoneDepthCondition(
              stone.offset() + 1,
              stone.addSurfaceDepth(),
              stone.secondaryDepthRange(),
              stone.surfaceType() == VerticalSurfaceType.CEILING);
      case NegativeRunDepthPredicate _ -> HoleMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.SurfaceMaterialCondition _ ->
          SurfaceMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.TemperatureMaterialCondition _ ->
          TemperatureMaterialCondition.instance;
      case SteepSlopePredicate _ -> SteepMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.WaterMaterialCondition water ->
          new WaterMaterialCondition(
              water.offset(), water.surfaceDepthMultiplier(), water.addStoneDepth());
      case LithostitchedConditions.SlopeCondition slope ->
          new SlopeCondition(slope.threshold().minInclusive(), slope.threshold().maxInclusive());
      default -> null;
    };
  }

  private static String getDescriptorForCondition(Class<?>... classes) {
    var baseClasses = new Class<?>[] {MaterialRuleContext.class};
    var types =
        Stream.concat(Arrays.stream(classes), Stream.of(baseClasses))
            .map(Type::getType)
            .toArray(Type[]::new);
    return Type.getMethodDescriptor(Type.BOOLEAN_TYPE, types);
  }

  public static final String MATERIALRULECOMPILER_NAME =
      Type.getInternalName(MaterialRuleCompilerFunctions.class);

  public static final String xoroVerticalGradient =
      getDescriptorForCondition(int.class, int.class, long.class, long.class);
  public static final String checkedVerticalGradient =
      getDescriptorForCondition(int.class, int.class, long.class);
  public static final String common_desc =
      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(MaterialRuleContext.class));
  public static final String loadIntDesc =
      Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(MaterialRuleContext.class));
  public static final String getMultipliedSecondaryDepthRange =
      Type.getMethodDescriptor(
          Type.INT_TYPE, Type.INT_TYPE, Type.getType(MaterialRuleContext.class));
  public static final String slopeCondDesc = getDescriptorForCondition(int.class, int.class);
  public static final String slopeCondDescAlt = getDescriptorForCondition(int.class);

  public static record AboveYCondition(int y, int surfaceDepthMultiplier, boolean addStoneDepth)
      implements StaticMaterialCondition {

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      loadY(cw, mv);
      if (addStoneDepth) {
        loadStoneDepthAbove(cw, mv);
        mv.add(Type.INT_TYPE);
      }
      mv.iconst(y);
      if (surfaceDepthMultiplier != 0) {
        loadRunDepth(cw, mv);

        if (surfaceDepthMultiplier == 1) {
          mv.add(Type.INT_TYPE);
        } else if (surfaceDepthMultiplier == -1) {
          mv.sub(Type.INT_TYPE);
        } else {
          mv.iconst(surfaceDepthMultiplier);
          mv.mul(Type.INT_TYPE);
          mv.add(Type.INT_TYPE);
        }
      }

      return IF_ICMPLT;
    }
  }

  public static record StoneDepthCondition(
      int offset, boolean addSurfaceDepth, int secondaryDepthRange, boolean ceiling)
      implements StaticMaterialCondition {

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      if (ceiling) loadStoneDepthBelow(cw, mv);
      else loadStoneDepthAbove(cw, mv);
      if (addSurfaceDepth) {
        loadRunDepth(cw, mv);
        mv.sub(Type.INT_TYPE);
      }

      if (secondaryDepthRange != 0) {
        getMultipliedSecondaryDepthRange(cw, mv, secondaryDepthRange);
        mv.sub(Type.INT_TYPE);
      }

      mv.iconst(offset);

      return IF_ICMPGT;
    }
  }

  public static record VerticalGradientConditionXoroshiro(
      int trueAtAndBelow, int falseAtAndAbove, long seedLo, long seedHi)
      implements StaticMaterialCondition {

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(trueAtAndBelow);
      mv.iconst(falseAtAndAbove);
      mv.lconst(seedLo);
      mv.lconst(seedHi);
      cw.loadCtx(mv);
      mv.invokestatic(
          MATERIALRULECOMPILER_NAME, "xoroVerticalGradient", xoroVerticalGradient, false);

      return IFEQ;
    }
  }

  public static record VerticalGradientConditionChecked(
      int trueAtAndBelow, int falseAtAndAbove, long seed) implements StaticMaterialCondition {
    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(trueAtAndBelow);
      mv.iconst(falseAtAndAbove);
      mv.lconst(seed);
      cw.loadCtx(mv);
      mv.invokestatic(
          MATERIALRULECOMPILER_NAME, "checkedVerticalGradient", checkedVerticalGradient, false);

      return IFEQ;
    }
  }

  public static final String STATIC_MATERIAL_CONDITION_INTERNAL_NAME =
      Type.getInternalName(StaticMaterialCondition.class);

  public static record HoleMaterialCondition() implements StaticMaterialCondition {
    public static HoleMaterialCondition instance = new HoleMaterialCondition();

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      loadRunDepth(cw, mv);
      return IFGT;
    }
  }

  public static record SurfaceMaterialCondition() implements StaticMaterialCondition {
    public static SurfaceMaterialCondition instance = new SurfaceMaterialCondition();

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "surfacePredicate", common_desc, false);
      return IFEQ;
    }
  }

  public static record TemperatureMaterialCondition() implements StaticMaterialCondition {
    public static TemperatureMaterialCondition instance = new TemperatureMaterialCondition();

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "biomeTemperaturePredicate", common_desc, false);
      return IFEQ;
    }
  }

  public static record SteepMaterialCondition() implements StaticMaterialCondition {
    public static SteepMaterialCondition instance = new SteepMaterialCondition();

    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "steepSlopePredicate", common_desc, false);
      return IFEQ;
    }
  }

  public static record WaterMaterialCondition(
      int offset, int surfaceDepthMultiplier, boolean addStoneDepth)
      implements StaticMaterialCondition {
    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {

      var retTrue = new Label();
      var retFalse = new Label();
      var finalL = new Label();

      loadFluidHeight(cw, mv);
      // mv.dup();
      mv.iconst(Integer.MIN_VALUE);
      mv.ificmpeq(retTrue);

      loadFluidHeight(cw, mv);
      if (offset != 0) {
        mv.iconst(offset);
        mv.add(Type.INT_TYPE);
      }
      if (surfaceDepthMultiplier != 0) {
        loadRunDepth(cw, mv);

        if (surfaceDepthMultiplier == 1) {
          mv.add(Type.INT_TYPE);
        } else if (surfaceDepthMultiplier == -1) {
          mv.sub(Type.INT_TYPE);
        } else {
          mv.iconst(surfaceDepthMultiplier);
          mv.mul(Type.INT_TYPE);
          mv.add(Type.INT_TYPE);
        }
      }

      loadY(cw, mv);
      if (addStoneDepth) {
        loadStoneDepthAbove(cw, mv);
        mv.add(Type.INT_TYPE);
      }

      mv.ificmpge(retFalse);

      mv.visitLabel(retTrue);
      mv.iconst(1);
      mv.goTo(finalL);

      mv.visitLabel(retFalse);
      mv.iconst(0);

      mv.visitLabel(finalL);

      return IFEQ;
    }
  }

  public static record SlopeCondition(int min, int max) implements StaticMaterialCondition {
    @Override
    public int emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      if (min == max) {
        mv.iconst(min);
        callFuncWithCtxAtEnd(cw, mv, "slopeConditionEquals", slopeCondDescAlt);
      } else if (min <= 0) {
        mv.iconst(max);
        callFuncWithCtxAtEnd(cw, mv, "slopeConditionBelow", slopeCondDescAlt);
      } else if (max == Integer.MAX_VALUE) {
        mv.iconst(min);
        callFuncWithCtxAtEnd(cw, mv, "slopeConditionAbove", slopeCondDescAlt);
      } else {
        mv.iconst(min);
        mv.iconst(max);
        callFuncWithCtxAtEnd(cw, mv, "slopeCondition", slopeCondDesc);
      }
      return IFEQ;
    }
  }
}
