---
layout: page
title: Background
nav_order: 110
---

<!-- @formatter:off -->
# Background
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
<!-- @formatter:on -->

## Motivation and History {#motivation-and-history}

Debug logging APIs are, at first glance, simple looking beasts. A user has a debug message which
they wish to log at some point in the code, often with runtime parameters. There's also usually a
concept of a "log level", which indicates which log statements should be emitted depending on some "
verbosity" setting.

This sort of API has been the staple of debug logging almost since the birth of high level computer
languages. Java introduced its own debug logging API
in [JDK 1.4](https://docs.oracle.com/cd/E13189_01/kodo/docs303/ref_guide_logging_jdk14.html) (2002).

Java's default logging API came with limitations and a degree of awkwardness which lead to many
alternatives being developed, each addressing one or more of the perceived pain points of the
original API, such as supporting variable numbers of arguments or improving message format syntax.

Today the most common alternative logging APIs
are [Apache Log4J](https://logging.apache.org/log4j/2.x) and [SLF4J](https://www.slf4j.org), but
these APIs are still largely based on the simple principles of the JDK logger.

Since the introduction of these basic logging APIs, software has scaled up and become much more
complex. Distributed servers accept many thousands of requests per second, and produce gigabytes of
logs every hour. A single request can touch dozens of backend machines, leaving a "vapour trail" of
logs in many places.

The challenges for debug logging are no longer limited to locally run applications, or servers on a
single machine. If debug logging is to remain a useful tool for developers, it needs to evolve to
address this new world.

{: .note }
> SLF4J introduced a "Flogger like" fluent API in version 2.0. This was directly inspired by
> Flogger's API (and while this is un-acknowledged by the developers, it is
> [obvious from the discussion and similarities in naming](https://github.com/qos-ch/slf4j/discussions/280)).
> Flogger's Fluent API was designed nearly 10 years before SLF4J adopted it.

### Why Was Flogger Created? {#why-was-flogger-created}

There was never a project started with the aim of making a single, unified logger for Java at
Google. Flogger began much more humbly as an API experiment with the aim of finding an intuitive API
which avoided the need to generate "varargs" arrays for disabled log statements. This is discussed
in depth in [Anatomy of an API](https://google.github.io/flogger/anatomy).

Once the concept of a "fluent" logging API existed, it became clear that it also allowed Flogger's
API to be extended with new functionality. This turned out to be extremely beneficial because it
allowed Flogger to be extended to support the use cases of many teams, and avoid any need for
"wrapper" APIs to be created.

Once it became clear that Flogger's API offered real benefits over the existing collection of
logging APIs used in Google, it was decided to proceed with the project.

### Benefits of a Unified Logging API {#benefits-of-a-unified-logging-api}

In a large and complex code base (such as exists in Google) it is inevitable that, without
intervention, multiple logging APIs will exist. As new code is added to the repository it might use
a different logging API (either because it was imported from elsewhere or because a project has some
custom logging requirement). In a code base the size of Google's you might easily end up with
dozens, or perhaps hundreds, of logging APIs, often little more than minor variants of a simple API
to "log a formatted message at a given level".

It is tempting to accept this situation as just "low cost" technical debt. After all, the logging is
still being done and these APIs are mostly simple enough for developers to switch between them
easily. However, this status quo leads to issues such as:

1. Needing to migrate code between logging APIs if it is moved.
2. Easy to miss mistakes when switching between logging APIs (e.g. using the wrong format syntax).
3. Difficulty when debugging due to confusion around logger configuration.
4. Confusion with respect to the testing of log statements.

Early on in Flogger's development, there was a clear decision made to avoid Flogger being "yet
another logging API". Having just another minor variant of a basic logging API wasn't acceptable,
and if Flogger was going to continue to be worked on, it couldn't be "a bit better" than the
alternatives.

![Standards Proliferation](https://imgs.xkcd.com/comics/standards.png)

If Flogger couldn't be made to satisfy the needs of the vast majority of users in Google's code
base, it wasn't worth doing at all. Just adding a new API, even if it was technically better than
the existing ones, just wasn't worth it.

An extensive analysis of existing open-source logging APIs was carried out to see if any of these
would easily support the use cases we had already identified as being needed by teams in Google. At
that time none of the APIs we looked at were sufficient, so we continued developing Flogger with the
aim of migrating as much code as possible to use it. This meant that a significant effort needed to
be put into developing a tool to reliably migrate millions of log statements, from many different
APIs, to use Flogger.

The benefits of having a unified Java logging API across Google's codebase gave a lot of benefits:

1. One API for programmers to learn.
2. New functionality can be easily added for everyone.
3. No duplicate efforts with regard to performance optimizations.
4. The API is stress tested everywhere and bugs are found quickly.
5. Static lint checks can be written to cover the entire codebase.
6. Only one security audit is required for logging across all projects.

### Summary {#summary}

Overall migrating to use Flogger for Java debug logging in Google was definitely the right choice.
The advantages of having a single, flexible, API across many hundreds of projects outweighed the
costs involved in doing a multi-year migration.

However, these decisions were made for a code base which is almost uniquely large and complicated,
and I don't want anyone reading this to think I am suggesting that migrating all your code to use
Flogger would definitely be worth it. Your code base has its own complexities and requirements and,
if you decide to work towards having a single logging API, you need to decide what's best for you.

## API Design Choices {#api-design-choices}

Logging APIs are different to a lot of other APIs in terms of use and expectation, and their design
is affected by many intersecting requirements.

During Flogger's development millions of log statements were analyzed, and ideas for Flogger's API
were tested rigorously against existing code. Every aspect of the API was discussed in depth, from
object lifecycles to the "simple" issue of method naming. To make an API which could replace
hundreds of existing logging APIs and wrapper classes, every use case needed to be addressed.

Injecting or passing logger instances between classes creates a "viral" dependency on a specific
logger implementation, which either prevents easy code refactoring or discourages classes from doing
logging at all, but if loggers are created only in the class they are used in, it means they must
have a low memory footprint per instance.

{: .highlight }
> Loggers should be thought of as "part of the environment" rather than "part of a system's API".

Flogger also had to be performant in terms of speed and low memory allocations, since you could
never predict in which code it would be used. When logging is disabled, logging should essentially
be zero cost.

It must also **never** hold locks at any point during a normal log statement since, even without a
risk of deadlocks, the use of locks can cause thread contention, leading to latency issues in
multi-threaded code.

A good logging API must also be usable from any piece of code, including static methods called
during class loading, which can occur before the application's `main()` method is invoked. This
leads to some complication around initialization and configuration.

A logging statement must also never cause an exception to be propagated into the calling code.
Enabling additional logging is something you do when debugging something else, it cannot be allowed
to cause more problems, and you cannot assume the user is able to recompile affected code easily.

**However, one of the most compelling design requirements for any logging API is simplicity.**

{: .highlight }
> Almost nobody ever comes to their code in order to write great log statements.

Log statements are not part of the business logic of the code they are in, they are mostly disabled
until such time as debugging occurs and in a lot of cases they are not explicitly tested. User's
often won't think hard about a log statement they are adding and don't want to have to make hard
decisions about which parts of the logging API to use.

When debugging an issue, log output and the log statements which produced them must be reliable and
easy to reason about. A user must be able to understand, at a glance, what a log statement will do,
and under what conditions they expect to see output.

{: .highlight }
> A user in the middle of debugging a serious issue should never also have to reason about some
> subtle behaviour in their logging API.

These principles of simplicity and readability informed the bulk of the API design for Flogger, and
even seemingly unimportant choices can often be tracked back to one or more of the above ideas.

### Instantiating Loggers {#instantiating-loggers}

One example of where a seemingly simple design choice was informed by lot of subtle reasoning, was
how Flogger logger instances are created:

<!-- @formatter:off -->
```java
// This is the only recommended way to instantiate a FluentLogger instance.
private static final FluentLogger logger = FluentLogger.forEnclosingClass();
```
<!-- @formatter:on -->

While other logging APIs often instantiate logger instances using a "name" or "tag" or some variety,
Flogger explicitly avoids this. In fact the naming of the static method `forEnclosingClass()` leaves
it deliberately ambiguous as to exactly what the "name" of the logger might be. The logger you get
is one that's "right for that class" according to the current logging configuration, including which
backend is used. The name `forEnclosingClass()` also acts to remind callers that the instance
created is intended for use only in that class and, implicitly, passing logger instances between
classes is discouraged

There's also no way for users to get the "name" of logger instance; it's just not something you
should care about.

{: .highlight }
> While the "name" of a logger is often the name of the class using it, that's not required for
> Flogger, and a Flogger backend could even supply exactly the same logger for every call to
> `forEnclosingClass()`, if configured to do so. The only thing you know is that the logger you've
> got is right for the class it's being used in.

### Fluent API Choices {#fluent-api-choices}

While a lot of this is discussed in [Anatomy of an API](https://google.github.io/flogger/anatomy),
it is probably worth reiterating a few key points here.

For Flogger's fluent API, the basic construction is of the form:

`logger.<level-selector>.<extensible-API-methods>.<terminal-log-statement>`

The `level-selector` (e.g. [`atInfo()`]({{site.AbstractLogger}}#atInfo())) always comes first,
because that's where the logger can return a "no-op" instance of the API when logging is disabled.

{: .highlight }
> Flogger's level selector methods are all prefixed with "at", rather than just
> being `info()`, `warning()` etc. This serves two purposes; it makes the variable level selector
> method [`at()`]({{site.AbstractLogger}}#at(java.util.logging.Level)) more discoverable, and it groups the levels together at the start of any API
> documentation and IDE auto-completion lists.

After the level selector comes an optional sequence of fluent API methods, before the log statement
is terminated with a [`log()`]({{site.LoggingApi}}#log(java.lang.String,java.lang.Object))
or [`logVarargs()`]({{site.LoggingApi}}#logVarargs(java.lang.String,java.lang.Object[])) method.

Every fluent method which accepts arguments must accept a "no-op" value to effectively disable its
behaviour (e.g. `withCause(null)` or  `atMostEvery(0, SECONDS)`). This is important to ensure that
callers can always write a single, self-contained log statement and avoid needing to conditionally
call any fluent methods.

<!-- @formatter:off -->
```java
// This should never be necessary with Flogger!!!
if (extraData != null) {
  logger.atInfo().withExtraData(extraData).log(<message and arguments>);
} else {
  logger.atInfo().log(<same message and arguments as above>);
}
```
<!-- @formatter:off -->

Since many log statements are normally disabled, and thus use the "no-op" API, any combination of
fluent methods must be permitted. Turning on additional logging cannot result in runtime exceptions
due to "bad combinations" of methods, and adding consistency checking to the no-op API would require
state to be kept, which is not feasible. This doesn't mean that all combinations of fluent methods
must have completely obvious behaviour however (e.g. combining multiple types of rate limiting for a
single log statement).

If fluent methods are intended to interact in a useful way with each other, it's important to name
the methods such that the final fluent log statement reads as naturally as possible. Naming might
suggest a natural order to methods, but the implementation must not care in which order methods were
called.

{: .warning }
> Of course this design is regrettably English-centric, and may not be as clear to users in all
> languages, so it's also important that the interactions are clearly documented.

For example, it's probably more natural for a user to write:

<!-- @formatter:off -->
```java
logger.atWarning().atMostEvery(50, MILLISECONDS).per(REQUEST).log(...);
```
<!-- @formatter:on -->

rather than:

<!-- @formatter:off -->
```java
logger.atWarning().per(REQUEST).atMostEvery(50, MILLISECONDS).log(...);
```
<!-- @formatter:on -->

but both examples must behave identically.

### Platform and Initialization {#platform-and-initialization}

Another area in which Flogger, and other logging APIs, have interesting requirements is
configuration and initialization. Unlike most libraries, logging libraries must be usable at any
point during application initialization. This includes having log statements called during class
loading, before the `main()` method is entered. This makes it impossible to control basic logger
initialization with either programmatic configuration, or command line flags.

Flogger uses a combination of system properties and service loading to allow a backend to be
specified as part of the environment. This allows the first log statement in a program to
self-initialize the logging platform at the point of first use.

However, this is not quite sufficient to avoid issues, since there are also complexities with
reentrant logging occurring while initialization is in progress, and the core Flogger library is
extremely careful to allow logging to occur in any code used as part of the logging environment (or
to document clearly when logging is prohibited).

Having all this complexity behind the scenes is worth it however, because it means that any user, in
any piece of code, can just initialize and use a Flogger logger without worrying.
