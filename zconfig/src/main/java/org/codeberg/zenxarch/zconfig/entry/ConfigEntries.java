package org.codeberg.zenxarch.zconfig.entry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.function.Consumer;

public class ConfigEntries {
  private final Object2ObjectOpenHashMap<String, BooleanConfigEntry> ENTRIES =
      new Object2ObjectOpenHashMap<>();
  private final Object2ObjectOpenHashMap<String, IntConfigEntry> INT_ENTRIES =
      new Object2ObjectOpenHashMap<>();

  protected BooleanConfigEntry createEntry(
      String key, Consumer<BooleanConfigEntry.Builder> builderFunc) {
    if (ENTRIES.containsKey(key)) return ENTRIES.get(key);
    var builder = BooleanConfigEntry.builder(key);
    builderFunc.accept(builder);
    var res = builder.build();
    ENTRIES.put(key, res);
    return res;
  }

  protected IntConfigEntry createIntEntry(
      String key, Consumer<IntConfigEntry.Builder> builderFunc) {
    if (INT_ENTRIES.containsKey(key)) return INT_ENTRIES.get(key);
    var builder = IntConfigEntry.builder(key);
    builderFunc.accept(builder);
    var res = builder.build();
    INT_ENTRIES.put(key, res);
    return res;
  }

  public Collection<BooleanConfigEntry> entries() {
    return this.ENTRIES.values();
  }

  public Collection<IntConfigEntry> intEntries() {
    return this.INT_ENTRIES.values();
  }
}
