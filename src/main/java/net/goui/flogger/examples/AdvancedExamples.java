package net.goui.flogger.examples;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.context.LogLevelMap;
import com.google.common.flogger.context.ScopedLoggingContexts;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Examples of using metadata and contexts with {@link FluentLogger}.
 *
 * <p>For JDK logging, run with system property:
 *
 * <pre>{@code
 * -Dflogger.backend_factory=com.google.common.flogger.backend.system.SimpleBackendFactory
 * }</pre>
 */
public class AdvancedExamples {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void main(String[] args) {
    SharedLogLevel.parse(args.length > 0 ? args[0] : "INFO").apply();
    metadataExample();
    metadataWithContextExample();
  }

  /**
   * The simplest form of metadata is via a single-valued key. The associated value is added to the
   * logging context.
   */
  private static final MetadataKey<String> SINGLE_LABEL =
      MetadataKey.single("single", String.class);

  /**
   * Metadata keys can also be repeated; they collect values in order. By default, these are emitted
   * as multiple key-value pairs.
   */
  private static final MetadataKey<String> REPEATED_LABEL =
      MetadataKey.repeated("repeated", String.class);

  /**
   * A custom metadata key can process its values.
   *
   * <p>Note: Some restrictions apply to how values can be processed and care must be taken to avoid
   * doing "work" since processing may happen very frequently.
   */
  private static final MetadataKey<String> CUSTOM_LABEL =
      new MetadataKey<>("custom", String.class, /* canRepeat= */ true) {
        @Override
        protected void emitRepeated(Iterator<String> values, KeyValueHandler kvh) {
          StringBuilder joined = new StringBuilder();
          values.forEachRemaining(v -> joined.append(v).append('/'));
          if (joined.length() > 0) {
            joined.setLength(joined.length() - 1);
          }
          kvh.handle(getLabel(), joined.toString());
        }
      };

  /**
   * A boolean metadata key can be used as a purely procedural key, pulling its current value from
   * the system environment.
   *
   * <p>Note: In this simple example the value is not cached, but it properly should be. A more
   * complex example might sample underlying system data with some minimum rate period to avoid a
   * lot of repeated work.
   */
  private static final MetadataKey<Boolean> USER_HOME =
      new MetadataKey<>("home", Boolean.class, /* canRepeat= */ false) {
        @Override
        protected void emit(Boolean value, KeyValueHandler kvh) {
          if (value) {
            String user = System.getProperty("user.home");
            kvh.handle(getLabel(), user != null ? user : "<unknown>");
          }
        }
      };

  /** Logging with metadata supplied at the log site. */
  private static void metadataExample() {
    // Structured metadata can be attached to log statements with metadata keys.
    // -------------------------------------------------------------------------------------------
    // 17:25:43.192 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Logging with simple metadata [CONTEXT single="foo" ]
    // -------------------------------------------------------------------------------------------
    logger.atInfo().with(SINGLE_LABEL, "foo").log("Logging with simple metadata");

    // Single valued metadata will take only the last set value.
    // -------------------------------------------------------------------------------------------
    // 17:25:43.210 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Single metadata has only one value [CONTEXT single="bar" ]
    // -------------------------------------------------------------------------------------------
    logger
        .atInfo()
        .with(SINGLE_LABEL, "foo")
        .with(SINGLE_LABEL, "bar")
        .log("Single metadata has only one value");

    // Repeated metadata collects all values the in order they were set.
    // -------------------------------------------------------------------------------------------
    // 17:25:43.210 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Repeated metadata has all values [CONTEXT repeated="foo" repeated="bar" ]
    // -------------------------------------------------------------------------------------------
    logger
        .atInfo()
        .with(REPEATED_LABEL, "foo")
        .with(REPEATED_LABEL, "bar")
        .log("Repeated metadata has all values");

    // Custom metadata can process the collected values for formatting.
    // -------------------------------------------------------------------------------------------
    // 17:32:08.794 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Custom metadata can process values [CONTEXT custom="foo/bar" ]
    // -------------------------------------------------------------------------------------------
    logger
        .atInfo()
        .with(CUSTOM_LABEL, "foo")
        .with(CUSTOM_LABEL, "bar")
        .log("Custom metadata can process values");

    // Custom metadata can be procedural, or a function of the collected values.
    // -------------------------------------------------------------------------------------------
    // 18:03:35.031 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Custom metadata can be procedural [CONTEXT home="C:\\Users\\David" ]
    // -------------------------------------------------------------------------------------------
    logger.atInfo().with(USER_HOME).log("Custom metadata can be procedural");
  }

  private static void subTaskWithLogging() {
    logger.atInfo().log("Starting subtask");
    // Pretend this is actually doing something.
    logger.atFine().with(CUSTOM_LABEL, "bar").log("Detailed logging within a task");
    logger.atInfo().log("Ending subtask");
  }

  /**
   * Metadata given only at the log site is not much more useful than logged arguments, but metadata
   * applied to contexts becomes very powerful.
   */
  private static void metadataWithContextExample() {
    // Metadata added to a context is propagated to all log statements in the context.
    // -------------------------------------------------------------------------------------------
    // 18:34:57.431 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Starting subtask [CONTEXT single="context" ]
    // 18:34:57.431 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Ending subtask [CONTEXT single="context" ]
    // -------------------------------------------------------------------------------------------
    ScopedLoggingContexts.newContext()
        .withMetadata(SINGLE_LABEL, "context")
        .run(AdvancedExamples::subTaskWithLogging);

    // This could also be a static constant.
    LogLevelMap fineLogging = LogLevelMap.create(Level.FINE);

    // Contexts can also be used to enable additional logging within a task. Metadata given at log
    // sites is merged with metadata from the context (outermost context first, log site last).
    // This can be done conditionally (e.g. to enable more logging for a specific server request).
    // -------------------------------------------------------------------------------------------
    // 18:50:34.681 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Starting subtask [CONTEXT custom="foo" forced=true ]
    // 18:50:34.681 [main] DEBUG net.goui.flogger.examples.MetadataExamples
    //     Detailed logging within a task [CONTEXT custom="foo/bar" forced=true ]
    // 18:50:34.681 [main] INFO  net.goui.flogger.examples.MetadataExamples
    //     Ending subtask [CONTEXT custom="foo" forced=true ]
    // -------------------------------------------------------------------------------------------
    ScopedLoggingContexts.newContext()
        .withLogLevelMap(fineLogging)
        .withMetadata(CUSTOM_LABEL, "foo")
        .run(AdvancedExamples::subTaskWithLogging);
  }
}
