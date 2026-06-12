package org.codeberg.zenxarch.fastnoise.surface;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
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

  public final Set<RegistryKey<Biome>> includedBiomeKeys;

  public final RegistryEntry<Biome>[] biomes;

  @SuppressWarnings("unchecked")
  public MaterialRuleContext(
      SurfaceBuilder surfaceBuilder,
      NoiseConfig noiseConfig,
      Chunk chunk,
      ChunkNoiseSampler chunkNoiseSampler,
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      Registry<Biome> registry,
      HeightContext heightContext,
      final Set<RegistryEntry<Biome>> includedBiomes,
      final Chunk[] chunks) {
    var biomes = includedBiomes.toArray(RegistryEntry[]::new);

    super(
        surfaceBuilder,
        noiseConfig,
        chunk,
        chunkNoiseSampler,
        mapPosToBiome(posToBiome, biomes, chunks),
        registry,
        heightContext);

    this.includedBiomeKeys = new ObjectArraySet<>(includedBiomes.size());
    includedBiomes.forEach(biome -> includedBiomeKeys.add(biome.getKey().get()));

    this.biomes = biomes;
  }

  public RegistryEntry<Biome> mapPosToBiome(BlockPos pos) {
    return this.posToBiome.apply(pos);
  }

  private static Function<BlockPos, RegistryEntry<Biome>> mapPosToBiome(
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      RegistryEntry<Biome>[] includedBiomes,
      final Chunk[] chunks) {
    if (FastNoiseConfig.OPTIMIZE_BIOME_ACCESS) {
      if (includedBiomes.length == 1) {
        return (_) -> includedBiomes[0];
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

    this.stoneDepthAbove = 0;
    this.fluidHeight = Integer.MIN_VALUE;
  }

  @Override
  public void initVerticalContext(
      int stoneDepthAbove,
      int stoneDepthBelow,
      int fluidHeight,
      int blockX,
      int blockY,
      int blockZ) {
    super.initVerticalContext(
        stoneDepthAbove, stoneDepthBelow, fluidHeight, blockX, blockY, blockZ);
  }

  @Override
  public int estimateSurfaceHeight() {
    return super.estimateSurfaceHeight();
  }

  public int getCurrentBiomeIdx() {
    var biome = this.biomeSupplier.get();
    return indexOf(biomes, biome);
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
    initVerticalContext(stoneDepthAbove, stoneDepthBelow, fluidHeight, this.blockX, y, this.blockZ);
  }
}
