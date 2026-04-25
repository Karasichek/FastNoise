package org.codeberg.zenxarch.fastnoise.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastnoise.mixin.SurfaceBuilderAccessor;
import org.codeberg.zenxarch.fastnoise.surface.FastSurfaceGen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin {

  @Final private BlockState defaultState;

  @WrapMethod(method = "buildSurface")
  public void zenxarch$buildSurface(
      final NoiseConfig noiseConfig,
      final BiomeAccess biomeAccess,
      final boolean useLegacyRandom,
      final HeightContext heightContext,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final MaterialRules.MaterialRule materialRule,
      final Set<RegistryEntry<Biome>> includedBiomes,
      Operation<Void> op) {
    if (!FastSurfaceGen.canUseSurfaceBuilder(chunk)) {
      op.call(
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
        (SurfaceBuilderAccessor) this,
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
