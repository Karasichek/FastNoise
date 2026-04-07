package org.codeberg.zenxarch.fastnoise.surface.biome;

import java.util.function.Supplier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

public interface BiomeProvider extends Supplier<RegistryEntry<Biome>> {
  public void updateXZ(int blockX, int blockZ);

  public void updateY(int blockY);
}
