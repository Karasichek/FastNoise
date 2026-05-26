package org.codeberg.zenxarch.fastnoise.surface.cache;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;

/*
  Explaination cause this gonna get confusing
  this is a reverse iterator over block column
  this uses a zero based index instead of minY conversions
*/
public final class FastColumnIterator {
  private final FastColumnBitMask[] isAirPrefetch;
  private final FastColumnBitMask[] isLiquidPrefetch;
  private final FastColumnBitMask[] isSolidPrefetch;
  private final FastColumnBitMask[] isDefaultPrefetch;

  private final int highestY;
  private final int size;

  private final MaterialRuleContext context;

  public FastColumnIterator(
      final ChunkSection[] sections,
      BlockState defaultState,
      final int minY,
      MaterialRuleContext context) {
    int highestNonEmptySection = sections.length - 1;
    for (; highestNonEmptySection >= 0; highestNonEmptySection--)
      if (!sections[highestNonEmptySection].isEmpty()) break;

    this.highestY = minY + (highestNonEmptySection << 4) + 0xf;
    this.context = context;

    this.isAirPrefetch = new FastColumnBitMask[256];
    this.isLiquidPrefetch = new FastColumnBitMask[256];
    this.isSolidPrefetch = new FastColumnBitMask[256];
    this.isDefaultPrefetch = new FastColumnBitMask[256];
    this.size = (highestNonEmptySection + 1) * 16;
    for (int i = 0; i < 256; i++) {
      int ly = size - 1;
      var isAir = FastColumnBitMask.builder(this.size);
      var isLiquid = FastColumnBitMask.builder(this.size);
      var isDefault = FastColumnBitMask.builder(this.size);

      for (int sy = 0; sy <= highestNonEmptySection; sy++) {
        final var section = sections[sy];
        for (int y = 0; y < 16; y++) {
          var state = section.getBlockState(i & 0xf, y, i >> 4);
          if (state.isAir()) isAir.set(ly);
          else if (!state.getFluidState().isEmpty()) isLiquid.set(ly);
          else if (state == defaultState) isDefault.set(ly);
          ly--;
        }
      }

      this.isAirPrefetch[i] = isAir.build();
      this.isLiquidPrefetch[i] = isLiquid.build();
      this.isSolidPrefetch[i] =
          FastColumnBitMask.nor(this.isAirPrefetch[i], this.isLiquidPrefetch[i], this.size);
      this.isDefaultPrefetch[i] = isDefault.build();
    }
  }

  private int localY;
  private int globalY;
  private int solidBlocksBelow;
  private FastColumnBitMask isAir;
  private FastColumnBitMask isLiquid;
  private FastColumnBitMask isSolid;
  private FastColumnBitMask isDefault;

  public void start(int x, int z) {
    var currentIdx = (z << 4) | x;
    this.isAir = this.isAirPrefetch[currentIdx];
    this.isLiquid = this.isLiquidPrefetch[currentIdx];
    this.isSolid = this.isSolidPrefetch[currentIdx];
    this.isDefault = this.isDefaultPrefetch[currentIdx];

    globalY = highestY;
    localY = 0;

    recalculateSolidBlocksBelowAtStart();
  }

  public boolean hasNext() {
    if (solidBlocksBelow == 0) {
      recalculateSolidBlocksBelow();
    }

    if (solidBlocksBelow == 0) return false;

    while (solidBlocksBelow > 0) {
      if (this.isDefault.isSet(localY)) return true;
      context.oreOrStone(globalY, solidBlocksBelow - 1);
      stepStone();
    }

    return hasNext();
  }

  public int nextY() {
    var result = globalY;
    context.oreOrStone(result, solidBlocksBelow - 1);
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

  private void recalculateSolidBlocksBelowAtStart() {
    if (this.isSolid.isSet(localY)) {
      solidBlocksBelow = computeSolidBlocksBelow();
      return;
    }

    recalculateSolidBlocksBelow();
  }

  // returns true if has finished
  private void recalculateSolidBlocksBelow() {
    var isAir = this.isAir.isSet(localY);
    var isLiquid = isAir ? false : this.isLiquid.isSet(localY);

    while (isAir || isLiquid) {
      if (isAir) {
        context.air();
        step(this.isAir.numberOfTrailingOnes(localY));
      } else if (isLiquid) {
        context.water(globalY);
        step(this.isLiquid.numberOfTrailingOnes(localY));
      }

      if (localY >= size) {
        solidBlocksBelow = 0;
        return;
      }

      isAir = this.isAir.isSet(localY);
      isLiquid = isAir ? false : this.isLiquid.isSet(localY);
    }

    solidBlocksBelow = computeSolidBlocksBelow();
  }

  private int computeSolidBlocksBelow() {
    return this.isSolid.numberOfTrailingOnes(localY);
  }
}
