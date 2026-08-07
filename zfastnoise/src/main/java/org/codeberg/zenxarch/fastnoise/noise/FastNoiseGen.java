package org.codeberg.zenxarch.fastnoise.noise;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.codeberg.zenxarch.fastnoise.heightmap.HeightmapUtil;

public class FastNoiseGen {
  public static final BlockState AIR = Blocks.AIR.getDefaultState();

  private static final Heightmap.Type[] heightmaps =
      HeightmapUtil.calculateHeightmaps(ChunkStatus.NOISE);

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
      int cellHeight) {

    var sections = chunk.getSectionArray();

    var fastSections = new FastChunkSection[sections.length];

    ChunkPos chunkPos = chunk.getPos();
    int chunkStartX = chunkPos.getStartX();
    int chunkStartZ = chunkPos.getStartZ();
    AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
    chunkNoiseSampler.sampleStartDensity();
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    int horizontalCellBlockCount = chunkNoiseSampler.getHorizontalCellBlockCount();
    int verticalCellBlockCount = chunkNoiseSampler.getVerticalCellBlockCount();
    int cellWidth = 16 / horizontalCellBlockCount;

    var minY = chunk.getBottomY();

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

            float verticalCellProgress = (float) verticalCellBlock / (float) verticalCellBlockCount;
            chunkNoiseSampler.interpolateY(blockY, verticalCellProgress);

            iterateCellXZ(
                chunk,
                chunkNoiseSampler,
                aquiferSampler,
                defaultBlockState,
                mutable,
                blockY,
                blockYInSection,
                minY,
                fastSections,
                sections,
                horizontalCellBlockCount,
                chunkStartX,
                chunkStartZ,
                cellX,
                cellZ);
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

    for (int i = 0; i < heightmaps.length; i++) {
      HeightmapUtil.populateHeightmapPostNoise(chunk, heightmaps[i], defaultBlockState, AIR);
    }
  }

  private static void iterateCellXZ(
      Chunk chunk,
      ChunkNoiseSampler chunkNoiseSampler,
      AquiferSampler aquiferSampler,
      BlockState defaultBlockState,
      BlockPos.Mutable mutable,
      int blockY,
      int blockYInSection,
      int minY,
      FastChunkSection[] fastSections,
      ChunkSection[] sections,
      int horizontalCellBlockCount,
      int chunkStartX,
      int chunkStartZ,
      int cellX,
      int cellZ) {
    final var postProcessingLists = chunk.getPostProcessingLists();

    var cy = (blockY - minY) >> 4;
    var fastSection = fastSections[cy];

    for (int cellBlockX = 0; cellBlockX < horizontalCellBlockCount; cellBlockX++) {
      int blockX = chunkStartX + cellX * horizontalCellBlockCount + cellBlockX;
      int blockXInSection = blockX & 15;
      float cellXProgress = (float) cellBlockX / (float) horizontalCellBlockCount;
      chunkNoiseSampler.interpolateX(blockX, cellXProgress);

      for (int cellBlockZ = 0; cellBlockZ < horizontalCellBlockCount; cellBlockZ++) {
        int blockZ = chunkStartZ + cellZ * horizontalCellBlockCount + cellBlockZ;
        int blockZInSection = blockZ & 15;
        float cellZProgress = (float) cellBlockZ / (float) horizontalCellBlockCount;

        chunkNoiseSampler.interpolateZ(blockZ, cellZProgress);

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

        if (aquiferSampler.needsFluidTick() && !state.getFluidState().isEmpty()) {
          Chunk.getList(postProcessingLists, cy)
              .add((short) (blockXInSection | blockYInSection << 4 | blockZInSection << 8));
        }
      }
    }
  }
}
