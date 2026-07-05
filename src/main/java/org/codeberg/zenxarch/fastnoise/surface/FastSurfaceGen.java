package org.codeberg.zenxarch.fastnoise.surface;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.codeberg.zenxarch.fastnoise.mixin.SurfaceBuilderAccessor;
import org.codeberg.zenxarch.fastnoise.surface.cache.FastColumnIterator;
import org.codeberg.zenxarch.fastnoise.surface.cache.FastIsFluidCache;
import org.codeberg.zenxarch.fastnoise.surface.cache.FastPaletteIndexCache;

public class FastSurfaceGen {

  public static boolean canUseSurfaceBuilder(
      Chunk chunk, Set<RegistryEntry<Biome>> includedBiomes, Registry<Biome> registry) {
    if (chunk.hasBelowZeroRetrogen()) return false;
    if (includedBiomes.size() > 64) return false;
    if (containsAny(
        registry,
        includedBiomes,
        BiomeKeys.ERODED_BADLANDS,
        BiomeKeys.FROZEN_OCEAN,
        BiomeKeys.DEEP_FROZEN_OCEAN)) return false;
    return sanityCheck(chunk);
  }

  public static Chunk[] collectChunks(ChunkRegion chunkRegion, Chunk center) {
    final var centerPos = center.getPos();
    final var cx = centerPos.x();
    final var cz = centerPos.z();
    final var chunks = new Chunk[9];

    for (int z = -1; z <= 1; z++) {
      for (int x = -1; x <= 1; x++) {
        final var idx = ((z + 1) * 3) + (x + 1);
        if ((chunks[idx] = chunkRegion.getChunk(cx + x, cz + z))
            .getStatus()
            .isEarlierThan(ChunkStatus.BIOMES)) return null;
      }
    }

    return chunks;
  }

  @SafeVarargs
  private static boolean containsAny(
      Registry<Biome> registry,
      Set<RegistryEntry<Biome>> includedBiomes,
      RegistryKey<Biome>... keys) {
    for (int i = 0; i < keys.length; i++)
      if (includedBiomes.contains(registry.getOrThrow(keys[i]))) return true;
    return false;
  }

  private static boolean sanityCheck(Chunk chunk) {
    var sections = chunk.getSectionArray();
    for (int i = 0; i < sections.length; i++) {
      var config = sections[i].blockStateContainer.data.configuration();
      var palette = sections[i].blockStateContainer.data.palette();
      if (palette instanceof SingularPalette) continue;
      if (palette instanceof ArrayPalette && config.bitsInMemory() == 4) continue;
      return false;
    }
    return true;
  }

  public static void buildSurface(
      SurfaceBuilderAccessor builder,
      final NoiseConfig noiseConfig,
      final BiomeAccess biomeAccess,
      final boolean useLegacyRandom,
      final HeightContext heightContext,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final MaterialRules.MaterialRule materialRule,
      final Set<RegistryEntry<Biome>> includedBiomes,
      final Chunk[] chunks,
      final Registry<Biome> registry) {

    final var defaultState = builder.zenxarch$getDefaultState();

    if (canSkipSurfaceBuilder(materialRule, defaultState)) {
      return;
    }

    final ChunkPos chunkPos = chunk.getPos();
    int minBlockX = chunkPos.getStartX();
    int minBlockZ = chunkPos.getStartZ();
    var column = new FastBlockColumn(chunk);

    var context =
        new MaterialRuleContext(
            (SurfaceBuilder) (Object) builder,
            noiseConfig,
            chunk,
            chunkNoiseSampler,
            biomeAccess::getBiome,
            registry,
            heightContext,
            includedBiomes,
            chunks);

    var rule = materialRule.apply(context);

    final int minY = chunk.getBottomY();

    if (!sanityCheck(chunk)) {
      throw new IllegalStateException("Some mod has made unexpected changes to chunk gen");
    }

    var sections = chunk.getSectionArray();
    final var indexCache = new FastPaletteIndexCache(sections);

    if (indexCache.size() == 0) return;

    final var iterator =
        FastColumnIterator.getIterator(sections, defaultState, minY, context, indexCache.size());
    final var isFluidCache = new FastIsFluidCache();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int blockX = minBlockX + x;
        int blockZ = minBlockZ + z;

        context.initHorizontalContext(blockX, blockZ);
        column.updateXZ(x, z);
        iterator.start(x, z);

        while (iterator.hasNext()) {
          final var y = iterator.nextY();
          final var state = rule.tryApply(blockX, y, blockZ);
          if (state == null || state == defaultState) continue;

          final var sy = (y - minY) >> 4;
          final var section = sections[sy];

          indexCache.updateBlock(sy, sections[sy].blockStateContainer, x, y & 0xF, z, state);

          updateExtras(section, x, y, z, state, column, chunk, sy, isFluidCache);
        }
      }
    }

    indexCache.finish(sections);
  }

  private static void updateExtras(
      ChunkSection section,
      int x,
      int iy,
      int z,
      BlockState state,
      FastBlockColumn column,
      Chunk chunk,
      int sy,
      FastIsFluidCache isFluidCache) {
    column.fastUpdateHeightmap(x, z, iy, state);

    final var y = iy & 0xF;
    if (isFluidCache.isFluid(state))
      Chunk.getList(chunk.getPostProcessingLists(), sy).add((short) (x | y << 4 | z << 8));
  }

  private static boolean canSkipSurfaceBuilder(
      MaterialRules.MaterialRule rule, BlockState defaultState) {
    if (!FastNoiseConfig.SKIP_TRIVIAL_SURFACE_BUILDER) return false;
    if (rule instanceof MaterialRules.BlockMaterialRule block) {
      return block.resultState() == defaultState;
    }
    return false;
  }
}
