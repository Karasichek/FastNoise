package org.codeberg.zenxarch.fastnoise;

import net.minecraft.world.chunk.ChunkStatus;
import org.codeberg.zenxarch.fastnoise.parity.ChunkStorage;
import org.codeberg.zenxarch.fastnoise.parity.ParityGlobals;
import org.codeberg.zenxarch.fastnoise.parity.ParityGlobals.ChunkStorageKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParityTest {

  public static final Logger LOGGER = LoggerFactory.getLogger(ParityGlobals.class);

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

    var base = ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius());
    var biomes =
        ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius() + 1);

    generateChunk(fakeWorld, base, biomes, "Vanilla " + name);
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

          if (ParityGlobals.isServer) {
            var pos = chunk.getPos();
            var against = ParityGlobals.get(new ChunkStorageKey(name, pos.x(), pos.z()));
            if (!against.matches(chunk)) {
              throw new IllegalStateException("Chunks don't match " + pos);
            }
          } else {
            var storage = new ChunkStorage(name, chunk);
            ParityGlobals.send(storage);
          }
        });

    LOGGER.info("Generated " + name);
  }
}
