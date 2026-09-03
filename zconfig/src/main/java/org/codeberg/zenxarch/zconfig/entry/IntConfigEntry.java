package org.codeberg.zenxarch.zconfig.entry;

import java.util.ArrayList;
import java.util.List;

public record IntConfigEntry(
    String key, String comment, int defaultVal, int min, int max, List<String> incompats)
    implements ConfigEntry<Integer> {

  public static Builder builder(String key) {
    return new Builder(key);
  }

  @Override
  public Integer defaultValue() {
    return defaultVal;
  }

  public int validate(int value) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

  public static final class Builder {
    private final String key;
    private List<String> comments = new ArrayList<>();
    int defaultValue = 0;
    private List<String> incompats = new ArrayList<>();
    int min = Integer.MIN_VALUE;
    int max = Integer.MAX_VALUE;

    public Builder(String key) {
      this.key = key;
    }

    @SafeVarargs
    public final Builder addComment(String... lines) {
      for (var line : lines) this.comments.add(line);
      return this;
    }

    public Builder defaultValue(int defaultValue) {
      this.defaultValue = defaultValue;
      addComment(" defaultValue: " + defaultValue);
      return this;
    }

    public Builder constraints(int min, int max) {
      this.min = min;
      this.max = max;
      addComment(
          ((min == Integer.MIN_VALUE) ? "" : " min: " + min)
              + ((max == Integer.MAX_VALUE) ? "" : " max: " + max));
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

    public IntConfigEntry build() {
      return new IntConfigEntry(key, join(comments), defaultValue, min, max, incompats);
    }
  }
}
