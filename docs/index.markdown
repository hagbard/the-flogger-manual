---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: home
---

# The Flogger Manual

How to use Flogger effectively to improve debugging and code maintainability.

---

### About This Manual

This manual will explain how to use Flogger to its full potential, as well as shed light on some of
the decisions that went into designing the API.

[Flogger](https://github.com/google/flogger) is an open-source Java and Kotlin compatible debug
logging API  published and maintained by Google. It is simple to use but comes with some powerful
features which  set it apart from a lot of the logging APIs you might have used before.

Before reading this manual, it is probably best to read
the [official documentation](https://github.com/google/flogger).

You can experiment with Flogger by installing the code samples in this project, which are linked
from the appropriate sections in the manual. Sections in the sidebar are listed in order of
increasing complexity, with more advanced topics building on earlier concepts.

---

### About The Author

David Beaumont started Flogger as a one-person 20% project in Google in 2012. It is now the only
supported Java debug logging API in Google and serves the needs of hundreds of projects across
many millions of log statements.

David implemented, or oversaw, every aspect of Flogger's design and implementation, as well as
leading a multi-year tool-driven migration of millions of log statements from existing logging APIs.