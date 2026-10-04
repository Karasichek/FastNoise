package org.codeberg.zenxarch.matcomp.config;

import org.codeberg.zenxarch.matcomp.MaterialRuleCompilerConstants;
import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;
import org.codeberg.zenxarch.zconfig.loader.Config;
import org.codeberg.zenxarch.zconfig.loader.ConfigLoader;
import org.codeberg.zenxarch.zconfig.loader.OverrideCollector;

public class MaterialRuleCompilerConfig {
  public static final Config CONFIG =
      new Config(
          new ConfigLoader(
              MaterialRuleCompilerConstants.MOD_ID, MaterialRuleCompilerConfigEntries.INSTANCE),
          new OverrideCollector(MaterialRuleCompilerConstants.MOD_ID));

  static boolean get(BooleanConfigEntry entry) {
    return CONFIG.get(entry);
  }

  static int get(IntConfigEntry entry) {
    return CONFIG.get(entry);
  }

  public static final boolean OPTIMIZE_BIOME_ACCESS =
      get(MaterialRuleCompilerConfigEntries.OPTIMIZE_BIOME_ACCESS);

  public static final int MAX_CACHE_SIZE =
      scaleByMemory(get(MaterialRuleCompilerConfigEntries.MAX_CACHE_SIZE_PER_GB));

  static {
    MaterialRuleCompilerConstants.LOGGER.info("Surface rule cache size {}", MAX_CACHE_SIZE);
  }

  private static int scaleByMemory(int value) {
    var memory = Runtime.getRuntime().maxMemory() / (1024.0 * 1024 * 1024);
    return (int) (value * memory);
  }
}
