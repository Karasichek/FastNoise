package org.codeberg.zenxarch.fastsurface.surface;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.SamplingRegion;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.BlockMaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastsurface.config.FastSurfaceConfig;
import org.codeberg.zenxarch.fastsurface.surface.cache.FastColumnIterator;
import org.codeberg.zenxarch.fastsurface.surface.cache.FastIsFluidCache;
import org.codeberg.zenxarch.fastsurface.surface.cache.FastPaletteIndexCache;
import org.jspecify.annotations.Nullable;

public class FastSurfaceGen {

  public static boolean canUseSurfaceBuilder(
      Chunk chunk, Set<RegistryEntry<Biome>> includedBiomes) {
    if (chunk.hasBelowZeroRetrogen()) return false;
    if (includedBiomes.size() > 64) return false;
    if (containsAny(
        includedBiomes,
        BiomeKeys.ERODED_BADLANDS,
        BiomeKeys.FROZEN_OCEAN,
        BiomeKeys.DEEP_FROZEN_OCEAN)) return false;
    return sanityCheck(chunk);
  }

  public static Chunk[] collectChunks(ChunkRegion chunkRegion, ChunkPos centerPos) {
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
      Set<RegistryEntry<Biome>> includedBiomes, RegistryKey<Biome>... keys) {
    for (var biome : includedBiomes) {
      for (int i = 0; i < keys.length; i++) if (biome.matchesKey(keys[i])) return true;
    }
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
      final SurfaceBuilder surfaceBuilder,
      final NoiseConfig noiseConfig,
      final BiomeAccess biomeAccess,
      final HeightContext heightContext,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final MaterialRule materialRule,
      final @Nullable Set<RegistryEntry<Biome>> includedBiomes) {

    final var defaultState = surfaceBuilder.defaultState;

    if (canSkipSurfaceBuilder(materialRule, defaultState)) {
      return;
    }

    final ChunkPos chunkPos = chunk.getPos();
    int minBlockX = chunkPos.getStartX();
    int minBlockZ = chunkPos.getStartZ();
    var column = new FastBlockColumn(chunk);

    int highestFilledSectionIndex = chunk.getHighestNonEmptySection();
    int maxBlockY = chunk.sectionIndexToCoord(highestFilledSectionIndex) * 16 + 15;
    SamplingRegion fullVolume = chunkNoiseSampler.getRegion();
    SamplingRegion narrowedVolume =
        new SamplingRegion(
            fullVolume.sizeX(),
            Math.max(maxBlockY - fullVolume.minBlockY() + 1, 1),
            fullVolume.sizeZ(),
            fullVolume.minBlockX(),
            fullVolume.minBlockY(),
            fullVolume.minBlockZ());
    MaterialRuleContext context =
        new MaterialRuleContext(
            surfaceBuilder,
            noiseConfig,
            narrowedVolume,
            chunkNoiseSampler.getContextualSamplerProvider(),
            MaterialRuleContextHelper.createPosToBiome(biomeAccess, chunk, includedBiomes),
            heightContext,
            includedBiomes);

    var sections = chunk.getSectionArray();

    var willUpdateHeightmap = true;
    var willAddWater = true;
    var willGrow = true;
    var rule = materialRule.getBlockStateRule(context);

    if (rule instanceof MaterialRuleInterpreterAccessor interpreter) {
      if (interpreter.zfastsurface$getRules().length == 0) {
        var states = interpreter.zfastsurface$getStates();

        willUpdateHeightmap = FastBlockColumn.willHeightmapUpdate(defaultState, states);
        willAddWater = willAddWater(states);

        var maxStates = 0;
        for (int i = 0; i < sections.length; i++)
          maxStates = Math.max(maxStates, sections[i].blockStateContainer.data.palette().getSize());

        willGrow = (maxStates + states.length) > 16;
      }
    }

    final int minY = chunk.getBottomY();

    final var indexCache = new FastPaletteIndexCache(sections);

    if (indexCache.size() == 0) return;

    final var iterator =
        FastColumnIterator.getIterator(sections, defaultState, minY, context, indexCache.size());
    final var isFluidCache = new FastIsFluidCache();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int blockX = minBlockX + x;
        int blockZ = minBlockZ + z;

        MaterialRuleContextHelper.initHorizontalContext(
            context, blockX, blockZ, getSteepnessX(chunk, x, z), getSteepnessZ(chunk, x, z));
        column.updateXZ(x, z);
        iterator.start(x, z);

        while (iterator.hasNext()) {
          final var y = iterator.nextY();
          final var state = rule.tryApply(blockX, y, blockZ);
          if (state == null || state == defaultState) continue;

          final var sy = (y - minY) >> 4;
          final var section = sections[sy];

          final var iy = y & 0xF;

          if (willGrow) indexCache.updateBlock(sy, section.blockStateContainer, x, iy, z, state);
          else indexCache.updateBlockFast(sy, section.blockStateContainer, x, iy, z, state);

          if (willUpdateHeightmap) {
            column.fastUpdateHeightmap(x, z, y, state);
          }

          if (willAddWater && isFluidCache.isFluid(state))
            Chunk.getList(chunk.getPostProcessingLists(), sy).add((short) (x | iy << 4 | z << 8));
        }
      }
    }

    indexCache.finish(sections);
  }

  private static int getSteepnessX(final Chunk chunk, final int localX, final int localZ) {
    return chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, Math.min(localX + 1, 15), localZ)
        - chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, Math.max(localX - 1, 0), localZ);
  }

  private static int getSteepnessZ(final Chunk chunk, final int localX, final int localZ) {
    return chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, Math.min(localZ + 1, 15))
        - chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, Math.max(localZ - 1, 0));
  }

  private static boolean willAddWater(final BlockState[] states) {
    for (int i = 0; i < states.length; i++) if (!states[i].getFluidState().isEmpty()) return true;
    return false;
  }

  private static boolean canSkipSurfaceBuilder(MaterialRule rule, BlockState defaultState) {
    if (!FastSurfaceConfig.SKIP_TRIVIAL_SURFACE_BUILDER) return false;
    if (rule instanceof BlockMaterialRule block) {
      return block.resultState() == defaultState;
    }
    return false;
  }
}
