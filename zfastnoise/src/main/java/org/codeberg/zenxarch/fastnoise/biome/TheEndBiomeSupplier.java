package org.codeberg.zenxarch.fastnoise.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

public record TheEndBiomeSupplier(TheEndBiomeSource biomeSource, MultiNoiseSampler sampler)
    implements BiomeSupplier {

  @Override
  public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler sampler) {
    return this.biomeSource.getBiome(x, y, z, sampler);
  }
}
