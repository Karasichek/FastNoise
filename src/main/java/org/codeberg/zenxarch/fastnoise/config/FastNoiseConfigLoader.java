package org.codeberg.zenxarch.fastnoise.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.util.Optional;
import net.neoforged.fml.loading.FMLPaths;
import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;

public final class FastNoiseConfigLoader {
  private static final String configFileName = FastNoiseConstants.MOD_ID + ".mixin.toml";
  private static final String VERSION_KEY = "version";
  private static final long CONFIG_VERSION = 1;

  public static final CommentedFileConfig CONFIG = getConfig();

  static {
    loadDefaults();
  }

  private static CommentedFileConfig getConfig() {
    return CommentedFileConfig.of(FMLPaths.CONFIGDIR.get().resolve(configFileName));
  }

  private static void loadBoolean(BooleanConfigEntry entry) {
    Optional<Boolean> value = CONFIG.getOptional(entry.key());
    if (value.isEmpty()) {
      CONFIG.set(entry.key(), entry.defaultValue());
    }
    CONFIG.setComment(entry.key(), entry.comment());
  }

  private static void initVersion() {
    CONFIG.set(FastNoiseConfigEntries.MIXIN_PERF_SURFACE.key(), true);
    CONFIG.set(FastNoiseConfigEntries.OPTIMIZE_BIOME_ACCESS.key(), true);
    CONFIG.set(FastNoiseConfigEntries.OPTIMIZE_END_BIOMES.key(), true);
    CONFIG.set(FastNoiseConfigEntries.OPTIMIZE_FIXED_BIOMES.key(), true);

    CONFIG.set(VERSION_KEY, 1L);
  }

  private static void updateConfig() {
    if (!CONFIG.contains(VERSION_KEY)) {
      initVersion();
    }
    if (CONFIG.getLong(VERSION_KEY) == CONFIG_VERSION) return;

    CONFIG.set(VERSION_KEY, CONFIG_VERSION);
  }

  private static void loadDefaults() {
    CONFIG.load();
    if (CONFIG.isEmpty()) CONFIG.set(VERSION_KEY, CONFIG_VERSION);
    for (var entry : FastNoiseConfigEntries.ENTRIES) {
      loadBoolean(entry);
    }
    updateConfig();
    CONFIG.save();
  }
}
