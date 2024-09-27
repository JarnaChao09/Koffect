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
- [Swift Macros](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/macros/)
- C++ compile time operations
  - [template metaprogramming](https://en.cppreference.com/w/cpp/language/templates)
    - [SFINAE](https://en.cppreference.com/w/cpp/language/sfinae)
    - [CTAD](https://en.cppreference.com/w/cpp/language/class_template_argument_deduction)
  - [concepts](https://en.cppreference.com/w/cpp/language/constraints)
  - [constexpr](https://en.cppreference.com/w/cpp/language/constexpr)
  - [consteval](https://en.cppreference.com/w/cpp/language/consteval)
- DLang compile time operations
  - [CTFE](https://tour.dlang.org/tour/en/gems/compile-time-function-evaluation-ctfe)
  - [template metaprogramming](https://tour.dlang.org/tour/en/gems/template-meta-programming)
- Nim Metaprogramming
  - [Nim Templates](https://nim-lang.org/docs/tut2.html#templates)
  - [Nim Macros](https://nim-lang.org/docs/macros.html)
- [Zig `comptime`](https://ziglang.org/documentation/master/#comptime)
- Ruby Metaprogramming
  - Ruby "hook" methods
- Python Decorators
  - [PEP 318: Decorators for Functions and Methods](https://peps.python.org/pep-0318/)
  - [PEP 3129: Class Decorators](https://peps.python.org/pep-3129/)

## Solution in Koffect

### Motivation

Metaprogramming can have many benefits, from reducing the need to write boilerplate to complete modifications of how a
program is compiled. The following examples serve as motivations for metaprogramming in Koffect. Not every motivating 
example will not require the same degree metaprogramming power. As such, these examples hope to discover and document each
desired semantics' required metaprogramming power, advantages, and disadvantages. 

<details>
<summary><b>Domain Specific Languages</b></summary>

> TODO

</details>

<details>
<summary><b>Encoding of additional properties into the type system</b></summary>

Encoding additional properties into the type system does not necessarily entail metaprogramming. Metadata on types can
be encoded simply with marker interfaces (such as Java's [`RandomAccess`](https://docs.oracle.com/javase/8/docs/api/java/util/RandomAccess.html)
interface). However, this is simply additional information about a type (hence the name metadata). Additional properties
is a step further than just information, it is also the encoding of an (additional) API onto a type or family of types
that can then be utilized by the developer. 

A prime example of an additional property that also exposes an API is [commutativity](https://en.wikipedia.org/wiki/Commutative_property).
For an arbitrary function `foo` of type `(A, B) -> C`, the only way to invoke the function is `foo(someA, someB)`, however, 
in some cases of `foo` and cases of `A`, `B`, and `C`, `foo(someA, someB) == foo(someB, someA)` and so an equivalently
valid overload for `foo` is `(B, A) -> C`. Under the commutativity property, a singular function can be called with a multitude
of differing argument orders. An example function would be numerical addition. Let `foo = +` and `A == B == C`, then the
operation `someA + someB == someC == someB + someA` is trivially solvable (definition of `+` on integers). For when `A != B`
but `A == C || B == C`, this case can also be trivially solvable with a more robust type of `C = Dominating of A or B` 
(definition of `+` on integers with implicit widening semantics, such that `i32 + i64` will implicitly be dominated by
`i64` and therefore implicitly widened).

If the property of commutativity is encoded into the type system, the compiler may be able to better inform overload
resolution to more performant versions of functions. A prime example already of this is already seen in C++: [`std::reduce`](https://en.cppreference.com/w/cpp/algorithm/reduce)
versus [`std::accumulate`](https://en.cppreference.com/w/cpp/algorithm/accumulate). Both `reduce` and `accumulate` perform
the same operation, a `fold`, however, the difference between the two functions is the assumptions made about binary operation
performed. `reduce` requires the binary operation to be *associative* and *commutative* due to the possibility of the order
of operations being rearranged. This allows for `reduce` to be able to be trivially parallelizable and as such this is
reflected in the API of both functions: `reduce` may accept an execution policy while `accumulate` may not.

While C++ leaves this property of commutativity to be an implicit contract of the `reduce` function which if not followed
leads to undefined behavior, said property could be promoted to a contextual declaration in Koffect. Furthermore, C++
leaves the definition of the binary operation to be restricted to the first case discussed above (where the parameter and
return types must be constructively equivalent, meaning they are either equivalent or implicitly convertible). To allow
for Koffect to handle both cases discussed above, this proposed metaprogramming solution would be when a function `foo` 
is marked with `context(Commutative)` (for example) with the type signature of `(A, B) -> C`, an equivalent overload is
generated with the signature `(B, A) -> C` which essentially just flip the order of the operands to call to the first
definition of `foo` (if `A == B` in the type signature, then secondary overload is necessary). In practice, this would 
look like the following:

```kotlin
context(Commutative)
fun intAddDouble(int: Int, double: Double): Double = int.toDouble() + double

// the following function signature would be generated
context(Commutative)
fun intAddDouble(double: Double, int: Int): Double = intAddDouble(int, double)
```

> TODO power, pros and cons

</details>

<details>
<summary><b>Additional Validation of API usage</b></summary>

> TODO

</details>

### Initial Thoughts

> TODO