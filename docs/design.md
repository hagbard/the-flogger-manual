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
1. TOC
{:toc}
</details>


## Log Statement Flow

The basic flow of a Flogger log statement can be illustrated as:

<img src="{{ site.baseurl }}/assets/log_statement_flow.svg">

### Level Selector

* A level selector is always the first method in a fluent log statement.
* This method returns the [`LoggingApi`]() instance on which further methods are invoked.
* In normal use this would be one of the named [`atXxxx()`]() methods, but you can also specify the 
  log level dynamically via [`at(Level)`]() (though this is generally discouraged).
* If the requested level is not enabled by log level (according to the backend), then a singleton 
  "no-op" instance is returned. This has the same API, but it is stateless and all its methods are 
  essentially empty. This allows Flogger to avoid any further work for disabled log statements.

### Fluent API Chain

* The fluent API chain is an optional sequence of fluent API calls from the [`LoggingApi`]().
* Fluent API calls typically just add metadata to the [`LogContext`]().
* It is important that little or no work is done in these methods since the log statement may 
  still be discarded (e.g. due to rate limiting).
* Fluent API calls may throw runtime exceptions for bad arguments, but if they do then the 
  corresponding "no-op" method must also do the same checks. This avoids situations where 
  enabling additional logging causes unexpected new problems to appear.

### Terminal Log Method

* The terminal log method is required to complete a fluent log statement.
* The log method is either one of the overloads for [`log(String, Object...)`](), 
  or the special case [`logVarargs(Object[])`]() method.
* This triggers post-processing of the log context, which tests for rate limiting and other 
  stateful behaviour.
* If the log statement is not discarded during post-processing, the logged arguments are packaged 
  into the context (including lazy argument evaluation), which is then passed to the logger backend.
* If the log statement called was one of the overloads which avoids auto-boxing or varargs
  array creation, but is discarded during post-processing, then it does not incur the cost of
  packaging log arguments.
* This ensures that as little work is done as possible until it can be determined that the log 
  statement will definitely be emitted.

{: .note}
> One consequence of having post-processing come before argument packaging is that log message
> arguments cannot be visible to the post-processing logic. This is probably a good thing since 
> it cleanly separates arguments to be logged, which cannot affect logging behaviour, from 
> metadata, which can.

