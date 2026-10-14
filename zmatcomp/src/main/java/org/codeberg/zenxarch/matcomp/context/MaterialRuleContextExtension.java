package org.codeberg.zenxarch.matcomp.context;

import java.util.Set;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

public interface MaterialRuleContextExtension {
  public Set<RegistryEntry<Biome>> zenxarch$getIncludedBiomes();

  public void zenxarch$setIncludedBiomes(Set<RegistryEntry<Biome>> biomes);

  public RegistryEntry<Biome> zenxarch$getCurrentBiome();

  public Registry<Biome> zenxarch$getRegistry();
}
