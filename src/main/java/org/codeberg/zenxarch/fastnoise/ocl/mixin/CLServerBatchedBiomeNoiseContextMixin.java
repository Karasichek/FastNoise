package org.codeberg.zenxarch.fastnoise.ocl.mixin;

import com.ishland.c2me.opts.accel.opencl.common.gen.CLServerBatchedBiomeNoiseContext;
import com.ishland.c2me.opts.dfc.common.gen.opencl.CLBlockStateMappings;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.ByteBuffer;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.codeberg.zenxarch.fastnoise.ocl.FastCopyBufferDataIntoChunks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CLServerBatchedBiomeNoiseContext.class)
public abstract class CLServerBatchedBiomeNoiseContextMixin {

  @Shadow @Final private ChunkPos startingPos;

  @WrapMethod(method = "writeBlocks")
  private void zenxarch$writeBlocks(
      BoundedRegionArray<ProtoChunk> chunks,
      CLBlockStateMappings clBlockStateMappings,
      int verticalSize,
      ChunkGeneratorSettings settings,
      int horizontalSize,
      ByteBuffer blockOutBufferData,
      Operation<Void> op) {
    FastCopyBufferDataIntoChunks.copyData(
        chunks,
        clBlockStateMappings,
        verticalSize,
        settings,
        horizontalSize,
        blockOutBufferData,
        startingPos);
  }
}
