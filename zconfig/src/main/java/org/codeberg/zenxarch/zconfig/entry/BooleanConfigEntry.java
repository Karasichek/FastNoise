package org.codeberg.zenxarch.zconfig.entry;

import java.util.ArrayList;
import java.util.List;

public record BooleanConfigEntry(
    String key, String comment, boolean defaultVal, List<String> incompats)
    implements ConfigEntry<Boolean> {

  public static Builder builder(String key) {
    return new Builder(key);
  }

  public boolean isMixin() {
    return this.key.startsWith("mixin.");
  }

  public Boolean defaultValue() {
    return this.defaultVal;
  }

  public static final class Builder {
    private final String key;
    private List<String> comments = new ArrayList<>();
    boolean defaultValue = false;
    private List<String> incompats = new ArrayList<>();

    public Builder(String key) {
      this.key = key;
    }

    @SafeVarargs
    public final Builder addComment(String... lines) {
      for (var line : lines) this.comments.add(line);
      return this;
    }

    public Builder defaultValue(boolean defaultValue) {
      this.defaultValue = defaultValue;
      addComment(" defaultValue: " + defaultValue);
      return this;
    }

    @SafeVarargs
    public final Builder addIncompat(String... modIds) {
      for (var modId : modIds) this.incompats.add(modId);
      addComment(" incompatibleWith: " + String.join(" , ", modIds));
      return this;
    }

    private static String join(List<String> lines) {
      var result = new StringBuffer();
      for (var line : lines) result.append(line).append("\n");
      return result.toString();
    }

    public BooleanConfigEntry build() {
      return new BooleanConfigEntry(key, join(comments), defaultValue, incompats);
    }
  }
}
