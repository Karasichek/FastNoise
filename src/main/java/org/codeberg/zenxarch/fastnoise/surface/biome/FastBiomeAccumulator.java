package org.codeberg.zenxarch.fastnoise.surface.biome;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;

public final class FastBiomeAccumulator {
  public static ReferenceSet<RegistryEntry<Biome>> accumulate(ChunkRegion region, ChunkPos cpos) {
    var items = new ReferenceArraySet<RegistryEntry<Biome>>(8);

    var sx = cpos.x();
    var sz = cpos.z();

    accumulate(region.getChunk(sx - 1, sz - 1), items);
    accumulate(region.getChunk(sx, sz - 1), items);
    accumulate(region.getChunk(sx + 1, sz - 1), items);

    accumulate(region.getChunk(sx - 1, sz), items);
    accumulate(region.getChunk(sx, sz), items);
    accumulate(region.getChunk(sx + 1, sz), items);

    accumulate(region.getChunk(sx - 1, sz + 1), items);
    accumulate(region.getChunk(sx, sz + 1), items);
    accumulate(region.getChunk(sx + 1, sz + 1), items);

    if (items.size() == 1) {
      for (var item : items) return ReferenceSets.singleton(item);
    }

    if (items.size() > 4) {
      return new ReferenceOpenHashSet<>(items, Hash.VERY_FAST_LOAD_FACTOR);
    }

    return items;
  }

  public static void accumulate(Chunk chunk, ReferenceSet<RegistryEntry<Biome>> biomeOut) {
    var sections = chunk.getSectionArray();
    for (int i = 0; i < sections.length; i++) {
      var palette =
          ((PalettedContainer<RegistryEntry<Biome>>) sections[i].biomeContainer).data.palette();
      for (int j = 0; j < palette.getSize(); j++) biomeOut.add(palette.get(j));
    }
  }
}
