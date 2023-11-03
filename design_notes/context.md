# Context

<!-- TOC -->
* [Context](#context)
  * [Inspirations](#inspirations)
  * [Initial Thoughts](#initial-thoughts)
    * [Definitions](#definitions)
    * [Context Declaration](#context-declaration)
    * [Context Introduction](#context-introduction)
    * [Context Removal](#context-removal)
    * [Context Resolution](#context-resolution)
    * [Definitions of Contexts](#definitions-of-contexts)
<!-- TOC -->

> Context resolution is one of the core topics of the Koffect language. As the languages evolves and matures, context 
> resolution may change and evolve with the language. Therefore, this document is also subject to change and should not 
> be taken as official documentation

## Inspirations

- [Kotlin Context Receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
- [Scala Contextual Parameters AKA Implicit Parameters](https://docs.scala-lang.org/tour/implicit-parameters.html)
- [Haskell Implicit Parameters](https://ghc.gitlab.haskell.org/ghc/doc/users_guide/exts/implicit_parameters.html)
- [Python Context Managers](https://docs.python.org/3/reference/datamodel.html#context-managers)

## Initial Thoughts

### Definitions

> The following code examples are not final. The syntax is due to change, as well as the semantics of the context objects.
> These context objects are just theoretical behavior at this current moment with the current mental model of how context
> resolution will work.

Koffect's primary programming paradigm is context-oriented programming. This leads context resolution to be the most core
semantic idea that will dictate many other design and implementation choices in the language. Therefore, it is paramount
that the terms utilized to describe these contexts are well-defined. 
- Context: A stated set of properties and functions available in the current scope. All children scopes will inherent the
contexts from their parent scopes. This is lexically resolved, meaning that captured scopes (such as closures and higher
order functions) will not inherent the contexts from the scope they are called in, unless they also specifically require
a context present in a parent scope.
  - A context can be thought of as a declaration of an API that implicitly exists for a block of code.
- Context Object: An object containing the implementation required by the context. All objects will live as long or longer
than the context scopes that require the specified context object. This is to say that a context object will at least be
alive when the context it is implementing is in scope. It may live after the scope has died (as it may be used later in 
the program by another scope). This means when captured by a closure, the captured context object will live as long as 
that closure lives as well, as the lifetime must be at least equivalent to the scope it is tied to.
  - A context object can be thought of as the concrete implementation of an API to be used for a block of code.

Simplifying these two definitions, we can boil them down to their core. A context is an API that implicitly exists in a
scope. A context object is the concrete implementation of all properties and functions for a context. For those familiar
with the literature behind algebraic effects, contexts can be thought of as coeffects and context objects can be thought
of as effect handlers. However, unlike effect handlers where each handler maps one to one for each effect, context objects
can map one context object to many contexts. This allows us to map contexts and context objects directly to interfaces 
and classes/objects from object-oriented programming respectively. For the sake of familiarity and composability in Koffect,
a context is an interface and a context object is an instance of a class that implements the required interface(s). This
allows for Koffect to reuse multiple inheritance as a way to declare an object handles multiple contexts (as seen below).

```kotlin
interface A {
  fun foo(): Unit
}

interface B {
  fun bar(): Unit
}

object AandB : A, B {
  override fun foo() = println("Hello as A::foo")
  
  override fun bar() = println("Hello as B::bar")
}

// ...

with(AandB) {
    foo() // Hello as A::foo
    bar() // Hello as B::bar
}
```
In the above code, `AandB` object implements interfaces `A` and `B`. This then means that the block of code following the
`with(AandB)` will allow for `foo` and `bar` to be resolved implicitly from the context object. 

For those familiar with Kotlin extension methods and extension lambdas, it can be deduced that the lambda block has a 
type of `AandB.() -> Unit`, where `foo` and `bar` are resolved through the call to an implicit `this` in scope. 

However, for the sake of understanding, let us assume that the lambda block had type `context(A, B) () -> Unit` (this 
will be covered more in depth at [context declaration](#context-declaration)). Now, we can see that the object `AandB` 
satisfies the requirements for both contexts `A` and `B` as it implements both interfaces. `foo` and `bar` are now 
resolved through context resolution to the `AandB` implementations of `foo` and `bar`. Furthermore, `foo` and `bar` are 
no longer resolved through the implicit `this` inside the current scope. This also means that the `AandB` object instance
is no longer accessible through the implicit `this` inside the current scope (this will be covered more in depth at
[context resolution](#context-resolution)).

### Context Declaration

> TODO

### Context Introduction

> TODO

### Context Removal

> TODO

### Context Resolution

> TODO

### Definitions of Contexts

> TODO