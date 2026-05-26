package org.codeberg.zenxarch.fastnoise.surface.cache;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;

public class FastPaletteIndexCache {
  private final BlockState[] states;
  private final int[] indices;
  private final boolean[] dirty;

  public FastPaletteIndexCache(ChunkSection[] sections) {
    int size = highestNonEmptySection(sections) + 1;

    this.indices = new int[size];
    this.states = new BlockState[size];
    this.dirty = new boolean[size];
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

  public int getIdx(int sy, PalettedContainer<BlockState> container, BlockState state) {
    if (states[sy] == state) return indices[sy];
    var index = container.data.palette().index(state, container);
    states[sy] = state;
    indices[sy] = index;
    dirty[sy] = true;
    return index;
  }

  public void finish(ChunkSection[] sections) {
    for (int i = 0; i < dirty.length; i++) {
      if (dirty[i]) sections[i].calculateCounts();
    }
  }
}
