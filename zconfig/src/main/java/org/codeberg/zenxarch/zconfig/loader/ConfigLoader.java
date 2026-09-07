package org.codeberg.zenxarch.zconfig.loader;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;

public class ConfigLoader {
  private final String configFileName;
  private final ConfigEntries entries;

  private final CommentedFileConfig CONFIG;

  public ConfigLoader(String configFileName, ConfigEntries entries) {
    this.configFileName = configFileName + ".mixin.toml";
    this.entries = entries;
    this.CONFIG = loadConfig();
    loadDefaults();
  }

  private CommentedFileConfig loadConfig() {
    return CommentedFileConfig.of(
        FabricLoader.getInstance().getConfigDir().resolve(configFileName));
  }

  public CommentedFileConfig getConfig() {
    return CONFIG;
  }

  public ConfigEntries getEntries() {
    return entries;
  }

  private void loadBoolean(BooleanConfigEntry entry) {
    Optional<Boolean> value = CONFIG.getOptional(entry.key());
    if (value.isEmpty()) {
      CONFIG.set(entry.key(), entry.defaultValue());
    }
    CONFIG.setComment(entry.key(), entry.comment());
  }

  private void loadInteger(IntConfigEntry entry) {
    Optional<Integer> value = CONFIG.getOptional(entry.key());
    if (value.isEmpty()) {
      CONFIG.set(entry.key(), entry.defaultValue());
    } else {
      CONFIG.set(entry.key(), entry.validate(value.get()));
    }
    CONFIG.setComment(entry.key(), entry.comment());
  }

  private void loadDefaults() {
    CONFIG.load();
    for (var entry : entries.entries()) {
      loadBoolean(entry);
    }
    for (var entry : entries.intEntries()) {
      loadInteger(entry);
    }
    CONFIG.save();
  }
}
