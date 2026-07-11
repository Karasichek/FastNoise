package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import java.util.List;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceBlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.SequenceMaterialRule;
import org.codeberg.zenxarch.fastnoise.surface.BiomePredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SequenceMaterialRule.class)
public abstract class SequenceMaterialRuleMixin {
  @Shadow @Final private List<MaterialRule> sequence;

  @Overwrite
  public BlockStateRule apply(final MaterialRuleContext context) {
    var size = sequence.size();
    if (size == 1) {
      return sequence.getFirst().apply(context);
    }

    var builder = ObjectArrayList.<BlockStateRule>of();

    for (int i = 0; i < size; i++) {
      var rule = sequence.get(i).apply(context);
      if (rule == BiomePredicate.ALWAYS_NULL) continue;
      builder.add(rule);
    }

    if (builder.isEmpty()) return BiomePredicate.ALWAYS_NULL;
    return new SequenceBlockStateRule(new ObjectImmutableList<BlockStateRule>(builder));
  }
}
