package org.codeberg.zenxarch.fastnoise.surface.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.SingularPalette;

public class FastChunkBiomeCache {
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
    final var size = biomes.length + 1;
    this.cache = new int[4][size];
    long[][] beforeMerge = new long[4][biomes.length];
    accumulateCenter(bb, biomes, beforeMerge);
    accumulatexMCenter(ab, biomes, beforeMerge);
    accumulatexPCenter(cb, biomes, beforeMerge);
    accumulateCenterzM(ba, biomes, beforeMerge);
    accumulateCenterzP(bc, biomes, beforeMerge);

    accumulatexMzM(aa, biomes, beforeMerge);
    accumulatexMzP(ac, biomes, beforeMerge);
    accumulatexPzM(ca, biomes, beforeMerge);
    accumulatexPzP(cc, biomes, beforeMerge);

    for (int i = 0; i < 4; i++) {
      // assumed
      // this.cache[i][0] = 0;
      for (int j = 1; j < size; j++) {
        this.cache[i][0] |= (this.cache[i][j] = merge(beforeMerge[i][j - 1]));
      }
    }
  }

  private static int merge(long value) {
    final long mask = (0x1FL << 24) | (0x1FL << 18) | (0x1FL << 12) | (0x1FL << 6) | 0x1FL;
    long a = value & mask;
    long b = (value >> 1) & mask;
    long c = (value >> 6) & mask;
    long d = (value >> 7) & mask;
    long e = a & b & c & d;
    // gauranteed to be positive, only first 25 bits used
    return (int)
        ((e & 0x1FL)
            | ((e & (0x1FL << 6)) >> 1)
            | ((e & (0x1FL << 12)) >> 2)
            | ((e & (0x1FL << 18)) >> 3)
            | ((e & (0x1FL << 24)) >> 4));
  }

  private static final long SINGLE_MASK = singleMask();

  // 0 0
  private static void accumulateCenter(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    if (!accumulateSinglePalette(palette, biomes, cache, SINGLE_MASK)) {
      if (container.getElementBits() == 1) {
        accumulateBiome1bitCenter(palette, biomes, cache, container.data.storage().getData()[0]);
      } else accumulateBiomesCenter(palette, biomes, cache, container.data.storage());
    }
  }

  // -1 0
  private static void accumulatexMCenter(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = (0x1L << 6) | (0x1L << 12) | (0x1L << 18) | (0x1L << 24);

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesEdge(palette, biomes, cache, container.data.storage(), 3, 2, 0, 6);
    }
  }

  // 1 0
  private static void accumulatexPCenter(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = ((0x1L << 6) | (0x1L << 12) | (0x1L << 18) | (0x1L << 24)) << 5;

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesEdge(palette, biomes, cache, container.data.storage(), 0, 2, 5, 6);
    }
  }

  // 0 -1
  private static void accumulateCenterzM(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = 0b01110L;

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesEdge(palette, biomes, cache, container.data.storage(), 12, 0, 0, 1);
    }
  }

  // 0 1
  private static void accumulateCenterzP(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = 0b011110L << 24;

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesEdge(palette, biomes, cache, container.data.storage(), 0, 0, 30, 1);
    }
  }

  // -1 -1
  private static void accumulatexMzM(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = 0x1;

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesCorner(palette, biomes, cache, container.data.storage(), 15, 0);
    }
  }

  // 1 -1
  private static void accumulatexPzM(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = 0x1 << (5);

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesCorner(palette, biomes, cache, container.data.storage(), 12, 5);
    }
  }

  // -1 1
  private static void accumulatexMzP(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = 0x1 << 30;

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {

      accumulateBiomesCorner(palette, biomes, cache, container.data.storage(), 3, 30);
    }
  }

  // 1 1
  private static void accumulatexPzP(
      ChunkSection c, RegistryEntry<Biome>[] biomes, long[][] cache) {
    var container = (PalettedContainer<RegistryEntry<Biome>>) c.biomeContainer;
    var palette = container.data.palette();

    final long MASK = 0x1 << 35;

    if (!accumulateSinglePalette(palette, biomes, cache, MASK)) {
      accumulateBiomesCorner(palette, biomes, cache, container.data.storage(), 0, 35);
    }
  }

  private static boolean accumulateSinglePalette(
      Palette<RegistryEntry<Biome>> palette,
      RegistryEntry<Biome>[] biomes,
      long[][] cache,
      long mask) {
    if (palette instanceof SingularPalette<RegistryEntry<Biome>> single) {
      accumulateSinglePalette(single, biomes, cache, mask);
      return true;
    }
    return false;
  }

  private static void accumulateSinglePalette(
      SingularPalette<RegistryEntry<Biome>> palette,
      RegistryEntry<Biome>[] biomes,
      long[][] cache,
      long MASK) {
    for (int i = 0; i < biomes.length; i++) {
      if (biomes[i] == palette.entry) {
        cache[0][i] |= MASK;
        cache[1][i] |= MASK;
        cache[2][i] |= MASK;
        cache[3][i] |= MASK;
        return;
      }
    }
    throw new IllegalStateException(
        "Can't be possible this singular palette contains a biome, not part of includedBiomes");
  }

  private static void accumulateBiome1bitCenter(
      Palette<RegistryEntry<Biome>> palette,
      RegistryEntry<Biome>[] biomes,
      long[][] cache,
      final long storage) {
    // 4x4x4 -> 6x6x6
    final int zeroIdx = indexOf(biomes, palette.get(0));
    final int oneIdx = indexOf(biomes, palette.get(1));

    final long zeroStorage = ~storage;

    cache[0][zeroIdx] = accumulateBiome1bitCenter(zeroStorage, 0);
    cache[1][zeroIdx] = accumulateBiome1bitCenter(zeroStorage, 1);
    cache[2][zeroIdx] = accumulateBiome1bitCenter(zeroStorage, 2);
    cache[3][zeroIdx] = accumulateBiome1bitCenter(zeroStorage, 3);

    cache[0][oneIdx] = accumulateBiome1bitCenter(storage, 0);
    cache[1][oneIdx] = accumulateBiome1bitCenter(storage, 1);
    cache[2][oneIdx] = accumulateBiome1bitCenter(storage, 2);
    cache[3][oneIdx] = accumulateBiome1bitCenter(storage, 3);
  }

  private static long accumulateBiome1bitCenter(long value, int y) {
    return (((value >> (y << 4)) & 0b1111L) << 7)
        | (((value >> ((y << 4) | 4)) & 0b1111L) << 13)
        | (((value >> ((y << 4) | 8)) & 0b1111L) << 19)
        | (((value >> ((y << 4) | 12)) & 0b1111L) << 25);
  }

  private static void accumulateBiomesCenter(
      Palette<RegistryEntry<Biome>> palette,
      RegistryEntry<Biome>[] biomes,
      long[][] cache,
      PaletteStorage storage) {
    var mappings = mappingsOf(palette, biomes);

    for (int y = 0; y < 4; y++) {
      for (int z = 0; z < 4; z++) {
        for (int x = 0; x < 4; x++) {
          cache[y][mappings[storage.get(((y << 2) | z) << 2 | x)]] |=
              0x1L << (((z + 1) * 6) + (x + 1));
        }
      }
    }
  }

  private static void accumulateBiomesEdge(
      Palette<RegistryEntry<Biome>> palette,
      RegistryEntry<Biome>[] biomes,
      long[][] cache,
      PaletteStorage storage,
      int staticIdxInStorage,
      int dynamicShiftInStorage,
      int staticIdxInCache,
      int dynamicMultiplierInCache) {

    var mappings = mappingsOf(palette, biomes);

    for (int y = 0; y < 4; y++) {
      for (int dynamic = 0; dynamic < 4; dynamic++) {
        cache[y][
                mappings[
                    storage.get(
                        (y << 4) | staticIdxInStorage | (dynamic << dynamicShiftInStorage))]] |=
            0x1L << (staticIdxInCache + ((dynamic + 1) * dynamicMultiplierInCache));
      }
    }
  }

  private static void accumulateBiomesCorner(
      Palette<RegistryEntry<Biome>> palette,
      RegistryEntry<Biome>[] biomes,
      long[][] cache,
      PaletteStorage storage,
      int staticIdxInStorage,
      int staticShiftInCache) {

    var mappings = mappingsOf(palette, biomes);

    for (int y = 0; y < 4; y++) {
      cache[y][mappings[storage.get((y << 4) | staticIdxInStorage)]] |= 0x1L << staticShiftInCache;
    }
  }

  private static int[] mappingsOf(
      Palette<RegistryEntry<Biome>> palette, RegistryEntry<Biome>[] biomes) {
    var result = new int[palette.getSize()];
    for (int i = 0; i < result.length; i++) result[i] = indexOf(biomes, palette.get(i));
    return result;
  }

  private static int indexOf(RegistryEntry<Biome>[] biomes, RegistryEntry<Biome> biome) {
    for (int i = 0; i < biomes.length; i++) if (biomes[i] == biome) return i;
    throw new IllegalStateException("Can't be possible this biome is not part of includedBiomes");
  }

  private static long singleMask() {
    long result = 0L;
    for (int i = 0; i < 36; i++) {
      var x = i % 6;
      var z = i / 6;

      if (x == 0 || x == 5) continue;
      if (z == 0 || z == 5) continue;
      result |= 0x1L << i;
    }
    return result;
  }
}
