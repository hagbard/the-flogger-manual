/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Copyright (c) 2023, David Beaumont (https://github.com/hagbard).

This program and the accompanying materials are made available under the terms of the
Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package net.goui.flogger.examples;

import com.google.common.flogger.FluentLogger;

/**
 * An example class which carries out dummy tasks and logs their status. This is used alongside
 * {@code TaskExampleTest} to illustrate the Flogger logs testing API.
 */
public class TaskExample {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void performSimpleTask(String taskName) {
    logger.atFine().log("Before task debug log statement");

    logger.atInfo().log("Task{type=SIMPLE} [start]: name=%s", taskName);
    logger.atFine().log("Debug log statement");
    logger.atFinest().log("Verbose log statement");
    logger.atInfo().log("Task{type=SIMPLE} [stop]: name=%s", taskName);

    logger.atFine().log("After task debug log statement");
  }
}
