---
layout: page
title: Basic Usage
nav_order: 20
---

<!-- @formatter:off -->
# Basic Usage
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
<!-- @formatter:on -->

For working code examples for basic usage,
see [BasicExamples.java]({{site.examples}}/BasicExamples.java).

## Hello World {#hello-world}

The simplest use of Flogger's [`LoggingApi`]({{site.LoggingApi}}) is to log a literal message, at a
specified log level.

<!-- @formatter:off -->
```java
logger.atInfo().log("Hello World");
```
<!-- @formatter:on -->

Or, with a printf formatted message and argument:

<!-- @formatter:off -->
```java
logger.atInfo().log("Hello %s World", argument);
```
<!-- @formatter:on -->

{: .note }
> The format syntax for [`FluentLogger`]({{site.FluentLogger}}) is 100% compatible with Java's "printf" syntax, but other
> placeholder syntax is possible in custom loggers.

Additionally, if you have a printf format message and argument array, you can call the
[`logVarargs()`]({{site.LoggingApi}}#logVarargs(java.lang.String,java.lang.Object[])) method to
format this directly. These API decisions are discussed in depth in
[Anatomy of an API](https://google.github.io/flogger/anatomy).

As well as supplying a formatted message, almost all Java logging APIs allow a "cause" (`Throwable`
instance) to be associated with a log statement.

With Flogger this is achieved via
the [`withCause(...)`]({{site.LoggingApi}}#withCause(java.lang.Throwable)) method and is the first
example of a fluent method in the API:

<!-- @formatter:off -->
```java
logger.atInfo().withCause(exception).log("Bad things happened!");
```
<!-- @formatter:on -->

## Rate Limiting {#rate-limiting}

One of the most common built-in features in Flogger are the various rate-limiting methods. These
allow users to easily apply efficient rate limiting to log statements without the need for
additional dependencies or helper objects. Simply specify the rate limit via one of the rate
limiting methods (
e.g. [`atMostEvery()`]({{site.LoggingApi}}#atMostEvery(int,java.util.concurrent.TimeUnit))
or [`every()`]({{site.LoggingApi}}#every(int))) to apply rate limiting independently to each log
statement.

For example:

<!-- @formatter:off -->
```java
// No more than one log statement per 30 seconds.
logger.atWarning().atMostEvery(30, SECONDS).log("Rate limited log statement!");
```
<!-- @formatter:on -->

The difference between these methods can be summarized as follows:

1. `atMostEvery()`: Emits at most one log statement per specified time period (periodic sampling).
    * This is probably the best method to use by default for most situations.
2. `every()`: Emits exactly one in every `N` log statements (deterministic sampling).
    * This can be useful if you expect a fairly consistent rate of logging.
3. `onAverageEvery()`: Emits approximately one in every `N` log statements (random sampling).
    * This is a slightly special case, but is useful if you wish to sample logs across many Java
      processes (`every()` will always log the first time it is encountered, and could give a biased
      result across many processes).

In all cases the number of log statements "skipped" since the previously emitted log message is
tracked and added as metadata to each log statement.

{: .important }
> It is possible to combine multiple types of rate-limiting into a single log statement, and while
> the behaviour is well-defined, it's not very intuitive and rarely useful. As such it's not
> recommended.

### When To Use Rate Limiting {#when-to-use-rate-limiting}

Flogger makes adding rate limited log statements easy, but that does not mean you should use this
everywhere. If a log statement produces excessive output, it should be treated like a bug in the
code. The utility of the log statement should be examined, and if necessary it should have its level
reduced (e.g. changing "info" to "fine"). In some cases the log statement can be split into several
log statements with different levels, and in other cases the log statement could just be removed if
it serves no important role.

Rate limiting should be reserved for things which must be seen (typically "warning" or above), and
for which the rate of logging is highly variable (e.g. in a web server which can be subject to
bursts activity).

{: .note }
> In system which produce a lot of logs, Flogger has other mechanism for managing log statements
> which are often better than simple rate limiting.

### Bypassing Rate Limiting {#bypassing-rate-limiting}

One question which often arises when using rate limiting, is how to test rate limited log
statements. To make this easy, Flogger provides a way to "force" logging to occur in specific
contexts. A forced log statement will bypass rate limiting methods, and by surrounding code under
test with a "forcing context", you can ensure that log statements will always be emitted.

This is [good for testing](testing), but can also be good when using contexts to enable
additional logging (e.g. when debugging a specific request or sub-task) since it allows you to
reliably emit every log statement encountered in a specific context.

See [Advanced Usage](advanced) for more information.

### Design Notes {#design-notes}

It is tempting to ask why the `atMostEvery(int, TimeUnit)` does not use a `Duration` to represent
its rate limit period. There are several reasons for this:

1. The existence of that method predates the existence of the `java.time` package. Flogger still
   supports Java 7 (JDK 1.7) in which `java.time` is not present.
2. A `Duration` instance doesn't provide enough information to format the rate limit period in a
   human-readable way (i.e. a `Duration` does not remember the units with which it was specified).
3. Having users specify a `Duration` to this call would either encourage people to allocate a new
   instance for each invocation (e.g. `atMostEvery(ofMinutes(2))`), or define a reusable static
   constant elsewhere in the class (e.g. `atMostEvery(TWO_MINUTES)`). The former is wasteful for
   disabled log statements, and the latter is more code with no obvious benefit (e.g. compared
   to `atMostEvery(2, MINUTES)`).
4. The use of a value-and-unit parameter pair often makes log statements more human-readable (i.e.
   "at most every two minutes"), and helps make it immediately clear as to what the log statement
   will do. See [API Design Choices](background#api-design-choices) for more on Flogger's design
   principles.

{: .note }
> It's quite possible that a `Duration` based method could be added in the future, and this would
> help in the rare cases where a variable rate limit period is needed. However, even if such a
> method existed, for most normal usage where the rate limit period is constant, the existing API
> would still be strongly recommended.

## Adding Stack Information {#adding-stack-information}

Another fairly common use case discovered when Flogger was being developed, was that of adding stack
information to a log statement to provide better context. In other logging APIs this is typically
carried out using a manually created `Exception` of some sort at the log site. For example:

<!-- @formatter:off -->
```java
// Creating the stack trace of an exception can be expensive.
logger.warning(new RuntimeException(), "Bad things: {}", message);
```
<!-- @formatter:on -->

There are several downsides to this approach:

1. The exception, including all the captured stack information, is created before the logger is
   called.
2. In most APIs an added exception is intended to be the "cause" of the log statement, and not just
   additional context.
3. The exception will capture the entire stack, which can be very verbose in log output.

Flogger solves this issue by adding
the [`withStackTrace()`]({{site.LoggingApi}}#withStackTrace(com.google.common.flogger.StackSize))
method. The advantages of this are:

1. Stack information is not conflated with a `Throwable` added using `withCause()`.
2. Stack size can be selected as `SMALL`, `MEDIUM`, `LARGE` or `FULL` (with `NONE` to provide a
   no-op argument).
3. It works efficiently with rate limiting and expensive stack analysis only occurs for log
   statements which will definitely be emitted.

<!-- @formatter:off -->
```java
// Stack analysis is only carried out for enabled log statements.
logger.atWarning().atMostEvery(30, SECONDS).withStackTrace(MEDIUM).log("Bad things: %s", message);
```
<!-- @formatter:on -->

## Enabled Log Statements and Lazy Evaluation {#enabled-log-statements-and-lazy-evaluation}

Most logging APIs provide some way to test whether a specific log statement would be emitted. This
is useful for guarding debug sections and avoiding calculating expensive values to be logged which
are never used.

Flogger supports this concept via the [`isEnabled()`]({{site.LoggingApi}}#isEnabled()) method.

<!-- @formatter:off -->
```java
if (logger.atFine().isEnabled()) {
  var loggedData = doExpensiveCalculation();
  logger.atFine().log("Expensive data: %s", loggedData);
}
```
<!-- @formatter:on -->

This follows the standard example of most logging APIs before it, but this approach can make code
less readable and risk causing issues:

1. If there is code unrelated to logging in the guarded section, then enabling or disabling logging
   may affect an application's behaviour.
2. If the guard-level and the log level can get out-of-sync, resulting in log statements being
   erroneously skipped.

Flogger provides a second, simpler way to defer calculation of logged arguments; the
[`lazy()`]({{site.LazyArgs}}#lazy(com.google.common.flogger.LazyArg)) method.

<!-- @formatter:off -->
```java
import static com.google.common.flogger.LazyArgs.lazy;

logger.atFine().log("Expensive data: %s", lazy(() -> doExpensiveCalculation()));
```
<!-- @formatter:on -->

This wraps a `Runnable` or lambda into an instance of [`LazyArg`]({{site.LazyArg}}), which can then
be evaluated only when logging will definitely occur.

<!-- @formatter:off -->

{: .pros }
> 1. It's concise and keeps everything in a single log statement.
> 2. It avoids mismatched log levels in guarded blocks.
> 3. **It works correctly for rate limited log statements** (because it is part of the log statement).

{: .cons }
> 1. It may (depending on the contents of the lambda) cause a small, short-lived allocation to be
     made (e.g. if local variables need to be captured).
> 2. If two or more pieces of logged data depend on each other, it may be impractical to evaluate
     them independently using `lazy()`.

<!-- @formatter:on -->

While `lazy()` can cause small allocations to be made, it is better integrated with features like
rate limiting, and will generally produce simpler and more maintainable code. In all but the
tightest inner loops or most complex cases, you should generally prefer using `lazy()`.

## Lazy Logger Instantiation {#lazy-logger-instantiation}

Occasionally it's desirable to use a logger within an interface's static or default methods, or
delay logger instantiation until first use (the latter case can be useful for code used by Flogger).

To address these problems, use a static "lazy holder" class:

```java
import com.google.common.flogger.FluentLogger;

public interface Foo {
  // ...

  default Foo load(Path path) {
    Lazy.logger.atFine().log("loading Foo from path: %s", path);
    // ...
  }

  private /* static */ class Lazy {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  }
}
```
