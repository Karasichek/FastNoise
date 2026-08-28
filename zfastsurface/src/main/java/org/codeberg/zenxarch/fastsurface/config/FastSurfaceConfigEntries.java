package org.codeberg.zenxarch.fastsurface.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;

public class FastSurfaceConfigEntries extends ConfigEntries {
  public static final FastSurfaceConfigEntries INSTANCE = new FastSurfaceConfigEntries();

  public static final IntConfigEntry MAX_CACHE_SIZE_PER_GB =
      INSTANCE.createIntEntry(
          "perf.surface.max_cache_size_per_gb",
          builder ->
              builder
                  .addComment("Max size of surface rule compiler cache per gb of allocated memory")
                  .defaultValue(100)
                  .constraints(16, 65536));

  public static final BooleanConfigEntry MIXIN_SURFACE =
      INSTANCE.createEntry(
          "mixin.perf.surface",
          builder ->
              builder
                  .addComment("Replace buildSurface with optimized implementation")
                  .defaultValue(true));

  public static final BooleanConfigEntry SKIP_TRIVIAL_SURFACE_BUILDER =
      INSTANCE.createEntry(
          "perf.surface.trivial",
          builder ->
              builder
                  .addComment(
                      "Skip trivial surface builder. Major speedup for end dimension. Depends on"
                          + " surface builder")
                  .addComment(" (depends on surface builder mixin)")
                  .defaultValue(true));

  public static final BooleanConfigEntry OPTIMIZE_BIOME_ACCESS =
      INSTANCE.createEntry(
          "perf.surface.biome",
          builder ->
              builder
                  .addComment(
                      "Predict biomes in advance in surface builder. Major speedup for surface"
                          + " builder")
                  .addComment(" (depends on surface builder mixin)")
                  .defaultValue(true));
}
