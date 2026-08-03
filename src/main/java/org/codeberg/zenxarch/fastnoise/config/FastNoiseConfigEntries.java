package org.codeberg.zenxarch.fastnoise.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;

public class FastNoiseConfigEntries extends ConfigEntries {
  public static final FastNoiseConfigEntries INSTANCE = new FastNoiseConfigEntries();

  public final BooleanConfigEntry MIXIN_BIOME =
      this.createEntry("mixin.perf.biome", builder -> builder.defaultValue(true));

  public final BooleanConfigEntry MIXIN_NOISE =
      this.createEntry("mixin.perf.noise", builder -> builder.defaultValue(true));
}
