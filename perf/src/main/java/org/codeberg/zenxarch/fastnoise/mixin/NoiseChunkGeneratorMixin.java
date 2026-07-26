package org.codeberg.zenxarch.fastnoise.mixin;

import java.util.concurrent.Executor;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {
  @ModifyArg(
      method =
          "populateNoise(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
  private Executor redirectPopulateNoiseExecutor(Executor executor) {
    return Runnable::run;
  }

  @ModifyArg(
      method =
          "populateBiomes(Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
  private Executor redirectBiomeExecutor(Executor executor) {
    return Runnable::run;
  }
}
