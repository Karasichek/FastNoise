package org.codeberg.zenxarch.fastnoise.surface;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.codeberg.zenxarch.fastnoise.mixin.SurfaceBuilderAccessor;
import org.codeberg.zenxarch.fastnoise.surface.cache.FastChunkCache;

public class FastSurfaceGen {

  public static boolean canUseSurfaceBuilder(Chunk chunk) {
    if (chunk.hasBelowZeroRetrogen()) return false;
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

    var posToBiome =
        MaterialRuleContext.mapPosToBiome(biomeAccess::getBiome, includedBiomes, chunks);

    var context =
        new MaterialRuleContext(
            (SurfaceBuilder) (Object) builder,
            noiseConfig,
            chunk,
            chunkNoiseSampler,
            posToBiome,
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
                  posToBiome.apply(
                      blockPos.set(blockX, useLegacyRandom ? 0 : startingHeight, blockZ));

          if (containsErodedBadlands && surfaceBiome.matchesKey(BiomeKeys.ERODED_BADLANDS)) {
            column.updateXZ(x, z);
            builder.zenxarch$placeBadlandsPillar(column, blockX, blockZ, startingHeight, chunk);
          }
        }
      }
    }

    if (!canUseSurfaceBuilder(chunk)) {
      throw new IllegalStateException("Some mod has made unexpected changes to chunk gen");
    }

    final var chunkCache = new FastChunkCache(chunk, defaultState);
    var sections = chunk.getSectionArray();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int blockX = minBlockX + x;
        int blockZ = minBlockZ + z;

        int height = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, x, z) + 1;
        context.initHorizontalContext(blockX, blockZ);
        int stoneAboveDepth = 0;
        int waterHeight = Integer.MIN_VALUE;
        int nextCeilingStoneY = Integer.MAX_VALUE;

        int y = height;
        if (y >= topY) { // assuming void air is air
          stoneAboveDepth = 0;
          waterHeight = Integer.MIN_VALUE;
          y = topY;
        }

        for (; y >= endY; y--) {
          final var section = column.getSection(y);
          if (chunkCache.isEmpty(x, y, z)) { // skip whole section
            y = y - (y & 0xF); // lowest y in current section;
            stoneAboveDepth = 0;
            waterHeight = Integer.MIN_VALUE;
            continue;
          }

          switch (chunkCache.getState(x, y, z)) {
            case AIR -> {
              stoneAboveDepth = 0;
              waterHeight = Integer.MIN_VALUE;
            }
            case WATER -> {
              if (waterHeight == Integer.MIN_VALUE) {
                waterHeight = y + 1;
              }
            }
            case ORE -> {
              if (nextCeilingStoneY >= y)
                nextCeilingStoneY =
                    nextNonDefaultBlock(builder, sections, y, endY, x, z, chunkCache) + 1;

              stoneAboveDepth++;
              int stoneBelowDepth = y - nextCeilingStoneY + 1;
              context.initVerticalContext(stoneAboveDepth, stoneBelowDepth, waterHeight, y);
            }
            case STONE -> {
              if (nextCeilingStoneY >= y)
                nextCeilingStoneY =
                    nextNonDefaultBlock(builder, sections, y, endY, x, z, chunkCache) + 1;

              stoneAboveDepth++;
              int stoneBelowDepth = y - nextCeilingStoneY + 1;
              context.initVerticalContext(stoneAboveDepth, stoneBelowDepth, waterHeight, y);
              setBlockState(section, x, y, z, rule.tryApply(blockX, y, blockZ), column, chunk);
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
  }

  private static void setBlockState(
      ChunkSection section,
      int x,
      int y,
      int z,
      BlockState state,
      FastBlockColumn column,
      Chunk chunk) {
    if (state == null) return;
    final int ly = y & 0xF;
    section.setBlockState(x, ly, z, state, false);
    column.fastUpdateHeightmap(x, z, y, state);
    if (state.getFluidState().isEmpty()) return;

    Chunk.getList(chunk.getPostProcessingLists(), column.getSectionIndex(y))
        .add((short) (x | ly << 4 | z << 8));
  }

  private static final BlockState VOID_AIR = Blocks.VOID_AIR.getDefaultState();

  private static int nextNonDefaultBlock(
      SurfaceBuilderAccessor builder,
      ChunkSection[] sections,
      int startY,
      int minY,
      int lx,
      int lz,
      FastChunkCache chunkCache) {
    final int wayBelowMinY = DimensionType.BELOW_MIN_HEIGHT;
    if (startY <= minY) {
      if (!builder.zenxarch$isDefaultBlock(VOID_AIR)) return minY - 1;
      return wayBelowMinY;
    }

    var y = chunkCache.nextNonSolidBlockY(lx, startY, lz);
    if (y >= minY) return y;

    if (!builder.zenxarch$isDefaultBlock(VOID_AIR)) return minY - 1;
    return wayBelowMinY;
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
