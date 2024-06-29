# Metaprogramming

<!-- TOC -->
* [Metaprogramming](#metaprogramming)
  * [Inspirations](#inspirations)
  * [Solution in Koffect](#solution-in-koffect)
    * [Motivation](#motivation)
    * [Initial Thoughts](#initial-thoughts)
<!-- TOC -->

> The metaprogramming story/solution in Koffect is not finalized. Thoughts and notes are listed below.
> Do not take this as official documentation as it is subject to change as the language matures.

Metaprogramming, while a loosely defined term and paradigm, is quite prevalent in modern programming. Metaprogramming can
have many benefits (improving readability, generating boilerplate code, reducing code repetition), but is a double-edged
sword having many downsides as well (increasing compilation times, generating code may be inflexible, incompatibility with
other language features). As metaprogramming is so loosely defined, Koffect must define its own terminology for metaprogramming.
This document's purpose is not only to describe the metaprogramming story/solution in Koffect (if one is to exist at all)
but to also determine if a metaprogramming story/solution has a place within the design of Koffect and if so, draw the 
boundaries on what will be considered programming versus metaprogramming and determine how powerful the metaprogramming
capabilities will be.

## Inspirations

- JVM Annotations
  - [Java](https://docs.oracle.com/javase/tutorial/java/annotations/)
  - [Kotlin](https://kotlinlang.org/docs/annotations.html)
    - [KSP](https://kotlinlang.org/docs/ksp-overview.html)
    - [Kapt](https://kotlinlang.org/docs/kapt.html)
  - [Scala](https://docs.scala-lang.org/tour/annotations.html)
  - [Groovy](https://groovy-lang.org/objectorientation.html#_annotations)
- Kotlin
  - [Kotlin DSLs](https://kotlinlang.org/docs/type-safe-builders.html) 
  - [Kotlin `inline` modifier](https://kotlinlang.org/docs/inline-functions.html)
  - Kotlin Compiler Plugins
- [Scala Metaprogramming](https://docs.scala-lang.org/scala3/reference/metaprogramming/)
  - [Scala Context Functions (Builder Pattern)](https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html#example-builder-pattern-1)  
  - [Scala `inline` modifier](https://docs.scala-lang.org/scala3/reference/metaprogramming/inline.html)
  - [Scala `comptime` package](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html)
  - [Scala macros](https://docs.scala-lang.org/scala3/reference/metaprogramming/macros.html)
  - [Scala Runtime Multi-Stage programming](https://docs.scala-lang.org/scala3/reference/metaprogramming/staging.html)
  - [Scala Reflection](https://docs.scala-lang.org/scala3/reference/metaprogramming/reflection.html)
  - [Scala TASTy Inspection](https://docs.scala-lang.org/scala3/reference/metaprogramming/tasty-inspect.html)
- [Clojure Macros](https://clojure-doc.org/articles/language/macros/)
  - General Lisp style quoted macros
- [Groovy Metaprogramming](https://groovy-lang.org/metaprogramming.html)
  - [Groovy DSLs](https://docs.groovy-lang.org/docs/latest/html/documentation/core-domain-specific-languages.html)
- [Rust Macros](https://doc.rust-lang.org/book/ch19-06-macros.html)
  - [Rust Procedural (Proc) Macros](https://doc.rust-lang.org/reference/procedural-macros.html)
- Crystal Metaprogramming
  - [Crystal Annotations](https://crystal-lang.org/reference/1.12/syntax_and_semantics/annotations/index.html)
  - [Crystal Macros](https://crystal-lang.org/reference/1.12/syntax_and_semantics/macros/index.html)
- [C/C++ preprocessor](https://en.cppreference.com/w/cpp/preprocessor)
- C++ compile time operations
  - [constexpr](https://en.cppreference.com/w/cpp/language/constexpr)
  - [consteval](https://en.cppreference.com/w/cpp/language/consteval)
- [Nim Macros](https://nim-lang.org/docs/macros.html)
- [Zig `comptime`](https://ziglang.org/documentation/master/#comptime)
- Ruby Metaprogramming
  - Ruby "hook" methods

## Solution in Koffect

### Motivation

> TODO

### Initial Thoughts

> TODO