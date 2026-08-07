package org.codeberg.zenxarch.fastnoise.surface.rules;

import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;

public record VanillaBiomePredicate(RegistryEntryList<Biome> biomes, MaterialRuleContext context)
    implements BooleanSupplier {

  @Override
  public boolean get() {
    return biomes.contains(context.getCurrentBiome());
  }
}
