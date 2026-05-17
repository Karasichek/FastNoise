package org.codeberg.zenxarch.fastnoise.surface;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.codeberg.zenxarch.fastnoise.mixin.SurfaceBuilderAccessor;
import org.codeberg.zenxarch.fastnoise.surface.cache.FastChunkCache;

public class FastSurfaceGen {

  public static boolean canUseSurfaceBuilder(
      Chunk chunk, Set<RegistryEntry<Biome>> includedBiomes) {
    if (chunk.hasBelowZeroRetrogen()) return false;
    if (includedBiomes.size() > 64) return false;
    return sanityCheck(chunk);
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
            heightContext,
            includedBiomes,
            chunks);

    var rule = materialRule.apply(context);
    BlockPos.Mutable blockPos = new BlockPos.Mutable();

    final int endY = chunk.getBottomY();
    final int topY = chunk.getTopYInclusive();

    @SuppressWarnings("unchecked")
    RegistryEntry<Biome>[] surfaceBiomes = new RegistryEntry[256];

    var containsErodedBadlands =
        !includedBiomes.contains(registry.getOrThrow(BiomeKeys.ERODED_BADLANDS));
    var containsIceberg =
        includedBiomes.contains(registry.getOrThrow(BiomeKeys.FROZEN_OCEAN))
            || includedBiomes.contains(registry.getOrThrow(BiomeKeys.DEEP_FROZEN_OCEAN));

    // TODO: just update the chunk cache instead
    if (containsErodedBadlands || containsIceberg) {
      for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
          int blockX = minBlockX + x;
          int blockZ = minBlockZ + z;
          int startingHeight = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, x, z) + 1;
          var surfaceBiome =
              surfaceBiomes[(x * 16) + z] =
                  context.mapPosToBiome(
                      blockPos.set(blockX, useLegacyRandom ? 0 : startingHeight, blockZ));

          if (containsErodedBadlands && surfaceBiome.matchesKey(BiomeKeys.ERODED_BADLANDS)) {
            column.updateXZ(x, z);
            builder.zenxarch$placeBadlandsPillar(column, blockX, blockZ, startingHeight, chunk);
          }
        }
      }
    }

    if (!sanityCheck(chunk)) {
      throw new IllegalStateException("Some mod has made unexpected changes to chunk gen");
    }

    final var chunkCache = new FastChunkCache(chunk, defaultState);
    var sections = chunk.getSectionArray();

    var dirty = new boolean[sections.length];

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int blockX = minBlockX + x;
        int blockZ = minBlockZ + z;

        int height = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, x, z) + 1;
        context.initHorizontalContext(blockX, blockZ);

        int y = height;
        if (y > topY) y = topY; // assuming void air is air

        var lastSy = column.getSectionIndex(y);
        var section = sections[lastSy];

        int lastIdx = -1;
        var lastState = (BlockState) null;

        for (; y >= endY; y--) {
          final int sy = column.getSectionIndex(y);
          if (sy != lastSy) {
            lastSy = sy;
            section = sections[sy];
            lastIdx = -1;
            lastState = null;
          }
          if (chunkCache.isEmpty(x, y, z)) { // skip whole section
            y = y - (y & 0xF); // lowest y in current section;
            context.air();
            continue;
          }

          switch (chunkCache.getState(x, y, z)) {
            case AIR -> context.air();
            case WATER -> context.water(y);
            case ORE -> context.oreOrStone(chunkCache, x, y, z);
            case STONE -> {
              context.oreOrStone(chunkCache, x, y, z);
              var state = rule.tryApply(blockX, y, blockZ);
              if (state == null) continue;
              dirty[sy] = true;

              var idx =
                  state == lastState
                      ? lastIdx
                      : section
                          .blockStateContainer
                          .data
                          .palette()
                          .index(state, section.blockStateContainer);

              setBlockState(section, x, y, z, state, idx, column, chunk, sy);

              lastIdx = idx;
              lastState = state;
            }
          }
        }

        // can't have it both be frozen ocean and eroded badlands
        var surfaceBiome = surfaceBiomes[(x * 16) + z];
        if (containsIceberg
            && (surfaceBiome.matchesKey(BiomeKeys.FROZEN_OCEAN)
                || surfaceBiome.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN))) {
          column.updateXZ(x, z);
          builder.zenxarch$placeIceberg(
              context.estimateSurfaceHeight(),
              surfaceBiome.value(),
              column,
              blockPos,
              blockX,
              blockZ,
              height);
        }
      }
    }

    for (int i = 0; i < dirty.length; i++) {
      if (dirty[i]) sections[i].calculateCounts();
    }
  }

  private static void setBlockState(
      ChunkSection section,
      int x,
      int y,
      int z,
      BlockState state,
      int pidx,
      FastBlockColumn column,
      Chunk chunk,
      int sy) {
    final int ly = y & 0xF;
    section.blockStateContainer.data.storage().set((((ly << 4) | z) << 4) | x, pidx);

    column.fastUpdateHeightmap(x, z, y, state);
    if (state.getFluidState().isEmpty()) return;

    Chunk.getList(chunk.getPostProcessingLists(), sy).add((short) (x | ly << 4 | z << 8));
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
