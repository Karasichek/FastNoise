package org.codeberg.zenxarch.fastnoise.noise;

import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;

public final class EndBiomeStorageCache {
  private EndBiomeStorageCache() {
    throw new IllegalStateException("Utility class");
  }

  // Possible palettes
  private static final int[][] palettes = {
    {}, // {0, 0, 0, 0},
    {0, 0, 0, 1},
    {0, 0, 1, 0},
    {0, 0, 1, 1},
    {0, 0, 1, 2},
    {0, 1, 0, 0},
    {0, 1, 0, 1},
    {0, 1, 0, 2},
    {0, 1, 1, 0},
    {0, 1, 1, 1},
    {0, 1, 1, 2},
    {0, 1, 2, 0},
    {0, 1, 2, 1},
    {0, 1, 2, 2},
    {0, 1, 2, 3},
  };
  private static final PaletteStorage[] caches = new PaletteStorage[15];

  private static int getBits(int[] values) {
    // Sanity checks
    assert (values.length == 4);
    var check = new boolean[4];
    for (int i = 0; i < 4; i++) check[values[i]] = true;

    assert (check[0]);
    assert (check[1]);
    if (!check[2]) assert (!check[3]);

    return check[2] ? 2 : 1;
  }

  private static PaletteStorage createCache(int[] values) {
    var storage = new PackedIntegerArray(getBits(values), 64);
    int idx = 0;
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 16; j++) {
        storage.set(idx++, values[i]);
      }
    }

    return storage;
  }

  private static PaletteStorage createCache(int idx) {
    if (idx == 0) return new EmptyPaletteStorage(64);
    return createCache(palettes[idx]);
  }

  static {
    for (int i = 0; i < 15; i++) {
      caches[i] = createCache(i);
    }
  }

  private static int indexFromValues(int[] values) {
    if (values[1] == 0) {
      if (values[2] == 0) return 1;
      return 2 + values[3];
    }
    // 0, 1, ?, ?
    return 5 + (values[2] * 3) + values[3];
  }

  public static PaletteStorage get(int[] values) {
    return caches[indexFromValues(values)];
  }
}
