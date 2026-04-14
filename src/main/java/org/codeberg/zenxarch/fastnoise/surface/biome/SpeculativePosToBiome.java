package org.codeberg.zenxarch.fastnoise.surface.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;

public final class SpeculativePosToBiome {

  public RegistryEntry<Biome>[][][] speculations;

  @SuppressWarnings("unchecked")
  public SpeculativePosToBiome(BiomeAccess biomeAccess, int startX, int startY, int startZ) {
    this.speculations = new RegistryEntry[5][5][5];
    fillArray(biomeAccess, startX, startY, startZ);
  }

  private void fillArray(BiomeAccess biomeAccess, int startX, int startY, int startZ) {
    @SuppressWarnings("unchecked")
    RegistryEntry<Biome>[][][] biomes = new RegistryEntry[6][6][6];

    int sx = BiomeCoords.fromBlock(startX) - 1;
    int sy = BiomeCoords.fromBlock(startY) - 1;
    int sz = BiomeCoords.fromBlock(startZ) - 1;

    for (int y = 0; y < 6; y++) {
      for (int z = 0; z < 6; z++) {
        for (int x = 0; x < 6; x++) {
          biomes[y][z][x] = biomeAccess.getBiomeForNoiseGen(sx + x, sy + y, sz + z);
        }
      }
    }

    for (int y = 0; y < 5; y++) {
      for (int z = 0; z < 5; z++) {
        for (int x = 0; x < 5; x++) {
          this.speculations[y][z][x] =
              (biomes[y][z][x] == biomes[y][z][x + 1]
                      && biomes[y][z][x] == biomes[y][z + 1][x]
                      && biomes[y][z][x] == biomes[y][z + 1][x + 1]
                      && biomes[y][z][x] == biomes[y + 1][z][x]
                      && biomes[y][z][x] == biomes[y + 1][z][x + 1]
                      && biomes[y][z][x] == biomes[y + 1][z + 1][x]
                      && biomes[y][z][x] == biomes[y + 1][z + 1][x + 1])
                  ? biomes[y][z][x]
                  : null;
        }
      }
    }
  }

  public RegistryEntry<Biome> getBiome(int localBiomeX, int localBiomeY, int localBiomeZ) {
    return speculations[localBiomeY][localBiomeZ][localBiomeX];
  }
}
