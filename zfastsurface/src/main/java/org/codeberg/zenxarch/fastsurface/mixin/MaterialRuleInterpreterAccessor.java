package org.codeberg.zenxarch.fastsurface.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "org.codeberg.zenxarch.matcomp.compiler.MaterialRuleInterpreter")
public interface MaterialRuleInterpreterAccessor {
  @Accessor("rules")
  public BlockStateRule[] getRules();

  @Accessor("states")
  public BlockState[] getStates();
}
