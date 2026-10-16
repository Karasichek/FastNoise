package org.codeberg.zenxarch.fastnoise.noise;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.OreVeinSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler.BlockStateSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunction.NoisePos;
import org.codeberg.zenxarch.fastnoise.util.AllocFreeRandom;
import org.codeberg.zenxarch.fastnoise.util.AllocFreeRandom.XoroshiroSeed;
import org.jetbrains.annotations.Nullable;

public record OreVeinSamplerImpl(
    DensityFunction veinToggle,
    DensityFunction veinRidged,
    DensityFunction veinGap,
    long seedLo,
    long seedHi,
    XoroshiroSeed seed)
    implements BlockStateSampler {

  @Override
  public @Nullable BlockState sample(NoisePos pos) {
    double oreVeininessNoiseValue = veinToggle.sample(pos);
    int posY = pos.blockY();
    var veinType =
        oreVeininessNoiseValue > 0.0
            ? OreVeinSampler.VeinType.COPPER
            : OreVeinSampler.VeinType.IRON;
    double veininessRidged = Math.abs(oreVeininessNoiseValue);
    int distanceFromTop = veinType.maxY - posY;
    int distanceFromBottom = posY - veinType.minY;
    if (distanceFromBottom >= 0 && distanceFromTop >= 0) {
      int distanceFromEdge = Math.min(distanceFromTop, distanceFromBottom);
      double edgeRoundoff = MathHelper.clampedMap((double) distanceFromEdge, 0.0, 20.0, -0.2, 0.0);
      if (veininessRidged + edgeRoundoff < 0.4F) return null;
      AllocFreeRandom.splitXoroshiro(seedLo, seedHi, pos.blockX(), posY, pos.blockZ(), seed);
      if (AllocFreeRandom.nextFloatXoroshiro(seed) > 0.7F) return null;
      if (veinRidged.sample(pos) >= 0.0) return null;
      double richness = MathHelper.clampedMap(veininessRidged, 0.4F, 0.6F, 0.1F, 0.3F);
      if (AllocFreeRandom.nextFloatXoroshiro(seed) < richness && veinGap.sample(pos) > -0.3F)
        return AllocFreeRandom.nextFloatXoroshiroLast(seed) < 0.02F
            ? veinType.rawOreBlock
            : veinType.ore;
      return veinType.stone;
    }
    return null;
  }
}
