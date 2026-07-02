package org.codeberg.zenxarch.fastnoise.noise;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.ChunkSection;
import org.codeberg.zenxarch.fastnoise.noise.container.BlockCountingPalettedContainer;

public final class FastChunkSection {

  private final ChunkSection section;

  private int defaultIdx = 0;
  private int minIdx = 0;

  private final long[] storage;
  private final ArrayPalette<BlockState> palette;
  private final BlockState[] states;
  private final BlockCountingPalettedContainer<BlockState> counter;

  public FastChunkSection(ChunkSection section) {
    this.section = section;
    this.states = new BlockState[16];
    this.storage = new long[256];
    this.palette = new ArrayPalette<>(this.states, 4, 1);

    this.states[0] = FastNoiseGen.AIR;

    this.counter =
        new BlockCountingPalettedContainer<>(
            this.section.blockStateContainer.paletteProvider,
            this.storage,
            this.states,
            this.palette);
    this.section.blockStateContainer = this.counter;
  }

  public void setDefaultBlockState(int x, int y, int z, BlockState state) {
    if (defaultIdx == 0) {
      this.states[(defaultIdx = this.palette.size++)] = state;
    }
    setBlockState(x, y, z, defaultIdx);
    this.counter.updateCount(defaultIdx);
  }

  private int getIndex(BlockState state) {
    if (minIdx == 0) {
      this.states[(minIdx = this.palette.size++)] = state;
      return minIdx;
    }
    for (int i = minIdx; i < palette.size; i++) {
      if (states[i] == state) return i;
    }
    states[palette.size] = state;
    return palette.size++;
  }

  public void setBlockState(int x, int y, int z, BlockState state) {
    var valIdx = getIndex(state);
    setBlockState(x, y, z, valIdx);
    this.counter.updateCount(valIdx);
  }

  private void setBlockState(int x, int y, int z, long value) {
    this.storage[(y << 4) | z] |= value << (x << 2);
  }

  public void recalculateCounts() {
    this.section.calculateCounts();
    this.section.blockStateContainer = this.counter.revert();
  }
}
