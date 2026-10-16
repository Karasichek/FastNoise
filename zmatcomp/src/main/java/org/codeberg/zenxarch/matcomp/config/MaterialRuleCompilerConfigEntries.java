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
                  .defaultValue(512)
                  .constraints(16, 1048576));

  public static final IntConfigEntry MAX_INVOCATIONS_BEFORE_COMPILE =
      INSTANCE.createIntEntry(
          "perf.compiler.invocations",
          builder ->
              builder
                  .addComment("No of invocations before a block state rule is compiled")
                  .defaultValue(256)
                  .constraints(2, 1048576));

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

  public static final BooleanConfigEntry MIXIN_PACKED =
      INSTANCE.createEntry(
          "mixin.perf.packed",
          builder ->
              builder
                  .addComment("Pack the collection of possible biomes to avoid non-occuring biomes")
                  .addComment(
                      "Because of how vanilla generates chunks, almost all chunk sections will"
                          + " have a reference to plains biome")
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
