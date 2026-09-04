package org.codeberg.zenxarch.fastnoise.mixin;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunkGenerator.class)
public interface NoiseChunkGeneratorAccessor {
  @Invoker(value = "method_38332")
  public Chunk zenxarch$method_38332(
      Chunk chunk,
      int cellHeight,
      GenerationShapeConfig generationShapeConfig,
      int minimumY,
      Blender blender,
      StructureAccessor structureAccessor,
      NoiseConfig noiseConfig,
      int minimumCellY);
}
