package net.goui.flogger.examples;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.UnaryOperator.identity;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

enum SharedLogLevel {
  ERROR(Level.SEVERE, org.apache.logging.log4j.Level.ERROR),
  WARN(Level.WARNING, org.apache.logging.log4j.Level.WARN),
  INFO(Level.INFO, org.apache.logging.log4j.Level.INFO),
  DEBUG(Level.FINE, org.apache.logging.log4j.Level.DEBUG),
  ALL(Level.ALL, org.apache.logging.log4j.Level.ALL);

  private static final ImmutableMap<String, SharedLogLevel> MAP =
      Arrays.stream(SharedLogLevel.values())
          .collect(toImmutableMap(SharedLogLevel::name, identity()));

  static SharedLogLevel parse(String name) {
    return checkNotNull(MAP.get(name), "No such log level; valid values are: %s", MAP.keySet());
  }

  final Level jdkLevel;
  final org.apache.logging.log4j.Level log4jLevel;

  SharedLogLevel(Level jdkLevel, org.apache.logging.log4j.Level log4jLevel) {
    this.jdkLevel = jdkLevel;
    this.log4jLevel = log4jLevel;
  }

  void apply() {
    setJdkLogLevel(jdkLevel);
    setLog4JLogLevel(log4jLevel);
  }

  /**
   * Set the log level based on a given log level name.
   *
   * <p>Since this example is built with the JDK system backend, we control the log level via the
   * JDK level.
   */
  private static void setJdkLogLevel(Level jdkLevel) {
    Arrays.stream(Logger.getLogger("").getHandlers()).forEach(h -> h.setLevel(jdkLevel));
    Logger.getLogger("net.goui.flogger.examples").setLevel(jdkLevel);
  }

  private static void setLog4JLogLevel(org.apache.logging.log4j.Level log4jLevel) {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    loggerConfig.setLevel(log4jLevel);
    ctx.updateLoggers();
  }
}
