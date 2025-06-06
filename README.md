# Koffect Programming Language

<!-- TOC -->
* [Koffect Programming Language](#koffect-programming-language)
  * [Koffect](#koffect)
  * [Language Goals](#language-goals)
  * [How to Install](#how-to-install)
    * [Build from Source](#build-from-source)
  * [How to Use](#how-to-use)
  * [Syntax Examples](#syntax-examples)
  * [Feature List](#feature-list)
  * [Inspirations / Prior Arts / Similar Features](#inspirations--prior-arts--similar-features)
<!-- TOC -->

## Koffect

Welcome to Koffect!

A research language focusing on the applications of context-oriented/context-aware programming
to general purpose programming. From applications of contextual rigor (such as mathematical proof writing), to resource
management (such as opening and closing files/connections/sockets), to domain specific languages, programming contains
a lot of implicit contexts and mental context management. This project aims to create a language around the concept of
explicit contexts to reduce the mental gymnastics and boilerplate needed to maintain context dependent code.

## Language Goals

The goal of this language is to research and implement a general purpose programming language specializing in
context-oriented programming.

Some areas of focus that this project hopes to tackle are:

- The application of context-oriented programming to tackle metaprogramming challenges
- The application of context-oriented programming to ensure code correctness, through the modelling of context dependent
  fields such as mathematical correctness or asynchronous code
- The application of context-oriented programming to model real world contextual dependent situations in code with
  minimal boilerplate
- The application of context-oriented programming to the creation and modelling of domain specific languages

## How to Install

### Build from Source

> TODO: Gradle scripts not finalized

## How to Use

> TODO: Gradle scripts not finalized

## Syntax Examples

> TODO: Finalization of language in progress

## Feature List

> TODO: Finalization of language in progress

## Inspirations / Prior Arts / Similar Features

- [Effects Bibliography](https://github.com/yallop/effects-bibliography)
    - Collection of relative literature applying to algebraic effects (and coeffects by proxy)
- [Koka](https://koka-lang.github.io/koka/doc/index.html)
    - Language implementing algebraic effect handlers with lexical effect tracking
- [Eff](https://www.eff-lang.org/)
    - Language implementing algebraic effect handlers working on general first-class computational effects
- [Effekt](https://effekt-lang.org/)
    - Language implementing algebraic effect handlers with effect safety and effect polymorphism following Scala syntax
- [Coeffect](https://tomasp.net/coeffects/)
    - Language implementing algebraic coeffects through implicit parameters
- [Flix](https://flix.dev/)
    - Language implementing algebraic effect handlers with effect polymorphism, effect exclusion, and associated effects
      targeting the JVM
- [OCaml Multicore](https://github.com/ocaml/ocaml)
    - Ocaml 5.0 Multicore added unchecked effect handlers for concurrent programming
- [Kotlin Context Receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
    - Kotlin 1.6.20 added experimental support for functions with multiple receivers, also coined as context receivers
    - Kotlin updated the design proposal to [Context Parameters](https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md)
- [Scala contextual/implicit parameters](https://docs.scala-lang.org/tour/implicit-parameters.html)
    - Scala 2 has contextual/implicit parameters which were later refined in Scala 3 to use the `using`/`given` syntax
- [Haskell implicit parameters](https://ghc.gitlab.haskell.org/ghc/doc/users_guide/exts/implicit_parameters.html)
    - Haskell implements implicit parameters as part of type system leading to full type system security with implicit
      parameters
- [Python Context Managers](https://docs.python.org/3/reference/datamodel.html#context-managers)
    - Python implements context handlers with the `__enter__` and `__exit__` methods to model runtime defined contexts
- [Ruby Refinements](https://ruby-doc.org/core-3.1.0/Refinement.html)
    - Ruby implements lexical monkey patching (introduction of a context into a scope) with refinements
- [Javascript Explicit Resource Management](https://github.com/tc39/proposal-explicit-resource-management)
    - Javascript TC39 proposal (currently) in stage 3 of adding resource managers using the `using` keyword
      and `Symbol.dispose` method
- [C# `using` and `IDisposable`](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/statements/using)
    - C# resource management through the use of the `IDisposable` interface and `using` keyword
- [Java try-with-resource and `AutoCloseable`](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
    - Java resource management through the use of the `Closeable` and `AutoCloseable` interfaces with the `try` keyword