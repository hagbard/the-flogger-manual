/*******************************************************************************
 * Copyright (c) 2023, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.examples;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.flogger.StackSize.SMALL;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.flogger.FluentLogger;
import java.time.Duration;
import java.util.function.BiConsumer;

/**
 * Basic examples of logging with {@link FluentLogger}.
 *
 * <p>For JDK logging, run with system property:
 *
 * <pre>{@code
 * -Dflogger.backend_factory=com.google.common.flogger.backend.system.SimpleBackendFactory
 * }</pre>
 */
public final class BasicExamples {
  /**
   * By declaring a static {@code FluentLogger} instance first in a class, it can always be used in
   * any code, including static class initialization.
   */
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static {
    // This is only necessary for the demo, where logging configuration is applied programmatically.
    // In normal use logging should be configured via external configuration and handled by the
    // underlying logging system during its initialization. This call is used to ensure that logging
    // works in this demo, even when no other logger configuration is present.
    LevelConfig.setLogLevel("INFO");

    // Even before any application code is run, a FluentLogger instance is usable.
    // -------------------------------------------------------------------------------------------
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     A static FluentLogger instance can be used anywhere in a class.
    // -------------------------------------------------------------------------------------------
    logger.atInfo().log("A static FluentLogger instance can be used anywhere in a class.");
  }

  public static void main(String[] args) {
    LevelConfig.setLogLevel(args.length > 0 ? args[0] : "FINE");
    basicExample();
    rateLimitExample(Duration.ofNanos(10000));
  }

  /** The simplest examples of using Flogger for debug logging. */
  static void basicExample() {
    // The most basic log statements. Use the fluent API to control the log level.
    // -------------------------------------------------------------------------------------------
    // [main] INFO net.goui.flogger.examples.BasicExamples - Hello World
    // [main] WARN net.goui.flogger.examples.BasicExamples - This is a warning!
    // -------------------------------------------------------------------------------------------
    logger.atInfo().log("Hello World");
    logger.atWarning().log("This is a warning!");
    logger.atFine().log("This log statement is disabled by default.");

    // Like most logging APIs, you can pass in an exception to be logged.
    // -------------------------------------------------------------------------------------------
    // [main] ERROR net.goui.flogger.examples.BasicExamples - Logging an error!
    // java.lang.IllegalArgumentException: null
    //	at com.google.common.base.Preconditions.checkArgument(Preconditions.java:131)
    //	at net.goui.flogger.examples.BasicExamples.fibonacciSampled(BasicExamples.java:123)
    //	at net.goui.flogger.examples.BasicExamples.basicExample(BasicExamples.java:79)
    //	at net.goui.flogger.examples.BasicExamples.main(BasicExamples.java:51)
    // -------------------------------------------------------------------------------------------
    try {
      Integer.parseInt("<not an integer>");
    } catch (NumberFormatException e) {
      logger.atSevere().withCause(e).log("Logging an error!");
    }

    // But if all you want is more stack information, you can just ask for it directly.
    // -------------------------------------------------------------------------------------------
    // [main] INFO net.goui.flogger.examples.BasicExamples - Logging with some caller context!
    // com.google.common.flogger.LogSiteStackTrace: SMALL
    //	at net.goui.flogger.examples.BasicExamples.basicExample(BasicExamples.java:92)
    //	at net.goui.flogger.examples.BasicExamples.main(BasicExamples.java:51)
    // -------------------------------------------------------------------------------------------
    logger.atInfo().withStackTrace(SMALL).log("Logging with some caller context!");
  }

  // Fibonacci(16) ~= 1000 and the naive algorithm takes ~1,800 steps.
  static void rateLimitExample(Duration pausePerStep) {
    // Note the additional context to remind you that logging is being sampled.
    // -------------------------------------------------------------------------------------------
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Sampled]: fib(2) = 1 [CONTEXT ratelimit_count=1000 ]
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Sampled]: fib(2) = 1 [CONTEXT ratelimit_count=1000 ]
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Sampled logging] = 987
    // -------------------------------------------------------------------------------------------
    Fibonacci sampledFibonacci =
        new Fibonacci(
            pausePerStep,
            (n, value) ->
                logger.atFine().every(1000).log("Fibonacci [Sampled]: fib(%d) = %d", n, value));
    logger.atInfo().log("Fibonacci [Sampled logging] = %d", sampledFibonacci.calculate(16));

    // When using rate limiting, the number of skipped log statements is tracked (this can be very
    // useful when debugging if the logging rate is very variable).
    // -------------------------------------------------------------------------------------------
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Rate Limited]: fib(2) = 1 [CONTEXT ratelimit_period="500 MILLISECONDS" ]
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Rate Limited]: fib(8) = 21 [CONTEXT ratelimit_period="500 MILLISECONDS
    // [skipped: 216]" ]
    // ...
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Rate Limited]: fib(2) = 1 [CONTEXT ratelimit_period="500 MILLISECONDS
    // [skipped: 32]" ]
    // [main] INFO net.goui.flogger.examples.BasicExamples
    //     Fibonacci [Rate limited logging] = 987
    // -------------------------------------------------------------------------------------------
    Fibonacci rateLimitedFibonacci =
        new Fibonacci(
            pausePerStep,
            (n, value) ->
                logger.atFine().atMostEvery(2, SECONDS).log(
                    "Fibonacci [Rate Limited]: fib(%d) = %d", n, value));
    logger.atInfo().log(
        "Fibonacci [Rate limited logging] = %d", rateLimitedFibonacci.calculate(16));
  }

  private static class Fibonacci {
    private final Duration pausePerStep;
    private final BiConsumer<Long, Long> logFn;

    public Fibonacci(Duration pausePerStep, BiConsumer<Long, Long> logFn) {
      this.pausePerStep = pausePerStep;
      this.logFn = logFn;
      // Don't let anyone set a ridiculous pause time.
      checkArgument(
          !pausePerStep.isNegative() && pausePerStep.compareTo(Duration.ofMillis(1)) <= 0);
    }

    public long calculate(long n) {
      checkArgument(n > 0);
      long value = (n <= 2) ? 1 : calculate(n - 1) + calculate(n - 2);
      pause();
      logFn.accept(n, value);
      return value;
    }

    /** A small pause to simulate work and allow easier demonstration of rate limiting. */
    private void pause() {
      if (!pausePerStep.isZero()) {
        try {
          Thread.sleep(0, pausePerStep.toNanosPart());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private BasicExamples() {}
}
