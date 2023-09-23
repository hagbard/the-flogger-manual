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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.goui.flogger.testing.LevelClass;
import org.apache.logging.log4j.core.config.Configurator;

/** Programmatic setting of log level for example code. */
final class LevelConfig {
  private static final String LOGGER_NAME = ""; // LevelConfig.class.getPackageName();
  private static final ImmutableMap<String, LevelClass> MAP;

  static {
    ImmutableMap.Builder<String, LevelClass> map = ImmutableMap.builder();
    for (LevelClass level : LevelClass.values()) {
      map.put(level.name(), level);
    }
    // Aliases cannot have names derived from the enum values.
    map.put("TRACE", LevelClass.TRACE);
    map.put("DEBUG", LevelClass.DEBUG);
    map.put("ERROR", LevelClass.ERROR);
    MAP = map.buildOrThrow();
  }

  public static void setLogLevel(String levelName) {
    LevelClass level = getLevel(levelName);
    setJdkLogLevel(level);
    setLog4JLogLevel(level);
  }

  private static LevelClass getLevel(String name) {
    return checkNotNull(MAP.get(name), "No such log level; valid values are: %s", MAP.keySet());
  }

  /**
   * Set the log level based on a given log level name.
   *
   * <p>Since this example is built with the JDK system backend, we control the log level via the
   * JDK level.
   */
  private static void setJdkLogLevel(LevelClass level) {
    // With JDK logging it's not necessary to set all intermediate loggers, since child loggers
    // will inherit the parent's setting unless it's explicitly set (and we only set *handler*
    // log levels normally for hooking up tests).
    Level jdkLogLevel = level.toJdkLogLevel();
    Logger rootLogger = Logger.getLogger("");
    // Must reset existing handlers (e.g. console handler) and the logger's own level.
    stream(rootLogger.getHandlers()).forEach(h -> h.setLevel(jdkLogLevel));
    rootLogger.setLevel(jdkLogLevel);
  }

  private static void setLog4JLogLevel(LevelClass level) {
    // For testing we want to output the logs from the root logger at the given level, but also
    // change any child loggers (e.g. those already used for FluentLogger instances) to also be at
    // that level. This is non-trivial, but luckily there's a utility class to do it for us.
    Configurator.setAllLevels("", toLog4JLevel(level));
  }

  private static org.apache.logging.log4j.Level toLog4JLevel(LevelClass level) {
    switch (level) {
      case FINEST:
        return org.apache.logging.log4j.Level.TRACE;
      case FINE:
        return org.apache.logging.log4j.Level.DEBUG;
      case INFO:
        return org.apache.logging.log4j.Level.INFO;
      case WARNING:
        return org.apache.logging.log4j.Level.WARN;
      case SEVERE:
        return org.apache.logging.log4j.Level.ERROR;
      default:
        throw new AssertionError();
    }
  }

  private LevelConfig() {}
}
