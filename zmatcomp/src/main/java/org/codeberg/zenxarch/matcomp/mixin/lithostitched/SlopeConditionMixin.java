package org.codeberg.zenxarch.matcomp.mixin.lithostitched;

import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedConditions;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedConditions.SlopeCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.surface.condition.SlopeCondition")
public abstract class SlopeConditionMixin implements SlopeCondition {
  @Overwrite
  public BooleanSupplier apply(MaterialRuleContext context) {
    return LithostitchedConditions.compileSlopeCondition(this.threshold(), context);
  }
}
