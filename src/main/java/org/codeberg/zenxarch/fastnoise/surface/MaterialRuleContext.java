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
import org.codeberg.zenxarch.fastnoise.surface.biome.FastBiomeCache;

public class MaterialRuleContext extends MaterialRules.MaterialRuleContext {

  public MaterialRuleContext(
      SurfaceBuilder surfaceBuilder,
      NoiseConfig noiseConfig,
      Chunk chunk,
      ChunkNoiseSampler chunkNoiseSampler,
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      HeightContext heightContext,
      final Set<RegistryEntry<Biome>> includedBiomes,
      final Chunk[] chunks) {
    super(
        surfaceBuilder,
        noiseConfig,
        chunk,
        chunkNoiseSampler,
        posToBiome,
        heightContext,
        includedBiomes);
  }

  public static Function<BlockPos, RegistryEntry<Biome>> mapPosToBiome(
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      Set<RegistryEntry<Biome>> includedBiomes,
      final Chunk[] chunks) {
    if (FastNoiseConfig.OPTIMIZE_BIOME_ACCESS) {
      if (includedBiomes.size() == 1) {
        for (var biome : includedBiomes) return (_) -> biome;
      }
      var cache = new FastBiomeCache(includedBiomes, chunks);
      return (posz) -> getBiomeOr(cache, posz, posToBiome);
    }
    return posToBiome;
  }

  private static RegistryEntry<Biome> getBiomeOr(
      FastBiomeCache cache, BlockPos pos, Function<BlockPos, RegistryEntry<Biome>> fallback) {
    var cached = cache.getBiomeOrNull(pos.getX(), pos.getY(), pos.getZ());
    if (cached == null) return fallback.apply(pos);
    return cached;
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
