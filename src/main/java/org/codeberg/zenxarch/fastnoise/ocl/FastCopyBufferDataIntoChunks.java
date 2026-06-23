package org.codeberg.zenxarch.fastnoise.ocl;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.codeberg.zenxarch.fastnoise.heightmap.HeightmapUtil;
import org.codeberg.zenxarch.fastnoise.noise.FastChunkSection;

public final class FastCopyBufferDataIntoChunks {
  /**
   * Copy byte data to startingPos + [0 ..< batchSize,0 ..< batchSize] chunks
   *
   * @param chunks
   * @param clBlockStateMappings length must be no more than 17, where 1 is AIR, 2 is default block
   *     state, 0 is invalid data
   * @param blockOutBufferData each byte represents a block with a stride of horizontalSize with
   *     indices [y][z][x] has repr [0..6 -> block idx,7 -> post processing flag]
   */
  public static void copyData(
      BoundedRegionArray<ProtoChunk> chunks,
      BlockState[] clBlockStateMappings,
      int verticalSize,
      ChunkGeneratorSettings settings,
      int horizontalSize,
      ByteBuffer blockOutBufferData,
      ChunkPos startingPos,
      int batchSize) {

    var blockDataSegment =
        MemorySegment.ofBuffer(blockOutBufferData)
            .reinterpret(horizontalSize * horizontalSize * verticalSize);

    for (int chunkOffZ = 0; chunkOffZ < batchSize; chunkOffZ++) {
      for (int chunkOffX = 0; chunkOffX < batchSize; chunkOffX++) {
        var chunk = chunks.get(startingPos.x() + chunkOffX, startingPos.z() + chunkOffZ);
        if (chunk.getStatus().isAtLeast(ChunkStatus.NOISE)) continue;
        copyDataIntoChunk(
            chunkOffX,
            chunkOffZ,
            chunk,
            clBlockStateMappings,
            verticalSize,
            blockDataSegment,
            horizontalSize);
        chunk.setStatus(ChunkStatus.NOISE);
      }
    }
  }

  private static void copyDataIntoChunk(
      int chunkOffX,
      int chunkOffZ,
      ProtoChunk chunk,
      BlockState[] mappings,
      int verticalSize,
      MemorySegment rawData,
      int horizontalSize) {
    var rawSections = chunk.getSectionArray();
    var sections = new FastChunkSection[rawSections.length];

    for (int i = 0; i < sections.length; i++) {
      sections[i] = new FastChunkSection(rawSections[i]);
    }

    final var postProcessingLists = chunk.getPostProcessingLists();

    var defaultState = mappings[2];

    for (int y = 0; y < verticalSize; y++) {
      var sy = y >> 4;
      var section = sections[sy];
      for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
          var value = getByte(rawData, getIndex(chunkOffX, chunkOffZ, x, y, z, horizontalSize));
          if (putBlock(x, y & 0xF, z, value, section, defaultState, mappings)) {
            Chunk.getList(postProcessingLists, sy).add((short) (x | (y & 0xF) << 4 | z << 8));
          }
        }
      }
    }

    finalizeChunks(sections, chunk, defaultState);
  }

  private static int getIndex(
      int chunkOffX, int chunkOffZ, int x, int y, int z, int horizontalSize) {
    return (chunkOffX * 16 + x) + (horizontalSize * ((chunkOffZ * 16 + z) + (horizontalSize * y)));
  }

  private static byte getByte(MemorySegment rawData, int idx) {
    return rawData.get(ValueLayout.JAVA_BYTE, idx);
  }

  private static long getLong(MemorySegment rawData, int idx) {
    return rawData.get(ValueLayout.JAVA_LONG, idx);
  }

  private static boolean putBlock(
      int x,
      int y,
      int z,
      byte value,
      FastChunkSection section,
      BlockState defaultState,
      BlockState[] mappings) {
    var blockIdx = value & ((byte) 0x7F);

    if (blockIdx == 1) return false;
    if (blockIdx == 2) {
      section.setDefaultBlockState(x, y, z, defaultState);
      return false;
    }

    var state = mappings[blockIdx];
    section.setBlockState(x, y, z, state);

    return (value & (byte) 0x80) != 0;
  }

  private static final BlockState AIR = Blocks.AIR.getDefaultState();

  private static final Heightmap.Type[] heightmaps =
      HeightmapUtil.calculateHeightmaps(ChunkStatus.NOISE);

  private static void finalizeChunks(
      FastChunkSection[] fastSections, Chunk chunk, BlockState defaultBlockState) {
    for (int i = 0; i < fastSections.length; i++)
      if (fastSections[i] != null) fastSections[i].recalculateCounts();

    for (int i = 0; i < heightmaps.length; i++) {
      HeightmapUtil.populateHeightmapPostNoise(chunk, heightmaps[i], defaultBlockState, AIR);
    }
  }
}
