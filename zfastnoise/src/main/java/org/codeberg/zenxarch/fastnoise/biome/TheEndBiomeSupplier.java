package org.codeberg.zenxarch.fastnoise.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

public record TheEndBiomeSupplier(TheEndBiomeSource biomeSource, MultiNoiseSampler sampler)
    implements BiomeSupplier {

  @Override
  public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
    return this.biomeSource.getBiome(biomeX, biomeY, biomeZ, sampler);
  }
}
