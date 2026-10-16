package org.codeberg.zenxarch.fastsurface.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;

public class FastSurfaceConfigEntries extends ConfigEntries {
  public static final FastSurfaceConfigEntries INSTANCE = new FastSurfaceConfigEntries();

  public static final BooleanConfigEntry MIXIN_SURFACE =
      INSTANCE.createEntry(
          "mixin.perf.surface",
          builder ->
              builder
                  .addComment("Replace buildSurface with optimized implementation")
                  .defaultValue(true)
                  .addIncompat("biolith"));

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
}
