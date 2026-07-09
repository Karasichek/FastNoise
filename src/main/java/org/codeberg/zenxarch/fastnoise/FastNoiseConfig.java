package org.codeberg.zenxarch.fastnoise;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

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
    var configPath = FMLPaths.CONFIGDIR.get().resolve(configFileName);

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
      Object2BooleanArrayMap<String> map, ModInfo info, String key, boolean value) {
    if (!map.containsKey(key)) {
      FastNoiseConstants.LOGGER.error(
          "Mod {} tried to override unknown key {}", info.getModId(), key);
    } else {
      FastNoiseConstants.LOGGER.info(
          "Mod {} override key {} with value {}", info.getModId(), key, value);
      map.put(key, value);
    }
  }

  private static void collectOverrides(
      Object2BooleanArrayMap<String> map, ModInfo info, List<?> array) {
    for (var value : array) {
      if (!(value instanceof String string)) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has array of not strings as overrides", info.getModId());
        continue;
      }
      collectOverrides(map, info, string, false);
    }
  }

  private static void collectOverrides(
      Object2BooleanArrayMap<String> map, ModInfo info, Map<?, ?> object) {
    for (var value : object.keySet()) {
      if (!(value instanceof String key)) continue;
      if (!(object.get(value) instanceof Boolean bl)) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has object of not booleans as overrides", info.getModId());
        continue;
      }
      collectOverrides(map, info, key, bl);
    }
  }

  private static void collectOverrides(Object2BooleanArrayMap<String> map) {
    for (var container : FMLLoader.getLoadingModList().getMods()) {
      var meta = container.getConfigElement(overridesKey);
      if (meta.isEmpty()) continue;

      var value = meta.get();
      if (value instanceof Map<?, ?> otherMap) collectOverrides(map, container, otherMap);
      else if (value instanceof List<?> list) collectOverrides(map, container, list);
      else if (value instanceof String string) collectOverrides(map, container, string, false);
      else {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has unsupported overrides of type {}",
            container.getModId(),
            value.getClass().getSimpleName());
      }
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
