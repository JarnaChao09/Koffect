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

> Section is currently under finalization, currently two options remain for declaring contexts.
> More time must be spent on defining the behavior of each option to get a better understand of the 
> overall effects each respectively has on the behavior and semantics of contexts
> 
> Currently documented below are the two working syntax
> 
> Update: syntax chosen to be the [2nd option](#option-2-)

#### Option 1
```kotlin
// context(!C) means the function will only be resolved in the context if C is not given
context(!C) fun foo() { ... }

// this can be combined with other normal context statements
context(A, B, !C) fun foo() { ... }

// is semantically equivalent to
context(A & B & !C) fun foo() { ... }

// this would allow for context declarations to be stored in a sort of context variable
context ctxVar = A & B & !C // similar to Typescript `type`
context(ctxVar) fun foo() { ... }
```
This syntax has many benefits. It would allow for context definitions to be decoupled from context declaration. This would
allow for context definitions to be reused across multiple contextual declarations. Furthermore, this would allow for a
creation of a "context algebra" where each context would be analogous to sets/types in set/type theory. With the basic
operations of union (`|`), intersect (`&`), and inverse (`!`), the foundations of a set/type algebra can be built up to
allow for a full context algebra to be described at context definitions. 

However, nothing is ever free. There are also many downsides to this syntax. This syntax may affect the readability and 
explicitness of context definitions. While context variables may allow for better descriptions of what each set of contexts
represents, the true contents of the context is still abstracted away. Furthermore, the reliance on the use of mathematical
notation to describe the composition of contexts may prove to be a detriment to readability as the syntax is quite abstract
for those not familiar with type algebras as seen in other languages such as Typescript. The theoretical notation may prove 
both redundant as modifications to the context equation can yield equivalent contexts with simpler base operations and 
unsound as this "context algebra" does not follow entirely with the axioms of set/type theory.

#### Option 2 

```kotlin
// instead of !C, all functionality to define when a context isn't given is instead of defining the function when
// C is not given, delete the function when C is given
context(C) fun foo() = delete("some compile time error message")

// now, if a function is only supposed to exist when a context isn't given, the function should just be defined to be
// deleted when the context does exist
context(A, B) fun foo() { ... }
context(A, B, C) fun foo() = delete("some compile time error message")
```
- it remains unclear whether it remains a good idea to ensure that the context variables should exist
- it would greatly complicate the language, and some explicit boilerplate for repeated branches does not seem 
to be a big hit to DX
- current pros:
  - this syntax would allow for context declaration to be as explicit as possible 
  - this syntax would also greatly reduce confusion, as it makes more sense to think in terms of only when contexts 
  are available as opposed to thinking when they are and aren't available 
  - this syntax would make deleting functions when a context is in scope more clear as now semantic analysis could 
  specifically look for the delete keyword/internal function (undecided)
- current cons:
  - this syntax does not currently allow for context variables, as they would be introducing a second set of 
  syntactical operations and would be redundant, but would not allow for the reusability afforded by context 
  variables (this may be offset by the fact that interfaces could be easily merged)
  - it is uncertain if this syntax has the same expressive power as the context "algebra" defined above 
  - it is not clear if replacing a "not given" resolution tactic with a delete if given tactic is semantically 
  equivalent in all scenarios (should be, but unsure, more deliberation is needed)

### Context Introduction

> TODO

### Context Removal

> TODO

### Context Resolution

> TODO

### Definitions of Contexts

> TODO