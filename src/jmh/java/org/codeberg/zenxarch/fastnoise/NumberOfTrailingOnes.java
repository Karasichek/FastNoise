package org.codeberg.zenxarch.fastnoise;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OperationsPerInvocation(2048) // L1 cache is 32kb so 16kb data array
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NumberOfTrailingOnes {
  Random random = new Random(0);
  private long[] data = new long[2048];

  @Setup(Level.Invocation)
  public void setup() {
    for (int i = 0; i < data.length; i++) data[i] = random.nextLong();
  }

  @Benchmark
  public void bitCount(Blackhole hole) throws Throwable {
    var value = 0x0;
    for (int i = 0; i < 2048; i++) {
      value ^= bitCount(data[i]);
    }
    hole.consume(value);
  }

  @Benchmark
  public void simple(Blackhole hole) {
    var value = 0x0;
    for (int i = 0; i < 2048; i++) {
      value ^= simple(data[i]);
    }
    hole.consume(value);
  }

  private int bitCount(long value) {
    return Long.bitCount(value & (-2 - value));
  }

  private int simple(long value) {
    return Long.numberOfTrailingZeros(~value);
  }
}
