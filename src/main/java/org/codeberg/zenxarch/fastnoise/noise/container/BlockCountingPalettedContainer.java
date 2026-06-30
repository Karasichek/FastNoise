package org.codeberg.zenxarch.fastnoise.noise.container;

import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.PaletteProvider;
import net.minecraft.world.chunk.PalettedContainer;
import org.codeberg.zenxarch.fastnoise.noise.FastNoisePaletteHelper;

public class BlockCountingPalettedContainer<T> extends PalettedContainer<T> {
  private final short[] blockCounts = new short[16];
  private T[] states;
  private ArrayPalette<T> palette;

  public BlockCountingPalettedContainer(
      PaletteProvider<T> paletteProvider, long[] storage, T[] states, ArrayPalette<T> palette) {
    this.states = states;
    super(
        paletteProvider,
        FastNoisePaletteHelper.ARRAY_4_TYPE,
        new PackedIntegerArray(4, 4096, storage),
        palette);
    this.palette = palette;
  }

  public void updateCount(int value) {
    this.blockCounts[value]++;
  }

  public void updateCount(int value, int amt) {
    this.blockCounts[value] += amt;
  }

  @Override
  public void count(Counter<T> output) {
    short airCount = 4096;
    for (int i = 1; i < this.palette.size; i++) {
      airCount -= blockCounts[i];
    }

    output.accept(states[0], airCount);
    for (int i = 1; i < this.palette.size; i++) {
      output.accept(states[i], blockCounts[i]);
    }
  }

  public PalettedContainer<T> revert() {
    return new PalettedContainer<T>(
        this.paletteProvider, this.data.configuration(), this.data.storage(), this.palette);
  }
}
