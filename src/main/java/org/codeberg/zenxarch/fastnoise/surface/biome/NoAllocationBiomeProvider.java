package org.codeberg.zenxarch.fastnoise.surface.biome;

import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public final class NoAllocationBiomeProvider implements BiomeProvider {

  private RegistryEntry<Biome> biome;
  private int blockX;
  private int blockY;
  private int blockZ;

  private final Function<BlockPos, RegistryEntry<Biome>> posToBiome;

  private final BlockPos.Mutable mutable = new BlockPos.Mutable();

  public NoAllocationBiomeProvider(Function<BlockPos, RegistryEntry<Biome>> posToBiome) {
    this.posToBiome = posToBiome;
  }

  @Override
  public RegistryEntry<Biome> get() {
    if (this.biome == null) this.biome = compute();
    return this.biome;
  }

  private RegistryEntry<Biome> compute() {
    this.mutable.set(blockX, blockY, blockZ);
    return this.posToBiome.apply(mutable);
  }

  @Override
  public void updateXZ(int blockX, int blockZ) {
    this.blockX = blockX;
    this.blockZ = blockZ;
  }

  @Override
  public void updateY(int blockY) {
    this.blockY = blockY;
    this.biome = null;
  }
}
