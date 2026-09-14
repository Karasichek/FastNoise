package org.codeberg.zenxarch.matcomp.mixin.perf.surface;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.OreVeinSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.DensityProvider;
import org.codeberg.zenxarch.matcomp.rules.BiomePredicate;
import org.codeberg.zenxarch.matcomp.rules.OreVeinMaterialRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OreVeinSampler.class)
public abstract class OreVeinSamplerMixin {

  @Shadow BlockState oreBlock;
  @Shadow BlockState rawOreBlock;
  @Shadow BlockState fillerBlock;
  @Shadow float rawOreChance;
  @Shadow DensityFunction density;
  @Shadow DensityFunction richness;
  @Shadow DensityFunction fillerGap;

  @WrapMethod(method = "getBlockStateRule")
  public BlockStateRule getBlockStateRule(
      final MaterialRuleContext context, Operation<BlockStateRule> op) {
    if (SharedConstants.DEBUG_DISABLE_ORE_VEINS) return BiomePredicate.ALWAYS_NULL;
    if (SharedConstants.DEBUG_ORE_VEINS) return op.call(context);

    RandomSplitter randomFactory = context.getOrCreateRandomSplitter(Identifier.ofVanilla("ore"));

    if (randomFactory instanceof Xoroshiro128PlusPlusRandom.Splitter xoroshiro) {
      DensityProvider densitySampler = context.getDensityProvider(this.density, true);
      DensityProvider richnessSampler = context.getDensityProvider(this.richness, true);
      DensityProvider fillerGapSampler = context.getDensityProvider(this.fillerGap, false);

      return OreVeinMaterialRule.create(
          xoroshiro,
          this.oreBlock,
          this.rawOreBlock,
          this.fillerBlock,
          this.rawOreChance,
          densitySampler,
          richnessSampler,
          fillerGapSampler);
    }

    return op.call(context);
  }
}
