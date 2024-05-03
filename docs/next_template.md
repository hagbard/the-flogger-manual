---
layout: page
title: "Next: String Templates"
nav_order: 51
---

# String Templates

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
- TOC
{:toc}
</details>

## Introduction

With JDK 21, Java now offers the ability to express formatted strings via the String Template
syntax. This lets you inline expressions into strings directly, such as:

<!-- @formatter:off -->
```java
String message = "Add: x=\{ x } + y=\{ y }: x+y=\{ x + y }"
```
<!-- @formatter:on -->

For some use cases this can make code more readably compared to alternatives such as:

<!-- @formatter:off -->
```java
String message = String.format("Add: x=%d + y=%d: x+y=%d", x, y, x + y);
```
<!-- @formatter:on -->

This mechanism now is integrated into Flogger Next's [`FluentLogger`]({{site.next.FluentLogger}})
implementation using the form:

<!-- @formatter:off -->
```java
logger.atInfo()."Add: x=\{ x } + y=\{ y }: x+y=\{ x + y }".log();
```
<!-- @formatter:on -->

It's as simple as that, and even works with multiline string literals!

## String Format vs String Templates

One downside to the default string template behaviour in Java is that there's no nice way to do
things like hexadecimal formatting of values. This is especially important for logging where you
want to defer the work of formatting.

<!-- @formatter:off -->
```java
// Even though template formatting is deferred, argument evaluation is not!
//                                     |-- always evaluted! --|
logger.atFine()."decimal=\{ x }, hex=\{ Integer.toHexString(x) }".log();
```
<!-- @formatter:on -->

While you can achieve this with the use of Flogger `LazyArg` mechanism, it's not ideal because
people may still forget to wrap formatted values.

<!-- @formatter:off -->
```java
//                                       |-- lazily evaluated, but messy! --|
logger.atFine()."decimal=\{ x }, hex=0x\{ lazy(() -> Integer.toHexString(x)) }".log();
```
<!-- @formatter:on -->

Luckily, the String Template mechanism allows for different template processors to be used, and one
of the provided processors permits optional String Formatter compatible directives. This is the
processor used by Flogger Next's [`FluentLogger`]({{site.next.FluentLogger}}), and it allows you to
write:

<!-- @formatter:off -->
```java
// Apply '%#x' to the next value ----vvv 
logger.atFine()."decimal=\{ x }, hex=%#x\{ x }".log();
```
<!-- @formatter:on -->

{: .note}
> There are a couple of minor differences between `String.format()` however, since indexed (`%3$s`)
> and relative (`%<s`) directives are not supported, due to the lack of an indexable argument list.
> However, as almost nobody ever uses these directives, this is unlikely to be a problem.

## Java vs Kotlin String Templates

String Templates in Java are similar to Kotlin's string templating mechanism, but have one vital
advantage for logging; Java String Templates can be lazy! This is essential for efficient log
statements which must do little or no work when disabled.

In Kotlin the expression:

<!-- @formatter:off -->
```kotlin
log.fine("fn(${ x }) = ${ fn(x) }");
```
<!-- @formatter:on -->

will always evaluate the string, including the potentially expensive call to `fn(x)` before calling
the log method. This makes it completely unsuitable for logging APIs as the vast majority of log
statements reached in code are disabled by default.

In Java however, the use of "template processors" allows string template evaluation to be deferred,
which is essential for Flogger.

{: .note}
> Even though template evaluation is deferred, the template itself must still be allocated even for
> disabled log statements, so while using String Templates in Flogger Next is "cheap", it's still
> some work compared to just passing a String Formatter literal to a normal log method.

## Installation

<!-- @formatter:off -->
```xml
<dependency>
  <groupId>net.goui.flogger.next</groupId>
  <artifactId>logger</artifactId>
  <version>${flogger-next.version}</version>
</dependency>
```
<!-- @formatter:on -->

Ensuring the JDK String Template mechanism is enabled (this step will eventually become
unnecessary):

<!-- @formatter:off -->
```xml
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <source>21</source>
          <target>21</target>
          <compilerArgs>--enable-preview</compilerArgs>
        </configuration>
      </plugin>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
        <configuration>
          <argLine>--enable-preview</argLine>
        </configuration>
      </plugin>
      ...
    </plugins>
  </pluginManagement>
</build>
```
<!-- @formatter:on -->
