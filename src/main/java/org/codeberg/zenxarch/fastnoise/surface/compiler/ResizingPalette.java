package org.codeberg.zenxarch.fastnoise.surface.compiler;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.IntFunction;

public final class ResizingPalette<T> {
  private final ObjectArrayList<T> array = new ObjectArrayList<>(16);
  private Object2IntOpenHashMap<T> map;

  public ResizingPalette() {}

  public int indexOf(T value) {
    if (array.size() < 4) {
      var idx = array.indexOf(value);
      if (idx != -1) return idx;
      idx = array.size();
      array.add(value);
      return idx;
    } else {
      var idx = getMap().getOrDefault(value, -1);
      if (idx != -1) return idx;
      idx = array.size();
      array.add(value);
      map.put(value, idx);
      return idx;
    }
  }

  private Object2IntOpenHashMap<T> getMap() {
    if (map == null) {
      map = new Object2IntOpenHashMap<>();
      final var size = array.size();
      for (int i = 0; i < size; i++) {
        map.put(array.get(i), i);
      }
    }
    return map;
  }

  public T[] build(IntFunction<T[]> generator) {
    return array.toArray(generator);
  }
}
