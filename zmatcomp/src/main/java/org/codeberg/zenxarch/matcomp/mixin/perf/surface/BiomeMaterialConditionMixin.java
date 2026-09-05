package org.codeberg.zenxarch.matcomp.mixin.perf.surface;

import java.util.Set;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BiomeMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeMaterialRule.class)
public abstract class BiomeMaterialConditionMixin {
  @Shadow
  abstract boolean isDisjoint(final Set<RegistryEntry<Biome>> contextBiomes);

  @Shadow
  abstract boolean isSuperset(final Set<RegistryEntry<Biome>> contextBiomes);

  @Overwrite
  public BooleanSupplier apply(final MaterialRuleContext context) {
    return BiomePredicate.getSupplier(((BiomeMaterialRule) (Object) this).biomes(), context);
  }
}
