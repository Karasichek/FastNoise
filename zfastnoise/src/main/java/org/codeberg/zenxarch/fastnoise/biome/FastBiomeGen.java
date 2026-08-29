package org.codeberg.zenxarch.fastnoise.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.gen.sampler.Sampler.Contextual;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.codeberg.zenxarch.fastnoise.noise.FastNoisePaletteHelper;

public final class FastBiomeGen {

  public static void populateBiomes(Chunk chunk, BiomeSupplier supplier) {

    if (FastNoiseConfig.OPTIMIZE_FIXED_BIOMES && supplier instanceof FixedBiomeSource fixed) {
      packSingleBiome(chunk.getSectionArray(), fixed.biome);
      return;
    }

    if (FastNoiseConfig.OPTIMIZE_END_BIOMES && supplier instanceof TheEndBiomeSupplier theEnd) {
      final var chunkPos = chunk.getPos();
      populateEndBiomes(theEnd, chunk, chunk.getSectionArray(), chunkPos.x(), chunkPos.z());
      return;
    }

    populateBiomesImpl(chunk, supplier);
  }

  private static void populateBiomesImpl(Chunk chunk, BiomeSupplier supplier) {

    var sections = chunk.getSectionArray();

    final var chunkPos = chunk.getPos();
    final int cx = chunkPos.x();
    final int cz = chunkPos.z();

    final int minY = chunk.getBottomY();
    final int x = cx << 2;
    int y = minY >> 2;
    final int z = cz << 2;

    @SuppressWarnings("unchecked")
    final RegistryEntry<Biome>[] biomes = new RegistryEntry[64];
    final var storage = new byte[64];

    for (int i = 0; i < sections.length; i++) {
      var section = sections[i];
      FastBiomeGen.populateBiomes(section, supplier, x, y, z, biomes, storage);
      y += 4;
    }
  }

  private static void packSingleBiome(ChunkSection[] sections, RegistryEntry<Biome> biome) {
    for (int i = 0; i < sections.length; i++) {
      FastNoisePaletteHelper.packSingleElement(
          (PalettedContainer<RegistryEntry<Biome>>) sections[i].biomeContainer, biome);
    }
  }

  private static void populateBiomes(
      ChunkSection section,
      BiomeSupplier biomeSupplier,
      int x,
      int y,
      int z,
      RegistryEntry<Biome>[] biomes,
      byte[] storage) {

    int size = 0;

    for (int ix = 0; ix < 4; ix++) {
      for (int iy = 0; iy < 4; iy++) {
        for (int iz = 0; iz < 4; iz++) {

          var biome = biomeSupplier.getBiomeForNoiseGen(x + ix, y + iy, z + iz);

          int bidx = -1;
          for (int i = 0; i < size; i++)
            if (biomes[i] == biome) {
              bidx = i;
              break;
            }

          if (bidx == -1) biomes[(bidx = size++)] = biome;

          storage[(iy << 2 | iz) << 2 | ix] = (byte) bidx;
        }
      }
      var container = ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer);
      FastNoisePaletteHelper.pack(container, biomes, size, storage);
    }
  }

  private static class EndBiomeNoisePos {
    private final int x;
    private final int z;
    public int y;
    private final Contextual erosion;

    public EndBiomeNoisePos(int x, int y, int z, MultiNoiseSampler sampler) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.erosion = sampler.erosion();
    }

    public double sampleAndStep() {
      var result = erosion.sample(this.x, this.y, this.z);
      this.y += 4;
      return (double) result;
    }
  }

  private static RegistryEntry<Biome> getEndBiomeFromHeight(
      TheEndBiomeSource source, double height) {
    if (height > 0.25) return source.highlandsBiome;
    if (height >= -0.0625) return source.midlandsBiome;
    if (height < -0.21875) return source.smallIslandsBiome;
    return source.barrensBiome;
  }

  private static void populateEndBiomes(
      TheEndBiomeSupplier supplier, Chunk chunk, ChunkSection[] sections, int cx, int cz) {
    var source = supplier.biomeSource();
    var sampler = supplier.sampler();
    if ((Math.abs(cx) <= 64) && ((Math.abs(cz) <= 64)) && ((cx * cx + cz * cz) <= 4096)) {
      packSingleBiome(sections, source.centerBiome);
      return;
    }

    final int x = (cx << 4) + 8;
    final int z = (cz << 4) + 8;

    var noisePos = new EndBiomeNoisePos(x, chunk.getBottomY(), z, sampler);

    for (int i = 0; i < sections.length; i++) {
      var a = getEndBiomeFromHeight(source, noisePos.sampleAndStep());
      var b = getEndBiomeFromHeight(source, noisePos.sampleAndStep());
      var c = getEndBiomeFromHeight(source, noisePos.sampleAndStep());
      var d = getEndBiomeFromHeight(source, noisePos.sampleAndStep());

      FastNoisePaletteHelper.packFourEntries(
          (PalettedContainer<RegistryEntry<Biome>>) sections[i].biomeContainer, a, b, c, d);
    }
  }
}
