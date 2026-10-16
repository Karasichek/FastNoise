package org.codeberg.zenxarch.matcomp.mixin;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MaterialRuleContext.class)
public abstract class MaterialRuleContextMixin implements MaterialRuleContextExtension {
  @Unique public Set<RegistryEntry<Biome>> zenxarch$includedBiomes;

  @Shadow public Supplier<RegistryEntry<Biome>> biomeSupplier;

  @Unique @Final public Registry<Biome> biomeRegistry;

  @Inject(at = @At("TAIL"), method = "<init>")
  public void zenxarch$captureRegistry(
      final SurfaceBuilder surfaceBuilder,
      final NoiseConfig noiseConfig,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      final Registry<Biome> biomeRegistry,
      final HeightContext heightContext,
      CallbackInfo ci) {
    this.biomeRegistry = biomeRegistry;
  }

  @Override
  public Registry<Biome> zenxarch$getRegistry() {
    return biomeRegistry;
  }

  @Override
  public RegistryEntry<Biome> zenxarch$getCurrentBiome() {
    return this.biomeSupplier.get();
  }

  @Override
  public Set<RegistryEntry<Biome>> zenxarch$getIncludedBiomes() {
    return this.zenxarch$includedBiomes;
  }

  @Override
  public void zenxarch$setIncludedBiomes(Set<RegistryEntry<Biome>> biomes) {
    this.zenxarch$includedBiomes = biomes;
  }
}
