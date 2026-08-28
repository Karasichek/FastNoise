package org.codeberg.zenxarch.fastnoise.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;

public final class FastNoiseConfigEntries extends ConfigEntries {
  public static final FastNoiseConfigEntries INSTANCE = new FastNoiseConfigEntries();
  public static final BooleanConfigEntry OPTIMIZE_END_BIOMES =
      of(
          "perf.biomes.end",
          "Optimize end biome generation (2x speed) but may cause mod incompatibility",
          true,
          "biolith");

  public static final BooleanConfigEntry OPTIMIZE_BIOME_TREE =
      of(
          "perf.biomes.tree",
          "Optimize biome tree by avoiding multiple calls to thread local",
          false,
          "biolith");

  public static final BooleanConfigEntry OPTIMIZE_FIXED_BIOMES =
      of(
          "perf.biomes.fixed",
          "Optimize single biome generation (major speedup) but may cause mod incompatibility",
          true);

  public static final BooleanConfigEntry MIXIN_PERF_BIOMES =
      mixin("perf.biome", "Replace populateBiomes with optimized implementation", true);

  public static final BooleanConfigEntry MIXIN_PERF_NOISE =
      mixin("perf.noise", "Replace populateNoise with optimized implementation", true);

  public static final BooleanConfigEntry MIXIN_DEBUG_DISABLE_CARVE_FEATURES_AND_ENTITIES =
      mixin(
          "debug.disable_carve_features_and_entities",
          "Disable carve, features and entites chunk generation step",
          false);

  private static BooleanConfigEntry of(String key, String comment, boolean defaultValue) {
    return INSTANCE.createEntry(
        key, (builder) -> builder.addComment(comment).defaultValue(defaultValue));
  }

  private static BooleanConfigEntry of(
      String key, String comment, boolean defaultValue, String... incompats) {
    return INSTANCE.createEntry(
        key,
        (builder) -> builder.addComment(comment).defaultValue(defaultValue).addIncompat(incompats));
  }

  private static BooleanConfigEntry mixin(String key, String comment, boolean defaultValue) {
    return INSTANCE.createEntry(
        "mixin." + key, (builder) -> builder.addComment(comment).defaultValue(defaultValue));
  }
}
