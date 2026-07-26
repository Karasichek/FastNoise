package org.codeberg.zenxarch.fastnoise.surface.cache;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;

public class TrivialFastColumnIterator implements FastColumnIterator {
  private final FastColumnBitMask[] isAirPrefetch;
  private final FastColumnBitMask[] isDefaultPrefetch;

  private final int highestY;
  private final int size;

  private final MaterialRuleContext context;

  public TrivialFastColumnIterator(
      final ChunkSection[] sections,
      final BlockState defaultState,
      final int minY,
      final MaterialRuleContext context,
      final int numSections) {

    this.highestY = minY + (numSections << 4) - 0x1;
    this.context = context;
    this.size = numSections << 4;

    this.isAirPrefetch = new FastColumnBitMask[256];
    this.isDefaultPrefetch = new FastColumnBitMask[256];

    long[][] data = new long[numSections][];
    for (int i = 0; i < numSections; i++)
      data[i] = sections[i].blockStateContainer.data.storage().getData();

    for (int i = 0; i < 256; i++) {
      int ly = this.size - 1;
      var isAir = FastColumnBitMask.builder(this.size);

      for (int sy = 0; sy < numSections; sy++)
        for (int y = 0; y < 16; y++) {
          if ((data[sy][y << 4 | (i >> 4)] >>> (i << 2) & 0xFL) == 0) isAir.set(ly);
          ly--;
        }

      this.isAirPrefetch[i] = isAir.build();
      this.isDefaultPrefetch[i] = FastColumnBitMask.not(this.isAirPrefetch[i], this.size);
    }
  }

  private int localY;
  private int globalY;
  private int solidBlocksBelow;
  private FastColumnBitMask isAir;
  private FastColumnBitMask isDefault;

  public void start(int x, int z) {
    var currentIdx = (z << 4) | x;
    this.isAir = this.isAirPrefetch[currentIdx];
    this.isDefault = this.isDefaultPrefetch[currentIdx];

    globalY = highestY;
    localY = 0;

    recalculateSolidBlocksBelowAtStart();
  }

  public boolean hasNext() {
    if (solidBlocksBelow == 0) {
      recalculateSolidBlocksBelow();
    }

    return solidBlocksBelow > 0;
  }

  public int nextY() {
    var result = globalY;
    context.oreOrStone(result, solidBlocksBelow);
    stepStone();
    return result;
  }

  private void step() {
    localY++;
    globalY--;
  }

  private void stepStone() {
    solidBlocksBelow--;
    step();
  }

  private void step(int count) {
    localY += count;
    globalY -= count;
  }

  private void recalculateSolidBlocksBelow() {
    if (localY >= size) return;
    step(this.isAir.numberOfTrailingOnes(localY));
    context.air();
    if (localY >= size) return;
    solidBlocksBelow = this.isDefault.numberOfTrailingOnes(localY);
  }

  private void recalculateSolidBlocksBelowAtStart() {
    solidBlocksBelow = this.isDefault.numberOfTrailingOnes(localY);
    if (solidBlocksBelow == 0) recalculateSolidBlocksBelow();
  }
}
