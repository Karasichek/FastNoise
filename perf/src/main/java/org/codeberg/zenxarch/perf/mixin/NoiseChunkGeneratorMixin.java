package org.codeberg.zenxarch.perf.mixin;

import java.util.concurrent.Executor;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {
  @ModifyArg(
      method =
          "Lnet/minecraft/world/gen/chunk/NoiseChunkGenerator;buildTerrain(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/world/ChunkRegion;Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
  private Executor redirectPopulateNoiseExecutor(Executor executor) {
    return Runnable::run;
  }
}
