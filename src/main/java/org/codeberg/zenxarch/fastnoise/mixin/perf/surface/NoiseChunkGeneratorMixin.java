package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.codeberg.zenxarch.fastnoise.mixin.SurfaceBuilderAccessor;
import org.codeberg.zenxarch.fastnoise.surface.FastSurfaceGen;
import org.codeberg.zenxarch.fastnoise.surface.biome.FastBiomeAccumulator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

  @Shadow @Final private RegistryEntry<ChunkGeneratorSettings> settings;

  @Shadow
  private ChunkNoiseSampler createChunkNoiseSampler(
      final Chunk chunk,
      final StructureAccessor world,
      final Blender blender,
      final NoiseConfig noiseConfig) {
    throw new IllegalStateException("Mixin class code called");
  }

  @WrapOperation(
      method =
          "buildSurface(Lnet/minecraft/world/ChunkRegion;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/chunk/Chunk;)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/gen/chunk/NoiseChunkGenerator;buildSurface(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/HeightContext;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/registry/Registry;Lnet/minecraft/world/gen/chunk/Blender;)V"))
  private void zenxarch$buildSurface(
      final NoiseChunkGenerator chunkGenerator,
      final Chunk chunk,
      final HeightContext context,
      final NoiseConfig noiseConfig,
      final StructureAccessor structureAccessor,
      final BiomeAccess biomeAccess,
      final Registry<Biome> registry,
      final Blender blender,
      Operation<Void> op,
      @Local final ChunkRegion chunkRegion) {

    var includedBiomes = FastBiomeAccumulator.accumulate(chunkRegion, chunk.getPos());

    if (!FastSurfaceGen.canUseSurfaceBuilder(chunk, includedBiomes, registry)) {
      op.call(
          chunkGenerator,
          chunk,
          context,
          noiseConfig,
          structureAccessor,
          biomeAccess,
          registry,
          blender);
      return;
    }

    final var chunks = FastSurfaceGen.collectChunks(chunkRegion, chunk);

    if (chunks == null) {
      op.call(
          chunkGenerator,
          chunk,
          context,
          noiseConfig,
          structureAccessor,
          biomeAccess,
          registry,
          blender);
      return;
    }

    var sampler =
        chunk.getOrCreateChunkNoiseSampler(
            chunkx ->
                this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig));

    var settings = this.settings.value();

    FastSurfaceGen.buildSurface(
        (SurfaceBuilderAccessor) noiseConfig.getSurfaceBuilder(),
        noiseConfig,
        biomeAccess,
        settings.usesLegacyRandom(),
        context,
        chunk,
        sampler,
        settings.surfaceRule(),
        includedBiomes,
        chunks,
        registry);
  }
}
