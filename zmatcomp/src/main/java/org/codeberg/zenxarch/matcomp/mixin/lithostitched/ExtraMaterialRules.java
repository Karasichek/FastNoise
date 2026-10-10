package org.codeberg.zenxarch.matcomp.mixin.lithostitched;

import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedConditions;
import org.codeberg.zenxarch.matcomp.rules.LithostitchedRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

public final class ExtraMaterialRules {
  private ExtraMaterialRules() {}

  @Pseudo
  @Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.surface.condition.BiomeCondition")
  public abstract static class LithostitchedBiomeConditionMixin
      implements LithostitchedConditions.LithostitchedBiomeCondition {
    @Overwrite
    public BooleanSupplier apply(MaterialRuleContext ctx) {
      return BiomePredicate.getSupplier(biomes().stream().map(v -> v.getKey().get()).toList(), ctx);
    }
  }

  @Pseudo
  @Mixin(
      targets =
          "dev.worldgen.lithostitched.impl.worldgen.surface.condition.internal.TagFilledCondition")
  public abstract static class TagFilledConditionMixin implements MaterialCondition {
    @Shadow
    public abstract RegistryEntryList<MaterialRule> rules();

    @Overwrite
    public BooleanSupplier apply(MaterialRuleContext ctx) {
      return rules().size() > 0 ? BiomePredicate.ALWAYS_TRUE : BiomePredicate.ALWAYS_FALSE;
    }
  }

  @Pseudo
  @Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.surface.rule.ReferenceRule")
  public abstract static class ReferenceRuleMixin implements LithostitchedRules.ReferenceRule {}
}
