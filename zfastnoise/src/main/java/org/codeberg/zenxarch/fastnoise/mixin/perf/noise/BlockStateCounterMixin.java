package org.codeberg.zenxarch.fastnoise.mixin.perf.noise;

import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.chunk.ChunkSection$BlockStateCounter")
public abstract class BlockStateCounterMixin {
  @Shadow public int nonEmptyBlockCount;
  @Shadow public int randomTickableBlockCount;
  @Shadow public int nonEmptyFluidCount;

  @Overwrite
  public void accept(BlockState blockState, int i) {
    if (!blockState.isAir()) {
      this.nonEmptyBlockCount += i;

      if (blockState.hasRandomTicks()) randomTickableBlockCount += i;

      if (!blockState.getFluidState().isEmpty()) this.nonEmptyFluidCount += i;
    }
  }
}
