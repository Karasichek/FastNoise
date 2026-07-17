package org.codeberg.zenxarch.fastnoise.mixin.debug.disable_carve_features_and_entities;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.AbstractChunkHolder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerating;
import net.minecraft.world.chunk.ChunkGenerationContext;
import net.minecraft.world.chunk.ChunkGenerationStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerating.class)
public abstract class ChunkGeneratingMixin {

  @Overwrite
  public static CompletableFuture<Chunk> carve(
      final ChunkGenerationContext context,
      final ChunkGenerationStep step,
      final BoundedRegionArray<AbstractChunkHolder> chunks,
      final Chunk chunk) {
    Heightmap.populateHeightmaps(
        chunk,
        EnumSet.of(
            Type.MOTION_BLOCKING,
            Type.MOTION_BLOCKING_NO_LEAVES,
            Type.OCEAN_FLOOR,
            Type.WORLD_SURFACE));
    return CompletableFuture.completedFuture(chunk);
  }

  @Inject(
      method = {"generateFeatures", "generateEntities"},
      at = @At("HEAD"),
      cancellable = true)
  private static void noopStuff(CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
    cir.setReturnValue(CompletableFuture.completedFuture(null));
  }
}
