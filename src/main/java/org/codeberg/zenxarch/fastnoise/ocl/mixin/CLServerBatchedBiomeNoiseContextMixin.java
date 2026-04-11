package org.codeberg.zenxarch.fastnoise.ocl.mixin;

import com.ishland.c2me.opts.accel.opencl.common.ducks.PalettedContainerExtension;
import com.ishland.c2me.opts.accel.opencl.common.gen.CLServerBatchedBiomeNoiseContext;
import com.ishland.c2me.opts.dfc.common.gen.opencl.CLBlockStateMappings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CLServerBatchedBiomeNoiseContext.class)
public abstract class CLServerBatchedBiomeNoiseContextMixin {

  @Shadow @Final private ChunkPos startingPos;

  @Shadow private static final BlockState AIR = Blocks.AIR.getDefaultState();

  @Shadow private static final int BATCH_SHIFT = Integer.bitCount(3);
  @Shadow private static final int BATCH_SIZE = 4; // must be power of 2, determines axis size
  @Shadow private static final int BATCH_MASK = BATCH_SIZE - 1;

  @WrapOperation(method = "writeBlocks")
  private void zenxarch$writeBlocks(
      BoundedRegionArray<ProtoChunk> chunks,
      CLBlockStateMappings clBlockStateMappings,
      int verticalSize,
      ChunkGeneratorSettings settings,
      int horizontalSize,
      ByteBuffer blockOutBufferData,
      Operation<Void> op) {
    // OCL Copy
    ProtoChunk startingChunk = chunks.get(this.startingPos.x(), this.startingPos.z());

    int curSectionIndex = startingChunk.getSectionIndex(startingChunk.getBottomY());
    ChunkSection[] chunkSection = new ChunkSection[BATCH_SIZE * BATCH_SIZE];
    int writeProtectedMask = 0;

    BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
      for (int chunkOffX = 0; chunkOffX < BATCH_SIZE; chunkOffX++) {
        int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffX;
        ProtoChunk chunk =
            chunks.get(this.startingPos.x() + chunkOffX, this.startingPos.z() + chunkOffZ);
        boolean writeProtected = chunk.getStatus().isAtLeast(ChunkStatus.NOISE);
        writeProtectedMask |= writeProtected ? (1 << idx) : 0;
        if (writeProtected) continue;
        chunkSection[idx] = chunk.getSection(curSectionIndex);
        chunkSection[idx].lock();
      }
    }

    for (int y = 0; y < verticalSize; y++) {
      int blockY = settings.generationShapeConfig().minimumY() + y;
      int sectionIndex = startingChunk.getSectionIndex(blockY);
      if (sectionIndex != curSectionIndex) {
        for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
          for (int chunkOffX = 0; chunkOffX < BATCH_SIZE; chunkOffX++) {
            int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffX;
            if ((writeProtectedMask & (1L << idx)) != 0) continue;
            chunkSection[idx].calculateCounts();
            chunkSection[idx].unlock();
            chunkSection[idx] =
                chunks
                    .get(this.startingPos.x() + chunkOffX, this.startingPos.z() + chunkOffZ)
                    .getSection(sectionIndex);
            chunkSection[idx].lock();
          }
        }
        curSectionIndex = sectionIndex;
      }

      for (int z = 0; z < horizontalSize; z++) {
        for (int x = 0; x < horizontalSize; x++) {
          int index = y * horizontalSize * horizontalSize + z * horizontalSize + x;
          byte value = blockOutBufferData.get(index);
          int chunkOffX = (x >> 4) & BATCH_MASK;
          int chunkOffZ = (z >> 4) & BATCH_MASK;
          int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffX;
          if ((writeProtectedMask & (1L << idx)) != 0) continue;
          boolean needsFluidTick = (value & (byte) ((byte) 1) << ((byte) 7)) != 0;
          int blockIdx = value & ((byte) ~(((byte) 1) << ((byte) 7)));
          BlockState blockState = clBlockStateMappings.getBlockState(blockIdx);
          if (blockState != AIR) {
            ((PalettedContainerExtension<BlockState>) chunkSection[idx].getBlockStateContainer())
                .c2me$setUnsafe(x & 15, blockY & 15, z & 15, blockState);
            if (needsFluidTick) {
              ProtoChunk chunk =
                  chunks.get(this.startingPos.x() + chunkOffX, this.startingPos.z() + chunkOffZ);
              mutablePos.set(
                  x + chunk.getPos().getStartX(), blockY, z + chunk.getPos().getStartZ());
              chunk.markBlockForPostProcessing(mutablePos);
            }
          }
        }
      }
    }

    for (int chunkOffZ = 0; chunkOffZ < BATCH_SIZE; chunkOffZ++) {
      for (int chunkOffX = 0; chunkOffX < BATCH_SIZE; chunkOffX++) {
        int idx = (chunkOffZ << BATCH_SHIFT) + chunkOffX;
        if ((writeProtectedMask & (1L << idx)) != 0) continue;

        chunkSection[idx].calculateCounts();
        chunkSection[idx].unlock();

        ProtoChunk chunk =
            chunks.get(this.startingPos.x() + chunkOffX, this.startingPos.z() + chunkOffZ);
        Heightmap.populateHeightmaps(
            chunk, EnumSet.of(Heightmap.Type.OCEAN_FLOOR_WG, Heightmap.Type.WORLD_SURFACE_WG));
        chunk.setStatus(ChunkStatus.NOISE);
      }
    }
  }
}
