package org.codeberg.zenxarch.perf.benchmarks;

import org.codeberg.zenxarch.perf.BenchmarkSettings;
import org.codeberg.zenxarch.perf.ChunkRegion;
import org.codeberg.zenxarch.perf.TestWorld;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Thread)
public class TerrainBenchmark {
  private TestWorld world;
  private ChunkRegion chunks;
  private ChunkRegion biomeSource;

  @Param(value = {"overworld", "nether", "end"})
  private String worldName;

  @Setup(Level.Trial)
  public void init() {
    var settings =
        switch (worldName) {
          case "overworld" -> BenchmarkSettings.overworld();
          case "nether" -> BenchmarkSettings.nether();
          default -> BenchmarkSettings.end();
        };
    world = new TestWorld(settings);

    this.chunks = ChunkRegion.of(world, settings.centerX(), settings.centerZ(), settings.radius());
    this.biomeSource =
        ChunkRegion.of(world, settings.centerX(), settings.centerZ(), settings.radius() + 1);

    biomeSource.forEach(
        chunk -> {
          world.biomes(biomeSource, chunk);
        });

    chunks.copyBiomes(biomeSource);
  }

  @TearDown(Level.Invocation)
  public void clearChunk() {
    chunks.forEach(TestWorld::resetNoise);
  }

  @Benchmark
  public void terrain() {
    chunks.forEach(chunk -> world.buildTerrain(chunk, biomeSource));
  }
}
