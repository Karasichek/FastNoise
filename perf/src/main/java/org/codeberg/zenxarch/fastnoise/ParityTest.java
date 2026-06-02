package org.codeberg.zenxarch.fastnoise;

import net.minecraft.world.chunk.ChunkStatus;
import org.codeberg.zenxarch.fastnoise.config.FastNoiseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParityTest {
  public static final Logger LOGGER = LoggerFactory.getLogger(ParityTest.class);

  public static void doParityTest() {
    LOGGER.info("Starting parity test");
    for (long i = 0; i < BenchmarkSettings.seedCount; i++) {
      LOGGER.info("Testing seed: {}", BenchmarkSettings.defaultSeed + i);
      doParityTest(BenchmarkSettings.overworld(BenchmarkSettings.defaultSeed + i));
      doParityTest(BenchmarkSettings.nether(BenchmarkSettings.defaultSeed + i));
      doParityTest(BenchmarkSettings.end(BenchmarkSettings.defaultSeed + i));
    }
  }

  private static void doParityTest(BenchmarkSettings settings) {
    var fakeWorld = new TestWorld(settings);

    var name = settings.dimensionOptions().getValue().getPath();
    LOGGER.info("Generating " + name);

    FastNoiseConfig.ENABLED = false;

    var original = ChunkRegion.of(fakeWorld, settings.region());
    var originalBiomes = ChunkRegion.of(fakeWorld, settings.biomeRegion());

    generateChunk(fakeWorld, original, originalBiomes, "Vanilla " + name);

    FastNoiseConfig.ENABLED = true;

    var modded = ChunkRegion.of(fakeWorld, settings.region());
    var moddedBiomes = ChunkRegion.of(fakeWorld, settings.biomeRegion());

    generateChunk(fakeWorld, modded, moddedBiomes, "Modded " + name);

    for (int i = 0; i < modded.chunks().length; i++) {
      var a = modded.chunks()[i];
      var b = original.getChunk(a.getPos());

      try {
        TestWorld.matches(a, b);
      } catch (Exception e) {
        TestWorld.dumpSections("Modded", a);
        TestWorld.dumpSections("Vanilla", b);
        throw e;
      }
    }
    LOGGER.info(name + " matches");
  }

  private static void generateChunk(
      TestWorld world, ChunkRegion base, ChunkRegion biomeRegion, String name) {
    for (int i = 0; i < biomeRegion.chunks().length; i++) {
      world.biomes(biomeRegion.chunks()[i]);
      biomeRegion.chunks()[i].setStatus(ChunkStatus.BIOMES);
    }

    LOGGER.info("Generated Biomes for " + name);

    base.copyBiomes(biomeRegion);

    for (int i = 0; i < base.chunks().length; i++) {
      base.chunks()[i].getOrCreateChunkNoiseSampler(world::createSampler);
      world.noise(base.chunks()[i]);
      world.surface(base.chunks()[i], biomeRegion);
    }
    LOGGER.info("Generated " + name);
  }
}
