package org.codeberg.zenxarch.fastnoise.surface;

import net.minecraft.util.math.MathHelper;

public class AllocFreeRandom {
  public static float nextFloatXoroshiro(long seedLo, long seedHi, int x, int y, int z) {
    seedLo = MathHelper.hashCode(x, y, z) ^ seedLo;

    if ((seedLo | seedHi) == 0L) {
      seedLo = -7046029254386353131L;
      seedHi = 7640891576956012809L;
    }

    final var next = Long.rotateLeft(seedLo + seedHi, 17) + seedLo;
    return (float) (next >>> (64 - 24)) * 5.9604645E-8F;
  }

  public static float nextFloatChecked(long seed, int x, int y, int z) {
    seed = MathHelper.hashCode(x, y, z) ^ seed;
    seed = (seed ^ 25214903917L) & 281474976710655L;

    seed = seed * 25214903917L + 11L & 281474976710655L;

    var next = (int) (seed >> 48 - 24);

    return next * 5.9604645E-8F;
  }
}
