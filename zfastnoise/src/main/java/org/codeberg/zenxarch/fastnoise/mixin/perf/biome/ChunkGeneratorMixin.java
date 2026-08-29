package org.codeberg.zenxarch.fastnoise.mixin.perf.biome;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.codeberg.zenxarch.fastnoise.biome.FastBiomeGen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

  @WrapOperation(
      method = "populateBiomesSync",
      at =
          @At(
              target =
                  "Lnet/minecraft/world/chunk/Chunk;populateBiomes(Lnet/minecraft/world/biome/source/BiomeSupplier;)V",
              value = "INVOKE"))
  private void zenxarch$populateBiomes(Chunk chunk, BiomeSupplier supplier, Operation<Void> op) {
    FastBiomeGen.populateBiomes(chunk, supplier);
  }
}
