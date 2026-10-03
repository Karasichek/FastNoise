package org.codeberg.zenxarch.matcomp.biome;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.PalettedContainer;
import org.codeberg.zenxarch.matcomp.config.MaterialRuleCompilerConfig;

public final class FastPosToBiome {
  public static Function<BlockPos, RegistryEntry<Biome>> createPosToBiome(
      final BiomeAccess biomeAccess,
      final Chunk chunk,
      final Set<RegistryEntry<Biome>> includedBiomes) {
    if (includedBiomes == null) return new VanillaGetter(biomeAccess);
    @SuppressWarnings("unchecked")
    var biomes = (RegistryEntry<Biome>[]) includedBiomes.toArray(RegistryEntry[]::new);

    if (biomes.length == 1) return new SingleBiomeGetter(biomes[0]);

    if (MaterialRuleCompilerConfig.OPTIMIZE_BIOME_ACCESS
        && biomeAccess.storage instanceof ChunkRegion region) {
      return new OptimizedGetter(
          new FastBiomeCache(biomes, collectChunks(region, chunk.getPos())), biomeAccess);
    }

    return new VanillaGetter(biomeAccess);
  }

  private static record SingleBiomeGetter(RegistryEntry<Biome> biome)
      implements Function<BlockPos, RegistryEntry<Biome>> {
    @Override
    public RegistryEntry<Biome> apply(BlockPos pos) {
      return biome;
    }
  }

  private static record VanillaGetter(BiomeAccess biomeAccess)
      implements Function<BlockPos, RegistryEntry<Biome>> {
    @Override
    public RegistryEntry<Biome> apply(BlockPos pos) {
      return biomeAccess.getBiome(pos);
    }
  }

  private static record OptimizedGetter(FastBiomeCache cache, BiomeAccess fallback)
      implements Function<BlockPos, RegistryEntry<Biome>> {
    @Override
    public RegistryEntry<Biome> apply(BlockPos pos) {
      var cached = cache.getBiomeOrNull(pos.getX(), pos.getY(), pos.getZ());
      if (cached == null) return fallback.getBiome(pos);
      return cached;
    }
  }

  public static Chunk[] collectChunks(ChunkRegion chunkRegion, ChunkPos centerPos) {
    final var cx = centerPos.x();
    final var cz = centerPos.z();
    final var chunks = new Chunk[9];

    for (int z = -1; z <= 1; z++) {
      for (int x = -1; x <= 1; x++) {
        final var idx = ((z + 1) * 3) + (x + 1);
        if ((chunks[idx] = chunkRegion.getChunk(cx + x, cz + z))
            .getStatus()
            .isEarlierThan(ChunkStatus.BIOMES)) return null;
      }
    }

    return chunks;
  }

  public static Set<RegistryEntry<Biome>> getBiomes(
      final ChunkRegion chunkRegion, final int rangeChunks) {
    final var result = new ObjectArraySet<RegistryEntry<Biome>>();
    final var reusableIntSet = new IntArraySet();
    final var cpos = chunkRegion.getCenterPos();
    final var cx = cpos.x();
    final var cz = cpos.z();
    for (int z = -rangeChunks; z <= rangeChunks; z++) {
      for (int x = -rangeChunks; x <= rangeChunks; x++) {
        var chunkSections = chunkRegion.getChunk(cx + x, cz + z).getSectionArray();
        for (int i = 0; i < chunkSections.length; i++) {
          getBiomes(
              result,
              (PalettedContainer<RegistryEntry<Biome>>) chunkSections[i].biomeContainer,
              reusableIntSet);
        }
      }
    }
    return result;
  }

  private static void getBiomes(
      Set<RegistryEntry<Biome>> biomes,
      PalettedContainer<RegistryEntry<Biome>> container,
      IntArraySet reusableIntSet) {
    var data = container.data;
    var palette = data.palette();
    if (palette.getSize() == 1) {
      biomes.add(palette.get(0));
      return;
    }

    reusableIntSet.clear();
    data.storage().forEach(reusableIntSet::add);

    reusableIntSet.forEach(k -> biomes.add(palette.get(k)));
  }
}
