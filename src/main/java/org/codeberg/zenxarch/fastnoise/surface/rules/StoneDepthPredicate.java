package org.codeberg.zenxarch.fastnoise.surface.rules;

import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.StoneDepthMaterialCondition;

/***
 *
 * StoneDepthPredicate
 * @param offset 1 + offset
 * @param addSurfaceDepth
 * @param secondaryDepthRange
 * @param ceiling surfaceType == VerticalSurfaceType.CEILING
 * @param context
 */
public record StoneDepthPredicate(
    int offset,
    boolean addSurfaceDepth,
    int secondaryDepthRange,
    boolean ceiling,
    MaterialRuleContext context)
    implements BooleanSupplier {

  public static StoneDepthPredicate create(
      StoneDepthMaterialCondition condition, MaterialRuleContext context) {
    return new StoneDepthPredicate(
        condition.offset() + 1,
        condition.addSurfaceDepth(),
        condition.secondaryDepthRange(),
        condition.surfaceType() == VerticalSurfaceType.CEILING,
        context);
  }

  @Override
  public boolean get() {
    var stoneDepth = ceiling ? context.stoneDepthBelow : context.stoneDepthAbove;
    if (addSurfaceDepth) stoneDepth -= context.runDepth;
    if (secondaryDepthRange != 0)
      stoneDepth -= (int) ((context.getSecondaryDepth() + 1.0) * 0.5 * secondaryDepthRange);

    return stoneDepth <= offset;
  }
}
