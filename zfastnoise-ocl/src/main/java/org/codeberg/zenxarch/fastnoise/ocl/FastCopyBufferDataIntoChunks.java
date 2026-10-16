package org.codeberg.zenxarch.fastnoise.ocl;

import it.unimi.dsi.fastutil.shorts.ShortList;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.codeberg.zenxarch.fastnoise.heightmap.HeightmapUtil;
import org.codeberg.zenxarch.fastnoise.noise.FastNoiseGen;
import org.codeberg.zenxarch.fastnoise.noise.FastNoisePaletteHelper;

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
        var chunk = chunks.get(startingPos.x + chunkOffX, startingPos.z + chunkOffZ);
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

  public static void copyData(
      BoundedRegionArray<ProtoChunk> chunks,
      RegistryEntry<Biome>[] biomeMappings,
      IntBuffer biomeOutBufferData,
      ChunkPos startingPos,
      int batchSize) {

    @SuppressWarnings("unchecked")
    RegistryEntry<Biome>[] reusablePaletteArray = new RegistryEntry[64];
    byte[] reusableByteArray = new byte[64];

    for (int chunkOffZ = 0; chunkOffZ < batchSize; chunkOffZ++) {
      for (int chunkOffX = 0; chunkOffX < batchSize; chunkOffX++) {
        var chunk = chunks.get(startingPos.x + chunkOffX, startingPos.z + chunkOffZ);
        if (chunk.getStatus().isAtLeast(ChunkStatus.BIOMES)) continue;

        copyDataIntoChunk(
            chunkOffX,
            chunkOffZ,
            chunk,
            biomeMappings,
            biomeOutBufferData,
            reusablePaletteArray,
            reusableByteArray,
            batchSize);

        chunk.setStatus(ChunkStatus.BIOMES);
      }
    }
  }

  private static void copyDataIntoChunk(
      int chunkOffX,
      int chunkOffZ,
      ProtoChunk chunk,
      RegistryEntry<Biome>[] biomeMappings,
      IntBuffer rawData,
      RegistryEntry<Biome>[] reusablePaletteArray,
      byte[] reusableByteArray,
      int batchSize) {
    final int horizontalBiomeSize = 4 * batchSize;
    final var baseOffset = (chunkOffZ * 4 * horizontalBiomeSize) + chunkOffX * 4;
    final var sections = chunk.getSectionArray();
    final var maxSy = sections.length;
    final var ySize = horizontalBiomeSize * horizontalBiomeSize;

    for (int sy = 0; sy < maxSy; sy++) {
      var size = 0;

      final var syOffset = (sy * ySize << 2) + baseOffset;

      var idx = 0;
      var lastRawIdx = -1;
      var lastBidx = -1;

      for (int y = 0; y < 4; y++) {
        final var yOffset = (y * ySize) + syOffset;
        for (int z = 0; z < 4; z++) {
          final var zOffset = (z * horizontalBiomeSize) + yOffset;
          for (int x = 0; x < 4; x++) {
            var rawIdx = rawData.get(zOffset + x);
            int bidx = -1;
            if (rawIdx == lastRawIdx) {
              bidx = lastBidx;
            } else {
              var value = biomeMappings[rawIdx];
              for (int i = 0; i < size; i++)
                if (reusablePaletteArray[i] == value) {
                  bidx = i;
                  break;
                }

              if (bidx == -1) reusablePaletteArray[(bidx = size++)] = value;
            }

            reusableByteArray[idx] = (byte) bidx;

            idx++;
          }
        }
      }

      FastNoisePaletteHelper.pack(
          (PalettedContainer<RegistryEntry<Biome>>) (sections[sy].biomeContainer),
          reusablePaletteArray,
          size,
          reusableByteArray);
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
            mappings,
            getLong(rawData, zidx + 8));
      }

      idx += yStep;
    }

    finalizeChunks(sections, chunk, mappings[2] == null ? FastNoiseGen.AIR : mappings[2]);
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

  private static void finalizeChunks(
      FastChunkSection[] fastSections, Chunk chunk, BlockState defaultBlockState) {
    for (int i = 0; i < fastSections.length; i++)
      if (fastSections[i] != null) fastSections[i].recalculateCounts();

    HeightmapUtil.populateHeightmapPostNoise(
        chunk, Heightmap.Type.OCEAN_FLOOR_WG, defaultBlockState, FastNoiseGen.AIR);
    HeightmapUtil.populateHeightmapPostNoise(
        chunk, Heightmap.Type.WORLD_SURFACE_WG, defaultBlockState, FastNoiseGen.AIR);
  }
}
