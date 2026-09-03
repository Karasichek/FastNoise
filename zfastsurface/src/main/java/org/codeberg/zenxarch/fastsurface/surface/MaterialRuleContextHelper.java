package org.codeberg.zenxarch.fastsurface.surface;

import java.util.Set;
import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.codeberg.zenxarch.fastsurface.config.FastSurfaceConfig;
import org.codeberg.zenxarch.fastsurface.surface.biome.FastBiomeCache;

public class MaterialRuleContextHelper {

  public static Function<BlockPos, RegistryEntry<Biome>> createPosToBiome(
      final BiomeAccess biomeAccess,
      final Chunk chunk,
      final Set<RegistryEntry<Biome>> includedBiomes) {
    if (includedBiomes == null) return new VanillaGetter(biomeAccess);
    @SuppressWarnings("unchecked")
    var biomes = (RegistryEntry<Biome>[]) includedBiomes.toArray(RegistryEntry[]::new);

    if (biomes.length == 1) return new SingleBiomeGetter(biomes[0]);

    if (FastSurfaceConfig.OPTIMIZE_BIOME_ACCESS
        && biomeAccess.storage instanceof ChunkRegion region) {
      return new OptimizedGetter(
          new FastBiomeCache(biomes, FastSurfaceGen.collectChunks(region, chunk.getPos())),
          biomeAccess);
    }

    return new VanillaGetter(biomeAccess);
  }

  private static record SingleBiomeGetter(RegistryEntry<Biome> biome)
      implements Function<BlockPos, RegistryEntry<Biome>> {
    @Override
    public RegistryEntry<Biome> apply(BlockPos pos) {
      return biome;
    }
  }

  private static record VanillaGetter(BiomeAccess biomeAccess)
      implements Function<BlockPos, RegistryEntry<Biome>> {
    @Override
    public RegistryEntry<Biome> apply(BlockPos pos) {
      return biomeAccess.getBiome(pos);
    }
  }

  private static record OptimizedGetter(FastBiomeCache cache, BiomeAccess fallback)
      implements Function<BlockPos, RegistryEntry<Biome>> {
    @Override
    public RegistryEntry<Biome> apply(BlockPos pos) {
      var cached = cache.getBiomeOrNull(pos.getX(), pos.getY(), pos.getZ());
      if (cached == null) return fallback.getBiome(pos);
      return cached;
    }
  }

  public static void initHorizontalContext(
      MaterialRuleContext context,
      final int blockX,
      final int blockZ,
      final int steepnessX,
      final int steepnessZ) {
    context.initHorizontalContext(blockX, blockZ, steepnessX, steepnessZ);
    context.stoneDepthAbove = 0;
    context.fluidHeight = Integer.MIN_VALUE;
  }

  public static void air(MaterialRuleContext context) {
    context.stoneDepthAbove = 0;
    context.fluidHeight = Integer.MIN_VALUE;
  }

  public static void water(MaterialRuleContext context, final int y) {
    if (context.fluidHeight == Integer.MIN_VALUE) context.fluidHeight = y + 1;
  }

  public static void oreOrStone(
      MaterialRuleContext context, final int y, final int stoneDepthBelow) {
    context.stoneDepthAbove++;
    context.initVerticalContext(context.stoneDepthAbove, stoneDepthBelow, context.fluidHeight, y);
  }
}
