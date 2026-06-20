package org.codeberg.zenxarch.fastnoise.surface.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public final class FastBiomeCache {
  private final Chunk[] chunks;
  private final FastChunkBiomeCache[] cache;
  private final RegistryEntry<Biome>[] biomes;

  private final int minYInBiomes;
  private final int maxYInBiomes;
  private final int heightInBiomes;

  public FastBiomeCache(RegistryEntry<Biome>[] biomes, final Chunk[] chunks) {
    this.chunks = chunks;
    this.biomes = biomes;

    // get height params from center chunk to avoid retrogen stuff
    var heightView = chunks[4].getHeightLimitView();

    this.minYInBiomes = heightView.getBottomY() >> 2;

    this.cache = new FastChunkBiomeCache[chunks[4].getSectionArray().length];

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

  private int[] getCacheForY(int by) {
    final var cby = MathHelper.clamp(by, minYInBiomes, maxYInBiomes);
    final var lby = cby & 0x3;
    return getForSectionY(cby).cache[lby];
  }

  private FastChunkBiomeCache getForSectionY(int biomeY) {
    int secY = (biomeY - minYInBiomes) >> 2;
    var cached = this.cache[secY];
    if (cached == null) {
      return (this.cache[secY] =
          new FastChunkBiomeCache(
              biomes,
              this.chunks[0].getSectionArray()[secY],
              this.chunks[1].getSectionArray()[secY],
              this.chunks[2].getSectionArray()[secY],
              this.chunks[3].getSectionArray()[secY],
              this.chunks[4].getSectionArray()[secY],
              this.chunks[5].getSectionArray()[secY],
              this.chunks[6].getSectionArray()[secY],
              this.chunks[7].getSectionArray()[secY],
              this.chunks[8].getSectionArray()[secY]));
    }
    return cached;
  }
}
