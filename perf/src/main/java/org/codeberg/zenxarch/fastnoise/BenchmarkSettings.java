package org.codeberg.zenxarch.fastnoise;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

public record BenchmarkSettings(
    int centerX,
    int centerZ,
    int radius,
    long seed,
    RegistryKey<World> worldKey,
    RegistryKey<DimensionOptions> dimensionOptions) {

  private static int getIntProperty(String name, int def, int min) {
    var value = System.getProperty(name);
    if (value == null) return def;
    try {
      return Math.max(Integer.parseInt(value), min);
    } catch (Exception e) {
      return def;
    }
  }

  private static long getLongProperty(String name, int def, int min) {
    var value = System.getProperty(name);
    if (value == null) return def;
    try {
      return Math.max(Long.parseLong(value), min);
    } catch (Exception e) {
      return def;
    }
  }

  public static final long defaultSeed = getLongProperty("zseed", 100, 0);
  private static final int WorldRadiusInChunks = getIntProperty("zworldradius", 16, 0);
  private static final int endCenter = getIntProperty("zendcenter", 128, Integer.MIN_VALUE);
  private static final int worldCenter = getIntProperty("zworldcenter", 0, Integer.MIN_VALUE);

  public static final long seedCount = getLongProperty("zseedcount", 16, 1);

  public static BenchmarkSettings overworld() {
    return overworld(defaultSeed);
  }

  public static BenchmarkSettings nether() {
    return nether(defaultSeed);
  }

  public static BenchmarkSettings end() {
    return end(defaultSeed);
  }

  public static BenchmarkSettings overworld(long seed) {
    return new BenchmarkSettings(
        worldCenter,
        worldCenter,
        WorldRadiusInChunks,
        seed,
        World.OVERWORLD,
        DimensionOptions.OVERWORLD);
  }

  public static BenchmarkSettings nether(long seed) {
    return new BenchmarkSettings(
        worldCenter, worldCenter, WorldRadiusInChunks, seed, World.NETHER, DimensionOptions.NETHER);
  }

  public static BenchmarkSettings end(long seed) {
    return new BenchmarkSettings(
        endCenter, endCenter, WorldRadiusInChunks, seed, World.END, DimensionOptions.END);
  }
}
