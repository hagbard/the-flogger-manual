/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Copyright (c) 2023, David Beaumont (https://github.com/hagbard).

This program and the accompanying materials are made available under the terms of the
Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package net.goui.flogger.examples;

import static net.goui.flogger.testing.LevelClass.FINE;
import static net.goui.flogger.testing.LevelClass.FINEST;
import static net.goui.flogger.testing.LevelClass.INFO;
import static net.goui.flogger.testing.truth.LogMatcher.after;
import static net.goui.flogger.testing.truth.LogMatcher.before;

import com.google.common.flogger.context.LogLevelMap;
import java.util.logging.Level;
import net.goui.flogger.testing.SetLogLevel;
import net.goui.flogger.testing.junit4.FloggerTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TaskExampleTest {
  @Rule public final FloggerTestRule logs = FloggerTestRule.forClassUnderTest(INFO);

  private static final LogLevelMap FINE_LOGGING_IN_TASK =
      LogLevelMap.builder().add(Level.FINE, TaskExample.class).build();

  @Test
  public void testSimpleTask() {
    TaskExample.performSimpleTask("foobar");

    logs.assertLogs().matchCount().isAtLeast(2);
    var assertTaskLogs =
        logs.assertLogs().withLevel(INFO).withMessageContaining("Task", "SIMPLE", "foobar");
    assertTaskLogs.matchCount().isEqualTo(2);

    var startLog = assertTaskLogs.withMessageContaining("[start]").getOnlyMatch();
    logs.assertLogs(before(startLog)).doNotOccur();
    logs.assertLogs(after(startLog)).never().haveLevel(FINE);
  }

  @Test
  @SetLogLevel(target = TaskExample.class, level = FINE)
  public void testSimpleTask_extraLogging() {
    // Run the same code as above, but this time we get FINE logs.
    TaskExample.performSimpleTask("foobar");

    logs.assertLogs().matchCount().isAtLeast(5);
    var assertTaskLogs =
        logs.assertLogs().withLevel(INFO).withMessageContaining("Task", "SIMPLE", "foobar");
    assertTaskLogs.matchCount().isEqualTo(2);

    var startLog = assertTaskLogs.withMessageContaining("[start]").getOnlyMatch();
    logs.assertLogs(before(startLog)).matchCount().isAtLeast(1);
    logs.assertLogs(after(startLog)).withLevel(FINE).matchCount().isAtLeast(2);
    logs.assertLogs().withLevel(FINEST).doNotOccur();
  }
}
