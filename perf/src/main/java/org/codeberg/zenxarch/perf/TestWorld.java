package org.codeberg.zenxarch.perf;

import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.AbstractChunkHolder;
import net.minecraft.world.chunk.ChunkGenerating;
import net.minecraft.world.chunk.ChunkGenerationContext;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.codeberg.zenxarch.perf.mixin.ChunkAccessor;

public final class TestWorld {
  public final ServerWorld serverWorld;
  private final ChunkGenerationContext context;
  private final NoiseChunkGenerator generator;

  public TestWorld(BenchmarkSettings settings) {
    this(settings.worldKey(), settings.dimensionOptions(), settings.seed());
  }

  public TestWorld(
      RegistryKey<World> worldKey, RegistryKey<DimensionOptions> optionsKey, long seed) {
    this.serverWorld = TestGlobals.create(worldKey, optionsKey, seed);
    this.context = TestGlobals.createContext(serverWorld);

    var generator = context.generator();
    if (!(generator instanceof NoiseChunkGenerator noise)) {
      throw new IllegalStateException("Not noise chunk generator");
    }
    this.generator = noise;
  }

  public static void resetNoise(ProtoChunk chunk) {
    var data = chunk.getSectionArray();
    for (int i = 0; i < data.length; i++)
      data[i].blockStateContainer = data[i].blockStateContainer.slice();
    var len = chunk.getHeightmap(Type.OCEAN_FLOOR_WG).asLongArray().length;
    chunk.setHeightmap(Type.OCEAN_FLOOR_WG, new long[len]);
    len = chunk.getHeightmap(Type.WORLD_SURFACE_WG).asLongArray().length;
    chunk.setHeightmap(Type.WORLD_SURFACE_WG, new long[len]);

    Arrays.fill(chunk.getPostProcessingLists(), null);
  }

  public static void resetBiomes(ProtoChunk chunk) {
    var data = chunk.getSectionArray();
    for (int i = 0; i < data.length; i++) data[i].biomeContainer = data[i].biomeContainer.slice();
  }

  // @SuppressWarnings("unchecked")
  // public void buildTerrain(ProtoChunk chunk, ChunkRegion biomeSource) {
  //   ChunkGenerating.buildSurface(
  //       context,
  //       ChunkGenerationSteps.GENERATION.get(ChunkStatus.TERRAIN),
  //       (BoundedRegionArray<AbstractChunkHolder>) (Object) biomeSource.storage(),
  //       chunk);
  //   chunk.setStatus(ChunkStatus.TERRAIN);
  // }

  @SuppressWarnings("unchecked")
  public void noise(ChunkRegion region, ProtoChunk chunk) {
    ChunkGenerating.populateNoise(
        context,
        ChunkGenerationSteps.GENERATION.get(ChunkStatus.NOISE),
        (BoundedRegionArray<AbstractChunkHolder>) (Object) region.storage(),
        chunk);
    chunk.setStatus(ChunkStatus.NOISE);
  }

  @SuppressWarnings("unchecked")
  public void biomes(ChunkRegion region, ProtoChunk chunk) {
    ChunkGenerating.populateBiomes(
        context,
        ChunkGenerationSteps.GENERATION.get(ChunkStatus.BIOMES),
        (BoundedRegionArray<AbstractChunkHolder>) (Object) region.storage(),
        chunk);
    chunk.setStatus(ChunkStatus.BIOMES);
  }

  @SuppressWarnings("unchecked")
  public void surface(ProtoChunk chunk, ChunkRegion biomeSource) {
    ChunkGenerating.buildSurface(
        context,
        ChunkGenerationSteps.GENERATION.get(ChunkStatus.SURFACE),
        (BoundedRegionArray<AbstractChunkHolder>) (Object) biomeSource.storage(),
        chunk);
    chunk.setStatus(ChunkStatus.SURFACE);
  }

  public static void matches(ProtoChunk a, ProtoChunk b) {
    if (!a.getPos().equals(b.getPos())) {
      throw new IllegalStateException("Wrong chunks are getting compared");
    }
    var self = a.getSectionArray();
    var other = b.getSectionArray();

    if (self.length != other.length)
      throw new IllegalStateException("Chunk sections length differ");

    var bottomCoord = a.getBottomSectionCoord();

    for (int i = 0; i < self.length; i++) {
      if (!matches(
          (PalettedContainer<RegistryEntry<Biome>>) self[i].biomeContainer,
          (PalettedContainer<RegistryEntry<Biome>>) other[i].biomeContainer,
          a.getPos(),
          i + bottomCoord)) {
        dumpPalette(a, b, self[i], other[i], i);
        throw new IllegalStateException("Chunk sections biomes differ");
      }
      if (!matches(
          self[i].blockStateContainer, other[i].blockStateContainer, a.getPos(), i + bottomCoord)) {
        dumpPalette(a, b, self[i], other[i], i);
        throw new IllegalStateException("Chunk sections blocks differ");
      }
      if (!matchesCounts(self[i], other[i])) {
        throw new IllegalStateException("Chunk sections block counts differ");
      }
    }

    for (var heightmap : a.getHeightmaps()) {
      if (!Arrays.equals(
          a.getHeightmap(heightmap.getKey()).asLongArray(),
          b.getHeightmap(heightmap.getKey()).asLongArray()))
        throw new IllegalStateException("Chunk sections heightmaps differ");
    }

    {
      var aList = ((ChunkAccessor) a).zenxarch$postProcessingLists();
      var bList = ((ChunkAccessor) b).zenxarch$postProcessingLists();
      if (aList.length != bList.length)
        throw new IllegalStateException("Chunk sections post processing arrays differ");
      for (int i = 0; i < aList.length; i++) {
        if (!matches(aList[i], bList[i]))
          throw new IllegalStateException("Chunks sections post processing lists differ");
      }
    }
  }

  public static void dumpSections(String name, ProtoChunk a) {
    var sections = a.getSectionArray();
    for (int i = 0; i < sections.length; i++) {
      BenchmarkMain.LOGGER.info(
          "{} {} -> {}", name, i, paletteToString(sections[i].blockStateContainer.data.palette()));
    }
  }

  private static void dumpPalette(
      ProtoChunk a, ProtoChunk b, ChunkSection ac, ChunkSection bc, int idx) {
    var pos = a.getPos();
    BenchmarkMain.LOGGER.info("x: {},z: {}", pos.getStartX(), pos.getStartZ());
    BenchmarkMain.LOGGER.info("y: {},cy: {}", a.sectionIndexToCoord(idx) * 16, idx);
    BenchmarkMain.LOGGER.info(
        "{} {}",
        ac.blockStateContainer.data.palette().getClass().getSimpleName(),
        bc.blockStateContainer.data.palette().getClass().getSimpleName());

    var apal = ac.blockStateContainer.data.palette();
    var bpal = bc.blockStateContainer.data.palette();

    BenchmarkMain.LOGGER.info("a: {}, b: {}", paletteToString(apal), paletteToString(bpal));
  }

  private static <T> String paletteToString(Palette<T> palette) {
    var size = palette.getSize();
    var result = new Object[size];
    for (int i = 0; i < size; i++) result[i] = palette.get(i);
    return Arrays.toString(result);
  }

  private static boolean matchesCounts(ChunkSection a, ChunkSection b) {
    return (a.nonEmptyBlockCount == b.nonEmptyBlockCount
        && a.nonEmptyFluidCount == b.nonEmptyFluidCount
        && a.randomTickableBlockCount == b.randomTickableBlockCount);
    // && a.randomTickableFluidCount == b.randomTickableFluidCount);
  }

  private static <T> boolean matches(
      PalettedContainer<T> self, PalettedContainer<T> other, ChunkPos pos, int cy) {
    if (self.data.storage().getSize() != other.data.storage().getSize()) return false;

    var selfPal = self.data.palette();
    var selfSt = self.data.storage();

    var otherPal = other.data.palette();
    var otherSt = other.data.storage();

    // skip invalid chunks
    if (selfPal.hasAny(Objects::isNull)) return true;
    if (otherPal.hasAny(Objects::isNull)) return true;

    for (int i = 0; i < selfSt.getSize(); i++) {
      var sel = selfPal.get(selfSt.get(i));
      var oth = otherPal.get(otherSt.get(i));
      if (sel != oth) {
        BenchmarkMain.LOGGER.info("Modded: {},Vanilla: {}", sel, oth);
        BenchmarkMain.LOGGER.info("Pos: {}", ProtoChunk.joinBlockPos((short) i, cy, pos));
        BenchmarkMain.LOGGER.info("a: {},b: {}", selfSt.get(i), otherSt.get(i));
        return false;
      }
    }
    return true;
  }

  private static boolean matches(ShortList a, ShortList b) {
    if (a == null || b == null) return a == b;
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (a.getShort(i) != b.getShort(i)) return false;
    }
    return true;
  }
}
