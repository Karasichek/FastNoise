package org.codeberg.zenxarch.fastnoise.surface.cache;

public final class FastColumnBitMask {
  private final long[] data;

  FastColumnBitMask(long[] data) {
    this.data = data;
  }

  public long get(int idx) {
    return data[idx >> 6] >>> idx & 0x1;
  }

  public boolean isSet(int idx) {
    return get(idx) != 0x0;
  }

  public boolean isUnset(int idx) {
    return get(idx) == 0x0;
  }

  public int numberOfTrailingOnes(int idx) {
    var result = numberOfTrailingOnes(data[idx >> 6] >>> idx);
    if (result == 0) return 0;

    if (((result + idx) & 0x3f) != 0x0) return result;

    int next = (idx >> 6) + 1;
    while (next < this.data.length) {
      if (data[next] == -1L) result += 64;
      else return result + numberOfTrailingOnes(data[next]);

      next++;
    }

    return result;
  }

  private static int numberOfTrailingOnes(long value) {
    return Long.bitCount(value & (-2 - value));
  }

  public static FastColumnBitMask nor(FastColumnBitMask a, FastColumnBitMask b, int size) {
    final long[] data = new long[a.data.length];
    for (int i = 0; i < data.length; i++) data[i] = ~(a.data[i] | b.data[i]);
    if (data.length > 0) data[data.length - 1] &= -1L >>> -size;
    return new FastColumnBitMask(data);
  }

  public static FastColumnBitMask not(FastColumnBitMask a, int size) {
    final long[] data = new long[a.data.length];
    for (int i = 0; i < data.length; i++) data[i] = ~a.data[i];
    if (data.length > 0) data[data.length - 1] &= -1L >>> -size;
    return new FastColumnBitMask(data);
  }

  public static Builder builder(int bits) {
    return new Builder(bits);
  }

  public static class Builder {
    private final long[] data;

    Builder(int bits) {
      this.data = new long[(bits - 1 >> 6) + 1];
    }

    public void set(int index) {
      this.data[index >> 6] |= 0x1L << index;
    }

    public FastColumnBitMask build() {
      return new FastColumnBitMask(data);
    }
  }
}
