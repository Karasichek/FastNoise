package org.codeberg.zenxarch.fastnoise.noise;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.SingularPalette;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.sampler.PooledSampleBuffer;
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
      DensityFunction finalDensityFunc) {

    var sections = chunk.getSectionArray();
    var fastSections = new FastChunkSection[sections.length];

    final var postProcessingLists = chunk.getPostProcessingLists();

    final var aquifer = chunkNoiseSampler.getAquiferSampler();
    final var volume = chunkNoiseSampler.getRegion();
    final var finalDensity =
        chunkNoiseSampler.getContextualSamplerProvider().getSampler(finalDensityFunc);

    final var minY = chunk.getBottomY();

    try (PooledSampleBuffer densityBuffer = finalDensity.getFilledBuffer(volume)) {
      for (int z = 0; z < volume.sizeZ(); z++) {
        int blockZ = volume.translateZ(z);

        for (int x = 0; x < volume.sizeX(); x++) {
          int blockX = volume.translateX(x);

          for (int y = volume.sizeY() - 1; y >= 0; y--) {
            int blockY = volume.translateY(y);

            var state =
                aquifer.apply(blockX, blockY, blockZ, densityBuffer.get(volume.getIndex(x, y, z)));

            var cy = (blockY - minY) >> 4;
            var fastSection = fastSections[cy];

            if (state == AIR) continue;

            if (fastSection == null) {
              fastSection = (fastSections[cy] = new FastChunkSection(sections[cy]));
            }

            final var blockXInSection = blockX & 0xF;
            final var blockYInSection = blockY & 0xF;
            final var blockZInSection = blockZ & 0xF;

            if (state == null) {
              fastSection.setDefaultBlockState(x, blockYInSection, z, defaultBlockState);
              continue;
            }

            fastSection.setBlockState(x, blockYInSection, z, state);

            if (aquifer.needsFluidTick() && !state.getFluidState().isEmpty()) {
              Chunk.getList(postProcessingLists, cy)
                  .add((short) (blockXInSection | blockYInSection << 4 | blockZInSection << 8));
            }
          }
        }
      }
    }

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
