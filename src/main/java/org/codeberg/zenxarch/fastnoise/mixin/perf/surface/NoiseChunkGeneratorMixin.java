package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Set;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.codeberg.zenxarch.fastnoise.surface.FastBiomeAccumulator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

  @WrapOperation(
      method =
          "buildSurface(Lnet/minecraft/world/ChunkRegion;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/chunk/Chunk;)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/gen/chunk/NoiseChunkGenerator;getBiomesAround(Lnet/minecraft/world/ChunkRegion;I)Ljava/util/Set;"))
  private Set<RegistryEntry<Biome>> zenxarch$getBiomesAround(
      ChunkRegion region, int radius, Operation<Set<RegistryEntry<Biome>>> op) {
    if (radius != 1) return op.call(region, radius);
    return FastBiomeAccumulator.accumulate(region);
  }
}
