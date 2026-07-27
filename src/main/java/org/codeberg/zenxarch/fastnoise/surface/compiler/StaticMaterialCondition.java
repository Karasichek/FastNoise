package org.codeberg.zenxarch.fastnoise.surface.compiler;

import java.lang.invoke.MethodHandle;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.AboveYMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.StoneDepthMaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.VerticalGradientMaterialCondition;
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
              config.getOrCreateRandomDeriver(vertical.randomName()));
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
}
