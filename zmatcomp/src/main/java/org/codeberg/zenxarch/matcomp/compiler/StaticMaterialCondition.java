package org.codeberg.zenxarch.matcomp.compiler;

import static org.objectweb.asm.Opcodes.IXOR;

import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.AboveYMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition.RegisteredCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.NegativeRunDepthPredicate;
import net.minecraft.world.gen.surfacebuilder.NotMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.SteepSlopePredicate;
import net.minecraft.world.gen.surfacebuilder.StoneDepthMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.VerticalGradientMaterialCondition;
import org.codeberg.zenxarch.matcomp.compiler.cw.MaterialRuleClassWriter;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public interface StaticMaterialCondition {
  public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv);

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
      case RegisteredCondition reg -> tryCreate(reg.holder().value(), context, config);
      case NotMaterialCondition not -> {
        var original = tryCreate(not.target(), context, config);
        if (original == null) yield null;
        if (original instanceof InvertedCondition inverted) yield inverted.original();
        yield new InvertedCondition(original);
      }
      case NegativeRunDepthPredicate _ -> HoleMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.SurfaceMaterialCondition _ ->
          SurfaceMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.TemperatureMaterialCondition _ ->
          TemperatureMaterialCondition.instance;
      case SteepSlopePredicate _ -> SteepMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.WaterMaterialCondition water ->
          new WaterMaterialCondition(
              water.offset(), water.surfaceDepthMultiplier(), water.addStoneDepth());
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

  public static final String aboveY =
      getDescriptorForCondition(int.class, int.class, boolean.class);
  public static final String stoneDepth =
      getDescriptorForCondition(int.class, boolean.class, int.class, boolean.class);
  public static final String xoroVerticalGradient =
      getDescriptorForCondition(int.class, int.class, long.class, long.class);
  public static final String checkedVerticalGradient =
      getDescriptorForCondition(int.class, int.class, long.class);
  public static final String water = getDescriptorForCondition(int.class, int.class, boolean.class);
  public static final String common_desc = getDescriptorForCondition();

  public static record AboveYCondition(int y, int surfaceDepthMultiplier, boolean addStoneDepth)
      implements StaticMaterialCondition {

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(y);
      mv.iconst(surfaceDepthMultiplier);
      mv.iconst(addStoneDepth ? 1 : 0);
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "aboveY", aboveY, false);
    }
  }

  public static record StoneDepthCondition(
      int offset, boolean addSurfaceDepth, int secondaryDepthRange, boolean ceiling)
      implements StaticMaterialCondition {

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(offset);
      mv.iconst(addSurfaceDepth ? 1 : 0);
      mv.iconst(secondaryDepthRange);
      mv.iconst(ceiling ? 1 : 0);
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "stoneDepth", stoneDepth, false);
    }
  }

  public static record VerticalGradientConditionXoroshiro(
      int trueAtAndBelow, int falseAtAndAbove, long seedLo, long seedHi)
      implements StaticMaterialCondition {

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(trueAtAndBelow);
      mv.iconst(falseAtAndAbove);
      mv.lconst(seedLo);
      mv.lconst(seedHi);
      cw.loadCtx(mv);
      mv.invokestatic(
          MATERIALRULECOMPILER_NAME, "xoroVerticalGradient", xoroVerticalGradient, false);
    }
  }

  public static record VerticalGradientConditionChecked(
      int trueAtAndBelow, int falseAtAndAbove, long seed) implements StaticMaterialCondition {
    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(trueAtAndBelow);
      mv.iconst(falseAtAndAbove);
      mv.lconst(seed);
      cw.loadCtx(mv);
      mv.invokestatic(
          MATERIALRULECOMPILER_NAME, "checkedVerticalGradient", checkedVerticalGradient, false);
    }
  }

  public static final String STATIC_MATERIAL_CONDITION_INTERNAL_NAME =
      Type.getInternalName(StaticMaterialCondition.class);

  public static record InvertedCondition(StaticMaterialCondition original)
      implements StaticMaterialCondition {

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      original.emitBytecode(cw, mv);
      mv.iconst(-1);
      mv.visitInsn(IXOR);
    }
  }

  public static record HoleMaterialCondition() implements StaticMaterialCondition {
    public static HoleMaterialCondition instance = new HoleMaterialCondition();

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "negativeRunDepth", common_desc, false);
    }
  }

  public static record SurfaceMaterialCondition() implements StaticMaterialCondition {
    public static SurfaceMaterialCondition instance = new SurfaceMaterialCondition();

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "surfacePredicate", common_desc, false);
    }
  }

  public static record TemperatureMaterialCondition() implements StaticMaterialCondition {
    public static TemperatureMaterialCondition instance = new TemperatureMaterialCondition();

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "biomeTemperaturePredicate", common_desc, false);
    }
  }

  public static record SteepMaterialCondition() implements StaticMaterialCondition {
    public static SteepMaterialCondition instance = new SteepMaterialCondition();

    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "steepSlopePredicate", common_desc, false);
    }
  }

  public static record WaterMaterialCondition(
      int offset, int surfaceDepthMultiplier, boolean addStoneDepth)
      implements StaticMaterialCondition {
    @Override
    public void emitBytecode(MaterialRuleClassWriter cw, InstructionAdapter mv) {
      mv.iconst(offset);
      mv.iconst(surfaceDepthMultiplier);
      mv.iconst(addStoneDepth ? 1 : 0);
      cw.loadCtx(mv);
      mv.invokestatic(MATERIALRULECOMPILER_NAME, "water", water, false);
    }
  }
}
