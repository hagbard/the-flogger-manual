---
layout: page
title: Advanced Usage
permalink: /advanced/
nav_order: 30
---

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

For working code examples for advanced usage, see [AdvancedExamples.java].

## Metadata, Contexts and Scopes {#metadata-contexts-and-scopes}

Apart from basic logging and rate limiting, Flogger provides several advanced mechanisms for
improving the value of log statements. While each of these has some value on its own, it is
when they are used together that they have the most impact.

### Simple Metadata {#simple-metadata}

The first mechanism is [`Metadata`]({{site.javadoc}}/backend/Metadata.html), which provides a way to
pass structured data to the backend. This allows for additional context to be passed to log
statements, which can be emitted as part of the log output or used to control behaviour in a
compatible logger backend.

To add your own metadata to a log statement, call
[`with(key, value)`]({{site.javadoc}}/LoggingApi.html#with(com.google.common.flogger.MetadataKey,T))
as part of your log statement. Each metadata value is associated with a strongly typed
[`MetadataKey`]({{site.javadoc}}/MetadataKey.html) instance, and the keys unambiguously 
identify the metadata to logger backends. Keys which are not known to a logger backend should
generally be emitted as part of the log statement (this is the default behaviour for an unknown
key).

In fact, you've already seen `Metadata` at work, it's used for methods such as
[`withCause()`]({{site.javadoc}}/LoggingApi.html#withCause(java.lang.Throwable)) and
[`withStackTrace()`]({{site.javadoc}}/LoggingApi.html#withStackTrace(com.google.common.flogger.StackSize)),
as well as any rate limiting. However, there are many more uses for it and when combined with a
metadata aware backend, it's a very powerful mechanism.

The [associated example code][AdvancedExamples.java] has some useful examples of defining and
using metadata.

### Logging Contexts {#logging-contexts}

On its own, supplying metadata to a log statement at the log site is somewhat useful, but it still
leaves the user needing to augment many log statements and doesn't fix the problem of how to add
context to log statements in code you don't own.

To address this, Flogger provides the
[`ScopedLoggingContext`]({{site.javadoc}}/context/ScopedLoggingContext.html) mechanism, which allows
a block of code to be surrounded with a scope, which propagates metadata and other information to
every log statement in the scope.

The logger context mechanism is based on Google's `gRPC` context library. Building logging contexts
on top of an existing context mechanism allows existing code using that library to seamlessly
get the advantages of logger contexts.

This means contexts can be nested and propagated between threads easily. When contexts are
nested, the metadata they carry is merged such that any log statements will see the combined
metadata for all the contexts they are in. For situations where order matters, contexts are
ordered from outermost to innermost and any metadata added at the log-site is "last".

To run a task with a new logging context, use the static factory methods in
[`ScopedLoggingContexts`]({{site.javadoc}}/context/ScopedLoggingContexts.html):

<!-- @formatter:off -->
```java
ScopedLoggingContexts.newContext().run(mySubTask(...));
```
<!-- @formatter:on -->

Other methods allow you to easily return values or create contexts explicitly for use with
try/catch blocks or when explicit lifecycle management is needed.

### Scoped Metadata and Tags {#scoped-metadata-and-tags}

The best way to attach metadata to a context, is to do so when the context is built:

<!-- @formatter:off -->
```java
ScopedLoggingContexts.newContext().withMetadata(TASK_KEY, value).run(mySubTask(...));
```
<!-- @formatter:on -->

If the metadata you wish to add consists of no more than a simple key/value pair which you
simply wish to appear in log statements, you can also use the `Tags` mechanism to achieve this.

<!-- @formatter:off -->
```java
ScopedLoggingContexts.newContext().withTags(Tags.of("label", "value")).run(mySubTask(...));
```
<!-- @formatter:on -->

The [`Tags`]({{site.javadoc}}/context/Tags.html) mechanism records all the unique key-value
pairs with which a context was tagged. It does not permit rewriting existing values and does not
preserve the order in which tags were applied.

Using tags is easier than creating metadata keys, but it's less structured and only intended for
modifying log statements rather than modifying logging behavior.

Since tags use strings for keys, a tag is not uniquely defined by its label, so to avoid
accidentally "overwriting" tag values, the tags mechanism merges tags with the same key rather
than overwriting them.

This means that if the tags `{"foo" => "bar"}` and `{"foo" => 23}` are added to contexts, then
the tags applied to a log statements are `{"foo" => ["bar", 23]}`. This even applies to boolean
tags, and it is possible to have the tags `{"flag" => [false, true]}`. While this may feel odd
at first, it guarantees there's never any risk of confusion if two bits of code accidentally use
the same label for their tags, since all tags are preserved.

### Explicit Lifecycle Control {#explicit-lifecycle-control}

In cases where tasks are conditionally modified after creation, you can also modify an
existing context. Consider a simple callback object for some managed task lifecycle:

<!-- @formatter:off -->
```java
  @Override
  public void start(Runnable task) {
    // Installs the context in the current thread.
    this.context=ScopedLoggingContexts.newContext().withMetadata(TASK_KEY, value).install();
    super.start(task);
  }

  @Override
  public void enableDebugging() {
    this.context.addTags(Tags.of("session_id", getDebugId()));
  }

  @Override
  public void close() {
    super.close();
    this.context.close();
  }
```
<!-- @formatter:on -->

### Log Level Control {#log-level-control}

Contexts are also able to control the log level for logging, including "forcing" additional log
statements to be emitted. This is a very powerful debugging technique since it permits the
targeted debugging within a single execution of a task. Combined with the ability to add a
per-invocation identifier via metadata, this lets you easily control logging to suit your
debugging needs.

{: .important }
> When a log statement if forced, it bypasses all rate limiting and sampling thresholds,
> ensuring that every log statement encountered is emitted. This even applies to log
> statements which would have been emitted normally anyway.

<!-- @formatter:off -->
```java
  private static final LogLevelMap DEBUG_LEVELS =
      LogLevelMap.builder()
          // By default everything at FINE or above is forced (including INFO etc.) even if they 
          // would normally be emitted. This ensures we don't lose anything to rate limiting.
          .setDefault(Level.FINE)
          // We are particularly interested in SomeClass.class so give it a lower level.
          .add(Level.FINEST, SomeClass.class)
          .build();

  public Result doTask(Request request) {
    if (isDebugEnabled(request)) {
      // Enable more logging and add the debug ID to all log statements emitted.
      return ScopedLoggingContext.newContext()
          .withTags(Tags.of("debug_id", request.getDebugId()))
          .withLogLevelMap(DEBUG_LEVELS)
          .call(() -> execute(request));
    } else {
      // Execute the task as normal with standard log output.
      return execute(request());
    }
  }
```
<!-- @formatter:on -->

## Log Aggregation {#log-aggregation}

Now you've had an introduction to metadata and contexts, it's time to introduce the next
mechanism to help improve your debug logs.

Log aggregation allows stateful log statements (e.g. those with rate limiting or other per
log-site state) to be aggregated according to a given "aggregation key". This technique only
works for stateful log statements however, and otherwise has no effect.

### Common Use Cases {#common-use-cases}

The commonest example of log aggregation is using the
[`per(Enum)`]({{site.javadoc}}/LoggingApi.html#per(java.lang.Enum)) method in Flogger's fluent
API to specify different rate limiting for different enum values.

Consider a log statement which is attempting to record warnings for some request:

<!-- @formatter:off -->
```java
logger.atWarning()
    .atMostEvery(30, SECONDS)
    .log("Error in request (type=%s): %s", requestTypeEnum, requestData);
```
<!-- @formatter:on -->

When there are several request types, and especially if one type creates most of the warnings,
it is possible that uncommon request types will never appear in the logs. This is a
potential problem with any log statement rate limiting, but with Flogger you can use log
aggregation to avoid it:

<!-- @formatter:off -->
```java
logger.atWarning()
    .atMostEvery(30, SECONDS)
    .per(requestTypeEnum)
    .log("Error in request: %s", requestData);
```
<!-- @formatter:on -->

This ensures that rate limiting is applied separately for each value of `requestTypeEnum` and
ensures each distinct type that's logged will appear.

### Per Request Log Aggregation {#per-request-log-aggregation}

By extending this concept to use contexts and metadata however, we can make it even more useful.
A [`ScopeType`]({{site.javadoc}}/context/ScopeType.html) is an API from which new scopes can be 
created, and which can be attached to contexts. The most common scope type is
[`ScopeType.REQUEST`]({{site.javadoc}}/context/ScopeType.html#REQUEST), but other values can be
created as needed.

To bind a `ScopeType` instance to a context, simply pass it to the context builder:

<!-- @formatter:off -->
```java
Result result = ScopedLoggingContext.newContext(REQUEST)
    .withTags(Tags.of("request_id", request.getId()))
    .call(() -> execute(request));
```
<!-- @formatter:on -->

Now, for any stateful log statement inside the context you can do:

<!-- @formatter:off -->
```java
logger.atWarning()
    .atMostEvery(500, MILLISECONDS)
    .per(REQUEST)
    .log("Error in request: %s", requestData);
```
<!-- @formatter:on -->

This log statement will now be aggregated separately for each context that's created with the
associate scope type (and if it is executed outside a context for that scope, it is simply not
aggregated).

With well-defined, project specific scopes, this mechanism can help aggregate stateful logging 
on a per-context basis and ensure that rate limited log statements are properly represented for
separate tasks.

## Custom Metadata Keys {#custom-metadata-keys}

For simple use cases, instances of [`MetadataKey`]({{site.javadoc}}/MetadataKey.html) are created
using the static factory methods in the class. You can create "single valued" or "repeated" keys 
associate metadata values with.

However, you can also subclass `MetadataKey` and override either its
[`emit(...)`]({{site.javadoc}}/MetadataKey.html#emit(T,com.google.common.flogger.MetadataKey.KeyValueHandler))
or
[`emitRepeated(...)`]({{site.javadoc}}/MetadataKey.html#emitRepeated(java.util.Iterator,com.google.common.flogger.MetadataKey.KeyValueHandler)) 
methods, which gives you access to the collected metadata value(s) for that key and lets you 
process them to determine what should actually be emitted.

A few examples of use cases for this might be:

1. Collecting repeated metadata and joining values into a single path-like result (e.g. 
   "foo/baz/baz").
2. Using metadata values as system property names, and looking up the value to the return via 
   `System.getProperty()`.
3. Having a procedural `MetadataKey<Boolean>` which  looks up some current system status value
   (e.g. remaining heap memory). This can then be called as just
   [`with(SHOW_GC_STATUS)`]({{site.javadoc}}/LoggingApi.html#with(com.google.common.flogger.MetadataKey)).

## Caveats and Limitations {#caveats-and-limitations}

While using metadata and contexts can be a powerful tool for improving the value of debug logs,
there are a few minor caveats to bear in mind.

1. Contexts must be strictly nested. This is enforced when using the `run()` or `call()` style
   methods, but can be violated when managing the lifecycle of a context explicitly, and failing
   to manage a context's lifecycle properly will result in undefined behavior.

2. Using a [`LogLevelMap`]({{site.javadoc}}/context/LogLevelMap.html) to "force" additional
   logging in a context has a small overhead for all log statements, even those not directly 
   affected. It should not cause additional memory allocations, but might be worth avoiding in 
   the tightest inner loops.

3. Using [`ScopeType`]({{site.javadoc}}/context/ScopeType.html) with log aggregation creates an
   implicit contract between the code creating the context and the code doing the logging (both 
   must agree on which value to use). This is fine when that code is in the same project, but 
   using `per(<scope-type>)` in a shared library requires that the specific type used is 
   advertised to users of the library, so they can create contexts using it.

4. When using the `per()` method which takes a `LogPerBucketingStrategy`, it is important to avoid
   using a strategy which produces a lot of unique results. For each unique value passed to the
   `per()` method of a stateful log statement, a new piece of internal state must be created, and
   this is held onto forever. This is why enums are recommended as the best approach, since they
   have only a well bounded finite set of values. A bucketing strategy which allows an unbounded
   number of values to be used will create a slow, but unbounded, memory leak.

5. When subclassing [`MetadataKey`]({{site.javadoc}}/MetadataKey.html), care must be taken to 
   ensure that any work done during the `emit()` or `emitRepeated()` method is efficient, and never 
   locks any user objects. For example, in the case of looking up system property names (see 
   above), it would be very good practice to cache resolved property values in a `ConcurrentMap` 
   of some kind to avoid repeated look-ups. For values which might change over time (e.g. system 
   memory status) it would be strongly advised to cache values with a minimum refresh interval 
   to avoid "thrashing" when log rates are high.

## Grpc Context Propagation {#gprc-context-propagation}

As mentioned above, logging contexts are based on the Google `gRPC context` library. This library
is part of the broader `gRPC` framework and provides the underlying mechanism for propagating
logging contexts between threads, but this does not happen by default.

Without integrating `gRPC contexts` into your system, a thread started from inside a logging
context will not have the logging contexts propagated to it. To have contexts propagated to
other threads, you must use the `gRPC context` API wherever threads are created.

The [gRPC Context API](https://grpc.github.io/grpc-java/javadoc/io/grpc/Context.html) is simple to
use and lets you easily wrap `Runnable`s, `Callable`s and `Executor`s to propagate contexts
properly.

{: .note }
> While the `gRPC context` mechanism can be used in isolation, there are also a lot of potential
> benefits in using the general `gRPC` mechanism in your projects.

[AdvancedExamples.java]: https://github.com/hagbard/the-flogger-manual/blob/main/src/main/java/net/goui/flogger/examples/AdvancedExamples.java
