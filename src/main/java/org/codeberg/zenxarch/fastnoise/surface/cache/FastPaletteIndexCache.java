package org.codeberg.zenxarch.fastnoise.surface.cache;

import java.util.Arrays;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;

public class FastPaletteIndexCache {
  private final BlockState[] states;
  private final int[] indices;
  private final boolean[] stillArray;

  public FastPaletteIndexCache(ChunkSection[] sections) {
    int size = highestNonEmptySection(sections) + 1;

    this.indices = new int[size];
    this.states = new BlockState[size];
    this.stillArray = new boolean[size];
    Arrays.fill(this.stillArray, true);
  }

  private int highestNonEmptySection(ChunkSection[] sections) {
    for (int i = (sections.length - 1); i >= 0; i--) {
      if (!sections[i].isEmpty()) return i;
    }

    return -1;
  }

  public int size() {
    return this.indices.length;
  }

  private int getIdx(int sy, PalettedContainer<BlockState> container, BlockState state) {
    if (states[sy] == state) return indices[sy];
    var index = container.data.palette().index(state, container);
    states[sy] = state;
    indices[sy] = index;
    if (stillArray[sy]) stillArray[sy] = container.data.configuration().bitsInMemory() == 4;
    return index;
  }

  public void updateBlock(
      int sy, PalettedContainer<BlockState> container, int x, int y, int z, BlockState state) {
    var idx = getIdx(sy, container, state);
    final var storage = container.data.storage();
    if (stillArray[sy]) {
      final int dataIdx = y << 4 | z;
      final int shift = x << 2;
      final var data = storage.getData();
      data[dataIdx] &= ~(0xFL << shift);
      data[dataIdx] |= ((long) idx) << shift;
    } else storage.set((y << 4 | z) << 4 | x, idx);
  }

  public void finish(ChunkSection[] sections) {
    for (int i = 0; i < states.length; i++) {
      if (states[i] != null) sections[i].calculateCounts();
    }
  }
}
