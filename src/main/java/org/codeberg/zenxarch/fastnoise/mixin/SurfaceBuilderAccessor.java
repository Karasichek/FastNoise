package org.codeberg.zenxarch.fastnoise.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.BlockColumn;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SurfaceBuilder.class)
public interface SurfaceBuilderAccessor {

  @Accessor("defaultState")
  public BlockState zenxarch$getDefaultState();

  @Invoker("isDefaultBlock")
  public boolean zenxarch$isDefaultBlock(BlockState state);

  @Invoker("placeBadlandsPillar")
  public void zenxarch$placeBadlandsPillar(
      final BlockColumn column,
      final int x,
      final int z,
      final int surfaceY,
      final HeightLimitView chunk);

  @Invoker("placeIceberg")
  public void zenxarch$placeIceberg(
      final int minY,
      final Biome biome,
      final BlockColumn column,
      final BlockPos.Mutable mutablePos,
      final int x,
      final int z,
      final int surfaceY);

  @Accessor("badlandsPillarNoise")
  public DoublePerlinNoiseSampler zenxarch$getBadlandsPillarNoise();

  @Accessor("badlandsPillarRoofNoise")
  public DoublePerlinNoiseSampler zenxarch$getBadlandsPillarRoofNoise();

  @Accessor("badlandsSurfaceNoise")
  public DoublePerlinNoiseSampler zenxarch$getBadlandsSurfaceNoise();
}
