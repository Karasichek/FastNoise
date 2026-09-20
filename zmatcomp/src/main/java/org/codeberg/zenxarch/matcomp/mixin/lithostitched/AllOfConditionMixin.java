package org.codeberg.zenxarch.matcomp.mixin.lithostitched;

import org.codeberg.zenxarch.matcomp.rules.LithostitchedConditions.AllOfCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.material.condition.AllOfCondition")
public interface AllOfConditionMixin extends AllOfCondition {}
