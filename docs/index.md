---
layout: home
---

Flogger is Google's open-source Java debug logging library. It's used extensively within Google to
satisfy the needs of many hundreds of Java and Kotlin projects and is the only debug logging API
that's supported by the core Java libraries team.

Flogger makes extensive use of the "fluent API" design pattern to provide an intuitive, powerful API
which can improve both the readability of log statements within code and the value of the emitted
logs.

With powerful features such as in-built rate limiting, structured metadata, logging contexts and
targeted log-level control, it gives users the tools they need to get the best from debug logs for
small or large scale applications.

Compatible with the JDK logging system, Log4J and other common logging frameworks, Flogger provides
a powerful alternative for Java/Kotlin developers.

---

## About This Manual

This manual will explain how to use Flogger to its full potential, as well as shed light on some of
the decisions that went into designing the API.

[Flogger](https://github.com/google/flogger) is an open-source Java and Kotlin compatible debug
logging API published and maintained by Google. It is simple to use but comes with some powerful
features which set it apart from a lot of the logging APIs you might have used before.

You can experiment with Flogger by installing the code samples in this project, which are linked
from the appropriate sections in the manual. Sections are listed in order of increasing complexity,
with more advanced topics building on earlier concepts.

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
