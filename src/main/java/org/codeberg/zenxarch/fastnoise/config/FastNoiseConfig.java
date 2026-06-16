package org.codeberg.zenxarch.fastnoise.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;

public class FastNoiseConfig {
  private static final String overridesKey = FastNoiseConstants.MOD_ID + ":overrides";

  private static void collectOverrides(
      CommentedConfig config, ModInfo info, String key, boolean value) {
    if (!config.contains(key)) {
      FastNoiseConstants.LOGGER.error(
          "Mod {} tried to override unknown key {}", info.getModId(), key);
    } else {
      FastNoiseConstants.LOGGER.info(
          "Mod {} override key {} with value {}", info.getModId(), key, value);
      config.set(key, value);
    }
  }

  private static void collectOverrides(CommentedConfig config, ModInfo meta, List<?> array) {
    for (var value : array) {
      if (!(value instanceof String string)) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has array of not strings as overrides", meta.getModId());
        continue;
      }
      collectOverrides(config, meta, string, false);
    }
  }

  private static void collectOverrides(CommentedConfig config, ModInfo meta, Map<?, ?> object) {
    for (var value : object.keySet()) {
      if (!(value instanceof String key)) continue;
      if (!(object.get(value) instanceof Boolean bl)) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has object of not booleans as overrides", meta.getModId());
        continue;
      }
      collectOverrides(config, meta, key, bl);
    }
  }

  private static void collectOverrides(CommentedConfig config) {
    for (var container : FMLLoader.getCurrent().getLoadingModList().getMods()) {
      var meta = container.getConfigElement(overridesKey);
      if (meta.isEmpty()) continue;

      var value = meta.get();
      switch (value) {
        case Map<?, ?> otherMap -> collectOverrides(config, container, otherMap);
        case List<?> list -> collectOverrides(config, container, list);
        case String string -> collectOverrides(config, container, string, false);
        default ->
            FastNoiseConstants.LOGGER.error(
                "Mod {} has unsupported overrides of type {}",
                container.getModId(),
                value.getClass().getSimpleName());
      }
    }
  }

  private static void collectIncompats(CommentedConfig config) {
    var loader = FMLLoader.getCurrent();
    for (var entry : FastNoiseConfigEntries.ENTRIES) {
      for (var modId : entry.incompats()) {
        if (loader.getLoadingModList().getMods().stream()
            .anyMatch(mod -> mod.getModId().equals(modId))) config.set(entry.key(), false);
      }
    }
  }

  private static UnmodifiableCommentedConfig getConfigWithOverrides() {
    var result = CommentedConfig.inMemory();
    result.addAll(FastNoiseConfigLoader.CONFIG);

    collectOverrides(result);
    collectIncompats(result);

    return result.unmodifiable();
  }

  public static final UnmodifiableCommentedConfig CONFIG_WITH_OVERRIDES = getConfigWithOverrides();

  static boolean get(BooleanConfigEntry entry) {
    return CONFIG_WITH_OVERRIDES.get(entry.key());
  }

  public static final boolean OPTIMIZE_END_BIOMES = get(FastNoiseConfigEntries.OPTIMIZE_END_BIOMES);
  public static final boolean OPTIMIZE_BIOME_TREE = get(FastNoiseConfigEntries.OPTIMIZE_BIOME_TREE);
  public static final boolean OPTIMIZE_FIXED_BIOMES =
      get(FastNoiseConfigEntries.OPTIMIZE_FIXED_BIOMES);
  public static final boolean SKIP_TRIVIAL_SURFACE_BUILDER =
      get(FastNoiseConfigEntries.SKIP_TRIVIAL_SURFACE_BUILDER);
  public static final boolean OPTIMIZE_BIOME_ACCESS =
      get(FastNoiseConfigEntries.OPTIMIZE_BIOME_ACCESS);

  public static Object2BooleanMap<String> loadConfig() {
    var result = new Object2BooleanArrayMap<String>();
    for (var entry : FastNoiseConfigEntries.ENTRIES) {
      if (!entry.isMixin()) continue;
      boolean r = get(entry);
      result.put(entry.key(), r);
    }

    return Object2BooleanMaps.unmodifiable(result);
  }
}
