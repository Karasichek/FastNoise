package org.codeberg.zenxarch.fastnoise.mixin.debug.disable_carve_features_and_entities;

import java.util.concurrent.CompletableFuture;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerating;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerating.class)
public abstract class ChunkGeneratingMixin {
  @Inject(
      method = {"generateFeatures", "generateEntities"},
      at = @At("HEAD"),
      cancellable = true)
  private static void noopStuff(CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
    cir.setReturnValue(CompletableFuture.completedFuture(null));
  }
}
