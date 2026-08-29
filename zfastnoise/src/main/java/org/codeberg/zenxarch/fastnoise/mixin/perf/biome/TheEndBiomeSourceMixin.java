package org.codeberg.zenxarch.fastnoise.mixin.perf.biome;

import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import org.codeberg.zenxarch.fastnoise.biome.TheEndBiomeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {
  @Overwrite
  public BiomeSupplier getBiomeSupplier(final MultiNoiseSampler sampler) {
    return new TheEndBiomeSupplier((TheEndBiomeSource) (Object) this, sampler);
  }
}
