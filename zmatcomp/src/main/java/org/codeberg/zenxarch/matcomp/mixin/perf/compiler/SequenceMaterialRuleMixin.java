package org.codeberg.zenxarch.matcomp.mixin.perf.compiler;

import java.util.List;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SequenceMaterialRule;
import org.codeberg.zenxarch.matcomp.compiler.CompiledMaterialRule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SequenceMaterialRule.class, priority = 2000)
public abstract class SequenceMaterialRuleMixin {
  @Shadow @Final private List<MaterialRule> sequence;

  @Overwrite
  public BlockStateRule getBlockStateRule(final MaterialRuleContext context) {
    return new CompiledMaterialRule((SequenceMaterialRule) (Object) this)
        .getBlockStateRule(context);
  }
}
