package org.codeberg.zenxarch.fastnoise.mixin;

import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkNoiseSampler.class)
public interface ChunkNoiseSamplerAccessor {
  @Invoker("createMultiNoiseSampler")
  public MultiNoiseSampler zenxarch$createMultiNoiseSampler(final NoiseRouter noiseRouter);
}
