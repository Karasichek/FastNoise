package org.codeberg.zenxarch.matcomp.rules;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.DensityProvider;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom.XoroshiroSeed;
import org.jspecify.annotations.Nullable;

public record OreVeinMaterialRule(
    long seedLo,
    long seedHi,
    XoroshiroSeed seed,
    BlockState oreBlock,
    BlockState rawOreBlock,
    BlockState fillerBlock,
    float rawOreChance,
    DensityProvider densitySampler,
    DensityProvider richnessSampler,
    DensityProvider fillerGapSampler)
    implements BlockStateRule {

  public static OreVeinMaterialRule create(
      Xoroshiro128PlusPlusRandom.Splitter xoroshiro,
      BlockState oreBlock,
      BlockState rawOreBlock,
      BlockState fillerBlock,
      float rawOreChance,
      DensityProvider densitySampler,
      DensityProvider richnessSampler,
      DensityProvider fillerGapSampler) {
    return new OreVeinMaterialRule(
        xoroshiro.seedLo,
        xoroshiro.seedHi,
        new XoroshiroSeed(),
        oreBlock,
        rawOreBlock,
        fillerBlock,
        rawOreChance,
        densitySampler,
        richnessSampler,
        fillerGapSampler);
  }

  @Override
  public @Nullable BlockState tryApply(int x, int y, int z) {
    float density = densitySampler.get();
    if (density <= 0.0F) return null;
    AllocFreeRandom.splitXoroshiro(seedLo, seedHi, x, y, z, seed);

    if (AllocFreeRandom.nextFloatXoroshiro(AllocFreeRandom.nextXoroshiro(seed)) > density)
      return null;

    if (AllocFreeRandom.nextFloatXoroshiro(AllocFreeRandom.nextXoroshiro(seed))
            < richnessSampler.get()
        && fillerGapSampler.get() < 0.0F)
      return AllocFreeRandom.nextFloatXoroshiro(
                  AllocFreeRandom.nextXoroshiro(seed.seedLo, seed.seedHi))
              < this.rawOreChance
          ? this.rawOreBlock
          : this.oreBlock;

    return this.fillerBlock;
  }
}
