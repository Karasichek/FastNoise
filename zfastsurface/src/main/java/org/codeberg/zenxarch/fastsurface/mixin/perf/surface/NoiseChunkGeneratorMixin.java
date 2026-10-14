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
      method =
          "buildSurface(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/HeightContext;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/world/gen/chunk/Blender;Ljava/util/Set;)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/gen/surfacebuilder/SurfaceBuilder;buildSurface(Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/biome/source/BiomeAccess;ZLnet/minecraft/world/gen/HeightContext;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler;Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRule;Ljava/util/Set;)V"))
  private void zenxarch$buildSurface(
      final SurfaceBuilder surfaceBuilder,
      final NoiseConfig noiseConfig,
      final BiomeAccess biomeAccess,
      final boolean useLegacyRandom,
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
          useLegacyRandom,
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
        useLegacyRandom,
        heightContext,
        chunk,
        chunkNoiseSampler,
        materialRule,
        includedBiomes);
  }
}
