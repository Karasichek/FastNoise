package org.codeberg.zenxarch.fastsurface.surface;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;

public interface MaterialRuleInterpreterAccessor {
  public BlockState[] zfastsurface$getExpectedStates();
}
