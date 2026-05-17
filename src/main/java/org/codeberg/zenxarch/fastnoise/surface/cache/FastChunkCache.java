package org.codeberg.zenxarch.fastnoise.surface.cache;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.SingularPalette;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;

public final class FastChunkCache {
  private final FastSectionCache[] caches;
  private final int minY;

  public FastChunkCache(Chunk chunk, BlockState defaultState) {
    var sections = chunk.getSectionArray();
    this.caches = new FastSectionCache[sections.length];
    for (int i = 0; i < sections.length; i++) {
      caches[i] =
          switch (sections[i].blockStateContainer.data.palette()) {
            case SingularPalette<BlockState> _ -> new FastSectionCache();
            case ArrayPalette<BlockState> array ->
                new FastSectionCache(
                    array, sections[i].blockStateContainer.data.storage().getData(), defaultState);
            default -> throw new IllegalStateException("Unexpected data found in chunk");
          };
    }
    this.minY = chunk.getBottomY();
  }

  public boolean isEmpty(int x, int y, int z) {
    var sy = (y - minY) >> 4;
    return caches[sy].isEmpty(x, z);
  }

  public int getState(int x, int y, int z) {
    var sy = (y - minY) >> 4;
    return caches[sy].getState(x, y & 0xF, z);
  }

  public boolean updateAndIsStone(MaterialRuleContext context, int x, int y, int z) {
    final int state = getState(x, y, z);
    if (state > 1) {
      context.oreOrStone(this, x, y, z);
      return state == 0x3;
    }

    if (state == 0x0) context.water(y);
    else context.air();

    return false;
  }

  public void setStateToDefaultBlock(int x, int y, int z) {
    var sy = (y - minY) >> 4;
    caches[sy].setStateToDefault(x, y & 0xF, z);
  }

  public int nextNonSolidBlockY(int x, int startY, int z) {
    if (startY <= minY) return minY - 1;

    var y = startY - 1;
    var cy = (y - minY) >> 4;

    {
      var next = caches[cy].nextNonSolidBlock(x, y & 0xF, z);
      if (next != -1) return (cy << 4) + next + minY;
    }

    cy--;

    while (cy >= 0) {
      var next = caches[cy].nextNonSolidBlock(x, z);
      if (next != -1) return (cy << 4) + next + minY;
      cy--;
    }

    return minY - 1;
  }
}
