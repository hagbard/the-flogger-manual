---
layout: page
title: Testing Logs
permalink: /testing/
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

## Testing logs properly is hard

An all too familiar story...

{: .highlight}
> We've all been there; decided to be a good citizen and test our debug logs. Perhaps we added a
> bunch of new logs during debugging, or maybe we discovered that the existing logs weren't really
> very useful.
>
> Then we find the existing test code for the class and look for the existing logging tests. Are
> their any? Do they pass? If you changed existing log statements, how many of them did you break?
>
> At some point you ask yourself if it's even really worth adding the logging tests if the existing
> code isn't well tested. Maybe it's fine to just leave it as it is?
>
> Or maybe you're on the other side of the fence; an engineer tasked with finding the cause of an
> urgent issue. You enable extra logging to see what's going on, but now tasks start having errors
> left and right! What's going on?!
>
> You realize that by enabling extra logging you just introduced a whole bunch of previously
> untested code paths into the production system. Insufficiently tested classes are now failing in
> `toString()`, or perhaps memory usage just spiked because you're suddenly outputting so many more
> logs. Thread contention for locks increases as large, mutable objects get locked during 
> formatting, and what was a smoothly running concurrent system is suddenly spluttering to a halt.
>
> <center>And you haven't even started to debug the issue you came here for!</center>

Or to put it another way:

{: .note}
> "*When you are up to your ass in alligators, it's hard to remember that you started
> out to drain the swamp*" -- Robert Anton Wilson

Good logging hygiene and good logs testing hygiene go hand in hand. Software engineers need to
be encouraged to write good debug logs, but they also need to be able to write and maintain tests 
for them. Without an easy-to-use logging API, combined with an easy-to-use logs testing API, 
logs testing is likely to be an afterthought during development.

Unlike most other programming statements, log statements never return a value which can be 
checked or create an observable state change within the program. A log statement should never 
affect, or be affected by the surrounding code, whether enabled or not. Apart from the overhead 
of logging in terms of additional allocations and time, the system should be essentially unaffected.
This means that many of the normal approaches to unit testing simply don't apply to testing logs.

Furthermore, most logging system (JDK logging, Apache LOG4J etc.) were initially designed before 
the advent of highly scalable data center environments for running code. Applications ran on a 
few machines and enabling extra logging for debugging or testing was less of a risk. Logger
configuration was simple, largely static, and operated on the "structural scope" of classes and
packages. However, if your system is running across thousands of machines, serving low latency 
request, and outputting gigabytes of logs a day, you can't just "turn up" the logging without 
facing some serious issues.

{: .note}
> Anecdotally, from my time at Google, a good rule of thumb is that for each level of additional
> logging you enable (e.g. `INFO⟶FINE` or `FINE⟶FINER`) you get approximately 10-times
> more log output. Finer logs typically run more often in innermost loops and usually log
> larger payloads. Not many production systems will cope well with being hit by >100-times more log
> output.

But on the flip side of this, fine-grained logging is potentially very valuable, and when done
properly it can help you identify issues quickly. Flogger supports `ScopedLoggingContext`,
`LogLevelMap` and `Metadata` to let you control logging within "temporal scopes" (single
requests or sub-tasks), which gives you much more freedom to enable additional logging only
where you need it without overburdening your system.

But with this new freedom to enable highly targeted debug logs, it becomes more important that
the code doing the logging is well tested. Tests need to exercise code with and without debug
logging enabled, but not be slowed down with excessive log output.

{: .important}
> It should be easy to write good logging tests which aren't brittle in the face of simple changes,
> such as moving log statements between methods/classes, or small changes to existing log
> statements.

## Help is at hand

Having recognized the difficulty and issues caused by insufficient or overly brittle logs
testing over many years at Google, I decided to finally do something about it.

Introducing the [Flogger Logs Testing Library](https://github.com/hagbard/flogger-testing). An
easy to use, readable, powerful API for logs testing. It's designed for Flogger but will work
with other logging libraries (with a slightly degraded set of features).

Would you like to be able to write readable log assertions like:

<!-- @formatter:off -->
```java
logs.assertLogs().withLevelAtLeast(WARNING).always().haveMetadata("request_id", REQUEST_ID);
```
<!-- @formatter:on -->

or:

<!-- @formatter:off -->
```java
var taskStart =
    logs.assertLogs().withLevel(INFO).withMessageContaining("Task", "[START]").getOnlyMatch(); 
logs.assertLogs(after(taskStart).inSameThread()).withLevel(WARNING).doNotOccur();
```
<!-- @formatter:on -->

which work for multiple logging backends without modification,

How about writing tests which can trivially test additional logging over the same code.

In the first test, logging is set to the default for the test class (e.g. "INFO") and we don't test
all the fine logs that might appear (in normal use fine logs would not even be captured):

<!-- @formatter:off -->
```java
@Test
public void testSuccessfulTask_infoLogs() {
  // In normal execution, no warning logs occur.
  logs.verify(assertLogs -> assertLogs.withLevel(WARNING).doNotOccur());

  // On success we see a sequence of INFO logs summarizing normal operation.
  var result = ClassUnderTest.doTestTask(goodRequest);
  assertThat(result.status()).isEqualTo(Task.SUCCESS);
  // Now do the usual testing of "info" logs for a successful operation...
  ...
}

@Test
@SetLogLevel(scope = CLASS_UNDER_TEST, level = FINEST)
public void testSuccessfulTask_enableAllLogs() {
  // Assert that the task completed as expected with all logging enabled. Knowing that all logging
  // code can be enabled without causing more problems is very valuable.
  var result = ClassUnderTest.doTestTask(goodRequest);
  assertThat(result.status()).isEqualTo(Task.SUCCESS);

  // Extract the subset of debug logs we care about testing (we tested "info" logs above).
  var debugLogs = assertLogs.withLevelLessThen(INFO);
  // Without checking the details, assert that an expected number of logs occurred.
  debugLogs.matchCount().isAtLeast(30);
  // Perhaps also test a specific logs policy (e.g. not using "fine" logs to report exceptions).
  debugLogs.never().haveCause();
  ...
}
```

And now an additional test, which runs the same code but triggers a failure:

<!-- @formatter:off -->
```java
@Test
@SetLogLevel(scope = CLASS_UNDER_TEST, level = FINE)
public void testFailedTask_debugLogs() {
  // On failure we see an initial warning followed by numerous FINE log statements, which we expect
  // to all have the correct task ID attached (among other things).
  ClassUnderTest.doTestTask(badRequest);

  var firstWarning =
    logs.assertLogs().withLevel(WARNING).withMessageContaining("[FAILED]", BAD_VALUE).getMatch(0);
  // Extract a subset of the logs after a specific event.
  var fineLogs = logs.assertLogs(after(firstWarning).inSameThread()).withLevel(FINE);
  fineLogs.matchCount().isAtLeast(10);
  // Assert that the logs we care about have good metadata to help debugging.
  fineLogs.always().haveMetadata("task_id", BAD_TASK_ID);
  // Perhaps also test some specific expectations about what should not be in these logs.
  fineLogs.withMessageContaining("load", "path=").never().haveMessageContaining("Access Denied");
  ...
}
```
<!-- @formatter:on -->

{: .note}
> Note how, if "FINE" logs are modified or new ones are added, this test is not brittle.

Having seen these simple examples:
* How much code would it take you to write equivalent tests with your current logs testing API?
* Have you considered writing these sort of logging tests before?
* Do you even have a standard logs testing API?

## Summary

If the idea of powerful, readable, easy to maintain logging tests appeals to you, learn more at
[https://github.com/hagbard/flogger-testing](https://github.com/hagbard/flogger-testing).

{: .note}
> What's more, this framework still works, in a more limited way, if you're just using JDK logging
> or Log4J directly. Test fixtures (e.g. `FloggerTestRule`) can still be installed, and logs are 
> still captured and can be tested, but you'll have to manage setting log levels yourself.

Install the logs testing API and get started today:

<!-- @formatter:off -->
```xml
<!-- https://mvnrepository.com/artifact/net.goui.flogger-testing.junit4 -->
<dependency>
    <groupId>net.goui.flogger-testing</groupId>
    <artifactId>junit4</artifactId>  <!-- or unit5 -->
    <version>${flogger-testing-version}</version>
    <scope>test</scope>
</dependency>
```
<!-- @formatter:on -->

And if you're using `Log4J`:

<!-- @formatter:off -->
```xml
<!-- https://mvnrepository.com/artifact/net.goui.flogger-testing.logj4 -->
<dependency>
    <groupId>net.goui.flogger-testing</groupId>
    <artifactId>log4j</artifactId>
    <version>${flogger-testing-version}</version>
    <scope>test</scope>
</dependency>
```
<!-- @formatter:on -->

{: .note }
> At the time of writing, the latest Flogger testing library version is `1.0.3`{: style="color: red"}.
