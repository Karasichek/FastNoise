package org.codeberg.zenxarch.fastnoise.mixin.perf.noise;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.SharedConstants;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.codeberg.zenxarch.fastnoise.noise.FastNoiseGen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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

  @WrapMethod(method = "method_38332")
  private Chunk zenxarch$populateNoise(
      Chunk chunk,
      int cellHeight,
      GenerationShapeConfig generationShapeConfig,
      int minimumY,
      Blender blender,
      StructureAccessor structureAccessor,
      NoiseConfig noiseConfig,
      int minimumCellY,
      Operation<Chunk> op) {
    if (((Object) this.getClass()) != NoiseChunkGenerator.class)
      return op.call(
          chunk,
          cellHeight,
          generationShapeConfig,
          minimumY,
          blender,
          structureAccessor,
          noiseConfig,
          minimumCellY);
    if (SharedConstants.isOutsideGenerationArea(chunk.getPos())) return chunk;

    var defaultBlock = settings.value().defaultBlock();

    if (SharedConstants.AQUIFERS
        || defaultBlock == FastNoiseGen.AIR
        || chunk.hasBelowZeroRetrogen()
        || !FastNoiseGen.isEmpty(chunk))
      return this.populateNoise(
          blender, structureAccessor, noiseConfig, chunk, minimumCellY, cellHeight);

    var sampler =
        chunk.getOrCreateChunkNoiseSampler(
            chunkx ->
                this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig));

    FastNoiseGen.populateNoise(sampler, defaultBlock, chunk, minimumCellY, cellHeight);

    return chunk;
  }
}
