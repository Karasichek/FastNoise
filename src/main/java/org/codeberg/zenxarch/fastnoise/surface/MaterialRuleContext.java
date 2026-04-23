package org.codeberg.zenxarch.fastnoise.surface;

import java.util.Set;
import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.codeberg.zenxarch.fastnoise.surface.biome.FastBiomeProvider;

public class MaterialRuleContext extends MaterialRules.MaterialRuleContext {

  public MaterialRuleContext(
      SurfaceBuilder surfaceBuilder,
      NoiseConfig noiseConfig,
      Chunk chunk,
      ChunkNoiseSampler chunkNoiseSampler,
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      HeightContext heightContext,
      final Set<RegistryEntry<Biome>> includedBiomes) {
    super(
        surfaceBuilder,
        noiseConfig,
        chunk,
        chunkNoiseSampler,
        mapPosToBiome(chunk, posToBiome),
        heightContext,
        includedBiomes);
  }

  private static Function<BlockPos, RegistryEntry<Biome>> mapPosToBiome(
      Chunk chunk, Function<BlockPos, RegistryEntry<Biome>> posToBiome) {
    if (FastNoiseConfig.OPTIMIZE_BIOME_ACCESS) return new FastBiomeProvider(chunk, posToBiome);
    return posToBiome;
  }

  @Override
  public void initHorizontalContext(int blockX, int blockZ) {
    super.initHorizontalContext(blockX, blockZ);
  }

  @Override
  public void initVerticalContext(
      int stoneDepthAbove, int stoneDepthBelow, int fluidHeight, int blockY) {
    super.initVerticalContext(stoneDepthAbove, stoneDepthBelow, fluidHeight, blockY);
  }

  @Override
  public int estimateSurfaceHeight() {
    return super.estimateSurfaceHeight();
  }
}
