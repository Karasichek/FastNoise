package org.codeberg.zenxarch.zconfig.loader;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import org.codeberg.zenxarch.zconfig.entry.BooleanConfigEntry;
import org.codeberg.zenxarch.zconfig.entry.IntConfigEntry;

public class Config {
  private final ConfigLoader loader;
  private final OverrideCollector overrideCollector;

  public final UnmodifiableCommentedConfig CONFIG_WITH_OVERRIDES;

  public Config(ConfigLoader loader, OverrideCollector collector) {
    this.loader = loader;
    this.overrideCollector = collector;
    CONFIG_WITH_OVERRIDES = getConfigWithOverrides();
  }

  private UnmodifiableCommentedConfig getConfigWithOverrides() {
    var result = CommentedConfig.inMemory();
    result.addAll(loader.getConfig());

    overrideCollector.collectOverrides(result);
    overrideCollector.collectIncompats(result, loader.getEntries().entries());

    return result.unmodifiable();
  }

  public boolean get(BooleanConfigEntry entry) {
    return CONFIG_WITH_OVERRIDES.get(entry.key());
  }

  public int get(IntConfigEntry entry) {
    return CONFIG_WITH_OVERRIDES.get(entry.key());
  }

  public Object2BooleanMap<String> loadMixinEntries() {
    var result = new Object2BooleanArrayMap<String>();
    for (var entry : this.loader.getEntries().entries()) {
      if (!entry.isMixin()) continue;
      boolean r = get(entry);
      result.put(entry.key(), r);
    }

    return Object2BooleanMaps.unmodifiable(result);
  }
}
