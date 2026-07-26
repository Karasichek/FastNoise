package org.codeberg.zenxarch.fastnoise.benchmarks;

import net.minecraft.world.chunk.ChunkStatus;
import org.codeberg.zenxarch.fastnoise.BenchmarkSettings;
import org.codeberg.zenxarch.fastnoise.ChunkRegion;
import org.codeberg.zenxarch.fastnoise.TestWorld;
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

    biomeSource
        .storage()
        .forEach(
            chunk -> {
              world.biomes(chunk);
              world.noise(chunk);
              chunk.setStatus(ChunkStatus.NOISE);
            });

    chunks.copyBiomes(biomeSource);
    chunks.copyNoiseAndHeightmap(biomeSource);

    chunks
        .storage()
        .forEach(
            chunk -> {
              chunk.getOrCreateChunkNoiseSampler(world::createSampler);
              chunk.setStatus(ChunkStatus.NOISE);
            });
  }

  @TearDown(Level.Invocation)
  public void clearChunk() {
    chunks.copyNoiseAndHeightmap(biomeSource);
  }

  @Benchmark
  public void surface() {
    chunks.storage().forEach(chunk -> world.surface(chunk, biomeSource));
  }
}
