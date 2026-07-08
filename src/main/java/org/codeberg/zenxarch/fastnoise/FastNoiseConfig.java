package org.codeberg.zenxarch.fastnoise;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue.CvArray;
import net.fabricmc.loader.api.metadata.CustomValue.CvObject;
import net.fabricmc.loader.api.metadata.CustomValue.CvType;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class FastNoiseConfig {
  private static final String configFileName = FastNoiseConstants.MOD_ID + ".mixin.properties";

  private static final String overridesKey = FastNoiseConstants.MOD_ID + ":overrides";

  private static final String[] keys = new String[] {"mixin.perf.noise", "mixin.perf.biome"};

  private static final Object2BooleanMap<String> defaults =
      new Object2BooleanArrayMap<>(
          Map.of(
              keys[0], true,
              keys[1], true));

  private static Path getConfigPath() {
    var configPath = FabricLoader.getInstance().getConfigDir().resolve(configFileName);

    if (!Files.exists(configPath)) {
      try {
        Files.createFile(configPath);
      } catch (Exception e) {
        FastNoiseConstants.LOGGER.error("Cannot create file " + configPath.toString(), e);
        return null;
      }
    }

    if (!Files.isRegularFile(configPath)) {
      FastNoiseConstants.LOGGER.error("Config file " + configPath.toString() + " is not a file");
      return null;
    }

    return configPath;
  }

  private static Properties loadProperties() {
    var result = new Properties();
    var configPath = getConfigPath();

    if (configPath == null) return result;

    try {
      result.load(Files.newInputStream(configPath));
    } catch (Exception e) {
      FastNoiseConstants.LOGGER.error("Cannot read file " + configPath.toString(), e);
      return result;
    }

    return result;
  }

  private static void saveProperties(Properties props) {
    var configPath = getConfigPath();

    if (configPath == null) return;

    try {
      props.store(Files.newOutputStream(configPath), "");
    } catch (Exception e) {
      FastNoiseConstants.LOGGER.error("Cannot write file " + configPath.toString(), e);
    }
  }

  private static void collectOverrides(
      Object2BooleanArrayMap<String> map, ModMetadata meta, String key, boolean value) {
    if (!map.containsKey(key)) {
      FastNoiseConstants.LOGGER.error("Mod {} tried to override unknown key {}", meta.getId(), key);
    } else {
      FastNoiseConstants.LOGGER.info(
          "Mod {} override key {} with value {}", meta.getId(), key, value);
      map.put(key, value);
    }
  }

  private static void collectOverrides(
      Object2BooleanArrayMap<String> map, ModMetadata meta, CvArray array) {
    for (var value : array) {
      if (value.getType() != CvType.STRING) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has array of not strings as overrides", meta.getId());
        continue;
      }
      collectOverrides(map, meta, value.getAsString(), false);
    }
  }

  private static void collectOverrides(
      Object2BooleanArrayMap<String> map, ModMetadata meta, CvObject object) {
    for (var value : object) {
      if (value.getValue().getType() != CvType.BOOLEAN) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has object of not booleans as overrides", meta.getId());
        continue;
      }
      collectOverrides(map, meta, value.getKey(), value.getValue().getAsBoolean());
    }
  }

  private static void collectOverrides(Object2BooleanArrayMap<String> map) {
    for (var container : FabricLoader.getInstance().getAllMods()) {
      var meta = container.getMetadata();
      if (!meta.containsCustomValue(overridesKey)) continue;

      var value = meta.getCustomValue(overridesKey);
      switch (value.getType()) {
        case CvType.OBJECT -> collectOverrides(map, meta, value.getAsObject());
        case CvType.ARRAY -> collectOverrides(map, meta, value.getAsArray());
        case CvType.STRING -> collectOverrides(map, meta, value.getAsString(), false);
        default ->
            FastNoiseConstants.LOGGER.error(
                "Mod {} has unsupported overrides of type {}", meta.getId(), value.getType());
      }
      ;
    }
  }

  public static Object2BooleanMap<String> loadConfig() {
    var props = loadProperties();
    var result = new Object2BooleanArrayMap<String>();
    for (var key : keys) {
      boolean r = defaults.getOrDefault(key, true);
      try {
        r = Boolean.parseBoolean(props.getProperty(key, Boolean.toString(r)));
      } catch (Exception e) {

      }
      props.setProperty(key, Boolean.toString(r));
      result.put(key, r);
    }

    saveProperties(props);
    collectOverrides(result);
    return Object2BooleanMaps.unmodifiable(result);
  }
}
