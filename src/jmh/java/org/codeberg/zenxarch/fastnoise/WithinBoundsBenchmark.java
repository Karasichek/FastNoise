package org.codeberg.zenxarch.fastnoise;

import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class WithinBoundsBenchmark {

  Random random = new Random();

  private boolean withinBoundsOpt(int i) {
    return ((0x1 << (i & 0xF)) & ~0b0011_1111_1111_1100) == 0;
  }

  @Benchmark
  public void withinBoundsOpt(Blackhole hole) {
    hole.consume(withinBoundsOpt(random.nextInt()));
  }

  private boolean withinBoundsSimple(int i) {
    var n = i & 0xF;
    return n < 2 || n > 13;
  }

  @Benchmark
  public void withinBoundsSimple(Blackhole hole) {
    hole.consume(withinBoundsSimple(random.nextInt()));
  }
}
