package org.codeberg.zenxarch.fastnoise.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;

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

  public static final BooleanConfigEntry SKIP_TRIVIAL_SURFACE_BUILDER =
      of(
          "perf.surface.trivial",
          "Skip trivial surface builder. Major speedup for end dimension. Depends on surface"
              + " builder\n"
              + " (depends on surface builder mixin)",
          true);

  public static final BooleanConfigEntry OPTIMIZE_BIOME_ACCESS =
      of(
          "perf.surface.biome",
          "Predict biomes in advance in surface builder. Major speedup for surface builder"
              + "\n (depends on surface builder mixin)",
          true);

  public static final BooleanConfigEntry MIXIN_PERF_BIOMES =
      mixin("perf.biome", "Replace populateBiomes with optimized implementation", true);

  public static final BooleanConfigEntry MIXIN_PERF_NOISE =
      mixin("perf.noise", "Replace populateNoise with optimized implementation", true);

  public static final BooleanConfigEntry MIXIN_PERF_SURFACE =
      mixin("perf.surface", "Replace buildSurface with optimized implementation", true, "biolith");

  public static final BooleanConfigEntry MIXIN_DEBUG_DISABLE_CARVE_FEATURES_AND_ENTITIES =
      mixin(
          "debug.disable_carve_features_and_entities",
          "Disable carve, features and entites chunk generation step",
          false);

  public static final IntConfigEntry MAX_CACHE_SIZE_PER_GB =
      INSTANCE.createIntEntry(
          "perf.surface.max_cache_size_per_gb",
          builder ->
              builder
                  .addComment("Max size of surface rule compiler cache per gb of allocated memory")
                  .defaultValue(100)
                  .constraints(16, 65536));

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

  private static BooleanConfigEntry mixin(
      String key, String comment, boolean defaultValue, String... incompats) {
    return INSTANCE.createEntry(
        "mixin." + key,
        (builder) -> builder.addComment(comment).defaultValue(defaultValue).addIncompat(incompats));
  }
}
