package org.codeberg.zenxarch.fastsurface.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import org.codeberg.zenxarch.fastsurface.surface.MaterialRuleInterpreterAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "org.codeberg.zenxarch.matcomp.compiler.MaterialRuleInterpreter")
public interface MaterialRuleInterpreterMixin extends MaterialRuleInterpreterAccessor {
  @Accessor("rules")
  public BlockStateRule[] zfastsurface$getRules();

  @Accessor("states")
  public BlockState[] zfastsurface$getStates();
}
