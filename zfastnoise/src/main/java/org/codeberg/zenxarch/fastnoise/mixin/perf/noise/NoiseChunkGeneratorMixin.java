package org.codeberg.zenxarch.fastnoise.mixin.perf.noise;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.codeberg.zenxarch.fastnoise.noise.FastNoiseGen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

  @Shadow @Final private RegistryEntry<ChunkGeneratorSettings> settings;

  @Redirect(
      method =
          "method_38332(Lnet/minecraft/world/chunk/Chunk;ILnet/minecraft/world/gen/chunk/GenerationShapeConfig;ILnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;I)Lnet/minecraft/world/chunk/Chunk;",
      at = @At(target = "Lnet/minecraft/world/chunk/ChunkSection;lock()V", value = "INVOKE"))
  private void zenxarch$cancelLocking(ChunkSection section) {}

  @Redirect(
      method =
          "method_38332(Lnet/minecraft/world/chunk/Chunk;ILnet/minecraft/world/gen/chunk/GenerationShapeConfig;ILnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;I)Lnet/minecraft/world/chunk/Chunk;",
      at = @At(target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", value = "INVOKE"))
  private boolean zenxarch$cancelSetAdd(Set<?> set, Object unused) {
    return false;
  }

  @Shadow
  protected abstract ChunkNoiseSampler createChunkNoiseSampler(
      Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig);

  @WrapMethod(
      method =
          "Lnet/minecraft/world/gen/chunk/NoiseChunkGenerator;populateNoise(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/chunk/Chunk;II)Lnet/minecraft/world/chunk/Chunk;")
  private Chunk zenxarch$populateNoise(
      final Blender blender,
      final StructureAccessor structureAccessor,
      final NoiseConfig noiseConfig,
      final Chunk chunk,
      final int minimumCellY,
      final int cellHeight,
      final Operation<Chunk> op) {

    if (SharedConstants.isOutsideGenerationArea(chunk.getPos())) return chunk;

    var defaultBlock = this.settings.value().defaultBlock();
    if (((Object) this.getClass()) != NoiseChunkGenerator.class
        || SharedConstants.AQUIFERS
        || defaultBlock == FastNoiseGen.AIR
        || chunk.hasBelowZeroRetrogen()
        || !FastNoiseGen.isEmpty(chunk)) {
      return op.call(blender, structureAccessor, noiseConfig, chunk, minimumCellY, cellHeight);
    }

    var sampler =
        chunk.getOrCreateChunkNoiseSampler(
            chunkx ->
                this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig));

    FastNoiseGen.populateNoise(sampler, chunk, defaultBlock, minimumCellY, cellHeight);
    return chunk;
  }
}
