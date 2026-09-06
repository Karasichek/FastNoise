package org.codeberg.zenxarch.fastsurface.mixin;

import net.minecraft.block.BlockState;
import org.codeberg.zenxarch.fastsurface.surface.MaterialRuleInterpreterAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "org.codeberg.zenxarch.matcomp.compiler.MaterialRuleInterpreter")
public interface MaterialRuleInterpreterMixin extends MaterialRuleInterpreterAccessor {
  @Invoker("getExpectedStates")
  public BlockState[] zfastsurface$getExpectedStates();
}
