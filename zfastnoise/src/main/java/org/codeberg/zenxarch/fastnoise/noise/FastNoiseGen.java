package org.codeberg.zenxarch.fastnoise.noise;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.codeberg.zenxarch.fastnoise.heightmap.HeightmapUtil;

public class FastNoiseGen {
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
      Chunk chunk,
      BlockState defaultBlockState,
      final int minimumCellY,
      final int cellHeight) {

    var sections = chunk.getSectionArray();
    var fastSections = new FastChunkSection[sections.length];

    final var postProcessingLists = chunk.getPostProcessingLists();

    final var aquifer = chunkNoiseSampler.getAquiferSampler();

    ChunkPos chunkPos = chunk.getPos();
    int chunkStartBlockX = chunkPos.getStartX();
    int chunkStartBlockZ = chunkPos.getStartZ();

    chunkNoiseSampler.sampleStartDensity();

    int horizontalCellBlockCount = chunkNoiseSampler.getHorizontalCellBlockCount();
    int verticalCellBlockCount = chunkNoiseSampler.getVerticalCellBlockCount();

    int cellWidth = 16 / horizontalCellBlockCount;

    final var minY = chunk.getBottomY();

    for (int cellXIndex = 0; cellXIndex < cellWidth; cellXIndex++) {
      chunkNoiseSampler.sampleEndDensity(cellXIndex);

      for (int cellZIndex = 0; cellZIndex < cellWidth; cellZIndex++) {
        for (int cellYIndex = cellHeight - 1; cellYIndex >= 0; cellYIndex--) {
          chunkNoiseSampler.onSampledCellCorners(cellYIndex, cellZIndex);

          for (int yInCell = verticalCellBlockCount - 1; yInCell >= 0; yInCell--) {
            int blockY = (minimumCellY + cellYIndex) * verticalCellBlockCount + yInCell;
            int blockYInSection = blockY & 15;

            double factorY = (double) yInCell / verticalCellBlockCount;
            chunkNoiseSampler.interpolateY(blockY, factorY);

            var cy = (blockY - minY) >> 4;
            var fastSection = fastSections[cy];

            for (int xInCell = 0; xInCell < horizontalCellBlockCount; xInCell++) {
              int blockX = chunkStartBlockX + cellXIndex * horizontalCellBlockCount + xInCell;
              int blockXInSection = blockX & 15;
              double factorX = (double) xInCell / horizontalCellBlockCount;
              chunkNoiseSampler.interpolateX(blockX, factorX);

              for (int zInCell = 0; zInCell < horizontalCellBlockCount; zInCell++) {
                int blockZ = chunkStartBlockZ + cellZIndex * horizontalCellBlockCount + zInCell;
                int blockZInSection = blockZ & 15;
                double factorZ = (double) zInCell / horizontalCellBlockCount;
                chunkNoiseSampler.interpolateZ(blockZ, factorZ);

                var state = chunkNoiseSampler.sampleBlockState();

                if (state == AIR) continue;

                if (fastSection == null) {
                  fastSection = (fastSections[cy] = new FastChunkSection(sections[cy]));
                }

                if (state == null) {
                  fastSection.setDefaultBlockState(
                      blockXInSection, blockYInSection, blockZInSection, defaultBlockState);
                  continue;
                }

                fastSection.setBlockState(blockXInSection, blockYInSection, blockZInSection, state);

                if (aquifer.needsFluidTick() && !state.getFluidState().isEmpty()) {
                  Chunk.getList(postProcessingLists, cy)
                      .add((short) (blockXInSection | blockYInSection << 4 | blockZInSection << 8));
                }
              }
            }
          }
        }
      }
      chunkNoiseSampler.swapBuffers();
    }

    chunkNoiseSampler.stopInterpolation();

    finalizeChunks(fastSections, chunk, defaultBlockState);
  }

  private static void finalizeChunks(
      FastChunkSection[] fastSections, Chunk chunk, BlockState defaultBlockState) {
    for (int i = 0; i < fastSections.length; i++)
      if (fastSections[i] != null) fastSections[i].recalculateCounts();

    HeightmapUtil.populateHeightmapPostNoise(
        chunk, Heightmap.Type.OCEAN_FLOOR_WG, defaultBlockState, AIR);
    HeightmapUtil.populateHeightmapPostNoise(
        chunk, Heightmap.Type.WORLD_SURFACE_WG, defaultBlockState, AIR);
  }
}
