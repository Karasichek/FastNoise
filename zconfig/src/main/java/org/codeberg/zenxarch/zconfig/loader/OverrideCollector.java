package org.codeberg.zenxarch.zconfig.loader;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.util.Collection;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue.CvArray;
import net.fabricmc.loader.api.metadata.CustomValue.CvObject;
import net.fabricmc.loader.api.metadata.CustomValue.CvType;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.codeberg.zenxarch.zconfig.ZConfig;
import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;

public final class OverrideCollector {
  private final String overridesKey;

  public OverrideCollector(String modId) {
    this.overridesKey = modId + ":overrides";
  }

  private void collectOverrides(
      CommentedConfig config, ModMetadata meta, String key, boolean value) {
    if (!config.contains(key)) {
      ZConfig.LOGGER.error("Mod {} tried to override unknown key {}", meta.getId(), key);
    } else {
      ZConfig.LOGGER.info("Mod {} override key {} with value {}", meta.getId(), key, value);
      config.set(key, value);
    }
  }

  private void collectOverrides(CommentedConfig config, ModMetadata meta, CvArray array) {
    for (var value : array) {
      if (value.getType() != CvType.STRING) {
        ZConfig.LOGGER.error("Mod {} has array of not strings as overrides", meta.getId());
        continue;
      }
      collectOverrides(config, meta, value.getAsString(), false);
    }
  }

  private void collectOverrides(CommentedConfig config, ModMetadata meta, CvObject object) {
    for (var value : object) {
      if (value.getValue().getType() != CvType.BOOLEAN) {
        ZConfig.LOGGER.error("Mod {} has object of not booleans as overrides", meta.getId());
        continue;
      }
      collectOverrides(config, meta, value.getKey(), value.getValue().getAsBoolean());
    }
  }

  public void collectOverrides(CommentedConfig config) {
    for (var container : FabricLoader.getInstance().getAllMods()) {
      var meta = container.getMetadata();
      if (!meta.containsCustomValue(overridesKey)) continue;

      var value = meta.getCustomValue(overridesKey);
      switch (value.getType()) {
        case CvType.OBJECT -> collectOverrides(config, meta, value.getAsObject());
        case CvType.ARRAY -> collectOverrides(config, meta, value.getAsArray());
        case CvType.STRING -> collectOverrides(config, meta, value.getAsString(), false);
        default ->
            ZConfig.LOGGER.error(
                "Mod {} has unsupported overrides of type {}", meta.getId(), value.getType());
      }
    }
  }

  public void collectIncompats(CommentedConfig config, Collection<BooleanConfigEntry> entries) {
    var loader = FabricLoader.getInstance();
    for (var entry : entries) {
      for (var modId : entry.incompats()) {
        if (loader.isModLoaded(modId)) config.set(entry.key(), false);
      }
    }
  }
}
