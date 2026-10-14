package org.codeberg.zenxarch.fastsurface.config;

import org.codeberg.zenxarch.fastsurface.FastSurfaceConstants;
import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;
import org.codeberg.zenxarch.zconfig.loader.Config;
import org.codeberg.zenxarch.zconfig.loader.ConfigLoader;
import org.codeberg.zenxarch.zconfig.loader.OverrideCollector;

public class FastSurfaceConfig {
  public static final Config CONFIG =
      new Config(
          new ConfigLoader(FastSurfaceConstants.MOD_ID, FastSurfaceConfigEntries.INSTANCE),
          new OverrideCollector(FastSurfaceConstants.MOD_ID));

  static boolean get(BooleanConfigEntry entry) {
    return CONFIG.get(entry);
  }

  static int get(IntConfigEntry entry) {
    return CONFIG.get(entry);
  }

  public static final boolean SKIP_TRIVIAL_SURFACE_BUILDER =
      get(FastSurfaceConfigEntries.SKIP_TRIVIAL_SURFACE_BUILDER);
}
