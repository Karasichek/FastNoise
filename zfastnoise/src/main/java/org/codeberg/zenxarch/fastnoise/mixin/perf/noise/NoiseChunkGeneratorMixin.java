package org.codeberg.zenxarch.fastnoise.mixin.perf.noise;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
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
          "method_1_10091(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/chunk/GenerationShapeConfig;Lnet/minecraft/world/biome/source/BiomeAccess;Ljava/util/Set;Lnet/minecraft/world/ChunkRegion;)Lnet/minecraft/world/chunk/Chunk;",
      at = @At(target = "Lnet/minecraft/world/chunk/ChunkSection;lock()V", value = "INVOKE"))
  private void zenxarch$cancelLocking(ChunkSection section) {}

  @Redirect(
      method =
          "method_1_10091(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/chunk/GenerationShapeConfig;Lnet/minecraft/world/biome/source/BiomeAccess;Ljava/util/Set;Lnet/minecraft/world/ChunkRegion;)Lnet/minecraft/world/chunk/Chunk;",
      at = @At(target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", value = "INVOKE"))
  private boolean zenxarch$cancelSetAdd(Set<?> set, Object unused) {
    return false;
  }

  @WrapMethod(method = "populateNoise")
  private void zenxarch$populateNoise(
      final ChunkNoiseSampler chunkNoiseSampler, final Chunk chunk, final Operation<Void> op) {
    var defaultBlock = this.settings.value().defaultBlock();
    if (((Object) this.getClass()) != NoiseChunkGenerator.class
        || SharedConstants.DEBUG_AQUIFERS
        || defaultBlock == FastNoiseGen.AIR
        || chunk.hasBelowZeroRetrogen()
        || !FastNoiseGen.isEmpty(chunk)) {
      op.call(chunkNoiseSampler, chunk);
      return;
    }

    FastNoiseGen.populateNoise(
        chunkNoiseSampler, chunk, defaultBlock, this.settings.value().noiseRouter().finalDensity());
  }
}
