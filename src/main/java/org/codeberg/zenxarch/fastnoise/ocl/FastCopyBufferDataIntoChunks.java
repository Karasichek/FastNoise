package org.codeberg.zenxarch.fastnoise.ocl;

import it.unimi.dsi.fastutil.shorts.ShortList;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.codeberg.zenxarch.fastnoise.heightmap.HeightmapUtil;
import org.codeberg.zenxarch.fastnoise.noise.FastNoiseGen;

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

    var blockDataSegment = MemorySegment.ofBuffer(blockOutBufferData);
    // .reinterpret(horizontalSize * horizontalSize * verticalSize);

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

    final var postProcessingLists = chunk.getPostProcessingLists();

    var defaultState = mappings[2];

    var idx = (chunkOffX * 16) + (horizontalSize * chunkOffZ * 16);
    final var yStep = horizontalSize * horizontalSize;

    for (int y = 0; y < verticalSize; y++) {
      var sy = y >> 4;

      for (int z = 0; z < 16; z++) {
        final var zidx = idx + (z * horizontalSize);
        handleLong(
            0,
            y,
            z,
            sy,
            sections,
            rawSections,
            postProcessingLists,
            defaultState,
            mappings,
            getLong(rawData, zidx));
        handleLong(
            8,
            y,
            z,
            sy,
            sections,
            rawSections,
            postProcessingLists,
            defaultState,
            mappings,
            getLong(rawData, zidx + 8));
      }

      idx += yStep;
    }

    finalizeChunks(sections, chunk, defaultState);
  }

  private static long getLong(MemorySegment rawData, int idx) {
    return rawData.get(ValueLayout.JAVA_LONG, idx);
  }

  private static void handleLong(
      int startX,
      int y,
      int z,
      int sy,
      FastChunkSection[] sections,
      ChunkSection[] chunkSections,
      ShortList[] postProcessingLists,
      BlockState defaultState,
      BlockState[] mappings,
      long value) {
    if (value == 0x0101010101010101L) return;

    var section = sections[sy];
    if (section == null)
      section = (sections[sy] = new FastChunkSection(chunkSections[sy], mappings));

    section.handleLong(startX, y & 0xF, z, value, mappings);

    if ((value & 0x8080808080808080L) == 0x0L) return;

    var list = Chunk.getList(postProcessingLists, sy);
    final short baseIdx = (short) (startX | ((y & 0xF) << 4) | (z << 8));

    for (short i = 0; i < 8; i++) {
      if (((value >>> (i << 3)) & 0x80L) == 0x80L) list.add((short) (i | baseIdx));
    }
  }

  private static final Heightmap.Type[] heightmaps =
      HeightmapUtil.calculateHeightmaps(ChunkStatus.NOISE);

  private static void finalizeChunks(
      FastChunkSection[] fastSections, Chunk chunk, BlockState defaultBlockState) {
    for (int i = 0; i < fastSections.length; i++)
      if (fastSections[i] != null) fastSections[i].recalculateCounts();

    for (int i = 0; i < heightmaps.length; i++) {
      HeightmapUtil.populateHeightmapPostNoise(
          chunk, heightmaps[i], defaultBlockState, FastNoiseGen.AIR);
    }
  }
}
