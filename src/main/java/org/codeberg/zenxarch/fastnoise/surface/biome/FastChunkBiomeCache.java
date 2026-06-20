package org.codeberg.zenxarch.fastnoise.surface.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.SingularPalette;

public final class FastChunkBiomeCache {
  public final int[][] cache;

  public FastChunkBiomeCache(
      RegistryEntry<Biome>[] biomes,
      ChunkSection aa,
      ChunkSection ab,
      ChunkSection ac,
      ChunkSection ba,
      ChunkSection bb,
      ChunkSection bc,
      ChunkSection ca,
      ChunkSection cb,
      ChunkSection cc) {
    this.cache = new int[4][biomes.length + 1];

    var sections =
        new ChunkSection[] {
          aa, ab, ac,
          ba, bb, bc,
          ca, cb, cc
        };

    for (int y = 0; y < 4; y++) {
      fillArray(cache[y], y, biomes, sections);
    }
  }

  private void fillArray(
      int[] cache, int y, RegistryEntry<Biome>[] biomes, ChunkSection[] sections) {
    int[][] localBiomes = new int[6][6];

    for (int i = 0; i < 4; i++) {
      localBiomes[0][i + 1] = indexOf(biomes, getBiome(sections, i, y, -1));
      localBiomes[5][i + 1] = indexOf(biomes, getBiome(sections, i, y, 4));
      localBiomes[i + 1][0] = indexOf(biomes, getBiome(sections, -1, y, i));
      localBiomes[i + 1][5] = indexOf(biomes, getBiome(sections, 4, y, i));
    }

    localBiomes[0][0] = indexOf(biomes, getBiome(sections, -1, y, -1));
    localBiomes[0][5] = indexOf(biomes, getBiome(sections, 4, y, -1));
    localBiomes[5][0] = indexOf(biomes, getBiome(sections, -1, y, 4));
    localBiomes[5][5] = indexOf(biomes, getBiome(sections, 4, y, 4));

    int singleBiome = -1;
    if (((PalettedContainer<RegistryEntry<Biome>>) sections[4].biomeContainer).data.palette()
        instanceof SingularPalette<RegistryEntry<Biome>> single) {
      singleBiome = indexOf(biomes, single.entry);
    }

    for (int z = 0; z < 4; z++) {
      for (int x = 0; x < 4; x++) {
        localBiomes[z + 1][x + 1] =
            singleBiome == -1 ? indexOf(biomes, sections[4].getBiome(x, y, z)) : singleBiome;
      }
    }

    for (int z = 0; z <= 4; z++) {
      for (int x = 0; x <= 4; x++) {
        if (localBiomes[z][x] == localBiomes[z][x + 1]
            && localBiomes[z][x] == localBiomes[z + 1][x]
            && localBiomes[z][x] == localBiomes[z + 1][x + 1])
          cache[localBiomes[z][x] + 1] = setBit(cache[localBiomes[z][x] + 1], (x) + (z * 5));
      }
    }

    for (int i = 1; i < cache.length; i++) {
      cache[0] |= cache[i];
    }
  }

  private int setBit(int f, int idx) {
    return f | (0x1 << idx);
  }

  private int indexOf(RegistryEntry<Biome>[] biomes, RegistryEntry<Biome> biome) {
    for (int i = 0; i < biomes.length; i++) {
      if (biomes[i] == biome) return i;
    }
    throw new IllegalStateException("This cannot happen");
  }

  private RegistryEntry<Biome> getBiome(ChunkSection[] sections, int x, int y, int z) {
    int cx = (x >> 2) + 1;
    int cz = (z >> 2) + 1;
    var section = sections[cx + (cz * 3)];

    int lx = (x & 0x3);
    int lz = (z & 0x3);

    return section.getBiome(lx, y, lz);
  }
}
