package org.codeberg.zenxarch.fastnoise.config;

public record IntConfigEntry(String key, String comment, int defaultValue, int min, int max) {
  public static IntConfigEntry of(String key, String comment, int defaultValue) {
    return new IntConfigEntry(
        key,
        comment + "\n" + "default: " + defaultValue,
        defaultValue,
        Integer.MIN_VALUE,
        Integer.MAX_VALUE);
  }

  public IntConfigEntry constraint(int min, int max) {
    return new IntConfigEntry(
        key, comment + "\n" + "min: " + min + " max: " + max, defaultValue, min, max);
  }

  public int validate(int value) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }
}
