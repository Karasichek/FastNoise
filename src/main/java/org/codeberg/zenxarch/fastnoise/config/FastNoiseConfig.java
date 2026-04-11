package org.codeberg.zenxarch.fastnoise.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue.CvArray;
import net.fabricmc.loader.api.metadata.CustomValue.CvObject;
import net.fabricmc.loader.api.metadata.CustomValue.CvType;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;

public class FastNoiseConfig {
  private static final String overridesKey = FastNoiseConstants.MOD_ID + ":overrides";

  private static void collectOverrides(
      CommentedConfig config, ModMetadata meta, String key, boolean value) {
    if (!config.contains(key)) {
      FastNoiseConstants.LOGGER.error("Mod {} tried to override unknown key {}", meta.getId(), key);
    } else {
      FastNoiseConstants.LOGGER.info(
          "Mod {} override key {} with value {}", meta.getId(), key, value);
      config.set(key, value);
    }
  }

  private static void collectOverrides(CommentedConfig config, ModMetadata meta, CvArray array) {
    for (var value : array) {
      if (value.getType() != CvType.STRING) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has array of not strings as overrides", meta.getId());
        continue;
      }
      collectOverrides(config, meta, value.getAsString(), false);
    }
  }

  private static void collectOverrides(CommentedConfig config, ModMetadata meta, CvObject object) {
    for (var value : object) {
      if (value.getValue().getType() != CvType.BOOLEAN) {
        FastNoiseConstants.LOGGER.error(
            "Mod {} has object of not booleans as overrides", meta.getId());
        continue;
      }
      collectOverrides(config, meta, value.getKey(), value.getValue().getAsBoolean());
    }
  }

  private static void collectOverrides(CommentedConfig config) {
    for (var container : FabricLoader.getInstance().getAllMods()) {
      var meta = container.getMetadata();
      if (!meta.containsCustomValue(overridesKey)) continue;

      var value = meta.getCustomValue(overridesKey);
      switch (value.getType()) {
        case CvType.OBJECT -> collectOverrides(config, meta, value.getAsObject());
        case CvType.ARRAY -> collectOverrides(config, meta, value.getAsArray());
        case CvType.STRING -> collectOverrides(config, meta, value.getAsString(), false);
        default ->
            FastNoiseConstants.LOGGER.error(
                "Mod {} has unsupported overrides of type {}", meta.getId(), value.getType());
      }
    }
  }

  private static void collectIncompats(CommentedConfig config) {
    var loader = FabricLoader.getInstance();
    for (var entry : FastNoiseConfigEntries.ENTRIES) {
      for (var modId : entry.incompats()) {
        if (loader.isModLoaded(modId)) config.set(entry.key(), false);
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
