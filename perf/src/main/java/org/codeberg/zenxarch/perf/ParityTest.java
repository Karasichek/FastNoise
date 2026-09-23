package org.codeberg.zenxarch.perf;

import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.codeberg.zenxarch.perf.parity.ChunkStorage;
import org.codeberg.zenxarch.perf.parity.ParityGlobals;
import org.codeberg.zenxarch.perf.parity.ParityGlobals.ChunkStorageKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParityTest {

  public static final Logger LOGGER = LoggerFactory.getLogger(ParityGlobals.class);

  private static final boolean MOD_LOADED = BenchmarkMain.getBooleanProperty("zmod").orElse(false);
  private static final boolean LOAD_RANDOM_CHUNKS =
      BenchmarkMain.getBooleanProperty("zparityrandom").orElse(true);
  private static final int LOAD_RANDOM_CHUNKS_COUNT =
      BenchmarkSettings.getIntProperty("zparityrandomCount", 10, 1);

  public static void doParityTest() {
    LOGGER.info("Starting parity test");
    for (long i = 0; i < BenchmarkSettings.seedCount; i++) {
      LOGGER.info("Testing seed: {}", BenchmarkSettings.defaultSeed + i);
      if (LOAD_RANDOM_CHUNKS) {
        doParityTestWithRandomCount(
            BenchmarkSettings.overworld(BenchmarkSettings.defaultSeed + i),
            LOAD_RANDOM_CHUNKS_COUNT);
        doParityTestWithRandomCount(
            BenchmarkSettings.nether(BenchmarkSettings.defaultSeed + i), LOAD_RANDOM_CHUNKS_COUNT);
        doParityTestWithRandomCount(
            BenchmarkSettings.end(BenchmarkSettings.defaultSeed + i), LOAD_RANDOM_CHUNKS_COUNT);
      } else {
        doParityTest(BenchmarkSettings.overworld(BenchmarkSettings.defaultSeed + i));
        doParityTest(BenchmarkSettings.nether(BenchmarkSettings.defaultSeed + i));
        doParityTest(BenchmarkSettings.end(BenchmarkSettings.defaultSeed + i));
      }
    }
  }

  private static void doParityTestWithRandomCount(BenchmarkSettings settings, int randomCount) {
    var x = settings.centerX();
    var z = settings.centerZ();
    var random = new Xoroshiro128PlusPlusRandom(x, z);
    for (int i = 0; i < randomCount; i++) {
      LOGGER.info("Testing coords: {} {}", x, z);
      doParityTest(settings.withCenter(x, z));
      x = random.nextInt(2097152) - 1048576;
      z = random.nextInt(2097152) - 1048576;
    }
  }

  private static void doParityTest(BenchmarkSettings settings) {
    var fakeWorld = new TestWorld(settings);

    var name = settings.dimensionOptions().getValue().getPath();
    LOGGER.info("Generating " + name);

    var base = ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius());
    var biomes =
        ChunkRegion.of(fakeWorld, settings.centerX(), settings.centerZ(), settings.radius() + 1);

    generateChunk(
        fakeWorld, base, biomes, MOD_LOADED ? "Modded " : "Vanilla " + name, settings.seed(), name);
    LOGGER.info(name + " matches");
  }

  private static void generateChunk(
      TestWorld world,
      ChunkRegion base,
      ChunkRegion biomeRegion,
      String name,
      long seed,
      String worldKey) {
    biomeRegion.forEach(
        (chunk) -> {
          world.biomes(biomeRegion, chunk);
        });

    LOGGER.info("Generated Biomes for " + name);

    base.copyBiomes(biomeRegion);

    base.forEach(
        (chunk) -> {
          // world.noise(base, chunk);
          // world.surface(chunk, biomeRegion);
          world.buildTerrain(chunk, biomeRegion);

          if (ParityGlobals.isServer) {
            var pos = chunk.getPos();
            var against = ParityGlobals.get(new ChunkStorageKey(seed, worldKey, pos.x(), pos.z()));
            var err = against.matches(chunk);
            if (err != null) {
              ParityGlobals.LOGGER.info(
                  "Ours: {} Theirs: {}", new ChunkStorage(seed, worldKey, chunk), against);
              throw new IllegalStateException("Chunks don't match " + pos + " " + err);
            }
          } else {
            var storage = new ChunkStorage(seed, worldKey, chunk);
            ParityGlobals.send(storage);
          }
        });

    LOGGER.info("Generated " + name);
  }
}
