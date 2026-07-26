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

    var original =
        ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius());
    var originalBiomes =
        ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius() + 1);

    generateChunk(fakeWorld, original, originalBiomes, "Vanilla " + name);

    FastNoiseConfig.ENABLED = true;

    var modded =
        ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius());
    var moddedBiomes =
        ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius() + 1);

    generateChunk(fakeWorld, modded, moddedBiomes, "Modded " + name);

    modded.forEach(
        (a) -> {
          var b = original.getChunk(a.getPos());

          try {
            TestWorld.matches(a, b);
          } catch (Exception e) {
            TestWorld.dumpSections("Modded", a);
            TestWorld.dumpSections("Vanilla", b);
            throw e;
          }
        });
    LOGGER.info(name + " matches");
  }

  private static void generateChunk(
      TestWorld world, ChunkRegion base, ChunkRegion biomeRegion, String name) {
    biomeRegion.forEach(
        (chunk) -> {
          world.biomes(biomeRegion, chunk);
          chunk.setStatus(ChunkStatus.BIOMES);
        });

    LOGGER.info("Generated Biomes for " + name);

    base.copyBiomes(biomeRegion);

    base.forEach(
        (chunk) -> {
          world.noise(base, chunk);
          world.surface(chunk, biomeRegion);
        });

    LOGGER.info("Generated " + name);
  }
}
