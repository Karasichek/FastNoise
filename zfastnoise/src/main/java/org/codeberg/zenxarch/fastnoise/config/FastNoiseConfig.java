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
}
