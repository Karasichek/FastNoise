package org.codeberg.zenxarch.matcomp.mixin.perf.compiler;

import java.util.List;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.CompiledBlockStateRule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SequenceMaterialRule.class, priority = 2000)
public abstract class SequenceMaterialRuleMixin {
  @Shadow @Final private List<MaterialRule> sequence;

  @Overwrite
  public BlockStateRule apply(final MaterialRuleContext context) {
    return new CompiledBlockStateRule(
        context, context.heightContext, context.noiseConfig, (SequenceMaterialRule) (Object) this);
  }
}
