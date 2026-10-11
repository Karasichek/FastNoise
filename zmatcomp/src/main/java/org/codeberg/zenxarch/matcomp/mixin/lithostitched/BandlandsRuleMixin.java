package org.codeberg.zenxarch.matcomp.mixin.lithostitched;

import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules.Bandlands;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules.BandlandsRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.surface.rule.BandlandsRule")
public abstract class BandlandsRuleMixin implements BandlandsRule {
  @Overwrite
  public BlockStateRule apply(MaterialRuleContext context) {
    return LithostitchedRules.compile(this, context);
  }

  @Pseudo
  @Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.bandlands.Bandlands")
  public static abstract class BandlandsMixin implements Bandlands {}
}
