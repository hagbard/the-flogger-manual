---
layout: page
title: Installing Flogger
permalink: /install/
nav_order: 10
---

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

## Maven Dependencies

The easiest way to try out Flogger is to install the code in this project and run the various
examples. If you've decided to try out Flogger in your own Maven project, the easiest way to install
and use it is by specifying its Maven dependencies.

The simplest way to get started is to install the "system" backend which logs via the JDK's built-in
logging libraries.

This dependency will provide the logging API for use in libraries, application and tests.

<!-- @formatter:off -->
```xml
<!-- Provides the basic Flogger API (required for any Fluent logger use). -->
<!-- https://mvnrepository.com/artifact/com.google.flogger/flogger -->
<dependency>
  <groupId>com.google.flogger</groupId>
  <artifactId>flogger</artifactId>
  <version>$flogger-version$</version>
</dependency>
<!-- The default backend implementation (only add this for applications and tests). -->
<!-- https://mvnrepository.com/artifact/com.google.flogger/flogger-system-backend -->
<dependency>
  <groupId>com.google.flogger</groupId>
  <artifactId>flogger-system-backend</artifactId>
  <version>$flogger-version$</version>
</dependency>
```
<!-- @formatter:on -->

{: .note }
> At the time of writing, the latest Flogger version is `0.7.4`.

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
  <version>$flogger-version$</version>
</dependency>
<!-- The underlying gRPC context library (needed for applications and tests, and optional for libraries). -->
<!-- https://mvnrepository.com/artifact/io.grpc/grpc-context -->
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-context</artifactId>
  <version>1.0.1</version>
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

See the [Advanced Usage](advanced.md) page for more information on scoped contexts and how to use them to improve debugging.

## Choosing Your Backend
](https://hagbard.github.io/the-flogger-manual/)