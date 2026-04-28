package org.codeberg.zenxarch.fastnoise.surface.biome;

import java.util.Set;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public final class FastBiomeCache {
  private final Chunk[] chunks;
  private final FastChunkBiomeCache[] cache;
  private final RegistryEntry<Biome>[] biomes;

  private final int minYInBiomes;
  private final int maxYInBiomes;
  private final int heightInBiomes;

  // b/c we can't be using generics for arrays, why java why
  @SuppressWarnings("unchecked")
  public FastBiomeCache(Set<RegistryEntry<Biome>> biomes, ChunkRegion region) {
    final var centerPos = region.getCenterPos();
    final var cx = centerPos.x();
    final var cz = centerPos.z();
    final var chunks = new Chunk[9];

    for (int z = -1; z <= 1; z++) {
      for (int x = -1; x <= 1; x++) {
        final var idx = ((z + 1) * 3) + (x + 1);
        chunks[idx] = region.getChunk(cx + x, cz + z);
      }
    }

    var biomeArr = new RegistryEntry[biomes.size()];
    int idx = 0;
    for (var biome : biomes) biomeArr[idx++] = biome;

    this((RegistryEntry<Biome>[]) biomeArr, chunks);
  }

  private FastBiomeCache(RegistryEntry<Biome>[] biomes, Chunk[] chunks) {
    this.chunks = chunks;
    this.biomes = biomes;

    // get height params from center chunk to avoid retrogen stuff
    var heightView = chunks[4].getHeightLimitView();

    this.minYInBiomes = heightView.getBottomY() >> 4;

    this.cache = new FastChunkBiomeCache[chunks[4].getSectionArray().length];

    this.heightInBiomes = this.cache.length << 2;
    this.maxYInBiomes = this.minYInBiomes + this.heightInBiomes;
  }

  public RegistryEntry<Biome> getBiomeOrNull(int blockX, int blockY, int blockZ) {
    final var bitIdx = ((blockX + 2) >> 2) + (((blockZ + 2) >> 2) * 5);
    final var biomeY = (blockY + 2) >> 2;

    final var lowerBiomeY = MathHelper.clamp(biomeY, minYInBiomes, maxYInBiomes);
    final var localLowerBiomeY = lowerBiomeY & 0x3;
    final var lowerCache = getForSectionY(lowerBiomeY).cache[localLowerBiomeY];

    if (((lowerCache[0] >> bitIdx) & 0x1) == 0) return null;

    final var upperBiomeY = MathHelper.clamp(biomeY + 1, minYInBiomes, maxYInBiomes);
    final var localUpperBiomeY = upperBiomeY & 0x3;
    final var upperCache = getForSectionY(upperBiomeY).cache[localUpperBiomeY];

    // too much work, leaving here b/c I like symmetry
    // if (((upperCache[0] >> bitIdx) & 0x1) == 0) return null;
    if ((((upperCache[0] & lowerCache[0]) >> bitIdx) & 0x1) == 0) return null;

    var idxInUpper = 0;
    for (int i = 1; i < upperCache.length; i++) {
      if (((upperCache[i] >> bitIdx) & 0x1) == 0x1) {
        idxInUpper = i;
        break;
      }
    }
    if (idxInUpper == 0) return null;

    if ((((upperCache[idxInUpper] & lowerCache[idxInUpper]) >> bitIdx) & 0x1) == 0) return null;
    return this.biomes[idxInUpper - 1];
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
