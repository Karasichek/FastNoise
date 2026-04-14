package org.codeberg.zenxarch.fastnoise.surface.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;

public final class FastBiomeProvider implements BiomeProvider {

  private RegistryEntry<Biome> biome;
  private int blockX;
  private int blockY;
  private int blockZ;

  private final SpeculativePosToBiome[] speculativePosToBiomes;
  private final BiomeAccess biomeAccess;

  private final BlockPos.Mutable mutable = new BlockPos.Mutable();
  private final int minY;

  public FastBiomeProvider(Chunk chunk, BiomeAccess biomeAccess) {

    this.biomeAccess = biomeAccess;
    this.minY = chunk.getBottomY();

    var sections = chunk.getSectionArray();
    this.speculativePosToBiomes = new SpeculativePosToBiome[sections.length];
  }

  @Override
  public RegistryEntry<Biome> get() {
    if (biome == null) biome = compute();
    return biome;
  }

  private RegistryEntry<Biome> compute() {
    var speculatedBiome = speculateBiome();
    if (speculatedBiome == null) {
      return this.biomeAccess.getBiome(mutable.set(blockX, blockY, blockZ));
    }
    return speculatedBiome;
  }

  private RegistryEntry<Biome> speculateBiome() {
    var y = blockY - minY;
    var ly = y & 0xF;
    var cy = y >> 4;

    if (this.speculativePosToBiomes[cy] == null) {
      this.speculativePosToBiomes[cy] =
          new SpeculativePosToBiome(
              biomeAccess, blockX - (blockX & 0xF), blockY - ly, blockZ - (blockZ & 0xF));
    }

    return this.speculativePosToBiomes[cy].getBiome(
        fromBlockToSpeculativePos(blockX),
        fromBlockToSpeculativePos(blockY),
        fromBlockToSpeculativePos(blockZ));
  }

  private int fromBlockToSpeculativePos(int pos) {
    return (((pos & 0xF) - 2) >> 2) + 1;
  }

  public void updateXZ(int blockX, int blockZ) {
    this.blockX = blockX;
    this.blockZ = blockZ;
  }

  public void updateY(int blockY) {
    this.blockY = blockY;
    this.biome = null;
  }
}
