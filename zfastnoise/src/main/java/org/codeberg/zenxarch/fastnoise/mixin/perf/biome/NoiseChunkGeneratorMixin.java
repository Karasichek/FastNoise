package org.codeberg.zenxarch.fastnoise.mixin.perf.biome;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.codeberg.zenxarch.fastnoise.mixin.ChunkGeneratorAccessor;
import org.codeberg.zenxarch.fastnoise.noise.FastBiomeGen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

  @Shadow @Final private RegistryEntry<ChunkGeneratorSettings> settings;

  @Shadow
  private ChunkNoiseSampler createChunkNoiseSampler(
      final Chunk chunk,
      final StructureAccessor world,
      final Blender blender,
      final NoiseConfig noiseConfig) {
    throw new IllegalStateException("Mixin method called");
  }

  @WrapMethod(
      method =
          "populateBiomes(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V")
  private void zenxarch$populateBiomes(
      final Blender blender,
      final NoiseConfig noiseConfig,
      final StructureAccessor structureAccessor,
      final Chunk chunk,
      Operation<Void> op) {
    var sampler =
        chunk.getOrCreateChunkNoiseSampler(
            chunkx ->
                this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig));

    var original = ((ChunkGeneratorAccessor) this).zenxarch$getBiomeSource();

    var supplier = BelowZeroRetrogen.getBiomeSupplier(blender.getBiomeSupplier(original), chunk);

    if (supplier != original) {
      op.call(blender, noiseConfig, structureAccessor, chunk);
      return;
    }

    FastBiomeGen.populateBiomes(chunk, supplier, sampler, noiseConfig, this.settings);
  }
}
