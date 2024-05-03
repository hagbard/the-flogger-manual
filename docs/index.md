---
layout: home
---

<!-- @formatter:off -->
# The Flogger Manual
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
<!-- @formatter:on -->

[Flogger](https://github.com/google/flogger) is Google's open-source Java debug logging library.

It is simple to use, and comes with some powerful features which sets it apart from many other
logging APIs. Flogger is used extensively within Google to satisfy the needs of many hundreds of
Java and Kotlin projects, and is the only Java logging API supported by the core Java libraries
team.

Flogger makes extensive use of the "fluent API" design pattern to provide an intuitive and
feature-rich API, which can improve both the readability of log statements within code and the value
of the emitted logs.

With advanced features such as in-built rate limiting, structured metadata, scoped logging contexts
and targeted log-level control, it gives users the tools they need to get the best from debug logs
in small or large scale applications.

Compatible with the JDK logging system, Log4J and other common logging frameworks, Flogger provides
a powerful alternative for Java/Kotlin developers.

---

## About This Manual

This manual will explain how to use Flogger to its full potential, as well as shed light on some of
the decisions that went into designing the API.

You can experiment with Flogger by installing the code samples in this project, which are linked
from the appropriate sections in the manual. Sections are listed in order of increasing complexity,
with more advanced topics building on earlier concepts.

In addition to explaining the basic Flogger library, this manual also describes:

1. The Flogger Next project, which provides artifacts for:
    * Extending Flogger's core API to utilize the new String template syntax (JDK 21 or above).
    * Improved logger backends, with better integration and customizable plugins.
2. The Flogger Testing project which provides an intuitive API for easily unit testing logging
   (either from Flogger or other loggers).

This manual is unofficial, but has been written by Flogger's original creator, and is more thorough
than the [official documentation](https://github.com/google/flogger) in many places.

---

## About The Author

David Beaumont started Flogger as a one-person 20% project in Google in 2012. It is now the only
supported Java debug logging API in Google and serves the needs of hundreds of projects across many
millions of log statements.

David implemented, or oversaw, every aspect of Flogger's design and implementation, as well as
leading a multi-year tool-driven migration of millions of log statements from existing logging APIs.
This was all completed without any disruption to production system.
