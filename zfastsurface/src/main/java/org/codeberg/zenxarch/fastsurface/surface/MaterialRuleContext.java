package org.codeberg.zenxarch.fastsurface.surface;

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
import org.codeberg.zenxarch.fastsurface.config.FastSurfaceConfig;
import org.codeberg.zenxarch.fastsurface.surface.biome.FastBiomeCache;

public class MaterialRuleContext extends MaterialRules.MaterialRuleContext {

  public final RegistryEntry<Biome>[] biomes;

  public int cachedBiomeIdx;
  private final FastBiomeCache biomeCache;
  private final Function<BlockPos, RegistryEntry<Biome>> fallback;
  public boolean negativeRunDepthCache;

  @SuppressWarnings("unchecked")
  public MaterialRuleContext(
      SurfaceBuilder surfaceBuilder,
      NoiseConfig noiseConfig,
      Chunk chunk,
      ChunkNoiseSampler chunkNoiseSampler,
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      HeightContext heightContext,
      final Set<RegistryEntry<Biome>> includedBiomes,
      final Chunk[] chunks) {
    var biomes = includedBiomes.toArray(RegistryEntry[]::new);

    FastBiomeCache biomeCache;

    if (FastSurfaceConfig.OPTIMIZE_BIOME_ACCESS) {
      biomeCache = biomes.length == 1 ? null : new FastBiomeCache(biomes, chunks);
    } else {
      biomeCache = null;
    }
    this.fallback = posToBiome;

    super(
        surfaceBuilder,
        noiseConfig,
        chunk,
        chunkNoiseSampler,
        mapPosToBiome(biomeCache, posToBiome, biomes, chunks),
        heightContext,
        includedBiomes);

    this.biomeCache = biomeCache;
    this.biomes = biomes;
  }

  public RegistryEntry<Biome> mapPosToBiome(BlockPos pos) {
    return this.posToBiome.apply(pos);
  }

  private static Function<BlockPos, RegistryEntry<Biome>> mapPosToBiome(
      FastBiomeCache cache,
      Function<BlockPos, RegistryEntry<Biome>> fallback,
      RegistryEntry<Biome>[] includedBiomes,
      final Chunk[] chunks) {
    if (FastSurfaceConfig.OPTIMIZE_BIOME_ACCESS) {
      if (includedBiomes.length == 1) {
        return (_) -> includedBiomes[0];
      }
      return (posz) -> getBiomeOr(cache, posz, fallback);
    }
    return fallback;
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

    this.stoneDepthAbove = 0;
    this.fluidHeight = Integer.MIN_VALUE;
    this.negativeRunDepthCache = this.runDepth <= 0;
  }

  @Override
  public void initVerticalContext(
      int stoneDepthAbove, int stoneDepthBelow, int fluidHeight, int blockY) {
    super.initVerticalContext(stoneDepthAbove, stoneDepthBelow, fluidHeight, blockY);
    cachedBiomeIdx = this.biomes.length == 1 ? 0 : -1;
  }

  @Override
  public int estimateSurfaceHeight() {
    return super.estimateSurfaceHeight();
  }

  public int getCurrentBiomeIdx() {
    if (cachedBiomeIdx != -1) return cachedBiomeIdx;
    if (this.currentBiome != null) {
      this.cachedBiomeIdx = indexOf(biomes, this.currentBiome);
      return this.cachedBiomeIdx;
    }
    if (this.biomeCache == null) {
      this.cachedBiomeIdx = indexOf(biomes, getCurrentBiome());
      return this.cachedBiomeIdx;
    }
    this.cachedBiomeIdx = this.biomeCache.getBiomeIdxOrNull(this.blockX, this.blockY, this.blockZ);
    if (this.cachedBiomeIdx == -1) {
      this.currentBiome = this.fallback.apply(this.pos.set(this.blockX, this.blockY, this.blockZ));
      this.cachedBiomeIdx = indexOf(biomes, this.currentBiome);
      return this.cachedBiomeIdx;
    }
    this.currentBiome = this.biomes[this.cachedBiomeIdx];
    return this.cachedBiomeIdx;
  }

  private static int indexOf(RegistryEntry<Biome>[] biomes, RegistryEntry<Biome> biome) {
    for (int i = 0; i < biomes.length; i++) if (biomes[i] == biome) return i;
    throw new IllegalStateException("Impossible biome returned, possible mod incompatibility");
  }

  public void air() {
    this.stoneDepthAbove = 0;
    this.fluidHeight = Integer.MIN_VALUE;
  }

  public void water(final int y) {
    if (fluidHeight == Integer.MIN_VALUE) fluidHeight = y + 1;
  }

  public void oreOrStone(final int y, final int stoneDepthBelow) {
    stoneDepthAbove++;
    initVerticalContext(stoneDepthAbove, stoneDepthBelow, fluidHeight, y);
  }
}
