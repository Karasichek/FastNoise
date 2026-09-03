package org.codeberg.zenxarch.fastnoise.mixin.perf.biome;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.chunk.Chunk;
import org.codeberg.zenxarch.fastnoise.biome.FastBiomeGen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Chunk.class)
public abstract class ChunkMixin {

  @WrapMethod(method = "populateBiomes")
  private void zenxarch$populateBiomes(
      Chunk chunk,
      final BiomeSupplier biomeSupplier,
      final MultiNoiseSampler sampler,
      Operation<Void> op) {
    FastBiomeGen.populateBiomes(chunk, biomeSupplier, sampler);
  }
}
