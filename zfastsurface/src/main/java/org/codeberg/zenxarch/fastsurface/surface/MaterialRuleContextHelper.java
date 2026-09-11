package org.codeberg.zenxarch.fastsurface.surface;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public class MaterialRuleContextHelper {

  public static void initHorizontalContext(
      MaterialRuleContext context, final int blockX, final int blockZ) {
    context.initHorizontalContext(blockX, blockZ);
    context.stoneDepthAbove = 0;
    context.fluidHeight = Integer.MIN_VALUE;
  }

  public static void air(MaterialRuleContext context) {
    context.stoneDepthAbove = 0;
    context.fluidHeight = Integer.MIN_VALUE;
  }

  public static void water(MaterialRuleContext context, final int y) {
    if (context.fluidHeight == Integer.MIN_VALUE) context.fluidHeight = y + 1;
  }

  public static void oreOrStone(
      MaterialRuleContext context, final int y, final int stoneDepthBelow) {
    context.stoneDepthAbove++;
    context.initVerticalContext(context.stoneDepthAbove, stoneDepthBelow, context.fluidHeight, y);
  }
}
