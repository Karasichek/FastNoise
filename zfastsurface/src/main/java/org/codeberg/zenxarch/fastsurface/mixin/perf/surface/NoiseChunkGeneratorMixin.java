package org.codeberg.zenxarch.fastsurface.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Set;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastsurface.surface.FastSurfaceGen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

  @WrapOperation(
      method = "buildSurface",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/gen/surfacebuilder/SurfaceBuilder;buildSurface(Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/world/gen/HeightContext;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler;Lnet/minecraft/world/gen/surfacebuilder/MaterialRule;Ljava/util/Set;)V"))
  private void zenxarch$buildSurface(
      final SurfaceBuilder surfaceBuilder,
      final NoiseConfig noiseConfig,
      final BiomeAccess biomeAccess,
      final HeightContext heightContext,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final MaterialRule materialRule,
      final @Nullable Set<RegistryEntry<Biome>> includedBiomes,
      Operation<Void> op) {
    if (!FastSurfaceGen.canUseSurfaceBuilder(chunk, includedBiomes)) {
      op.call(
          surfaceBuilder,
          noiseConfig,
          biomeAccess,
          heightContext,
          chunk,
          chunkNoiseSampler,
          materialRule,
          includedBiomes);
      return;
    }

    FastSurfaceGen.buildSurface(
        surfaceBuilder,
        noiseConfig,
        biomeAccess,
        heightContext,
        chunk,
        chunkNoiseSampler,
        materialRule,
        includedBiomes);
  }
}
