/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Copyright (c) 2023, David Beaumont (https://github.com/hagbard).

This program and the accompanying materials are made available under the terms of the
Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package net.goui.flogger.examples;

import static net.goui.flogger.testing.LevelClass.FINE;
import static net.goui.flogger.testing.LevelClass.INFO;
import static net.goui.flogger.testing.LevelClass.SEVERE;
import static net.goui.flogger.testing.LevelClass.WARNING;
import static net.goui.flogger.testing.SetLogLevel.Scope.CLASS_UNDER_TEST;
import static net.goui.flogger.testing.truth.LogMatcher.before;
import static net.goui.flogger.testing.truth.LogSubject.assertThat;

import com.google.common.flogger.LogSiteStackTrace;
import java.time.Duration;
import net.goui.flogger.testing.LogEntry;
import net.goui.flogger.testing.SetLogLevel;
import net.goui.flogger.testing.junit4.FloggerTestRule;
import net.goui.flogger.testing.truth.LogsSubject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicExamplesTest {
  @Rule public final FloggerTestRule logs = FloggerTestRule.forClassUnderTest(INFO);

  @Test
  public void testBasicExample_usingAssertions() {
    BasicExamples.basicExample();

    var helloWorld = logs.assertLogs().withMessageContaining("Hello World").getOnlyMatch();
    assertThat(helloWorld).hasLevel(INFO);
    logs.assertLogs(before(helloWorld).fromSameMethod()).doNotOccur();

    logs.assertLogs().withLevel(WARNING).always().haveMessageContaining("warning");
    logs.assertLogs().withLevel(SEVERE).always().haveCause();
    logs.assertLogs()
        .withMessageContaining("caller context")
        .always()
        .haveCause(LogSiteStackTrace.class);
  }

  @Test
  public void testBasicExample_usingExpectations() {
    logs.verify(LogsSubject::doNotOccur);

    logs.expectLogs(log -> log.withMessageContaining("Hello World")).once();
    logs.expectLogs(log -> log.withLevel(WARNING).withMessageContaining("warning")).once();
    logs.expectLogs(log -> log.withMessageContaining("caller context")).once();
    logs.expectLogs(
            log ->
                log.withLevel(SEVERE)
                    .withMessageContaining("error")
                    .withCause(NumberFormatException.class))
        .once();

    // An example of why using "expectations" (vs direct assertions) can be fragile. The
    // following log statement appears in the static initialization of the BasicExamples class,
    // which may, or may not, be triggered for this test, but since we need to account for all
    // logs emitted, we must add something for it (even though it may not occur).
    // Other tests do not need to care about this log statement.
    logs.expectLogs(log -> log.withMessageContaining("FluentLogger", "instance")).atMost(1);

    BasicExamples.basicExample();
  }

  @Test
  public void testBasicExample_hybridApproach() {
    // Set up the logging policy (in a normal test this would usually be up as a static field).
    logs.verify(assertLogs -> assertLogs.withLevelAtLeast(WARNING).doNotOccur());

    // Account for warnings via expectations. They aren't the subject of this test (even though they
    // violate the logging policy) so we permit them.
    logs.expectLogs(log -> log.withLevel(WARNING).withMessageContaining("warning")).atLeast(1);

    BasicExamples.basicExample();

    // Locate the log entry we actually want to test, and make direct assertions on it.
    LogEntry severe = logs.assertLogs().withLevel(SEVERE).getOnlyMatch();
    assertThat(severe).hasMessageContaining("error");
    assertThat(severe).hasCause(NumberFormatException.class);
    logs.expect(severe);
  }

  @Test
  public void testRateLimitExample_notTestingFineLogs() {
    // Turn on fine logging, even though we are not testing/forcing it. This should show the
    // difference between output in the forced vs non-forced cases in the console. Only INFO
    // and above logs are actually forced/captured for testing and the fine logs remaining rate
    // limited.
    LevelConfig.setLogLevel("FINE");
    BasicExamples.rateLimitExample(Duration.ofNanos(1000));

    logs.assertLogs()
        .withLevel(INFO)
        .withMessageMatching("Fibonacci")
        .always()
        .haveMessageContaining("987");
    // Even though these logs should be visible in the test output, they are not captured here.
    logs.assertLogs().withLevelLessThan(INFO).doNotOccur();
  }

  @Test
  @SetLogLevel(scope = CLASS_UNDER_TEST, level = FINE)
  public void testRateLimitExample_testingFineLogs() {
    BasicExamples.rateLimitExample(Duration.ZERO);

    LogsSubject assertSampledLogs =
        logs.assertLogs().withLevel(FINE).withMessageContaining("Fibonacci", "[Sampled]");
    assertSampledLogs.always().haveMessageMatching("fib\\(\\d+\\) = \\d+");
    // Compared to the test above, we are capturing (and forcing) the fine logs, so we get a lot.
    assertSampledLogs.matchCount().isAtLeast(1800);

    LogsSubject assertRateLimitedLogs =
        logs.assertLogs().withLevel(FINE).withMessageContaining("Fibonacci", "[Rate Limited]");
    assertRateLimitedLogs.always().haveMessageMatching("fib\\(\\d+\\) = \\d+");
    assertRateLimitedLogs.matchCount().isAtLeast(1800);
  }
}
