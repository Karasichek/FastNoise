package org.codeberg.zenxarch.fastnoise.mixin.perf.noise;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.codeberg.zenxarch.fastnoise.noise.FastChunkSection;
import org.codeberg.zenxarch.fastnoise.noise.FastWorldgen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

  @Shadow @Final private RegistryEntry<ChunkGeneratorSettings> settings;

  @Shadow
  protected abstract ChunkNoiseSampler createChunkNoiseSampler(
      Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig);

  @Shadow
  protected abstract Chunk populateNoise(
      Blender blender,
      StructureAccessor structureAccessor,
      NoiseConfig noiseConfig,
      Chunk chunk,
      int minimumCellY,
      int cellHeight);

  @Redirect(
      method =
          "populateNoise(Ljava/util/concurrent/Executor;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;",
      at = @At(target = "Lnet/minecraft/world/chunk/ChunkSection;lock()V", value = "INVOKE"))
  private void zenxarch$cancelLock(ChunkSection section) {}

  @Redirect(
      method =
          "populateNoise(Ljava/util/concurrent/Executor;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;",
      at = @At(target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", value = "INVOKE"))
  private boolean zenxarch$cancelAdd(Set<ChunkSection> set, Object section) {
    return false;
  }

  @WrapMethod(method = "method_38332")
  private Chunk zenxarch$populateNoise(
      Blender blender,
      StructureAccessor structureAccessor,
      NoiseConfig noiseConfig,
      Chunk chunk,
      int minimumCellY,
      int cellHeight,
      Operation<Chunk> op) {

    if (((Object) this.getClass()) != NoiseChunkGenerator.class) {
      return op.call(blender, structureAccessor, noiseConfig, chunk, minimumCellY, cellHeight);
    }

    if (SharedConstants.isOutsideGenerationArea(chunk.getPos())) return chunk;

    var defaultBlock = settings.value().defaultBlock();

    if (defaultBlock == FastWorldgen.AIR
        || chunk.hasBelowZeroRetrogen()
        || !FastWorldgen.isEmpty(chunk))
      return this.populateNoise(
          blender, structureAccessor, noiseConfig, chunk, minimumCellY, cellHeight);

    var fastSections = new FastChunkSection[chunk.getSectionArray().length];

    var chunkNoiseSampler =
        chunk.getOrCreateChunkNoiseSampler(
            chunkx ->
                this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig));
    BlockState defaultBlockState = settings.value().defaultBlock();

    FastWorldgen.populateNoise(
        chunkNoiseSampler,
        defaultBlockState,
        chunk,
        minimumCellY,
        cellHeight,
        fastSections,
        chunk.getBottomY());

    return chunk;
  }
}
