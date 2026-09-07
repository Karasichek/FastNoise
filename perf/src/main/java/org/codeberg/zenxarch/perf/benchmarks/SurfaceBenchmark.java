package org.codeberg.zenxarch.perf.benchmarks;

import net.minecraft.world.chunk.ChunkStatus;
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
public class SurfaceBenchmark {
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
          world.noise(biomeSource, chunk);
        });

    chunks.copyBiomes(biomeSource);
    chunks.copyNoiseAndHeightmap(biomeSource);

    chunks.forEach(
        chunk -> {
          chunk.setStatus(ChunkStatus.NOISE);
        });
  }

  @TearDown(Level.Invocation)
  public void clearChunk() {
    chunks.copyNoiseAndHeightmap(biomeSource);
  }

  @Benchmark
  public void surface() {
    chunks.forEach(chunk -> world.surface(chunk, biomeSource));
  }
}
