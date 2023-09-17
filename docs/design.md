---
layout: page
title: Flogger Design
permalink: /design/
nav_order: 40
---

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
- TOC
{:toc}
</details>

## Log Statement Flow

The basic flow of a Flogger log statement can be illustrated as:

<img src="{{site.baseurl}}/assets/log_statement_flow.svg">

### Level Selector

* A level selector is always the first method in a fluent log statement.
* This method returns the [`LoggingApi`]({{site.LoggingApi}}) instance on which further methods are
  invoked.
* In normal use this would be one of the named `atXxxx()` methods, but you can also specify the log
  level dynamically via [`at()`]({{site.AbstractLogger}}#at(java.util.logging.Level))
  (though this is generally discouraged).
* If the requested level is not enabled by log level (according to the backend), then a singleton
  "no-op" instance is returned. This has the same API, but it is stateless and all its methods are
  essentially empty. This allows Flogger to avoid any further work for disabled log statements.

### Fluent API Chain

* The fluent API chain is an optional sequence of fluent API calls from the
  [`LoggingApi`]({{site.LoggingApi}}).
* Fluent API calls typically just add metadata to the [`LogContext`]({{site.LogContext}}).
* It is important that little or no work is done in these methods since the log statement may still
  be discarded (e.g. due to rate limiting).
* Fluent API calls may throw runtime exceptions for bad arguments, but if they do then the
  corresponding "no-op" method must also do the same checks. This avoids situations where enabling
  additional logging causes unexpected new problems to appear.

### Terminal Log Method

* The terminal log method is required to complete a fluent log statement.
* The log method is either one of the overloads for
  [`log()`]({{site.LoggingApi}}#log(java.lang.String,java.lang.Object)) or the special case
  [`logVarargs()`]({{site.LoggingApi}}#logVarargs(java.lang.String,java.lang.Object[])) method.
* This triggers post-processing of the log context, which tests for rate limiting and other stateful
  behaviour.
* If the log statement is not discarded during post-processing, the logged arguments are packaged
  into the context (including lazy argument evaluation), which is then passed to the logger backend.
* If the log statement called was one of the overloads which avoids auto-boxing or varargs array
  creation, but is discarded during post-processing, then it does not incur the cost of packaging
  log arguments.
* This ensures that as little work is done as possible until it can be determined that the log
  statement will definitely be emitted.

{: .note}
> One consequence of having post-processing come before argument packaging is that log message
> arguments cannot be visible to the post-processing logic. This is probably a good thing since
> it cleanly separates arguments to be logged, which cannot affect logging behaviour, from
> metadata, which can.

## Logger Backends

Understanding Flogger's design for logging backends will help you understand some of the key design
decisions in the rest of the API, and perhaps even give you the confidence to write your own backend
implementation (which is nowhere near as hard as you might think).

{: .important}
> A well written logger backend should work with any Flogger logger implementation.

### General Responsibilities

A logger backend is responsible to accepting and processing the [`LogData`]({{site.LogData}})
instance created by a fluent log statement. It has a minimal internal API, which is agnostic to the
specifics of the user facing logging API. For example, a logging backend has no requirement to
understand features such as rate limiting and is not tied to a specific log message syntax.

A logger backend is also responsible for advertising the current log level configured by the
underlying logging system.

### Log Message Formatting and Structured Logging

Log message formatting parsing is handled entirely by the logger backend, and may even be avoided
altogether until the log entry processed in some external logs system.

While the default [`FluentLogger`]({{site.FluentLogger}}) implementation uses Java's
*printf* style message syntax (exactly the same as `String.format()`), different logger
implementations can supply their own syntax parsers to the backend. See
[`MessageParser`]({{site.MessageParser}}),
[`PrintfMessageParser`]({{site.PrintfMessageParser}}) and
[`BraceStyleMessageParser`]({{site.BraceStyleMessageParser}}) for details.

If a backend uses the provided [`MessageParser`]({{site.MessageParser}}) from the logger it's
attached to, it need not care what the user facing syntax was. See
[`SimpleMessageFormatter`]({{site.SimpleMessageFormatter}}) for a basic example of how to handle
parsing of log messages without making assumptions about format syntax.

### Metadata Processing

When a logger backend is given metadata (in the form of the [`Metadata`]({{site.Metadata}})
attached to [`LogData`]({{site.LogData}}), or contextual metadata extracted via
`getContextDataProvider()` on the [`Platform`]({{site.Platform}}) class, it can choose to interpret
some of that data in a special way, but it should always accept any metadata from the user.

In general, a backend can choose to either handler known metadata explicitly, ignore it, or format
it as part of the "context" section using the default format mechanism.

{: .important}
> By default, unknown metadata should always be formatted as part of the context section to avoid
> losing information. Only ignore metadata that you know is explicitly okay to ignore.

An example of explicitly handled metadata is the "cause" attached to a log statement (almost all log
systems have a built in concept of this). When receiving metadata with the key
[`LogContext.Key.LOG_CAUSE`]({{site.LogContext-Key}}#LOG_CAUSE), the value can be used at the cause
in the underlying log entry that's created, and the key can be ignored for further metadata
processing.

In general, metadata that's not ignored should be formatted and added to the outgoing log entry
somewhere. For example, this is what happens to the metadata for rate limited log statements.

This means that even if a backend doesn't know about a piece of metadata from a new logger
implementation, it will not silently discard it.

### Forced Logs

Another function of the logger backend is to handle "forced" logs. Forced logging is intended to
bypass rate limiting, but can also be used to temporarily change the effective log level to emit
additional logs (this is especially useful during tests). A backend should attempt to allow forced
logs to be emitted *without* having to change the underlying log level (since that would affect all
logs emitted at the same time). Exactly how this happens if dependent on the logging subsystem the
backend uses.
