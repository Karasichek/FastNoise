package org.codeberg.zenxarch.fastnoise;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.AquiferSampler.FluidLevelSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes.Beardifying;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.codeberg.zenxarch.fastnoise.mixin.ChunkAccessor;
import org.codeberg.zenxarch.fastnoise.mixin.NoiseChunkGeneratorAccessor;
import org.codeberg.zenxarch.fastnoise.mixin.SurfaceBuilderAccessor;
import org.codeberg.zenxarch.fastnoise.surface.FastSurfaceGen;
import org.codeberg.zenxarch.fastnoise.surface.biome.FastBiomeAccumulator;

public final class TestWorld {
  public final ChunkGeneratorSettings settings;
  private final NoiseConfig noiseConfig;
  private final FluidLevelSampler fluidLevelSampler;
  private final NoiseChunkGenerator generator;
  private final long seed;

  private static final Beardifying beardifying = new BeardifyingImpl();

  private final FakeWorld world;

  public TestWorld(BenchmarkSettings settings) {
    this(settings.dimensionOptions(), settings.seed());
  }

  public TestWorld(RegistryKey<DimensionOptions> optionsKey, long seed) {
    this.seed = seed;
    var manager = TestGlobals.getManager();
    var options =
        manager
            .getEntryOrThrow(WorldPresets.DEFAULT)
            .value()
            .createDimensionsRegistryHolder()
            .dimensions()
            .get(optionsKey);

    if (!(options.chunkGenerator() instanceof NoiseChunkGenerator chunkGenerator)) {
      throw new IllegalStateException("Chunk generator must be noise chunk generator");
    }

    this.generator = chunkGenerator;

    this.settings = chunkGenerator.getSettings().value();
    this.world =
        new FakeWorld(
            this.settings.generationShapeConfig().height(),
            this.settings.generationShapeConfig().minimumY());

    this.noiseConfig =
        NoiseConfig.create(this.settings, manager.getOrThrow(RegistryKeys.NOISE_PARAMETERS), seed);

    var lava = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
    var water =
        new AquiferSampler.FluidLevel(this.settings.seaLevel(), this.settings.defaultFluid());
    int cutoff = Math.min(-54, this.settings.seaLevel());
    this.fluidLevelSampler = (x, y, z) -> y < cutoff ? lava : water;
  }

  public ProtoChunk createChunk(ChunkPos pos) {
    return new ProtoChunk(
        pos, UpgradeData.NO_UPGRADE_DATA, this.world, TestGlobals.getFactory(), null);
  }

  public ChunkNoiseSampler createSampler(Chunk chunk) {
    return ChunkNoiseSampler.create(
        chunk, noiseConfig, beardifying, this.settings, fluidLevelSampler, Blender.getNoBlending());
  }

  public static void resetNoise(ProtoChunk chunk) {
    var data = chunk.getSectionArray();
    for (int i = 0; i < data.length; i++)
      data[i].blockStateContainer = data[i].blockStateContainer.slice();
    var len = chunk.getHeightmap(Type.OCEAN_FLOOR_WG).asLongArray().length;
    chunk.setHeightmap(Type.OCEAN_FLOOR_WG, new long[len]);
    len = chunk.getHeightmap(Type.WORLD_SURFACE_WG).asLongArray().length;
    chunk.setHeightmap(Type.WORLD_SURFACE_WG, new long[len]);
  }

  public static void resetBiomes(ProtoChunk chunk) {
    var data = chunk.getSectionArray();
    for (int i = 0; i < data.length; i++) data[i].biomeContainer = data[i].biomeContainer.slice();
  }

  public void noise(ProtoChunk chunk) {
    var shapeConfig = this.settings.generationShapeConfig().trimHeight(this.world);
    int minY = shapeConfig.minimumY();
    int minimumCellY = MathHelper.floorDiv(minY, shapeConfig.verticalCellBlockCount());
    int cellHeight =
        MathHelper.floorDiv(shapeConfig.height(), shapeConfig.verticalCellBlockCount());
    ((NoiseChunkGeneratorAccessor) (Object) this.generator)
        .zenxarch$method_38332(
            chunk,
            cellHeight,
            shapeConfig,
            minY,
            Blender.getNoBlending(),
            null,
            this.noiseConfig,
            minimumCellY);
  }

  public void biomes(ProtoChunk chunk) {
    chunk.chunkNoiseSampler = this.createSampler(chunk);
    this.generator.populateBiomes(Blender.getNoBlending(), this.noiseConfig, null, chunk);
  }

  public void surface(ProtoChunk chunk, ChunkRegion biomeSource) {
    var registry = TestGlobals.getManager().getOrThrow(RegistryKeys.BIOME);

    if (FastNoiseConfig.ENABLED
        && fastSurface(
            chunk,
            biomeSource,
            new BiomeAccess(biomeSource, this.seed),
            new HeightContext(generator, chunk),
            chunk.getOrCreateChunkNoiseSampler(null),
            biomeSource)) return;

    this.generator.buildSurface(
        chunk,
        new HeightContext(generator, chunk),
        noiseConfig,
        null,
        new BiomeAccess(biomeSource, this.seed),
        Blender.getNoBlending(),
        getBiomesAround(biomeSource, chunk.getPos(), 1));
  }

  public static ReferenceSet<RegistryEntry<Biome>> accumulate(ChunkRegion region, ChunkPos cpos) {
    var items = new ReferenceArraySet<RegistryEntry<Biome>>(8);

    var sx = cpos.x();
    var sz = cpos.z();

    FastBiomeAccumulator.accumulate(region.getChunk(sx - 1, sz - 1), items);
    FastBiomeAccumulator.accumulate(region.getChunk(sx, sz - 1), items);
    FastBiomeAccumulator.accumulate(region.getChunk(sx + 1, sz - 1), items);

    FastBiomeAccumulator.accumulate(region.getChunk(sx - 1, sz), items);
    FastBiomeAccumulator.accumulate(region.getChunk(sx, sz), items);
    FastBiomeAccumulator.accumulate(region.getChunk(sx + 1, sz), items);

    FastBiomeAccumulator.accumulate(region.getChunk(sx - 1, sz + 1), items);
    FastBiomeAccumulator.accumulate(region.getChunk(sx, sz + 1), items);
    FastBiomeAccumulator.accumulate(region.getChunk(sx + 1, sz + 1), items);

    if (items.size() == 1) {
      for (var item : items) return ReferenceSets.singleton(item);
    }

    if (items.size() > 4) {
      return new ReferenceOpenHashSet<>(items, Hash.VERY_FAST_LOAD_FACTOR);
    }

    return items;
  }

  private boolean fastSurface(
      ProtoChunk chunk,
      ChunkRegion biomeSource,
      BiomeAccess biomeAccess,
      HeightContext context,
      ChunkNoiseSampler sampler,
      ChunkRegion region) {
    var includedBiomes = accumulate(region, chunk.getPos());
    final var registry = TestGlobals.getManager().getOrThrow(RegistryKeys.BIOME);

    if (!FastSurfaceGen.canUseSurfaceBuilder(chunk, includedBiomes, registry)) return false;

    final var centerPos = chunk.getPos();
    final var cx = centerPos.x();
    final var cz = centerPos.z();
    final var chunks = new Chunk[9];

    for (int z = -1; z <= 1; z++) {
      for (int x = -1; x <= 1; x++) {
        final var idx = ((z + 1) * 3) + (x + 1);
        chunks[idx] = region.getChunk(cx + x, cz + z);
      }
    }

    FastSurfaceGen.buildSurface(
        (SurfaceBuilderAccessor) noiseConfig.getSurfaceBuilder(),
        noiseConfig,
        biomeAccess,
        settings.usesLegacyRandom(),
        context,
        chunk,
        sampler,
        settings.surfaceRule(),
        includedBiomes,
        chunks,
        registry);
    return true;
  }

  private static Set<RegistryEntry<Biome>> getBiomesAround(
      final ChunkRegion region, ChunkPos center, final int radiusChunks) {
    Set<RegistryEntry<Biome>> chunkBiomes = new ReferenceOpenHashSet<>();

    for (int z = center.z() - radiusChunks; z <= center.z() + radiusChunks; z++) {
      for (int x = center.x() - radiusChunks; x <= center.x() + radiusChunks; x++) {
        region.getChunk(x, z).getBiomes(chunkBiomes);
      }
    }

    return chunkBiomes;
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
        && a.randomTickableBlockCount == b.randomTickableBlockCount
        && a.randomTickableFluidCount == b.randomTickableFluidCount);
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

  private static record FakeWorld(int height, int bottomY) implements HeightLimitView {

    @Override
    public int getHeight() {
      return height;
    }

    @Override
    public int getBottomY() {
      return bottomY;
    }
  }

  private static class BeardifyingImpl implements Beardifying {

    @Override
    public double maxValue() {
      return 1.0;
    }

    @Override
    public double minValue() {
      return 0.0;
    }

    @Override
    public double sample(NoisePos pos) {
      return 0.5;
    }
  }
}
