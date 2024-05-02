---
layout: page
title: "Next: Options"
nav_order: 52
---

# Options

<details open markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
- TOC
{:toc}
</details>

## Introduction

Flogger Next supports several configurable features, and to enable these to be configured, an
options system is provided.

Flogger Next options are supplied in a form compatible with the underlying logging system used. For
the JDK logger backend, options are specified in the `logging.properties` file (or wherever logging
properties reside). For the Log4J2 backend, properties are specified in the `log4j2.xml` file, or
wherever Log4J2 options reside.

### JDK Logger Backend

For the JDK backend, options are simply in the form of Java property key/value pairs:

```properties
flogger.some.option.name=Arbitrary option value string!
```

### Log4J2 Logger Backend

For the Log4J2 backend, options are specified as Log4J properties:

<!-- @formatter:off -->
```xml
<Configuration>
    <Properties>
        <Property name="flogger.some.option.name">Arbitrary option value string!</Property>
        ...
    </Properties>
    ...
</Configuration>
```
<!-- @formatter:on -->

{: .note}
> In most examples in this documentation, the JDK form is used to show example option values due
> to its simplicity.

## Option Namespace and Layout

1. Flogger Next options are defined via dot-separated option names, starting with `flogger.`.
2. Options are always string/string pairs, though internally options are expected to have some
   predefined type when resolved (e.g. boolean, integer, etc.).
3. A list/array of options can be specified using the following pattern:
    * Provide a `.size` property (e.g. `flogger.some.list.size=3`).
    * Provide a sequence of `size` elements using zero-indexed indexices for the option name at the
      same level (e.g. `flogger.some.list.N=<some value>` for `N=0..2`).
4. Any option can be aliased by supplying a value which is the name of another option, prefixed
   with `@`.
    * Aliases can be applied at any level to define sub-hierarchies of options (e.g.
      defining `flogger.foo=@alias` and `alias.bar=First`, `alias.baz=Second` is equivalent
      to `flogger.foo.bar=First` and `flogger.foo.baz=Second`).
    * This applies to lists as well as individual values (e.g. `flogger.some.list=@my.other.values`
      and `my.other.values.size=N` etc.).
   * To specify a normal value which starts with `@`, simply prefix a second one (i.e. `@@value`).
   * Aliased properties do not need to start with `flogger.` and, to avoid any risk of clashing
      option names in the future, they probably shouldn't.
