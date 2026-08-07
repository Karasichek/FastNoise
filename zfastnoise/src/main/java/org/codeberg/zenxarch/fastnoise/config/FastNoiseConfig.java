package org.codeberg.zenxarch.fastnoise.config;

import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;
import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;
import org.codeberg.zenxarch.zconfig.loader.Config;
import org.codeberg.zenxarch.zconfig.loader.ConfigLoader;
import org.codeberg.zenxarch.zconfig.loader.OverrideCollector;

public class FastNoiseConfig {
  public static final Config CONFIG =
      new Config(
          new ConfigLoader(FastNoiseConstants.MOD_ID, FastNoiseConfigEntries.INSTANCE),
          new OverrideCollector(FastNoiseConstants.MOD_ID));

  static boolean get(BooleanConfigEntry entry) {
    return CONFIG.get(entry);
  }

  static int get(IntConfigEntry entry) {
    return CONFIG.get(entry);
  }

  public static final boolean OPTIMIZE_END_BIOMES = get(FastNoiseConfigEntries.OPTIMIZE_END_BIOMES);
  public static final boolean OPTIMIZE_BIOME_TREE = get(FastNoiseConfigEntries.OPTIMIZE_BIOME_TREE);
  public static final boolean OPTIMIZE_FIXED_BIOMES =
      get(FastNoiseConfigEntries.OPTIMIZE_FIXED_BIOMES);
  public static final boolean SKIP_TRIVIAL_SURFACE_BUILDER =
      get(FastNoiseConfigEntries.SKIP_TRIVIAL_SURFACE_BUILDER);
  public static final boolean OPTIMIZE_BIOME_ACCESS =
      get(FastNoiseConfigEntries.OPTIMIZE_BIOME_ACCESS);

  public static final int MAX_CACHE_SIZE =
      scaleByMemory(get(FastNoiseConfigEntries.MAX_CACHE_SIZE_PER_GB));

  private static int scaleByMemory(int value) {
    var memory = Runtime.getRuntime().maxMemory() / (1024.0 * 1024 * 1024);
    return (int) (value * memory);
  }
}
