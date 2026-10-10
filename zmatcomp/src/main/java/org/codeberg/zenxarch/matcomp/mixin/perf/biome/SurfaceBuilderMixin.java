package org.codeberg.zenxarch.matcomp.mixin.perf.biome;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.function.Function;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.codeberg.zenxarch.matcomp.biome.FastPosToBiome;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin {

  @WrapOperation(
      method = "buildSurface",
      at =
          @At(
              value = "NEW",
              target =
                  "Lnet/minecraft/world/gen/surfacebuilder/MaterialRules$MaterialRuleContext;"))
  private MaterialRuleContext zenxarch$addIncludedBiomes(
      final SurfaceBuilder builder,
      final NoiseConfig noiseConfig,
      final Chunk chunk,
      final ChunkNoiseSampler chunkNoiseSampler,
      final Function<BlockPos, RegistryEntry<Biome>> posToBiome,
      final Registry<Biome> biomeRegistry,
      final HeightContext heightContext,
      final Operation<MaterialRuleContext> op,
      @Local final BiomeAccess biomeAccess) {
    ReferenceArraySet<RegistryEntry<Biome>> includedBiomes = null;

    var cpos = chunk.getPos();
    var cx = cpos.x();
    var cz = cpos.z();
    if (biomeAccess.storage instanceof ChunkRegion region) {
      includedBiomes = new ReferenceArraySet<>();
      var intset = new IntArraySet(4);
      for (int z = -1; z <= 1; z++) {
        for (int x = -1; x <= 1; x++) {
          var sections = region.getChunk(cx + x, cz + z).getSectionArray();
          for (int i = 0; i < sections.length; i++) {
            var biomes =
                ((PalettedContainer<RegistryEntry<Biome>>) sections[i].biomeContainer).data;
            if (biomes.palette().getSize() == 1) includedBiomes.add(biomes.palette().get(0));
            else {
              intset.clear();
              biomes.storage().forEach(intset::add);
              for (int k : intset) includedBiomes.add(biomes.palette().get(k));
            }
          }
        }
      }
    }

    var res =
        op.call(
            builder,
            noiseConfig,
            chunk,
            chunkNoiseSampler,
            FastPosToBiome.createPosToBiome(biomeAccess, chunk, includedBiomes),
            biomeRegistry,
            heightContext);
    ((MaterialRuleContextExtension) res).zenxarch$setIncludedBiomes(includedBiomes);
    return res;
  }
}
