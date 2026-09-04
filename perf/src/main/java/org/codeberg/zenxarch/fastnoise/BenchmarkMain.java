package org.codeberg.zenxarch.fastnoise;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Util;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.ForkedRunnerAccessor;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkMain {
  public static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkMain.class);

  private static Optional<Boolean> getBooleanProperty(String name) {
    var value = System.getProperty(name);
    if (value == null) return Optional.empty();
    try {
      return Optional.of(Boolean.parseBoolean(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static void runTest(String[] args) {
    var zmod = getBooleanProperty("zmod");
    if (zmod.isPresent()) {
      runBenchmark("Benchmark ", zmod.get());
    } else {
      var a = runBenchmark("Benchmark", false);
      var b = runBenchmark("Benchmark", true);
      ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out).writeOut(a);
      ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out).writeOut(b);
    }
  }

  public static boolean isParityTest() {
    return System.getProperty("zparity") != null;
  }

  public static boolean isForked() {
    return System.getProperty("zforked") != null;
  }

  public static void runForked(String[] args) {
    try {
      ForkedRunnerAccessor.main(args);
    } catch (Exception e) {
      LOGGER.error("Huh?", e);
    }
    Util.shutdownExecutors();
  }

  public static void runParityTest() {
    ParityTest.doParityTest();
    Util.shutdownExecutors();
  }

  private static int getIntProperty(String name, int def, int min) {
    var value = System.getProperty(name);
    if (value == null) return def;
    try {
      return Math.max(Integer.parseInt(value), min);
    } catch (Exception e) {
      return def;
    }
  }

  private static Mode getMode() {
    try {
      return Mode.deepValueOf(System.getProperty("zbenchmode"));
    } catch (Exception e) {
      return Mode.AverageTime;
    }
  }

  private static Collection<RunResult> runBenchmark(String outputPrefix, boolean zmod) {
    ChainedOptionsBuilder options =
        new OptionsBuilder()
            .forks(getIntProperty("zforks", 1, 1))
            .jvmArgsAppend("-Dzforked=true", "-Dzmixin=" + zmod)
            .mode(getMode())
            .warmupTime(TimeValue.seconds(getIntProperty("zwarmuptime", 5, 1)))
            .measurementTime(TimeValue.seconds(getIntProperty("zmeasuretime", 5, 1)))
            .warmupIterations(getIntProperty("zwarmups", 5, 1))
            .measurementIterations(getIntProperty("zmeasures", 5, 1))
            .timeUnit(TimeUnit.MILLISECONDS)
            .threads(getIntProperty("zthreads", 1, 1));

    var benchmarkName = "Vanilla";

    if (zmod) benchmarkName = "Modded";

    if (System.getProperty("zperfbenchmark") != null) {
      var benchRegex = System.getProperty("zperfbenchmark");
      options = options.include(benchRegex);
      benchmarkName += " ";
      benchmarkName += benchRegex;
    }

    if (System.getProperty("zworldname") != null) {
      var worldName = System.getProperty("zworldname");
      options = options.param("worldName", worldName.split(","));
      benchmarkName += " ";
      benchmarkName += worldName;
    }

    options =
        options.result(outputPrefix + benchmarkName + ".txt").resultFormat(ResultFormatType.TEXT);

    if (System.getProperty("zuseasync") != null) {
      options =
          options.addProfiler(
              "async",
              "libPath=" + System.getProperty("zuseasync") + ";sig=true;output=flamegraph");
    }

    options = options.shouldDoGC(true);

    var runner = new Runner(options.build());
    try {
      return runner.run();
    } catch (RunnerException exception) {
      LOGGER.info("Cannot run jmh: {}", exception.getMessage());
      throw new IllegalStateException("Cannot run jmh {}", exception);
    }
  }
}
