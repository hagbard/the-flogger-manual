/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.examples;

import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.context.ScopedLoggingContexts;
import net.goui.flogger.FluentLogger;

public class FloggerNextExamples {
  public static final class Key {
    public static final MetadataKey<Integer> TASK_ID = MetadataKey.single("task_id", Integer.class);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void main(String[] args) {
    // Attach a task ID for every log statement, which has custom formatting.
    try (var ctx = ScopedLoggingContexts.newContext().withMetadata(Key.TASK_ID, 1234).install()) {
      // Examples of the String Template syntax, with and without printf format directives.
      int a = 23, b = 19;
      logger.atInfo()."Using String Templates: \{a} + \{b} = \{a + b}".log();
      logger.atInfo()."With printf formatting: %#x\{a} + %#x\{b} = %#x\{a + b}".log();
      logger.atInfo()."""
          With multi-line formatting:
            %#x\{a}
            + %#x\{b}
            = %#x\{a + b}""".log();

      // Example of suppressing a metadata key (ignore LOG_EVERY_N key, but not AT_MOST_EVERY key).
      logger.atInfo().atMostEvery(5, MINUTES).log("With emitted rate limit metadata.");
      logger.atInfo().every(100).log("With ignored rate limit metadata.");
    }
  }
}
