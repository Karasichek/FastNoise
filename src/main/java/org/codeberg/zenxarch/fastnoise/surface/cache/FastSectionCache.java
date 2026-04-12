package org.codeberg.zenxarch.fastnoise.surface.cache;

import java.util.Arrays;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ArrayPalette;

public final class FastSectionCache {
  /*
    DEFAULT + !SOLID = AIR 1 0
    !DEFAULT + !SOLID = WATER 0 0
    DEFAULT + SOLID = STONE 1 1
    !DEFAULT + SOLID = ORE 0 1
  */
  private final short[] IS_SOLID = new short[256];
  private final short[] IS_DEFAULT = new short[256];

  private static int index(int x, int z) {
    return x + (z << 4);
  }

  public FastSectionCache(ArrayPalette<BlockState> palette, long[] data, BlockState defaultState) {
    short isSolid = 0x0;
    short isDefault = 0x0;

    for (int i = 0; i < palette.size; i++) {
      var state = fromBlockState(palette.get(i), defaultState);
      if (state == STATE.STONE || state == STATE.AIR) isDefault = setBit(isDefault, i);
      if (state == STATE.STONE || state == STATE.ORE) isSolid = setBit(isSolid, i);
    }

    for (int z = 0; z < 16; z++) {
      for (int x = 0; x < 16; x++) {
        fillFromColumn(palette, data, defaultState, x, z, isDefault, isSolid);
      }
    }
  }

  // empty section ctor
  public FastSectionCache() {
    // Arrays.fill(IS_SOLID, (short) 0x0); // not needed
    Arrays.fill(IS_DEFAULT, (short) 0xFFFF);
  }

  private void fillFromColumn(
      ArrayPalette<BlockState> palette,
      long[] data,
      BlockState defaultState,
      int x,
      int z,
      short isDefault,
      short isSolid) {
    short risSolid = 0x0, risDefault = 0x0;

    for (int y = 0; y < 16; y++) {
      var idx = (y << 4) + z;

      var value = (int) ((data[idx] >> (x << 2)) & 0xF);

      if (getBit(isDefault, value)) risDefault = setBit(risDefault, y);
      if (getBit(isSolid, value)) risSolid = setBit(risSolid, y);
    }

    var index = index(x, z);
    IS_SOLID[index] = risSolid;
    IS_DEFAULT[index] = risDefault;
  }

  private static STATE fromBlockState(BlockState state, BlockState defaultState) {
    if (state.isAir()) return STATE.AIR;
    if (!state.getFluidState().isEmpty()) return STATE.WATER;
    if (state == defaultState) return STATE.STONE;
    return STATE.ORE;
  }

  private static short setBit(short in, int bitIdx) {
    return (short) (in | (0x1 << bitIdx));
  }

  private static boolean getBit(short in, int bitIdx) {
    return ((in >> bitIdx) & 0x1) != 0x0;
  }

  public STATE getState(int x, int y, int z) {
    var index = index(x, z);
    if (getBit(IS_SOLID[index], y)) {
      return getBit(IS_DEFAULT[index], y) ? STATE.STONE : STATE.ORE;
    } else {
      return getBit(IS_DEFAULT[index], y) ? STATE.AIR : STATE.WATER;
    }
  }

  public void setStateToDefault(int x, int y, int z) {
    var index = index(x, z);
    IS_DEFAULT[index] = setBit(IS_DEFAULT[index], y);
    IS_SOLID[index] = setBit(IS_SOLID[index], y);
  }

  public boolean isEmpty(int x, int z) {
    var index = index(x, z);
    if (IS_SOLID[index] != 0x0) return false;
    return IS_DEFAULT[index] == 0xFFFF;
  }

  public int nextNonSolidBlock(int x, int ly, int z) {
    var mask = (0x1 << (ly + 1)) - 1;
    var value = ~IS_SOLID[index(x, z)] & mask;

    return 31 - Integer.numberOfLeadingZeros(value);
  }

  public int nextNonSolidBlock(int x, int z) {
    return 31 - Integer.numberOfLeadingZeros(~IS_SOLID[index(x, z)]);
  }

  public static enum STATE {
    AIR,
    WATER,
    STONE,
    ORE
  }
}
