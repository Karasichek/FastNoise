package org.codeberg.zenxarch.fastnoise.surface.cache;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;

/*
  Explaination cause this gonna get confusing
  this is a reverse iterator over block column
  this uses a zero based index instead of minY conversions
*/
public final class GenericFastColumnIterator implements FastColumnIterator {
  private final FastColumnBitMask[] isAirPrefetch;
  private final FastColumnBitMask[] isLiquidPrefetch;
  private final FastColumnBitMask[] isSolidPrefetch;
  private final FastColumnBitMask[] isDefaultPrefetch;

  private final int highestY;
  private final int size;

  private final MaterialRuleContext context;

  public GenericFastColumnIterator(
      final ChunkSection[] sections,
      final BlockState defaultState,
      final int minY,
      final MaterialRuleContext context,
      final int numSections) {

    this.highestY = minY + (numSections << 4) - 0x1;
    this.context = context;

    this.isAirPrefetch = new FastColumnBitMask[256];
    this.isLiquidPrefetch = new FastColumnBitMask[256];
    this.isSolidPrefetch = new FastColumnBitMask[256];
    this.isDefaultPrefetch = new FastColumnBitMask[256];
    this.size = numSections << 4;

    int[] airMask = new int[numSections];
    int[] liquidMask = new int[numSections];
    int[] defaultIdx = new int[numSections];

    for (int i = 0; i < numSections; i++) {
      if (sections[i].isEmpty()) {
        airMask[i] = -1;
        liquidMask[i] = 0x0;
        defaultIdx[i] = -1;
      } else {
        var palette = sections[i].blockStateContainer.data.palette();
        if (sections[i].hasFluids()) {
          airMask[i] = air(palette);
          liquidMask[i] = liquid(palette);
          defaultIdx[i] = defaultIdx(palette, defaultState);
        } else {
          airMask[i] = air(palette);
          liquidMask[i] = 0;
          defaultIdx[i] = defaultIdx(palette, defaultState);
        }
      }
    }

    long[][] data = new long[numSections][];
    for (int i = 0; i < numSections; i++)
      data[i] = sections[i].blockStateContainer.data.storage().getData();

    for (int i = 0; i < 256; i++) {
      int ly = this.size - 1;
      var isAir = FastColumnBitMask.builder(this.size);
      var isLiquid = FastColumnBitMask.builder(this.size);
      var isDefault = FastColumnBitMask.builder(this.size);

      for (int sy = 0; sy < numSections; sy++) {
        if (data[sy].length == 0)
          for (int y = 0; y < 16; y++)
            updateBuilders(
                0L, airMask, liquidMask, defaultIdx, isAir, isLiquid, isDefault, sy, ly--);
        else
          for (int y = 0; y < 16; y++)
            updateBuilders(
                data[sy][y << 4 | (i >> 4)] >>> (i << 2) & 0xFL,
                airMask,
                liquidMask,
                defaultIdx,
                isAir,
                isLiquid,
                isDefault,
                sy,
                ly--);
      }

      this.isAirPrefetch[i] = isAir.build();
      this.isLiquidPrefetch[i] = isLiquid.build();
      this.isSolidPrefetch[i] =
          FastColumnBitMask.nor(this.isAirPrefetch[i], this.isLiquidPrefetch[i], this.size);
      this.isDefaultPrefetch[i] = isDefault.build();
    }
  }

  private static void updateBuilders(
      long stateIdx,
      int[] airMask,
      int[] liquidMask,
      int[] defaultIdx,
      FastColumnBitMask.Builder isAir,
      FastColumnBitMask.Builder isLiquid,
      FastColumnBitMask.Builder isDefault,
      int sy,
      int ly) {
    if ((airMask[sy] >>> stateIdx & 0x1) == 0x1) isAir.set(ly);
    else if ((liquidMask[sy] >>> stateIdx & 0x1) == 0x1) isLiquid.set(ly);
    else if (stateIdx == defaultIdx[sy]) isDefault.set(ly);
  }

  private int air(Palette<BlockState> palette) {
    short result = 0x0;
    for (int i = 0; i < palette.getSize(); i++) if (palette.get(i).isAir()) result |= 0x1 << i;
    return result;
  }

  private int liquid(Palette<BlockState> palette) {
    short result = 0x0;
    for (int i = 0; i < palette.getSize(); i++)
      if (!palette.get(i).getFluidState().isEmpty()) result |= 0x1 << i;
    return result;
  }

  private int defaultIdx(Palette<BlockState> palette, BlockState state) {
    for (int i = 0; i < palette.getSize(); i++) {
      if (palette.get(i) == state) return i;
    }
    return -1;
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
      context.oreOrStone(globalY, solidBlocksBelow);
      stepStone();
    }

    return hasNext();
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

  private void recalculateSolidBlocksBelowAtStart() {
    solidBlocksBelow = computeSolidBlocksBelow();
    if (solidBlocksBelow == 0) recalculateSolidBlocksBelow();
  }

  // returns true if has finished
  private void recalculateSolidBlocksBelow() {
    if (localY >= size) return;
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
