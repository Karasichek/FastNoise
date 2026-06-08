package org.codeberg.zenxarch.fastnoise.noise;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;

public class FastWorldgen {
  public static final BlockState AIR = Blocks.AIR.getDefaultState();

  public static boolean isEmpty(Chunk chunk) {
    var sections = chunk.getSectionArray();
    for (int i = 0; i < sections.length; i++) {
      if (sections[i].blockStateContainer.data.palette() instanceof SingularPalette) continue;
      return false;
    }
    return true;
  }

  public static void populateNoise(
      ChunkNoiseSampler chunkNoiseSampler,
      BlockState defaultBlockState,
      Chunk chunk,
      int minimumCellY,
      int cellHeight,
      FastChunkSection[] fastSections,
      int minimumY) {
    ChunkPos chunkPos = chunk.getPos();
    int chunkStartX = chunkPos.getStartX();
    int chunkStartZ = chunkPos.getStartZ();
    AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
    chunkNoiseSampler.sampleStartDensity();
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    int horizontalCellBlockCount = chunkNoiseSampler.getHorizontalCellBlockCount();
    int verticalCellBlockCount = chunkNoiseSampler.getVerticalCellBlockCount();
    int cellWidth = 16 / horizontalCellBlockCount;

    final var sections = chunk.getSectionArray();

    for (int cellX = 0; cellX < cellWidth; cellX++) {
      chunkNoiseSampler.sampleEndDensity(cellX);

      for (int cellZ = 0; cellZ < cellWidth; cellZ++) {
        for (int cellY = cellHeight - 1; cellY >= 0; cellY--) {
          chunkNoiseSampler.onSampledCellCorners(cellY, cellZ);

          for (int verticalCellBlock = verticalCellBlockCount - 1;
              verticalCellBlock >= 0;
              verticalCellBlock--) {
            int blockY = (minimumCellY + cellY) * verticalCellBlockCount + verticalCellBlock;
            int blockYInSection = blockY & 15;

            double verticalCellProgress =
                (double) verticalCellBlock / (double) verticalCellBlockCount;
            chunkNoiseSampler.interpolateY(blockY, verticalCellProgress);

            final var sectionIdx = (blockY - minimumY) >> 4;

            for (int cellBlockX = 0; cellBlockX < horizontalCellBlockCount; cellBlockX++) {
              int blockX = chunkStartX + cellX * horizontalCellBlockCount + cellBlockX;
              int blockXInSection = blockX & 15;
              double cellXProgress = (double) cellBlockX / (double) horizontalCellBlockCount;
              chunkNoiseSampler.interpolateX(blockX, cellXProgress);

              for (int cellBlockZ = 0; cellBlockZ < horizontalCellBlockCount; cellBlockZ++) {
                int blockZ = chunkStartZ + cellZ * horizontalCellBlockCount + cellBlockZ;
                int blockZInSection = blockZ & 15;
                double cellZProgress = (double) cellBlockZ / (double) horizontalCellBlockCount;

                chunkNoiseSampler.interpolateZ(blockZ, cellZProgress);

                var state = chunkNoiseSampler.sampleBlockState();
                if (state == AIR) continue;

                final var fastSection =
                    fastSections[sectionIdx] != null
                        ? fastSections[sectionIdx]
                        : (fastSections[sectionIdx] = new FastChunkSection(sections[sectionIdx]));

                if (state == null) {
                  fastSection.setDefaultBlockState(
                      blockXInSection, blockYInSection, blockZInSection, defaultBlockState);
                  continue;
                } else
                  fastSection.setBlockState(
                      blockXInSection, blockYInSection, blockZInSection, state);

                if (aquiferSampler.needsFluidTick() && !state.getFluidState().isEmpty()) {
                  mutable.set(blockX, blockY, blockZ);
                  chunk.markBlockForPostProcessing(mutable);
                }
              }
            }
          }
        }
      }

      chunkNoiseSampler.swapBuffers();
    }

    chunkNoiseSampler.stopInterpolation();

    for (int i = 0; i < fastSections.length; i++)
      if (fastSections[i] != null) fastSections[i].recalculateCounts();

    Heightmap.populateHeightmaps(
        chunk, ObjectArraySet.of(Heightmap.Type.OCEAN_FLOOR_WG, Heightmap.Type.WORLD_SURFACE_WG));
  }

  public static void populateBiomes(
      Chunk chunk, BiomeSupplier supplier, MultiNoiseSampler sampler) {
    var chunkPos = chunk.getPos();
    var world = chunk.getHeightLimitView();

    int x = chunkPos.x * 4;
    int y = world.getBottomY() >> 2;
    int z = chunkPos.z * 4;

    final int maxIdx = world.getHeight() >> 4;
    var sections = chunk.getSectionArray();

    @SuppressWarnings("unchecked")
    final RegistryEntry<Biome>[] biomes = new RegistryEntry[64];
    final var storage = new byte[64];

    for (int i = 0; i < maxIdx; i++) {
      var section = sections[i];
      FastBiomeGen.populateBiomes(section, supplier, sampler, x, y, z, biomes, storage);
      y += 4;
    }
  }
}
