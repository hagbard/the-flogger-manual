/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Copyright (c) 2023, David Beaumont (https://github.com/hagbard).

This program and the accompanying materials are made available under the terms of the
Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package net.goui.flogger.examples;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.context.ScopedLoggingContexts;
import com.google.common.flogger.context.Tags;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * An example class which carries out dummy tasks and logs their status. This is used alongside
 * {@code TaskExampleTest} to illustrate the Flogger logs testing API.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MultiThreadedExample {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void runParallelTasks(int taskCount, int steps)
      throws InterruptedException, ExecutionException {
    ListeningExecutorService service =
        GrpcPropagatingExecutorService.wrap(Executors.newFixedThreadPool(taskCount));
    try {
      Supplier<Duration> randomDuration = getDurationSupplier();
      ImmutableList<ListenableFuture<Boolean>> futures =
          IntStream.rangeClosed(1, taskCount)
              .mapToObj(n -> new TestTask("task" + n, randomDuration.get(), steps))
              .map(service::submit)
              .collect(toImmutableList());
      Boolean allSuccess =
          Futures.whenAllSucceed(futures)
              .call(() -> futures.stream().allMatch(Futures::getUnchecked), service)
              .get();
      logger.atInfo().log("Parallel Tasks Complete (success=%s)", allSuccess);
    } finally {
      service.shutdown();
    }
  }

  private static Supplier<Duration> getDurationSupplier() {
    Random rnd = new Random();
    return () -> Duration.ofMillis((long) (1000 * (Math.abs(rnd.nextGaussian()) + 1)));
  }

  private static class TestTask implements Callable<Boolean> {
    private final String name;
    private final Duration duration;
    private final int steps;

    TestTask(String name, Duration duration, int steps) {
      this.name = name;
      this.duration = duration;
      this.steps = steps;
    }

    @Override
    public Boolean call() {
      try (var ctx = ScopedLoggingContexts.newContext().withTags(Tags.of("task", name)).install()) {
        logger.atInfo().log("Task: START (duration=%sms, steps=%d)", duration.toMillis(), steps);
        Instant endTime = Instant.now().plus(duration);
        for (int n = 0; n < steps; n++) {
          logger.atFine().log("Task: Step %d/%d", n + 1, steps);
          if (!pause(Duration.between(Instant.now(), endTime).dividedBy(steps - n))) {
            return false;
          }
        }
        logger.atInfo().log("Task: END");
      }
      return true;
    }

    // Note how this helper method does not have to be passed the task name.
    // Since it's run inside the task's logging context, it gets the logging metadata "for free".
    private static boolean pause(Duration delta) {
      if (delta.toMillis() <= 0) {
        return true;
      }
      try {
        logger.atFinest().log("Task: Sleep (duration=%dms)", delta.toMillis());
        Thread.sleep(delta.toMillis());
        logger.atFinest().log("Task: Awake");
        return true;
      } catch (InterruptedException e) {
        logger.atWarning().withCause(e).log("Task: Interrupted");
        return false;
      }
    }
  }

  private MultiThreadedExample() {}
}
