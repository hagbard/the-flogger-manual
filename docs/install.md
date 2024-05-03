---
layout: page
title: Installing Flogger
nav_order: 10
---

<!-- @formatter:off -->
# Installing Flogger
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
<!-- @formatter:on -->

## Maven Dependencies

The easiest way to try out Flogger is to install the code in this project, and run the various
examples. If you've decided to try out Flogger in your own Maven project, you just need to specify
its Maven dependencies.

To get started, install the "system" backend which logs via the JDK's built-in logging libraries.
This logger backend is always available, and once you've got things working it's easy to switch to
a backend of your choice.

This dependency will provide the logging API for use in libraries, applications and tests.

<!-- @formatter:off -->
```xml
<!-- The default backend implementation (only add this for applications and tests). -->
<!-- https://mvnrepository.com/artifact/com.google.flogger/flogger-system-backend -->
<dependency>
  <groupId>com.google.flogger</groupId>
  <artifactId>flogger-system-backend</artifactId>
  <version>${flogger-version}</version>
</dependency>
```
<!-- @formatter:on -->

{: .note }
> At the time of writing, the latest Flogger version is `0.8`{: style="color: red"}.

Adding these dependencies will install the core library and system backend, allowing you to write
your first Flogger log statements.

<!-- @formatter:off -->
```java
// Declare a logger in a class ...
private static final FluentLogger logger = FluentLogger.forEnclosingClass();
```

```java
// A simple log statement ...
logger.atInfo().log("Hello World");
```
<!-- @formatter:on -->

In addition to the basic dependencies, it's also worth considering installing the additional
gRPC-based context library. This allows Flogger to take advantage of gRPC contexts to propagate
metadata within logging contexts.

<!-- @formatter:off -->
```xml
<!-- The Flogger context API (required for any use of logging contexts). -->
<!-- https://mvnrepository.com/artifact/com.google.flogger/flogger-grpc-context -->
<dependency>
  <groupId>com.google.flogger</groupId>
  <artifactId>flogger-grpc-context</artifactId>
  <version>${flogger-version}</version>
</dependency>
```
<!-- @formatter:on -->

With this installed, a "scoped logging context" can be opened to propagate metadata to any log
statements within the scope.

<!-- @formatter:off -->
```java
try (var ctx = ScopedLoggingContexts.newContext().withTags(Tags.of("foo", true)).install()) {
  // Any log statements called inside a context will contain the context's metadata.
  logger.atInfo().log("This log statement will have 'foo' metadata added.");

  // This includes log statements called by other code.
  OtherClass.doSomethingElseWhichMightCauseLogging();

  // Contexts can be nested and metadata is merged.
  ScopedLoggingContexts.newContext()
      .withTags(Tags.of("bar", "baz"))
      .run(() -> logger.atInfo().log("The log statement has 'foo' and 'bar' metadata."));
}
```
<!-- @formatter:on -->

See [Advanced Usage](advanced#logging-contexts) for more information on scoped contexts and how
to use them to improve debugging.

## Choosing Your Backend

Flogger supports several of the most common logger backends, and all it takes is a top level
dependency in your application to select the one you want.

If you are already using `Log4J2` then it's all easy as adding:

<!-- @formatter:off -->
```xml
<dependency>
  <groupId>com.google.flogger</groupId>
  <artifactId>flogger-log4j2-backend</artifactId>
  <version>${flogger-version}</version>
</dependency>
```
<!-- @formatter:on -->

There's even an `SLF4J` backend if you prefer to defer to that, and at the time of writing there's
a `Log4J v1` backend, but this is very deprecated and could be removed at any time due to numerous
known unfixed (and unfixable) issues with `Log4J v1`.
