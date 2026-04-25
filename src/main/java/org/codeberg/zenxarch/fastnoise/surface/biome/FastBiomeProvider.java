package org.codeberg.zenxarch.fastnoise.surface.biome;

import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.SingularPalette;

public final class FastBiomeProvider implements Function<BlockPos, RegistryEntry<Biome>> {

  private final RegistryEntry<Biome>[] singleBiomes;
  private final Function<BlockPos, RegistryEntry<Biome>> posToBiome;

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
  public RegistryEntry<Biome> apply(BlockPos pos) {
    var single = getSingleBiome(pos.getX(), pos.getY(), pos.getZ());
    if (single == null) {
      return this.posToBiome.apply(pos);
    }
    return single;
  }

  private RegistryEntry<Biome> getSingleBiome(int blockX, int blockY, int blockZ) {
    var withinBounds = withinBounds(blockX) && withinBounds(blockZ);

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

  private static boolean withinBounds(int i) {
    return ((0x1 << (i & 0xF)) & ~0b0011_1111_1111_1100) == 0;
  }
}
