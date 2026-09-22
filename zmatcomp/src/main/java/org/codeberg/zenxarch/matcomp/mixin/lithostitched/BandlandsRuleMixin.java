package org.codeberg.zenxarch.matcomp.mixin.lithostitched;

import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules.BandlandsRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.material.rule.BandlandsRule")
public abstract class BandlandsRuleMixin implements BandlandsRule {
  @Overwrite
  public BlockStateRule getBlockStateRule(MaterialRuleContext context) {
    return LithostitchedRules.compile(this, context);
  }
}
