package org.codeberg.zenxarch.fastnoise.noise;

import java.util.List;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PaletteType;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.PalettedContainer.Data;
import net.minecraft.world.chunk.SingularPalette;

public final class FastNoisePaletteHelper {
  private FastNoisePaletteHelper() {
    throw new IllegalStateException("Utility class");
  }

  private static final Palette.Factory ARRAY = ArrayPalette::create;
  private static final Palette.Factory SINGULAR = SingularPalette::create;

  private static final PaletteType SINGULAR_TYPE = new PaletteType.Static(SINGULAR, 0);
  private static final PaletteType ARRAY_1_TYPE = new PaletteType.Static(ARRAY, 1);
  private static final PaletteType ARRAY_2_TYPE = new PaletteType.Static(ARRAY, 2);
  private static final PaletteType ARRAY_3_TYPE = new PaletteType.Static(ARRAY, 3);
  public static final PaletteType ARRAY_4_TYPE = new PaletteType.Static(ARRAY, 4);
  private static final PaletteType ARRAY_5_TYPE = new PaletteType.Static(ARRAY, 5);
  private static final PaletteType ARRAY_6_TYPE = new PaletteType.Static(ARRAY, 6);

  private static final PaletteType[] biomePaletteTypes =
      new PaletteType[] {
        ARRAY_1_TYPE, ARRAY_2_TYPE, ARRAY_3_TYPE, ARRAY_4_TYPE, ARRAY_5_TYPE, ARRAY_6_TYPE
      };

  public static void pack(
      PalettedContainer<RegistryEntry<Biome>> container,
      RegistryEntry<Biome>[] biomes,
      int size,
      byte[] storage) {
    if (size == 1) {
      packSingleElement(container, biomes[0]);
      return;
    }
    int bits = MathHelper.ceilLog2(size);
    @SuppressWarnings("unchecked")
    RegistryEntry<Biome>[] downSizedBiomes = new RegistryEntry[1 << bits];
    System.arraycopy(biomes, 0, downSizedBiomes, 0, size);
    container.data =
        new Data<RegistryEntry<Biome>>(
            biomePaletteTypes[bits - 1],
            repackBiomeStorage(biomes, bits, storage),
            new ArrayPalette<>(downSizedBiomes, bits, size));
  }

  private static final int[] biomeStorageSizes = new int[] {-1, 1, 2, 4, 4, 6, 7};

  private static PaletteStorage repackBiomeStorage(
      RegistryEntry<Biome>[] palette, int bits, byte[] data) {
    var storage = new long[biomeStorageSizes[bits]];

    int idx = 0;
    for (int i = 0; i < bits; i++) {
      for (int j = 0; (j + bits) < 65; j += bits) {
        storage[i] |= ((long) data[idx++]) << j;
      }
    }
    int j = 0;
    while (idx < 64) {
      storage[bits] |= ((long) data[idx++]) << j;
      j += bits;
    }

    return new PackedIntegerArray(bits, 64, storage);
  }

  public static <T> void packSingleElement(PalettedContainer<T> container, T element) {
    if (container.data.palette() instanceof SingularPalette<T> palette) {
      palette.entry = element;
    } else {
      container.data =
          new Data<>(
              SINGULAR_TYPE, new EmptyPaletteStorage(64), new SingularPalette<>(List.of(element)));
    }
  }

  private static int index(RegistryEntry<Biome>[] palette, int size, RegistryEntry<Biome> value) {
    for (int i = 0; i < size; i++) {
      if (palette[i] == value) return i;
    }
    return size;
  }

  @SuppressWarnings("unchecked")
  public static void packFourEntries(
      PalettedContainer<RegistryEntry<Biome>> container,
      RegistryEntry<Biome> a,
      RegistryEntry<Biome> b,
      RegistryEntry<Biome> c,
      RegistryEntry<Biome> d) {
    if (a == b && b == c && c == d) {
      packSingleElement(container, a);
      return;
    }
    var palette = (RegistryEntry<Biome>[]) new RegistryEntry[4];
    var index = new int[4];
    int size = 1;
    {
      palette[0] = a;
      index[0] = 0;
    }
    {
      var next = index(palette, size, b);
      index[1] = next;
      palette[next] = b;
      if (next == size) size++;
    }
    {
      var next = index(palette, size, c);
      index[2] = next;
      palette[next] = c;
      if (next == size) size++;
    }
    {
      var next = index(palette, size, d);
      index[3] = next;
      palette[next] = d;
      if (next == size) size++;
    }

    var storage = EndBiomeStorageCache.get(index);
    if (size == 2) {
      container.data =
          new Data<RegistryEntry<Biome>>(
              ARRAY_1_TYPE,
              storage.copy(),
              new ArrayPalette<RegistryEntry<Biome>>(
                  (RegistryEntry<Biome>[]) new RegistryEntry[] {palette[0], palette[1]}, 1, size));
    } else {
      container.data =
          new Data<RegistryEntry<Biome>>(
              ARRAY_2_TYPE,
              storage.copy(),
              new ArrayPalette<RegistryEntry<Biome>>(palette.clone(), 2, size));
    }
  }
}
