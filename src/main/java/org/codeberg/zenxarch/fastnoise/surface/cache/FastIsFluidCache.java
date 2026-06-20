package org.codeberg.zenxarch.fastnoise.surface.cache;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class FastIsFluidCache {
  private final BlockState[] states;
  private final boolean[] isFluid;
  private int nextIdx;

  private static final BlockState[] STATES =
      new BlockState[] {
        Blocks.GRASS_BLOCK.getDefaultState(),
        Blocks.DIRT.getDefaultState(),
        Blocks.SAND.getDefaultState(),
        Blocks.DEEPSLATE.getDefaultState()
      };

  private static final boolean[] IS_FLUID =
      new boolean[] {
        !STATES[0].getFluidState().isEmpty(),
        !STATES[1].getFluidState().isEmpty(),
        !STATES[2].getFluidState().isEmpty(),
        !STATES[3].getFluidState().isEmpty()
      };

  public FastIsFluidCache() {
    this.states = STATES.clone();
    this.isFluid = IS_FLUID.clone();
    nextIdx = 0;
  }

  public boolean isFluid(BlockState state) {
    for (int i = 0; i < 4; i++) if (states[i] == state) return isFluid[i];

    var isStateFluid = !state.getFluidState().isEmpty();
    states[nextIdx] = state;
    isFluid[nextIdx] = isStateFluid;
    nextIdx = (nextIdx + 1) & 0x3;

    return isStateFluid;
  }
}
