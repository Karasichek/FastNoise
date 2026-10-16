package org.codeberg.zenxarch.matcomp.rules;

import java.util.List;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;

public record VanillaBiomePredicate(List<RegistryKey<Biome>> biomes, MaterialRuleContext context)
    implements BooleanSupplier {

  @Override
  public boolean get() {
    return biomes.contains(
        ((MaterialRuleContextExtension) context).zenxarch$getCurrentBiome().getKey().get());
  }
}
