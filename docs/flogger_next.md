---
layout: page
title: "Flogger Next"
nav_order: 50
---

<!-- @formatter:off -->
# Flogger Next
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
<!-- @formatter:on -->

## What is Flogger Next?

Flogger Next is a project to enhance and extend the original Flogger API with additional features
and provide improved integration with the underlying logging system. If provides a drop-in
replacement [`FluentLogger`]({{site.next.FluentLogger}}) implementation with additional features as
well as new backend implementations and features.

## String Template Syntax Integration

Utilize Java's new `StringTemplate` syntax to improve readability of your log statements (JDK 21
and above).

<!-- @formatter:off -->
```java
int x = 23;
int y = 19;
logger.atInfo()."\{ x } + \{ y } = \{ x + y }".log();
```
<!-- @formatter:on -->

Use optional String Formatter (a.k.a. "printf") syntax if desired.

<!-- @formatter:off -->
```java
int n = getValue();
logger.atInfo()."%d\{ n } = %#x\{ n } in hexadecimal".log();
```
<!-- @formatter:on -->

Use lazy arguments for greater efficiency in log statements that are disabled by default.

<!-- @formatter:off -->
```java
logger.atFine()."Statistics: \{ lazy(() -> this.collectStatsExpensive()) }".log();
```
<!-- @formatter:on -->

See [String Templates](templates) for more.

## Custom Log Message Formatting

Replace Flogger's original, fixed, message format with a flexible new formatter.

* Configure message formatting with simple logging property strings.
* Add elements to the basic log message (log site, timestamp, level).
* Customize formatting of known metadata values.
* Ignore unwanted metadata values (e.g. rate limiting information).

By adding a few simple logging properties:

```text
My log message [CONTEXT task_id=1234 ratelimit_count=100 ]
```

can become:

```text
com.foo.bar.MyClass#myMethod (task@1234) My log message
```

See [Custom Formating](formatter) for more.

## Reduce Class Initialization Costs

Instead of allocating one logger backend for each Fluent logger instance, share backends between
classes with the same logging requirements.

Configure Flogger to allocate logger backends to suit your needs. For example:

* Allocate a single backend instance per package.
* Avoid allocating large numbers of backends in deep package hierarchies.
* Explicitly define the roots of sub-packages which should share a single logger.
* Inherit system settings for consistency between Flogger backends and system logger configuration.
* Cache backend instances to reduce class initialization cost.

This lets you balance your need for runtime logging control, with the desire for a reduction in
object allocations and class initialization cost.

{: .note }
> While sharing logger backends can reduce the granularity of logging control via the underlying
> logging system, Flogger offers additional logging control features not found in other logging
> APIs which make up for this.

See [Backend Naming](backend) for more.
