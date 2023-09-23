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

import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.testing.LevelClass.FINE;
import static net.goui.flogger.testing.LevelClass.FINEST;
import static net.goui.flogger.testing.LevelClass.INFO;
import static net.goui.flogger.testing.SetLogLevel.Scope.CLASS_UNDER_TEST;
import static net.goui.flogger.testing.truth.LogMatcher.after;
import static net.goui.flogger.testing.truth.LogMatcher.before;
import static net.goui.flogger.testing.truth.LogMatcher.fromSameMethodAs;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import net.goui.flogger.testing.LogEntry;
import net.goui.flogger.testing.SetLogLevel;
import net.goui.flogger.testing.junit4.FloggerTestRule;
import net.goui.flogger.testing.truth.LogsSubject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MultiThreadedExampleTest {
  @Rule public final FloggerTestRule logs = FloggerTestRule.forClassUnderTest(INFO);

  private static final int TEST_TASKS = 8;
  private static final int TASK_STEPS = 10;
  private static final String TASK_TAG = "task";

  @Test
  @SetLogLevel(scope = CLASS_UNDER_TEST, level = FINEST)
  public void testParallelTasks() throws InterruptedException, ExecutionException {
    // Runs a parallel task which spawns multiple threads to complete the "work".
    MultiThreadedExample.runParallelTasks(TEST_TASKS, TASK_STEPS);

    // First ensure we saw the expected number of task starts being logged.
    LogsSubject assertStartLogs =
        logs.assertLogs().withLevel(INFO).withMessageContaining("Task", "START");
    assertStartLogs.matchCount().isEqualTo(TEST_TASKS);
    // An example of how you can always dig into the matched log entries if you want to.
    // It's a slightly brittle test because there's no guarantee that other code didn't
    // also add a tag with the label "task". However, in a unit test it's likely fine.
    long distinctTaskNameCount =
        assertStartLogs.getAllMatches().stream()
            .map(MultiThreadedExampleTest::getExpectedTaskName)
            .distinct()
            .count();
    // We don't care exactly what the task names are, but they exist and are all different.
    assertThat(distinctTaskNameCount).isEqualTo(TEST_TASKS);

    // Then find the start log for a specific (arbitrary) task ...
    String taskName = randomTaskName(TEST_TASKS);
    LogEntry taskStart = assertStartLogs.withMetadata(TASK_TAG, taskName).getOnlyMatch();
    // ... and assert nothing was logged (from the task) before it.
    logs.assertLogs(before(taskStart).inSameThread()).doNotOccur();

    // Then use the start log to find all the logs for that task ...
    LogsSubject assertTaskLogs = logs.assertLogs(after(taskStart).inSameThread());
    // ... and assert that all logs have the expected metadata.
    assertTaskLogs.always().haveMetadata(TASK_TAG, taskName);
    // ... and that there were no more than TASK_STEPS steps taken.
    assertTaskLogs
        .withLevel(FINE)
        .withMessageMatching("Step \\d+/10")
        .matchCount()
        .isAtMost(TASK_STEPS);
    // ... and that we see finest logs which show the sleep duration.
    assertTaskLogs
        .withLevel(FINEST)
        .withMessageContaining("Task", "Sleep")
        .always()
        .haveMessageMatching("\\(duration=\\d+ms\\)");

    // An example of restricting logs by the method they appear in.
    //
    // Even though FINEST logs appear in the "pause()" method, they do no appear in "run()". This
    // isn't a hugely useful assertion really, and it's a bit brittle, but it might represent some
    // policy of splitting code so that verbose logging doesn't occur at higher levels.
    assertTaskLogs.matching(fromSameMethodAs(taskStart)).withLevelLessThan(FINE).doNotOccur();

    // From the set of task logs, find the expected last entry, and verify nothing came after it.
    LogEntry taskEnd = assertTaskLogs.withMessageContaining("Task", "END").getOnlyMatch();
    logs.assertLogs(after(taskEnd).inSameThread()).doNotOccur();
  }

  private static String randomTaskName(int maxTasks) {
    return "task" + (1 + ThreadLocalRandom.current().nextInt(maxTasks));
  }

  private static String getExpectedTaskName(LogEntry entry) {
    ImmutableList<Object> values = entry.metadata().get(TASK_TAG);
    assertThat(values).isNotNull();
    assertThat(values.size()).isEqualTo(1);
    assertThat(values.get(0)).isInstanceOf(String.class);
    return (String) values.get(0);
  }
}
