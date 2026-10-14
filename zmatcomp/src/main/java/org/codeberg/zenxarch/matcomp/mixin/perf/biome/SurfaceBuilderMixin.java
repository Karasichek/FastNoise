package org.codeberg.zenxarch.matcomp.mixin.perf.biome;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.matcomp.biome.FastPosToBiome;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin {

  @WrapOperation(
      method = "buildSurface",
      at =
          @At(
              value = "NEW",
              target =
                  "Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRuleContext;"))
  private MaterialRuleContext zenxarch$addIncludedBiomes(
      final SurfaceBuilder builder,
      final NoiseConfig noiseConfig,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      final Registry<Biome> biomeRegistry,
      final HeightContext heightContext,
      final Operation<MaterialRuleContext> op,
      @Local final BiomeAccess biomeAccess) {
    Set<RegistryEntry<Biome>> includedBiomes = null;

    if (biomeAccess.storage instanceof ChunkRegion region) {
      includedBiomes = FastPosToBiome.getBiomes(region, chunk.getPos(), 1);
    }

    var res =
        op.call(
            builder,
            noiseConfig,
            chunk,
            chunkNoiseSampler,
            FastPosToBiome.createPosToBiome(biomeAccess, chunk, includedBiomes),
            biomeRegistry,
            heightContext);
    ((MaterialRuleContextExtension) res).zenxarch$setIncludedBiomes(includedBiomes);
    return res;
  }
}
