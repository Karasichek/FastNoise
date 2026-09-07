package org.codeberg.zenxarch.perf.benchmarks;

import org.codeberg.zenxarch.perf.BenchmarkSettings;
import org.codeberg.zenxarch.perf.ChunkRegion;
import org.codeberg.zenxarch.perf.TestWorld;
import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
public class BiomesBenchmark {
  private TestWorld world;
  private ChunkRegion chunks;

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
  }

  @TearDown(Level.Invocation)
  public void clearChunk() {
    this.chunks.forEach(TestWorld::resetBiomes);
  }

  @Benchmark
  public void biomegen() {
    this.chunks.forEach(chunk -> world.biomes(chunks, chunk));
  }
}
