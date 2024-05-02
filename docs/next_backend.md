---
layout: page
title: "Next: Backend Naming"
permalink: /next/backend/
nav_order: 54
---

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
- TOC
{:toc}
</details>

## Introduction

{: .note}
> This page is best read in conjunction with the section [Debugging With Flogger](debugging.md).

Almost every modern Java logging system has the concept of "named loggers". In most cases, the name
of a logger is just the name of the class in which that logger is used, and class will initialize
their logger instance something like:

```java
import java.util.logging.Logger;

class MyClass {
  private static final Logger logger = Logger.getLogger(MyClass.class.getName());

  ...
}
```

However, contrary to what some people might think, the logger name is not used to determine the "log
site" information often displayed in log messages. Log site information must be accurate, and this
means resolving nested or inner class names as necessary.

```text
2024-04-20T13:50:12.3456Z INFO [com.thing.project.foo.Frobulator$Listener#handleFrob] <message>
                                \----- This is NOT the logger name -----/
```

{: .note}
> So, if a logger's name is not used to determine log site information, and it doesn't affect the
> content of log messages, why not just have a single logger instance for all classes?

## Why Logger Naming Matters

Logger naming is primarily important for logging configuration, and allows different logger
instances to hold distinct logging configurations. This is most commonly used to set different log
levels for different classes.

While you could implement a mapping from "logger name" to logging configuration in many ways, the
fact that logger instances typically hold their configuration directly is a very important design
choice. By holding configuration, especially the logger's enabled log level, directly in the
instance, a logger can rapidly determine which log statements are disabled and should be ignored.

This matters because it is the more fine-grained log statements which are disabled and, since these
most often appear in loops and innermost code, they are processed hundreds or thousands of times
more often than enabled ones.

This means that minimizing the cost of deciding to ignore disabled log statements is one of the most
important factors in efficient logger design. For example, the JDK logger goes to great lengths to
make retrieving a logger's log level as simple as a single read of a `volatile` integer field.

## How Existing Logging Systems Handle Naming

Since a logger's configuration is typically held directly in the instance, and becomes desirable to
have many logger instance, since sharing one instance between many classes would prevent you from
being able to control log levels freely. This is especially important for library code which is
expected to be used in many environments, and cannot predict how it might need to be configured
during debugging.

This results in the most common logger configuration being "one logger per class", with the logger
instance named after the class. Given this scheme, and the ability to create "intermediate" loggers
named after parent packages, it's possible to provide any combination of logging configuration, from
simple to complex, since every location in the namespace can have logging configuration associated
with it.

<img src="{{site.baseurl}}/assets/logger_hierarchy.svg">

While simple, this means that every single class must allocate a new logger instance during class
initialization, which both adds to the time taken to load the class and incurs hundreds or thousands
of potentially non-trivially sized allocations.

And in reality, almost all of these logger instances will end up with effectively identical logging
configurations almost all the time, since it's only during debugging that the ability to
individually control loggers on a per-class basis becomes important.

## What Flogger Does Differently

Flogger sits above other logging systems, and [`FluentLogger`]({{site.FluentLogger}}) instances are
associated with a [`LoggerBackend`]({{site.LoggerBackend}}), which provides an abstraction layer.
Because Flogger was designed to handle different logger naming schemes it does not directly expose
the backend name to the user.

In fact, very deliberately, there is no way to request a [`FluentLogger`]({{site.FluentLogger}})
instance with a specific name, and it is only possible to ask for "the logger suitable for the
current class".

```java
import net.goui.flogger.FluentLogger;

class MyClass {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
}
```

By using a static method, rather than a constructor, it is possible for the same logger instance to
be shared between many classes. Furthermore, there is no requirement that
each [`FluentLogger`]({{site.FluentLogger}})
instance be associated with its own [`LoggerBackend`]({{site.LoggerBackend}}) instance, as these too
can be shared. And finally, there is no public API for the use to obtain either the name of
a [`FluentLogger`]({{site.FluentLogger}}) instance, or
its [`LoggerBackend`]({{site.LoggerBackend}}).

{: .important}
> However, there is still the need to provide sufficient logger configurability during debugging, and
> especially to ensure that things like `ScopedLoggingContext` work as expected.

### Why is ScopedLoggingContext important?

The [`ScopedLoggingContext`]({{site.ScopedLoggingContext}}) mechanism (see
also [Advanced Usage](../../advanced/)) is an important Flogger feature to enhance debugging via log
statements. It permits logging to be "forced" within user defined contexts for arbitrary packages or
classes. This largely replaces the need for users to edit and reload logging configuration while
debugging, while providing proper scoping for log level debugging changes (e.g. for a single
request).

This class specifies the log level to be modified by using class or package names, and so exposes an
assumed mapping from class name to logger behaviour. Thus while there is no explicit way for users
to determine a logger's name, there is a need to have some kind of internal mapping which can
associate loggers and class names.

## Flogger Next's Naming Scheme

Flogger Next offers the user a configurable naming scheme designed to be flexible enough to handle
the need for fine-grained logger control, while dramatically reducing the number of logger backends
which need to be allocated.

To achieve this, a user specifiable [`LoggerBackend`]({{site.LoggerBackend}}) naming strategy is
installed at application startup, mapping logging class names to backend names.

<img src="{{site.baseurl}}/assets/flogger_next_naming.svg">

In this example, any classes under the `com.foo.*` namespace are configured to use a
shared [`LoggerBackend`]({{site.LoggerBackend}}) named `com.foo`. This is achieved with the addition
of simple Flogger Next properties:

```properties
flogger.backend_naming.roots.size=1
flogger.backend_naming.roots.0=com.foo
```

### Reducing Class Initialization Cost

While there is still the need for the Flogger Next `FluentLogger` instances to be allocated on a
per-class basis, these instances are now small (two fields) and reference a cached,
shared [`LoggerBackend`]({{site.LoggerBackend}}) instance. And since Flogger Next also defers the
creation of the underlying logger instances until first use, the overall static initialization cost
is typically one or two small object allocations with no additional overhead.

## Logger Backend Naming Strategies

Of course, not every application will want the same naming strategy for the logger backends, so
Flogger Next provides a simple way to configure backend naming via Flogger properties. Different
strategies can be applied for different package hierarchies, and existing system logging
configuration can be imported for use by Flogger.

By default, Flogger Next names each [`LoggerBackend`]({{site.LoggerBackend}}) after a class with
a `FluentLogger` in. This is the most flexible approach when debugging because every logger can be
controlled individually, but may cost you hundreds or even thousands of additional logger
allocations, and affect class loading and static initialization performance.

At the other extreme, you could configure a single root logger which would result in
every `FluentLogger` sharing a single [`LoggerBackend`]({{site.LoggerBackend}}) instance. This would
mean you could no longer control log levels separately during debugging, means if you need to
enable `FINE` logging somewhere, you need to enable it everywhere, which could produce an excessive
amount of unhelpful log output and even affect performance.

### Naming Strategy: No Name Mapping

This is the default strategy if no naming properties are set and matches Google's Fluent Logger
behaviour. Each [`FluentLogger`]({{site.FluentLogger}}) will be associated with
a [`LoggerBackend`]({{site.LoggerBackend}})
with the same name as the logging class.

{: .note}
> For this strategy, logger backend caching is not enabled, since backends are not shared.

### Naming Strategy: Per Package Loggers

This strategy is probably the simplest non-default strategy and should result in a significant
reduction in allocation of underlying logger instances. Each [`FluentLogger`]({{site.FluentLogger}})
will be associated with a [`LoggerBackend`]({{site.LoggerBackend}}) with *the name of the package of
the logging class*.

To enable this strategy, simply set the `trim_at_least` naming option:

```properties
flogger.backend_naming.trim_at_least=1
```

You can also set higher values for name trimming to switch to using parent package names etc.

{: .note}
> If `trim_at_least` has a positive value, `LoggerBackend` caching is enabled.

### Naming Strategy: Maximum Package Depth

For applications with deep class hierarchies, it may also be useful to limit the backend package
depth. This strategy can be used alongside `trim_at_least`, but `trim_at_least` will be applied
first, so you can always ensure backends use package names rather than individual class names.

To enable this strategy, simply set the `retain_at_most` naming option:

```properties
flogger.backend_naming.retain_at_most=N
```

{: .note}
> If `retain_at_most` has a positive value, `LoggerBackend` caching is enabled.

### Naming Strategy: Explicit Package Roots

As well as defining how arbitrary logging class names are mapped to backend names, you can also
explicitly set package roots to be used for any classes in that hierarchy.

To enable this strategy, add package specifiers to the `flogger.backend_naming.roots` array.

```properties
flogger.backend_naming.roots.size=3
flogger.backend_naming.roots.0=com.myproject.foo
flogger.backend_naming.roots.1=com.myproject.bar
flogger.backend_naming.roots.2=org.other.project.package
```

To avoid having to maintain a large number of package roots, it is also possible to append one or
more "wildcard" suffixes (`.*`) to package root specifications. The configuration below has the same
effect as the one above, except it also defines roots for any other packages within `com.myproject`.

```properties
flogger.backend_naming.roots.size=2
flogger.backend_naming.roots.0=com.myproject.*
flogger.backend_naming.roots.1=org.other.project.package
```

{: .note}
> If a logging class name matches a root specifier, then options `trim_at_least` and
> `retain_at_most` are not applied.

### Naming Strategy: System Package Roots

If the underlying logging system defines a static configuration for loggers, this can be imported to
provide additional package roots which are then merged with any explicitly provided roots.

For example, given the following configuration in a `log4j2.xml` file:

<!-- @formatter:off -->
```xml
<Loggers>
    <!-- Root logger referring to console appender -->
    <Root level="warn" additivity="false">
        <AppenderRef ref="console"/>
    </Root>
    <Logger name="com.myproject.foo" level="info"/>
    <Logger name="com.myproject.bar" level="warn"/>
    <Logger name="org.other.project.package" level="info"/>
</Loggers>
```
<!-- @formatter:on -->

The system roots `com.myproject.foo`, `com.myproject.bar` and `org.other.project.package` will be
used automatically. This is very useful because it automatically aligns the loggers for which an
initial configuration was defined with Flogger's naming strategy. This ensures that any statically
configured logger can be used to control FluentLogger instances.

To enable this strategy, set `use_system_roots`:

```properties
flogger.backend_naming.use_system_roots=true
```

{: .note}
> The unnamed "root" logger will not be imported as an explicit system root, since it always exists
> and no name mapping should be applied to it.

{: .important}
> When using the JDK logger backend, it is necessary to install the
> `net.goui.flogger.backend.system.FloggerConfig` as the logging config class in order to extract
> system roots.

```properties
-Djava.util.logging.config.class=net.goui.flogger.backend.system.FloggerConfig
```

### Default Extension For Root Entries

You can also set a default depth to extend matched root entries by, which applies to both explicit
root entries and system roots:

```properties
flogger.backend_naming.default_root_extend=N
```

This has the same effect as adding `N` copies of the wildcard `.*` suffix to all roots (including
inherited system roots) for which no wildcard was specified. This is a useful way to extend system
roots by some number of levels, to gain a little extra backend configurability at runtime.

## Summary

The following diagram sums up the difference between the traditional approach (left), where every
logger can be individually configured, with Flogger Next's approach (right), where logging
configuration is typically controlled at the package level,
with [`FluentLogger`]({{site.FluentLogger}}) instances inheriting configuration from the nearest
configurable system logger.

<img src="{{site.baseurl}}/assets/naming_hierarchy.svg">

Even in this simple example, the number of required system loggers is less than half, and for
typical code structure where many classes exist per package, it would probably be at least an order
of magnitude reduction.

However, for modern server based applications, this reduction in fine-grained configurability is not
expected to be an issue, because in many cases it would be inefficient or impractical to try to
debug an issue by altering the log levels of individual classes.
See [Debugging With Flogger](debugging.md) for more.

## Installation

<!-- @formatter:off -->
```xml
<dependency>
    <groupId>net.goui.flogger.next</groupId>
    <artifactId>backend-system</artifactId>
    <version>${flogger-next.version}</version>
</dependency>
```
<!-- @formatter:on -->

<!-- @formatter:off -->
```xml
<dependency>
    <groupId>net.goui.flogger.next</groupId>
    <artifactId>backend-log4j</artifactId>
    <version>${flogger-next.version}</version>
</dependency>
```
<!-- @formatter:on -->
