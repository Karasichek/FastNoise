package org.codeberg.zenxarch.fastnoise.config;

import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;
import org.codeberg.zenxarch.zconfig.loader.Config;
import org.codeberg.zenxarch.zconfig.loader.ConfigLoader;
import org.codeberg.zenxarch.zconfig.loader.OverrideCollector;

public class FastNoiseConfig extends Config {

  public FastNoiseConfig(ConfigLoader loader, OverrideCollector collector) {
    super(loader, collector);
  }

  public static final FastNoiseConfig CONFIG =
      new FastNoiseConfig(
          new ConfigLoader(FastNoiseConstants.MOD_ID, FastNoiseConfigEntries.INSTANCE),
          new OverrideCollector(FastNoiseConstants.MOD_ID));
}
