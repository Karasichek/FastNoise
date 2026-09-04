package org.codeberg.zenxarch.perf;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.function.Consumer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import org.codeberg.zenxarch.perf.TestGlobals.FakeAbstractChunkHolder;
import org.codeberg.zenxarch.perf.mixin.ChunkAccessor;

public record ChunkRegion(BoundedRegionArray<FakeAbstractChunkHolder> storage)
    implements BiomeSupplier {
  public static ChunkRegion of(TestWorld world, int centerX, int centerZ, int radius) {

    var storage = TestGlobals.createRegionArray(world.serverWorld, centerX, centerZ, radius);
    return new ChunkRegion(storage);
  }

  @Override
  public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
    return this.getChunk(biomeX >> 2, biomeZ >> 2).getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
  }

  public void copyNoiseAndHeightmap(ChunkRegion region) {
    this.forEach(
        (dest) -> {
          var src = region.getChunk(dest.getPos());

          var srcData = src.getSectionArray();
          var destData = dest.getSectionArray();

          for (int j = 0; j < srcData.length; j++) {
            destData[j] =
                new ChunkSection(srcData[j].blockStateContainer.copy(), destData[j].biomeContainer);
          }

          for (var heightmap : src.getHeightmaps()) {
            dest.setHeightmap(heightmap.getKey(), heightmap.getValue().asLongArray());
          }

          var destList = ((ChunkAccessor) dest).zenxarch$postProcessingLists();
          var srcList = ((ChunkAccessor) src).zenxarch$postProcessingLists();
          if (destList.length != srcList.length) {
            throw new IllegalStateException("Copying invalid data ");
          }
          for (int j = 0; j < srcList.length; j++) {
            destList[j] = copy(srcList[j]);
          }
        });
  }

  private static ShortList copy(ShortList src) {
    if (src == null) return null;
    if (src.isEmpty()) return null;
    var result = new ShortArrayList();
    result.addAll(src);
    return result;
  }

  public void copyBiomes(ChunkRegion region) {
    this.forEach(
        (dest) -> {
          var src = region.getChunk(dest.getPos());

          var srcData = src.getSectionArray();
          var destData = dest.getSectionArray();

          for (int j = 0; j < srcData.length; j++) {
            destData[j] =
                new ChunkSection(destData[j].blockStateContainer, srcData[j].biomeContainer);
          }
        });
  }

  public ProtoChunk getChunk(ChunkPos pos) {
    return getChunk(pos.x(), pos.z());
  }

  public ProtoChunk getChunk(int cx, int cz) {
    return this.storage.get(cx, cz).getChunk();
  }

  public void forEach(Consumer<ProtoChunk> consumer) {
    this.storage.forEach(chunkHolder -> consumer.accept(chunkHolder.getChunk()));
  }
}
