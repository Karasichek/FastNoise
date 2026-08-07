package org.codeberg.zenxarch.fastnoise.heightmap;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ArrayPalette;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.SingularPalette;
import org.codeberg.zenxarch.fastnoise.mixin.HeightmapAccessor;

public final class HeightmapUtil {
  private HeightmapUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static Heightmap.Type[] calculateHeightmaps(ChunkStatus status) {
    var result = new Heightmap.Type[status.getHeightmapTypes().size()];
    return status.getHeightmapTypes().toArray(result);
  }

  public static boolean updateHeightmap(
      int lx,
      int lz,
      PaletteStorage storage,
      Predicate<BlockState> predicate,
      BlockState state,
      int y,
      ChunkSection[] sections) {
    int idx = lx + (lz << 4);
    int current = storage.get(idx);
    if ((y + 2) > current) {
      if (predicate.test(state)) {
        if (y >= current) storage.set(idx, y + 1);
      } else { // go down the whole chunk till we hit something
        storage.set(idx, getHeightmapY(idx, sections, y - 1, predicate));
        return false;
      }
    }
    return true;
  }

  private static int getHeightmapY(
      int hidx, ChunkSection[] sections, int startY, Predicate<BlockState> predicate) {
    int ly = startY & 15;
    int sy = startY >> 4;
    while (sy >= 0) {
      var section = sections[sy];
      var palette = section.blockStateContainer.data.palette();
      var storage = section.blockStateContainer.data.storage();
      while (ly >= 0) {
        if (predicate.test(palette.get(storage.get((ly << 8) + hidx)))) {
          return (sy << 4) + ly + 1;
        }
        ly--;
      }
      ly = 15;
      sy--;
    }

    return 0;
  }

  /**
   * Use ArrayMap to cache predicate Cache defaultBlock and skip air
   *
   * @param chunk
   * @param heightmap
   */
  private static class StatePredicateCache {
    public BlockState[] states = new BlockState[16];
    public boolean[] values = new boolean[16];
    private int size = 0;

    public boolean get(BlockState state, Predicate<BlockState> predicate) {
      for (int i = 0; i < size; i++) if (states[i] == state) return values[i];
      states[size] = state;
      values[size] = predicate.test(state);
      return values[size++];
    }
  }

  private static int getLocalY(
      ChunkSection section, int hidx, StatePredicateCache cache, Predicate<BlockState> predicate) {
    var palette = section.blockStateContainer.data.palette();
    var storage = section.blockStateContainer.data.storage();
    if (palette instanceof SingularPalette) { // just assume air
      return -1;
    }
    var array = (ArrayPalette<BlockState>) palette;

    for (int ly = 15; ly >= 0; ly--) {
      var val = storage.get((ly << 8) + hidx);
      if (val == 0) continue;
      if (cache.get(array.get(val), predicate)) return ly;
    }

    return -1;
  }

  private static int findFirstNonEmptySection(
      ChunkSection[] sections, StatePredicateCache cache, Predicate<BlockState> predicate) {
    for (int sy = (sections.length - 1); sy >= 0; sy--) {
      var palette = sections[sy].blockStateContainer.data.palette();
      int psize = palette.getSize();
      if (psize == 1) continue;
      var array = ((ArrayPalette<BlockState>) palette);
      for (int i = 1; i < array.size; i++) {
        if (cache.get(array.get(i), predicate)) return sy;
      }
    }

    return -1; // Empty chunk
  }

  public static void populateHeightmapPostNoise(
      Chunk chunk, Heightmap.Type typex, BlockState defaultBlockState, BlockState AIR) {
    var heightmap = chunk.getHeightmap(typex);
    var predicate = ((HeightmapAccessor) heightmap).zenxarch$getBlockPredicate();
    var storage = ((HeightmapAccessor) heightmap).zenxarch$getStorage();

    if (predicate.test(AIR)) { // We don't like air being counted in a heightmap
      Heightmap.populateHeightmaps(chunk, EnumSet.of(typex));
      return;
    }

    var sections = chunk.getSectionArray();

    var cache = new StatePredicateCache();
    cache.get(defaultBlockState, predicate); // Cache default block first

    var nonEmptySection = findFirstNonEmptySection(sections, cache, predicate);
    if (nonEmptySection == -1) return; // Chunk is empty no-op

    for (int hidx = 0; hidx < 256; hidx++) {
      for (int sy = nonEmptySection; sy >= 0; sy--) {
        var ly = getLocalY(sections[sy], hidx, cache, predicate);
        if (ly >= 0) {
          storage.set(hidx, (sy << 4) + ly + 1);
          break;
        }
      }
    }
  }
}
