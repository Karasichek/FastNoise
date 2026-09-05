package org.codeberg.zenxarch.matcomp.util;

import net.minecraft.util.math.MathHelper;

public class AllocFreeRandom {

  public static final class XoroshiroSeed {
    public long seedLo;
    public long seedHi;
  }

  public static void splitXoroshiro(
      long seedLo, long seedHi, int x, int y, int z, XoroshiroSeed seed) {
    seedLo = MathHelper.hashCode(x, y, z) ^ seedLo;

    if ((seedLo | seedHi) == 0L) {
      seedLo = -7046029254386353131L;
      seedHi = 7640891576956012809L;
    }

    seed.seedLo = seedLo;
    seed.seedHi = seedHi;
  }

  public static long nextXoroshiro(XoroshiroSeed seed) {
    long s0 = seed.seedLo;
    long s1 = seed.seedHi;
    long result = Long.rotateLeft(s0 + s1, 17) + s0;
    s1 ^= s0;
    seed.seedLo = Long.rotateLeft(s0, 49) ^ s1 ^ s1 << 21;
    seed.seedHi = Long.rotateLeft(s1, 28);
    return result;
  }

  public static long nextXoroshiro(long s0, long s1) {
    return Long.rotateLeft(s0 + s1, 17) + s0;
  }

  public static float nextFloatXoroshiro(long result) {
    return (float) (result >>> 64 - 24) * 5.9604645E-8F;
  }

  public static float nextFloatXoroshiro(long seedLo, long seedHi, int x, int y, int z) {
    seedLo = MathHelper.hashCode(x, y, z) ^ seedLo;

    if ((seedLo | seedHi) == 0L) {
      seedLo = -7046029254386353131L;
      seedHi = 7640891576956012809L;
    }

    return nextFloatXoroshiro(nextXoroshiro(seedLo, seedHi));
  }

  public static float nextFloatChecked(long seed, int x, int y, int z) {
    seed = MathHelper.hashCode(x, y, z) ^ seed;
    seed = (seed ^ 25214903917L) & 281474976710655L;

    seed = seed * 25214903917L + 11L & 281474976710655L;

    var next = (int) (seed >> 48 - 24);

    return next * 5.9604645E-8F;
  }

  public static double nextDoubleXoroshiro(long seedLo, long seedHi, int x, int y, int z) {
    seedLo = MathHelper.hashCode(x, y, z) ^ seedLo;

    if ((seedLo | seedHi) == 0L) {
      seedLo = -7046029254386353131L;
      seedHi = 7640891576956012809L;
    }

    final var next = Long.rotateLeft(seedLo + seedHi, 17) + seedLo;
    return (next >>> (64 - 53)) * 1.110223E-16F;
  }

  public static double nextDoubleChecked(long seed, int x, int y, int z) {
    seed = MathHelper.hashCode(x, y, z) ^ seed;
    seed = (seed ^ 25214903917L) & 281474976710655L;

    seed = seed * 25214903917L + 11L & 281474976710655L;

    var upper = (int) (seed >> 48 - 26);

    seed = seed * 25214903917L + 11L & 281474976710655L;
    var lower = (int) (seed >> 48 - 27);

    long combined = ((long) upper << 27) + lower;
    return combined * 1.110223E-16F;
  }
}
