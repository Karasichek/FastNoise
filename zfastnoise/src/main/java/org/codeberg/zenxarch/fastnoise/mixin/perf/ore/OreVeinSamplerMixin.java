package org.codeberg.zenxarch.fastnoise.mixin.perf.ore;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.OreVeinSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler.BlockStateSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.codeberg.zenxarch.fastnoise.noise.OreVeinSamplerImpl;
import org.codeberg.zenxarch.fastnoise.util.AllocFreeRandom.XoroshiroSeed;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(OreVeinSampler.class)
public abstract class OreVeinSamplerMixin {
  @WrapMethod(method = "create")
  private static BlockStateSampler zenxarch$create(
      final DensityFunction veinToggle,
      final DensityFunction veinRidged,
      final DensityFunction veinGap,
      final RandomSplitter randomDeriver,
      Operation<BlockStateSampler> op) {
    if (SharedConstants.ORE_VEINS) return op.call(veinToggle, veinRidged, veinGap, randomDeriver);

    if (randomDeriver instanceof Xoroshiro128PlusPlusRandom.Splitter xoroshiro) {
      return new OreVeinSamplerImpl(
          veinToggle, veinRidged, veinGap, xoroshiro.seedLo, xoroshiro.seedHi, new XoroshiroSeed());
    }
    return op.call(veinToggle, veinRidged, veinGap, randomDeriver);
  }
}
