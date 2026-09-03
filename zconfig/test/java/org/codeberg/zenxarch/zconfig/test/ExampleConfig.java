package org.codeberg.zenxarch.zconfig.test;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;
import org.codeberg.zenxarch.zconfig.loader.Config;
import org.codeberg.zenxarch.zconfig.loader.ConfigLoader;
import org.codeberg.zenxarch.zconfig.loader.OverrideCollector;
import org.junit.jupiter.api.Test;

public class ExampleConfig extends ConfigEntries {
  public static final ExampleConfig INSTANCE = new ExampleConfig();

  public static final BooleanConfigEntry MAIN =
      INSTANCE.createEntry(
          "main",
          builder -> builder.addComment("Main entry").defaultValue(false).addIncompat("sodium"));

  public static final IntConfigEntry OTHER =
      INSTANCE.createIntEntry(
          "other", builder -> builder.addComment("Other entry").defaultValue(0).constraints(-1, 1));

  public static final Config CONFIG =
      new Config(
          new ConfigLoader("example_mod_id", INSTANCE), new OverrideCollector("example_mod_id"));

  @Test
  public void loadConfig() {
    IO.println(CONFIG.get(MAIN));
    IO.println(CONFIG.get(OTHER));
    IO.println(CONFIG.loadMixinEntries());
  }
}
