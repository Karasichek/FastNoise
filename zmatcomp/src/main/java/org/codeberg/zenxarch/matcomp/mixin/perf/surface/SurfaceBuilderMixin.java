package org.codeberg.zenxarch.matcomp.mixin.perf.surface;

import net.minecraft.util.math.noise.NoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.matcomp.util.AllocFreeRandom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin {

  @Shadow @Final NoiseSampler surfaceNoise;
  @Shadow @Final RandomSplitter randomDeriver;

  @Overwrite
  public int sampleRunDepth(final int blockX, final int blockZ) {
    double noiseValue = (double) this.surfaceNoise.sample((double) blockX, 0.0D, (double) blockZ);
    var next =
        switch (this.randomDeriver) {
          case Xoroshiro128PlusPlusRandom.Splitter xoro ->
              AllocFreeRandom.nextDoubleXoroshiro(xoro.seedLo, xoro.seedHi, blockX, 0, blockZ);
          case CheckedRandom.Splitter checked ->
              AllocFreeRandom.nextDoubleChecked(checked.seed, blockX, 0, blockZ);
          default -> this.randomDeriver.split(blockX, 0, blockZ).nextDouble();
        };
    return (int) (noiseValue * 2.75D + 3.0D + next * 0.25D);
  }
}
