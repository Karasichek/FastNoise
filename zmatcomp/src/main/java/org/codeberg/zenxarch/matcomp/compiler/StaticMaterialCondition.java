package org.codeberg.zenxarch.matcomp.compiler;

import java.lang.invoke.MethodHandle;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.AboveYMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition.RegisteredCondition;
import net.minecraft.world.gen.surfacebuilder.NegativeRunDepthPredicate;
import net.minecraft.world.gen.surfacebuilder.NotMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.SteepSlopePredicate;
import net.minecraft.world.gen.surfacebuilder.StoneDepthMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.VerticalGradientMaterialCondition;
import org.jspecify.annotations.Nullable;

public interface StaticMaterialCondition {
  MethodHandle compiled();

  @Nullable
  public static StaticMaterialCondition tryCreate(
      MaterialCondition condition, HeightContext context, NoiseConfig config) {
    return switch (condition) {
      case VerticalGradientMaterialCondition vertical ->
          new VerticalGradientCondition(
              vertical.trueAtAndBelow().getY(context),
              vertical.falseAtAndAbove().getY(context),
              config.getOrCreateRandomSplitter(vertical.randomName()));
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
      case NotMaterialCondition not -> {
        var original = tryCreate(not.target(), context, config);
        if (original == null) yield null;
        yield new InvertedCondition(original);
      }
      case NegativeRunDepthPredicate _ -> HoleMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.SurfaceMaterialCondition _ ->
          SurfaceMaterialCondition.instance;
      case net.minecraft.world.gen.surfacebuilder.TemperatureMaterialCondition _ ->
          TemperatureMaterialCondition.instance;
      case SteepSlopePredicate _ -> SteepMaterialCondition.instance;
      default -> null;
    };
  }

  public static record AboveYCondition(int y, int surfaceDepthMultiplier, boolean addStoneDepth)
      implements StaticMaterialCondition {
    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.createAboveY(y, surfaceDepthMultiplier, addStoneDepth);
    }
  }

  public static record StoneDepthCondition(
      int offset, boolean addSurfaceDepth, int secondaryDepthRange, boolean ceiling)
      implements StaticMaterialCondition {
    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.createStoneDepthTest(
          offset, addSurfaceDepth, secondaryDepthRange, ceiling);
    }
  }

  public static record VerticalGradientCondition(
      int trueAtAndBelow, int falseAtAndAbove, RandomSplitter splitter)
      implements StaticMaterialCondition {
    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.createVerticalGradientTest(
          trueAtAndBelow, falseAtAndAbove, splitter);
    }
  }

  public static record InvertedCondition(StaticMaterialCondition original)
      implements StaticMaterialCondition {
    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.createInverted(original.compiled());
    }
  }

  public static record HoleMaterialCondition() implements StaticMaterialCondition {
    public static HoleMaterialCondition instance = new HoleMaterialCondition();

    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.negativeRunDepth;
    }
  }

  public static record SurfaceMaterialCondition() implements StaticMaterialCondition {
    public static SurfaceMaterialCondition instance = new SurfaceMaterialCondition();

    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.surfacePredicate;
    }
  }

  public static record TemperatureMaterialCondition() implements StaticMaterialCondition {
    public static TemperatureMaterialCondition instance = new TemperatureMaterialCondition();

    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.biomeTemperaturePredicate;
    }
  }

  public static record SteepMaterialCondition() implements StaticMaterialCondition {
    public static SteepMaterialCondition instance = new SteepMaterialCondition();

    @Override
    public MethodHandle compiled() {
      return MaterialRuleCompiler.steepSlopePredicate;
    }
  }
}
