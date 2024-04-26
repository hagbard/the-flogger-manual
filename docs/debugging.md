---
layout: page
title: "Debugging With Flogger"
permalink: /debugging/
nav_order: 45
---

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
- TOC
{:toc}
</details>

## Logging Configuration And Debugging

The purpose of debug logging is to aid debugging. This sounds rather obvious, but it's important to
remember this when thinking of logging configuration, because logging configuration is only valuable
if it helps achieve that goal.

Because "finer" log statements most often appear in inner code (e.g. loops, innermost logic), and
often have larger payloads with more detail in, when they are enabled they typically account for a
disproportionately large fraction of log output.

{: .note}
> As a rule-of-thumb it was determined that, for large Java applications at Google, each additional
> level of "fine" logging that was enabled would yield approximately 10-times more log output. This
> results in apporoximately 1000x more log output for the most verbose log level.

This means that in large user-facing production applications handling large numbers of simultaneous
requests, it is often infeasible to simply enable finer log levels for classes or packages when
debugging as it can slow down latency sensitive systems too much.

Faced with the prospect of a hundred, or even a thousand, times more log output, it is no longer
sufficient to think of just enabling more logging in classes or packages when debugging production
systems. A different approach needs to be found for effective debugging with logs.

## A Change Of Context

Part of the problem comes from the fact that the original JDK logging system design is nearly 30
years old, and came from a time when high performance cloud based servers were much less common. For
most "Java applications", simply turning on more logging for some fraction of classes would have
been a viable way debug issues, whereas now it can often be impractical or impossible.

Furthermore, as application size and complexity has grown, it is no longer straightforward to
isolate specific parts of an application's logic as belonging to some code location, making it hard
to know for which classes or packages extra logging will be useful during debugging.

A useful way to think about log events is as a point in a 2-dimensional space, with time and code
location as the axes. The logs you collect are points scattered across this space.

### Log Events As 2D-Coordinates

In the following diagrams we show the log events for some ficticious (and not very busy) application
serving user requests. The vertical axis shows four distinct code locations (classes or
packages, `A` to `D`) for which logging configuration can be changed, though it's important to note
that in this visualization there's no meaningful measure of vertical distance between locations.

The colors of the points represents the severity of the log event, and by default we imaging that
the log system is set to emit logs at level "INFO" or the equivalent. Initially only INFO (green),
WARNING (orange) and ERROR (red) logs are shown.

Let's imagine we want to debug the error marked on the right hand side, a log event in location `B`.

<details>
  <summary>Basic Visualization (Click to open)</summary>
  <img src="{{site.baseurl}}/assets/log_events.svg">
</details>

### Increasing Log Output By Location

We might consider enabling more logging (e.g. the typical "FINE" or "DEBUG" log level) for the
location in which the error occurred, but what does this tell us about its cause?

While some bugs may be caused by code in the immediate vicinity of the reported error, this will
often only be for the bugs which are easiest to diagnose. The bugs which are typically hardest to
diagnose are those for which the cause is "a long way" from the point at which the error is
reported, and these are the bugs for additional log information is most important.

<details>
  <summary>Showing More Debug Logs (Click to open)</summary>
  <img src="{{site.baseurl}}/assets/apparent_trace.svg">
</details>

### Correlation Is Not Causation

While the increase in logging for the location associated with the error appears to give a lot more
information for debugging, this can be somewhat of an illusion. In modern server based applications,
there are typically many requests flowing though the same code at the same time. Furthermore, to
imnprove scalability, most modern applications are designed to isolate concurrent requests from each
other as much as possible.

Thus, if a system handles 100 simultaneous queries, and only one of these "bad requests" causes an
error, then enabling more logging in the vicinity of the error will just result in orders of
magnitude more log events, but mostly for queries which do not encounter the issue, and have no
effect on the ones which do.

In most logging systems it's possible to attach unique IDs to some, or all, of the log statements
emitted for a request. If we highlight the trace of a request through the log event space, we see a
clearer picture of where enabling additional logging might be important.

<details>
  <summary>Tracing log events for a single request (Click to open)</summary>
  <img src="{{site.baseurl}}/assets/limited_trace.svg">
</details>

### Diagnose Behaviour, Not Location

As we can see from the previous example, log events related to a task we are debugging will trace
through many locations prior to the point where the error occurs. However, it would still often be
prohibitive to enable all logging, even this subset of locations in a production system.

However, if we could enable all logging just for the trace of the request which encounters an error
we would gain the maximal amount of debugging information for it, but this is only possible once the
error can be triggered somewhat reliably.

<details>
  <summary>All log events of single task</summary>
  <img src="{{site.baseurl}}/assets/logging_trace.svg">
</details>

## Conclusion

If turning on finest logging for all requests in a location is infeasible, but we don't know in
advance which locations to enable additional logging for, then we can think of debugging as a
multi-stage process.

1. Globally configure logging for "info" level and above by default.
2. Apply task/request IDs to log events and determine the likely locations for which additional
   logging will be useful by looking at the "info" level traces for problematic requests.
    * Flogger makes this easy using `ScopedLoggingContext`, which automatically adds metadata to all
      Flogger logger statements within a defined context.
3. When an error occurs, enable more debugging at these locations and attempt to determine how to
   reproduce an error reliably with this additional information.
    * Due to the often prohibitive cost of enabling "finest" logging, this step will likely only
      enable some additional logging (i.e. "fine", not "finest").
    * When using Flogger, this can be achieved using the standard configuration mechanism for the
      underlying logging system.
4. Once the error can be triggered reliably, enable "finest" logging for any problematic requests to
   get the maximum amount of debug information without generating an excessive amount of log output.
    * Flogger makes this easy by allowing a "log level map" to be applied to
      a `ScopedLoggingContext`, and thus only enables "finest" logging for that request, and avoids
      excessive log output.
    * As a bonus, when using the feature, Flogger will bypass any rate limiting on all emitted log
      statements for that context, to ensure nothing is missed.

{: .note}
> With this approach in mind, it becomes much less important to control log levels for locations
> with fine granularity, as you would normally only need to enable a moderate amount of additional
> logging for some set of packages, rather than controlling log levels on a per class basis.

Learn more about how [logger backend naming](next_backend.md) in Flogger Next works well with this
approach, while reducing class initialization costs.
