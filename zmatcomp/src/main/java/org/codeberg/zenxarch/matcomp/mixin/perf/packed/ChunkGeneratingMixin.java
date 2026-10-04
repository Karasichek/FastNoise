package org.codeberg.zenxarch.matcomp.mixin.perf.packed;

import java.util.Set;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.codeberg.zenxarch.matcomp.biome.FastPosToBiome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NoiseChunkGenerator.class)
public abstract class ChunkGeneratingMixin {
  @Overwrite
  private static Set<RegistryEntry<Biome>> getBiomesAround(
      final ChunkRegion region, final int radiusChunks) {
    return FastPosToBiome.getBiomes(region, radiusChunks);
  }
}
