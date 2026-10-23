package org.codeberg.zenxarch.matcomp.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public final class FastBiomeCache {
  private final ChunkSection[][] sections;
  private final int[][][] cache;
  private final RegistryEntry<Biome>[] biomes;

  private final int minYInBiomes;
  private final int maxYInBiomes;
  private final int heightInBiomes;
  private final int[][] ALWAYS_NULL;

  public FastBiomeCache(RegistryEntry<Biome>[] biomes, final Chunk[] chunks) {
    this.sections = new ChunkSection[9][];
    for (int i = 0; i < 9; i++) this.sections[i] = chunks[i].getSectionArray();
    this.biomes = biomes;
    this.ALWAYS_NULL = new int[4][biomes.length + 1];

    // get height params from center chunk to avoid retrogen stuff
    var heightView = chunks[4].getHeightLimitView();

    this.minYInBiomes = heightView.getBottomY() >> 2;

    this.cache = new int[chunks[4].getSectionArray().length][][];

    this.heightInBiomes = this.cache.length << 2;
    this.maxYInBiomes = this.minYInBiomes + this.heightInBiomes - 1;
  }

  private static int getBit(int bits, int bitIdx) {
    return (bits >> bitIdx) & 0x1;
  }

  private static int findIdx(int[] cache, int bitIdx) {
    for (int i = 1; i < cache.length; i++) if (getBit(cache[i], bitIdx) == 0x1) return i;
    return 0;
  }

  private static int getBitIdx(int blockX, int blockZ) {
    final int mbx = ((blockX & 0xF) - 2) >> 2;
    final int mbz = ((blockZ & 0xF) - 2) >> 2;
    return (mbx + 1) + ((mbz + 1) * 5);
  }

  public RegistryEntry<Biome> getBiomeOrNull(int blockX, int blockY, int blockZ) {
    final int mby = (blockY - 2) >> 2;
    final var bitIdx = getBitIdx(blockX, blockZ);

    return getBiomeOrNull(mby, bitIdx);
  }

  private RegistryEntry<Biome> getBiomeOrNull(int minBiomeY, int bitIdx) {
    final var lowerCache = getCacheForY(minBiomeY);

    if (getBit(lowerCache[0], bitIdx) == 0x0) return null;

    final var upperCache = getCacheForY(minBiomeY + 1);

    // too much work, leaving here b/c I like symmetry
    // if (getBit(upperCache[0], bitIdx) == 0x0) return null;
    if (getBit(lowerCache[0] & upperCache[0], bitIdx) == 0x0) return null;

    int idxInUpper;
    if ((idxInUpper = findIdx(upperCache, bitIdx)) == 0) return null;

    if (getBit(upperCache[idxInUpper] & lowerCache[idxInUpper], bitIdx) == 0x0) return null;

    return this.biomes[idxInUpper - 1];
  }

  public int getBiomeIdxOrNull(int blockX, int blockY, int blockZ) {
    final int mby = (blockY - 2) >> 2;
    final var bitIdx = getBitIdx(blockX, blockZ);

    return getBiomeIdxOrNull(mby, bitIdx);
  }

  private int getBiomeIdxOrNull(int minBiomeY, int bitIdx) {
    final var lowerCache = getCacheForY(minBiomeY);

    if (getBit(lowerCache[0], bitIdx) == 0x0) return -1;

    final var upperCache = getCacheForY(minBiomeY + 1);

    // too much work, leaving here b/c I like symmetry
    // if (getBit(upperCache[0], bitIdx) == 0x0) return null;
    if (getBit(lowerCache[0] & upperCache[0], bitIdx) == 0x0) return -1;

    int idxInUpper;
    if ((idxInUpper = findIdx(upperCache, bitIdx)) == 0) return -1;

    if (getBit(upperCache[idxInUpper] & lowerCache[idxInUpper], bitIdx) == 0x0) return -1;

    return idxInUpper - 1;
  }

  private int[] getCacheForY(int by) {
    final var cby = MathHelper.clamp(by, minYInBiomes, maxYInBiomes);
    final var lby = cby & 0x3;
    return getForSectionY(cby)[lby];
  }

  private int[][] getForSectionY(int biomeY) {
    int secY = (biomeY - minYInBiomes) >> 2;
    var cached = this.cache[secY];
    if (cached == null) {
      return (this.cache[secY] = this.getForSectionYUncached(secY));
    }
    return cached;
  }

  private int[][] getForSectionYUncached(int secY) {
    try {
      return FastChunkBiomeCache.createCache(
          biomes,
          this.sections[0][secY],
          this.sections[1][secY],
          this.sections[2][secY],
          this.sections[3][secY],
          this.sections[4][secY],
          this.sections[5][secY],
          this.sections[6][secY],
          this.sections[7][secY],
          this.sections[8][secY]);
    } catch (Exception e) {
      return ALWAYS_NULL;
    }
  }
}
