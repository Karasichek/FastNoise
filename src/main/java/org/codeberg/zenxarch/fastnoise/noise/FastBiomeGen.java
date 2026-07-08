package org.codeberg.zenxarch.fastnoise.noise;

import java.util.List;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.PalettedContainer.Data;
import net.minecraft.world.chunk.PalettedContainer.DataProvider;
import net.minecraft.world.chunk.PalettedContainer.PaletteProvider;
import net.minecraft.world.chunk.SingularPalette;

public final class FastBiomeGen {

  private static final Palette.Factory ARRAY = PaletteProvider.ARRAY;
  private static final Palette.Factory SINGULAR = PaletteProvider.SINGULAR;

  static final DataProvider<RegistryEntry<Biome>> SINGULAR_TYPE = new DataProvider<>(SINGULAR, 0);
  private static final DataProvider<RegistryEntry<Biome>> ARRAY_1_TYPE =
      new DataProvider<>(ARRAY, 1);
  private static final DataProvider<RegistryEntry<Biome>> ARRAY_2_TYPE =
      new DataProvider<>(ARRAY, 2);
  private static final DataProvider<RegistryEntry<Biome>> ARRAY_3_TYPE =
      new DataProvider<>(ARRAY, 3);
  private static final DataProvider<RegistryEntry<Biome>> ARRAY_4_TYPE =
      new DataProvider<>(ARRAY, 4);
  private static final DataProvider<RegistryEntry<Biome>> ARRAY_5_TYPE =
      new DataProvider<>(ARRAY, 5);
  private static final DataProvider<RegistryEntry<Biome>> ARRAY_6_TYPE =
      new DataProvider<>(ARRAY, 6);

  @SuppressWarnings("unchecked")
  private static final DataProvider<RegistryEntry<Biome>>[] types =
      new DataProvider[] {
        ARRAY_1_TYPE, ARRAY_2_TYPE, ARRAY_3_TYPE, ARRAY_4_TYPE, ARRAY_5_TYPE, ARRAY_6_TYPE
      };

  public static void populateBiomes(
      ChunkSection section,
      BiomeSupplier biomeSupplier,
      MultiNoiseUtil.MultiNoiseSampler sampler,
      int x,
      int y,
      int z,
      RegistryEntry<Biome>[] biomes,
      byte[] storage) {

    int size = 0;

    for (int ix = 0; ix < 4; ix++) {
      for (int iy = 0; iy < 4; iy++) {
        for (int iz = 0; iz < 4; iz++) {

          var biome = biomeSupplier.getBiome(x + ix, y + iy, z + iz, sampler);

          int bidx = -1;
          for (int i = 0; i < size; i++)
            if (biomes[i] == biome) {
              bidx = i;
              break;
            }

          if (bidx == -1) biomes[(bidx = size++)] = biome;

          storage[(iy << 2 | iz) << 2 | ix] = (byte) bidx;
        }
      }
    }

    var container = ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer);
    var idList = container.idList;
    if (size == 1) {
      if (container.data.palette() instanceof SingularPalette<RegistryEntry<Biome>> palette) {
        palette.entry = biomes[0];
      } else {
        container.data =
            new Data<>(
                SINGULAR_TYPE,
                new EmptyPaletteStorage(64),
                new SingularPalette<>(idList, container, List.of(biomes[0])));
      }
    } else {
      int bits = MathHelper.ceilLog2(size);
      @SuppressWarnings("unchecked")
      RegistryEntry<Biome>[] downSizedBiomes = new RegistryEntry[1 << bits];
      System.arraycopy(biomes, 0, downSizedBiomes, 0, size);
      container.data =
          new Data<RegistryEntry<Biome>>(
              types[bits],
              toStorage(biomes, bits, storage),
              new ArrayPalette<>(idList, downSizedBiomes, container, bits, size));
    }
  }

  private static final int[] sizes = new int[] {-1, 1, 2, 4, 4, 6, 7};

  private static PaletteStorage toStorage(RegistryEntry<Biome>[] palette, int bits, byte[] data) {
    var storage = new long[sizes[bits]];

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
}
