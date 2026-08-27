package org.codeberg.zenxarch.matcomp.config;

import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.ConfigEntries;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;

public class MaterialRuleCompilerConfigEntries extends ConfigEntries {
  public static final MaterialRuleCompilerConfigEntries INSTANCE =
      new MaterialRuleCompilerConfigEntries();

  public static final IntConfigEntry MAX_CACHE_SIZE_PER_GB =
      INSTANCE.createIntEntry(
          "perf.surface.max_cache_size_per_gb",
          builder ->
              builder
                  .addComment("Max size of surface rule compiler cache per gb of allocated memory")
                  .defaultValue(100)
                  .constraints(16, 65536));

  public static final BooleanConfigEntry MIXIN_COMPILER =
      INSTANCE.createEntry(
          "mixin.perf.compiler",
          builder -> builder.addComment("Enable material rule compiler?").defaultValue(true));

  public static final BooleanConfigEntry MIXIN_SURFACE =
      INSTANCE.createEntry(
          "mixin.perf.surface",
          builder -> builder.addComment("Optimize surface rules").defaultValue(true));
}
