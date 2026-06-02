package org.codeberg.zenxarch.fastnoise;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionOptions;

public record BenchmarkSettings(
    ChunkRegion region,
    ChunkRegion biomeRegion,
    long seed,
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

  private static ChunkRegion overworldRegion() {
    return ChunkRegion.of(worldCenter - WorldRadiusInChunks, worldCenter + WorldRadiusInChunks);
  }

  private static ChunkRegion overworldBiomeRegion() {
    return ChunkRegion.of(
        worldCenter - WorldRadiusInChunks - 1, worldCenter + WorldRadiusInChunks + 1);
  }

  private static ChunkRegion endRegion() {
    return ChunkRegion.of(endCenter - WorldRadiusInChunks, endCenter + WorldRadiusInChunks);
  }

  private static ChunkRegion endBiomeRegion() {
    return ChunkRegion.of(endCenter - WorldRadiusInChunks - 1, endCenter + WorldRadiusInChunks + 1);
  }

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
        overworldRegion(), overworldBiomeRegion(), seed, DimensionOptions.OVERWORLD);
  }

  public static BenchmarkSettings nether(long seed) {
    return new BenchmarkSettings(
        overworldRegion(), overworldBiomeRegion(), seed, DimensionOptions.NETHER);
  }

  public static BenchmarkSettings end(long seed) {
    return new BenchmarkSettings(endRegion(), endBiomeRegion(), seed, DimensionOptions.END);
  }

  public static record ChunkRegion(ChunkPos min, ChunkPos max) {
    public static ChunkRegion of(int min, int max) {
      return new ChunkRegion(new ChunkPos(min, min), new ChunkPos(max, max));
    }
  }
}
