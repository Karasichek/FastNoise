package org.codeberg.zenxarch.fastsurface.mixin.perf.surface;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.fastsurface.surface.MaterialRuleContextHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin {

  @ModifyArg(
      method = "buildSurface",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRuleContext;<init>(Lnet/minecraft/world/gen/surfacebuilder/SurfaceBuilder;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/chunk/ChunkNoiseSampler;Ljava/util/function/Function;Lnet/minecraft/world/gen/HeightContext;Ljava/util/Set;)V"),
      index = 4)
  private Function<BlockPos, RegistryEntry<Biome>> zenxarch$setPosToBiome(
      Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      @Local BiomeAccess biomeAccess,
      @Local Chunk chunk,
      @Local Set<RegistryEntry<Biome>> includedBiomes) {
    return MaterialRuleContextHelper.createPosToBiome(biomeAccess, chunk, includedBiomes);
  }
}
