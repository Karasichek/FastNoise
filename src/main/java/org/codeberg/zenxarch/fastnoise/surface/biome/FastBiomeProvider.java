package org.codeberg.zenxarch.fastnoise.surface.biome;

import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.SingularPalette;

public final class FastBiomeProvider implements BiomeProvider {

  private RegistryEntry<Biome> biome;
  private boolean withinBounds;
  private int blockX;
  private int blockY;
  private int blockZ;

  private final RegistryEntry<Biome>[] singleBiomes;
  private final Function<BlockPos, RegistryEntry<Biome>> posToBiome;

  private final BlockPos.Mutable mutable = new BlockPos.Mutable();
  private final int minY;

  @SuppressWarnings("unchecked")
  public FastBiomeProvider(Chunk chunk, Function<BlockPos, RegistryEntry<Biome>> posToBiome) {

    this.posToBiome = posToBiome;
    this.minY = chunk.getBottomY();

    var sections = chunk.getSectionArray();

    this.singleBiomes = new RegistryEntry[sections.length];
    for (int i = 0; i < sections.length; i++) {
      var container = (PalettedContainer<RegistryEntry<Biome>>) sections[i].biomeContainer;
      if (container.data.palette() instanceof SingularPalette<RegistryEntry<Biome>> single) {
        singleBiomes[i] = single.entry;
      } else {
        singleBiomes[i] = null;
      }
    }
  }

  @Override
  public RegistryEntry<Biome> get() {
    if (biome == null) biome = compute();
    return biome;
  }

  private RegistryEntry<Biome> compute() {
    var single = getSingleBiome();
    if (single == null) {
      return this.posToBiome.apply(mutable.set(blockX, blockY, blockZ));
    }
    return single;
  }

  private RegistryEntry<Biome> getSingleBiome() {
    if (!withinBounds || !withinBounds(blockY)) {
      return null;
    }

    var y = blockY - minY;
    var ly = y & 0xF;
    var cy = y >> 4;

    var single = singleBiomes[cy];

    if (single == null) return null;

    if (ly < 2) {
      if (cy == 0) return null;
      if (singleBiomes[cy] != singleBiomes[cy - 1]) return null;
    }

    if (ly > 13) {
      if (cy == (this.singleBiomes.length - 1)) return null;
      if (singleBiomes[cy] != singleBiomes[cy + 1]) return null;
    }

    return single;
  }

  public void updateXZ(int blockX, int blockZ) {
    withinBounds = withinBounds(blockX) && withinBounds(blockZ);
    this.blockX = blockX;
    this.blockZ = blockZ;
  }

  public void updateY(int blockY) {
    this.blockY = blockY;
    this.biome = null;
  }

  private boolean withinBounds(int i) {
    return ((0x1 << (i & 0xF)) & ~0b0011_1111_1111_1100) == 0;
  }
}
