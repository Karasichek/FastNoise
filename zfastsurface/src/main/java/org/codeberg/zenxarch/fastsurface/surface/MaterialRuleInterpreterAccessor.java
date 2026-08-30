package org.codeberg.zenxarch.fastsurface.surface;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;

public interface MaterialRuleInterpreterAccessor {
  public BlockStateRule[] zfastsurface$getRules();

  public BlockState[] zfastsurface$getStates();
}
