package org.codeberg.zenxarch.matcomp.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;

public class MaterialRuleCompilerConfigEntries extends ConfigEntries {
  public static final MaterialRuleCompilerConfigEntries INSTANCE =
      new MaterialRuleCompilerConfigEntries();

  public static final IntConfigEntry MAX_CACHE_SIZE_PER_GB =
      INSTANCE.createIntEntry(
          "perf.compiler.max_cache_size_per_gb",
          builder ->
              builder
                  .addComment("Max size of surface rule compiler cache per gb of allocated memory")
                  .defaultValue(4096)
                  .constraints(16, 1048576));

  public static final IntConfigEntry MAX_DEPTH =
      INSTANCE.createIntEntry(
          "perf.compiler.max_depth",
          builder ->
              builder
                  .addComment("Max depth of surface rule program")
                  .defaultValue(7)
                  .constraints(2, 128));

  public static final BooleanConfigEntry MIXIN_COMPILER =
      INSTANCE.createEntry(
          "mixin.perf.compiler",
          builder -> builder.addComment("Enable material rule compiler?").defaultValue(true));

  public static final BooleanConfigEntry MIXIN_SURFACE =
      INSTANCE.createEntry(
          "mixin.perf.surface",
          builder -> builder.addComment("Optimize surface rules").defaultValue(true));

  public static final BooleanConfigEntry MIXIN_BIOME =
      INSTANCE.createEntry(
          "mixin.perf.biome",
          builder ->
              builder
                  .addComment("Replace biome access in material rule context w/ optimized variant")
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
