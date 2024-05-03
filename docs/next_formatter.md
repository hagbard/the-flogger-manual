---
layout: page
title: "Next: Custom Formating"
nav_order: 53
---

# Custom Formating

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
- TOC
{:toc}
</details>

## Introduction

Probably the single biggest omission from the version of Flogger released by Google is a lack of
configurable message format. Flogger Next fixes this by providing logger backend implementations
which can take advantage of an easily configured customizable message formatter.

To change the message format for Flogger backends, simply provide
a [Flogger Next option](next_options) of the form:

```properties
flogger.message_formatter.pattern=<message template>
```

{: .note}
> This will define the message format for Flogger, but not affect any additional formatting added
> by the underling logging system. As such, it might also be beneficial to modify the underlying
> formatting to avoid duplicate message elements etc.

## Message Pattern Syntax

The message syntax is based on simple token substitution, where known tokens (e.g. `%{message}`)
are replaced by the specific element value, and other text is treated literally.

A simple example might be:

```properties
flogger.message_formatter.pattern=${message} %{metadata}
```

However, for tokens with optional values (e.g. metadata keys which may not be set for a specific log
event) there is the ability to set a prefix and suffix for a token which will only be emitted if the
token is substituted. This makes it easy to add a bit of structure to log messages while avoiding
additional separators or empty lists (e.g. `[]`).

This is achieved by adding optional prefix and suffix text in the token descriptor, such
as `%{token-name/prefix-text}` or `%{token-name/prefix-text/suffix-text}`

For example, to avoid having a trailing space in the above example, it is better to write:

```properties
flogger.message_formatter.pattern=${message}%{metadata/ }
```

And to wrap metadata with array-like formatting with `[`, `]`:

```properties
flogger.message_formatter.pattern=${message}%{metadata/ [/]}
```

{: .note}
> Prefix and suffix text is arbitrary, and you can use `\` to escape meta-characters such as
> `}`, `/` and `\` itself.

## Known Tokens

The formatting of token values is handled by a series of plugin classes. This permits common cases
to be handled by configuring the default plugin, but allows most plugins to be replaced, if needed,
by user code.

### Tokens With Non-Optional Values

The following tokens always emit a value, so prefix or suffix text is not allowed.

* `%{message}`:
    * The formatted base message passed to the `log()` method. This is formatted as per Java's
      String Formatter syntax and cannot be customized directly.
    * This is a built-in format, and cannot be replaced with a plugin.
* `%{timestamp}`:
    * The timestamp of the log event. By default, this is formatted according to ISO-8601 format by
      default, but this can be modified via the `flogger.message_formatter.timestamp.pattern`
      option.
    * See [DefaultTimestampFormatter]({{site.next.DefaultTimestampFormatter}}) for more details.
    * This plugin can be changed via `flogger.message_formatter.timestamp.impl`
* `%{level}`:
    * The level of log event. By default, this is formatted using the inbuilt name (e.g. "INFO")
      but can be set to use the internationalized name via
      the `flogger.message_formatter.level.use_localized_name` option.
    * See [DefaultLevelFormatter]({{site.next.DefaultLevelFormatter}}) for more details.
    * This plugin can be changed via `flogger.message_formatter.level.impl`
* `%{location}`:
    * This emits the log site information for the log event in the
      form `<class-name>#<method-name>`. Current there are no options for the default level
      formatter.
    * See [DefaultLocationFormatter]({{site.next.DefaultLocationFormatter}}) for more details.
    * This plugin can be changed via `flogger.message_formatter.location.impl`

### Tokens With Optional Values

Tokens with optional values can have prefix and suffix text provided.

* `%{metadata}`
    * This emits a sequence of space separated metadata key/value pairs in the
      form `<key-label>=<value>`.
    * Individual keys can be excluded from appearing in the output by
      listing [`MetadataKey`]({{site.MetadataKey}}) fields in
      the `flogger.message_formatter.metadata.ignore` options list (see below).
    * The behaviour of individual keys is customizable via [`MetadataKey`]({{site.MetadataKey}})
      subclasses, and not this plugin.
    * This is a built-in token and cannot be replaced with a plugin.
* `%{key.<key-name>}`
    * This emits the value of the [`MetadataKey`]({{site.MetadataKey}}) (if present) that's linked
      from the `flogger.message_formatter.metadata.key.<key-name>` option, without the key label or
      any other formatting.
    * This is a built-in token and cannot be replaced with a plugin.

## Metadata Keys

For the `%{key.xxx}` tokens and ignored keys in the `%{metadata}` token, you need to specify
a [`MetadataKey`]({{site.MetadataKey}}) by name. To do this, specify the field of the metadata key
in the form `<class-name>#<method-name>`. However, there are a couple of important notes about
metadata keys to be aware of.

1. To reference a key by name, the key must be a `public static final` field of a public class.
2. To avoid triggering unwanted class initialization or reentrant logging early in an application's
   lifetime, metadata keys should be held in a class of their own (possibly a nested class).

An example of a metadata key field suitable for loading by Flogger Next:

```java
package org.something.myapp;

import com.google.common.flogger.MetadataKey;

final class MyApplicationClass {
  /**
   * Public metadata keys for MyApplication.
   *
   * <p>Nested class to avoid static initialization when the key is loaded by the formatter.
   */
  public static final class Keys {
    /** Shows the current task ID. */
    public static final MetadataKey<String> TASK_ID = MetadataKey.single("task", String.class);
  }
}
```

With the above setup, the `TASK_ID` can be referenced via:

```
org.something.myapp.MyApplicationClass$Keys#TASK_ID
```

If a custom [`MetadataKey`]({{site.MetadataKey}}) subclass emits multiple values with non-default
labels, the value can be referenced by adding the custom label name to the end of the key
identifier.

```
org.something.myapp.MyApplicationClass$Keys#TASK_ID:my_custom_label
```

## Formatter Plugins

Almost all Flogger Next formatting, including the overall pattern formatter, is controlled via
Flogger Next's plugin mechanism. This lets implementation be replaced by specifying a class name to
an `impl` option.

A formatter plugin class must adhere to two basic API contraints:

1. Implement the [`LogMessageFormatter`]({{site.LogMessageFormatter}}) interface for appending to
   the log format buffer.
    * Since this code is always called for every log statement, the plugin code MUST NOT do any
      logging of its own or call any code which might do logging.
2. Provide a public constructor which accepts an [`Options`]({{site.next.Options}}) instance to
   configure the plugin.
    * Options passed to a plugin will be scoped to the base name of the plugin (i.e. the options
      for `flogger.message_formatter.xxxx.impl` will be scoped to `flogger.message_formatter.xxxx`).

Formatter plugins can be specified by one of the following Flogger Next options:

* `flogger.message_formatter.impl`: The overall message formatter class. If this is replaced, none
  of the other plugins will have any effect.
* `flogger.message_formatter.level.impl`: The log level formatter class.
* `flogger.message_formatter.location.impl`: The log site location formatter class.
* `flogger.message_formatter.timestamp.impl`: The log timestamp formatter class.

## Installation

JDK logging backend (replaces the `com.google.flogger:flogger-system-backend` dependency):

<!-- @formatter:off -->
```xml
<dependency>
  <groupId>net.goui.flogger.next</groupId>
  <artifactId>backend-system</artifactId>
  <version>${flogger-next.version}</version>
</dependency>
```
<!-- @formatter:on -->

Log4J 2 backend (replaces the `com.google.flogger:flogger-log4j2-backend` dependency):

<!-- @formatter:off -->
```xml
<dependency>
    <groupId>net.goui.flogger.next</groupId>
    <artifactId>backend-log4j</artifactId>
    <version>${flogger-next.version}</version>
</dependency>
```
<!-- @formatter:on -->
