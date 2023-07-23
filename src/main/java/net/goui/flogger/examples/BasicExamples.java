package net.goui.flogger.examples;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.flogger.StackSize.SMALL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.flogger.FluentLogger;

/**
 * Basic examples of logging with {@link FluentLogger}.
 *
 * <p>For JDK logging, run with system property:
 * <pre>{@code
 * -Dflogger.backend_factory=com.google.common.flogger.backend.system.SimpleBackendFactory
 * }</pre>
 */
public class BasicExamples {
  /**
   * By declaring a static {@code FluentLogger} instance first in a class, it can always be used in
   * any code, including static class initialization.
   */
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  static {
    // This is only necessary for the demo, where logging configuration is applied programmatically.
    // In normal use logging should be configured via external configuration and handled by the
    // underlying logging system during its initialization. This class is used to ensure that
    // logging works in this demo even when no other logger configuration is present.
    SharedLogLevel.INFO.apply();

    // Even before any application code is run, a FluentLogger instance is usable.
    logger.atInfo().log("A static FluentLogger instance can be used anywhere in a class.");
  }

  public static void main(String[] args) {
    SharedLogLevel.parse(args.length > 0 ? args[0] : "INFO").apply();
    basicExample();
    rateLimitExample();
  }

  /** The simplest examples of using Flogger for debug logging. */
  private static void basicExample() {
    // The most basic log statements. Use the fluent API to control the log level.
    // -------------------------------------------------------------------------------------------
    // 15:59:40.804 [main] INFO  net.goui.flogger.examples.HelloWorld
    //     Hello World
    // 15:59:40.819 [main] WARN  net.goui.flogger.examples.HelloWorld
    //     This is a warning!
    // 15:59:40.819 [main] WARN  net.goui.flogger.examples.HelloWorld
    //     Logging an error!
    // -------------------------------------------------------------------------------------------
    logger.atInfo().log("Hello World");
    logger.atWarning().log("This is a warning!");
    logger.atFine().log("This log statement is disabled by default.");

    // Like most logging APIs, you can pass in an exception to be logged.
    // -------------------------------------------------------------------------------------------
    // 15:59:40.819 [main] WARN  net.goui.flogger.examples.HelloWorld
    //     Logging an error!
    //   java.lang.IllegalArgumentException: null
    //	   at com.google.common.base.Preconditions.checkArgument(Preconditions.java:131)
    //     at net.goui.flogger.examples.HelloWorld.fibonacciSampled(HelloWorld.java:78)
    //     at net.goui.flogger.examples.HelloWorld.doBasicLogging(HelloWorld.java:43)
    //     at net.goui.flogger.examples.HelloWorld.main(HelloWorld.java:15)
    // -------------------------------------------------------------------------------------------
    try {
      fibonacciSampled(-1);
    } catch (IllegalArgumentException e) {
      logger.atWarning().withCause(e).log("Logging an error!");
    }

    // But if all you want is more stack information, you can just ask for it directly.
    // -------------------------------------------------------------------------------------------
    // 15:59:40.832 [main] INFO  net.goui.flogger.examples.HelloWorld
    //     Logging with some caller context!
    //   com.google.common.flogger.LogSiteStackTrace: SMALL
    //     at net.goui.flogger.examples.HelloWorld.doBasicLogging(HelloWorld.java:56)
    //     at net.goui.flogger.examples.HelloWorld.main(HelloWorld.java:15)
    // -------------------------------------------------------------------------------------------
    logger.atInfo().withStackTrace(SMALL).log("Logging with some caller context!");
  }

  // Fibonacci(16) ~= 1000 and the naive algorithm takes >1,000 steps.
  private static void rateLimitExample() {
    // Note the additional context to remind you that logging is being sampled.
    // -------------------------------------------------------------------------------------------
    // 15:59:42.956 [main] INFO  net.goui.flogger.examples.HelloWorld
    //     Fibonacci: fib(2) = 1 [CONTEXT ratelimit_count=1000 ]
    // -------------------------------------------------------------------------------------------
    logger.atInfo().log("Fibonacci [Sampled logging] = %d", fibonacciSampled(16));

    // When using rate limiting, the number of skipped log statements is tracked (this can be very
    // useful when debugging if the logging rate is very variable).
    // -------------------------------------------------------------------------------------------
    // 15:59:45.955 [main] INFO  net.goui.flogger.examples.HelloWorld
    //     Fibonacci: fib(4) = 3 [CONTEXT ratelimit_period="500 MILLISECONDS [skipped: 245]" ]
    // -------------------------------------------------------------------------------------------
    logger.atInfo().log("Fibonacci [Rate limited logging] = %d", fibonacciRateLimited(16));
  }

  private static long fibonacciSampled(long n) {
    checkArgument(n > 0);
    long value = (n <= 2) ? 1 : fibonacciSampled(n - 1) + fibonacciSampled(n - 2);
    pause();
    // Sample only 1-in-1000 log messages.
    logger.atInfo().every(1000).log("Fibonacci: fib(%d) = %d", n, value);
    return value;
  }

  private static long fibonacciRateLimited(long n) {
    checkArgument(n > 0);
    long value = (n <= 2) ? 1 : fibonacciRateLimited(n - 1) + fibonacciRateLimited(n - 2);
    pause();
    // Rate limit logging to at most once every 1/2 second.
    logger.atInfo().atMostEvery(500, MILLISECONDS).log("Fibonacci: fib(%d) = %d", n, value);
    return value;
  }

  /** A small pause to simulate work and allow easier demonstration of rate limiting. */
  private static void pause() {
    try {
      Thread.sleep(0, 100000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
