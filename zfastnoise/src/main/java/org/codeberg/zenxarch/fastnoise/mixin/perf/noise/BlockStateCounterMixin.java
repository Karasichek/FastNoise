package org.codeberg.zenxarch.fastnoise.mixin.perf.noise;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets = "net.minecraft.world.chunk.ChunkSection$BlockStateCounter")
public abstract class BlockStateCounterMixin {
  @WrapWithCondition(
      method = "accept",
      at =
          @At(
              value = "FIELD",
              opcode = Opcodes.PUTFIELD,
              target =
                  "Lnet/minecraft/world/chunk/ChunkSection$BlockStateCounter;nonEmptyBlockCount:I"),
      slice =
          @Slice(
              from =
                  @At(value = "INVOKE:ONE", target = "Lnet/minecraft/fluid/FluidState;isEmpty()Z")))
  public boolean removeDoubleCountOfSolidBlocks(@Coerce Object unused, int unusedCount) {
    return false;
  }

  @ModifyExpressionValue(
      method = "accept",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;hasRandomTicks()Z"))
  public boolean removeRandomTickCheck(boolean og) {
    return true;
  }
}
