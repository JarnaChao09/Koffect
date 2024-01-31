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

Simplifying these two definitions, they can be boiled down to their core. A context is an API that implicitly exists in 
a scope. A context object is the concrete implementation of all properties and functions for a context. For those familiar
with the literature behind algebraic effects, contexts can be thought of as coeffects and context objects can be thought
of as effect handlers. However, unlike effect handlers where each handler maps one to one for each effect, context objects
can map one context object to many contexts. This allows for the mapping of contexts and context objects directly to 
interfaces and classes/objects from object-oriented programming respectively. For the sake of familiarity and composability
in Koffect, a context is an interface and a context object is an instance of a class that implements the required interface(s).
This allows for Koffect to reuse multiple inheritance as a way to declare an object handles multiple contexts (as seen below).

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

However, for the sake of understanding, assume that the lambda block had type `context(A, B) () -> Unit` (this will be 
covered more in depth at [context declaration](#context-declaration)). Now, it can be seen that the object `AandB` satisfies the requirements
for both contexts `A` and `B` as it implements both interfaces. `foo` and `bar` are now resolved through context resolution
to the `AandB` implementations of `foo` and `bar` and not through the implicit `this` inside the current scope. This also 
means that the `AandB` object instance is no longer accessible through the implicit `this` inside the current scope (this
will be covered more in depth at [context resolution](#context-resolution)).

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
// to state that a function exists only in a context is with a context declaration
// for example, the following function definition of foo will only be resolved if and only if
// context A is given
context(A) 
fun foo() { /* ... */ }

// functions can also exist when a context does not exist, and is removed from scope once a context
// does exist in the current scope
// this is achieved with an ! signifying that if the context is not given, the following function 
// definition will exist
// for example, the following function definition of foo will only be resolved if and only if the 
// context C is not given
context(!C) 
fun foo() { /* ... */ }

// given and not given contexts can be merged into one context declaration by utilizing 
// basic operations
// for example, the following function definition of foo will only be resolved if and only if
// context A and B are given and C is not given
context(A & B & !C) 
fun foo() { /* ... */ }

// this could allow for context declarations to be stored in a context variable
// (similar to Typescript `type`s)
// using these "context operations" allow for definitions of contexts to stored in a 
// "context variable"
// for example, the context variable ctxVar can be created as such to allow for the same
// context declaration of the above foo definition
context ctxVar = A & B & !C
context(ctxVar) 
fun foo() { /* ... */ }
```

This syntax allows context definitions to be decoupled from context declaration. This allows context definitions to be 
reused across multiple contextual declarations. Furthermore, this allows for a creation of a "context algebra" where each
context would be analogous to sets/types in set/type theory. With the basic operations of union (`|`), intersect (`&`), 
and inverse (`!`), the foundations of a set/type algebra can be built up to allow for a full context algebra to be described 
at context definitions. 

However, nothing is free. There are many downsides to this syntax. This syntax may affect the readability and explicitness 
of context definitions. While context variables may allow for better descriptions of what each set of contexts represents,
the true contents of the context is still abstracted away. Furthermore, the reliance on the use of mathematical notation
to describe the composition of contexts may prove to be a detriment to readability as the syntax is quite abstract for those
not familiar with type algebras as seen in other languages such as Typescript. The theoretical notation may prove both 
redundant as modifications to the context equation can yield equivalent contexts with simpler base operations and unsound
as this "context algebra" does not follow entirely with the axioms of set/type theory.

#### Option 2 

```kotlin
// as with the previous option, to state that a function exists only in a context is with a context
// declaration
// for example, the following function definition of foo will only be resolved if and only if
// context A is given
context(A) 
fun foo() { /* ... */ }

// instead of using ! to state that a function exists when a context is not given, 
// functions can be deleted when a context is given
// for example, to mimic the context(!C) definition of foo above, the following functions can be defined

// the definition of foo will exist in the global context
fun foo() { /* ... */ }

// and when the context C exists, delete foo
context(C) 
fun foo() = delete("function foo() cannot exist when context C exists")

// without the distinction of given and not given contexts, contexts can now be merged 
// into one context declaration with the simple `,` operation
// for example, the following function definition of foo will only be resolved if and only if
// context A and B are given
context(A, B) 
fun foo() { /* ... */ }

// and now, if a function is only supposed to exist when a context isn't given, the function 
// should just be defined to be deleted when the context does exist
// for example, the following function definitions are equivalent to A & B & !C from the previous syntax option
context(A, B) 
fun foo() { /* ... */ }
context(A, B, C) 
fun foo() = delete("function foo() cannot exist when context C exists")
```

This syntax would make context definitions tightly coupled with corresponding context declarations, making declarations
as explicit as possible. And with the introduction of `delete`, context definitions become simpler as now the developer
must only think in terms of when contexts are available as opposed to thinking when contexts are and aren't available.
Furthermore, removing a function when a context is in scope with `delete` provides clearer compile time error reports and
boosts error analysis efficiency through the use of semantic analysis.

This option isn't all upsides, there are some features being sacrificed from the first option. Such features are context
variables, the "context algebra", and not given context resolution. Below are the justifications for the sacrifice of 
these features.

#### "Context Algebra" soundness and redundancy

As previously mentioned, the "context algebra" described previously is unsound. The best evidence for this claim is in 
the definitions of the union, intersect, and inverse operations and how they interact with contexts. From [definitions](
#definitions), a context is a stated set of properties and functions available to a scope. Therefore, it logically follows
that the definitions of union and intersect follow closely with set theory. However, inverse (`!`)&mdash;the operation to define
that a context is not given&mdash;does not have a well-defined definition following set theory. Therefore, following set 
theory will not be enough to describe the desired sets of operations for working on contexts. Another aspect defined in 
[definitions](#definitions) is that contexts are analogous (and in Koffect are equivalent) to interfaces. Therefore, it 
logically follows that the definitions of union and intersect follow closely with type theory. Once again, inverse (`!`)
does not have a well-defined operation within the context of type theory. Therefore, following type theory will not be 
enough to describe the desired sets of operations for working on contexts as well. 

With differing perspectives on contexts, the resulting definitions for union and intersect also clash. If purely following
set theory, the union of two contexts would be defined as the set of all properties and functions that exist in both of 
the respective contexts. If purely following type theory, the union of two contexts would be defined as the set of all 
properties and functions that exist in one of the two respective contexts at one time. Furthermore, if purely following 
set theory, the intersection of two contexts would be defined as the set of all properties and functions that exist only 
in both respective contexts. If purely following type theory, the intersection of two contexts would be defined as the set 
of all properties and functions that exist in both of the respective contexts. This collision in definitions leads to the
realization that a full "context algebra" built off of set and type theory notation is both unsound and redundant.

> An interesting observation is that the definition of set unions is, in this context, semantically equivalent to the 
> definition of type intersection.

The "context algebra" would be redundant as set intersection and type union do not have any practical use (as of writing),
and set union and type intersection can be unified into one operation. This operation follows to be the `,` operation in
context declarations depicted in the 2nd syntax option, which follows from the convention of adding another context to 
the context list similar to the use of `,` in function argument lists seen in many other languages. With the introduction 
of `delete`, it is shown that the inverse operation is no longer needed.

#### The removal of not given contexts

With the introduction of `delete`, the statement that "a definition should only exist when a context is not given" can be
flipped to become "a definition should not exist when a context is given". `delete` creates a simplified method of defining
contexts; the developer's cognitive load is lightened to focusing on given contexts rather than considering both given and
not given contexts. This change has also been shown to be more powerful. Take the following example:

Say a function `foo` is defined in third-party code not owned by the developer, and therefore cannot be modified by the 
developer.

```kotlin
fun foo() { /* ... */ }
```

Now say the developer wants to remove `foo` when a context `A` is given. How would a developer go about modeling their
codebase this way? With the first syntax option, this is not as straightforward as one might think. While it would be
possible to define a set intersection of the global context and a context where `foo` does not exist but all other functions
from the global context do exist, this second context (the one where `foo` does not exist, but all other functions from
the global context do exist) is not easily constructable without introducing more syntax for the developer to remember 
(and this syntax may also hurt readability). However, with the second option, the code is the following:

```kotlin
context(A) 
fun foo() = delete("deleted")
```

#### The removal of context variables

With the realization that the "context algebra" is both unsound and redundant (as seen [above](#context-algebra-soundness-and-redundancy)), 
context variables also seemed unnecessary. Furthermore, due to contexts in Koffect just being interfaces, the conciseness
of context variables could be achieved with interface inheritance instead. A "context variable" would now just be an interface
that inherited from all the desired contexts (as this is equivalent to the `,` operation defined [above as well](#context-algebra-soundness-and-redundancy)).
Furthermore, this would also allow for the creation of context objects that conformed to "context variables" as it would
just be an object conforming to an interface.

#### Context Lambdas

Lambda types can also declare a context declaration. As lambda types are first class citizens in Koffect, context lambdas
can be passed around as an object to different parts of the code. To declare a lambda type is a context lambda, the `context`
declaration is added before the lambda type as seen below.

```kotlin
// the following definition is of a binary input to unary output context lambda 
// which requires the contexts A and B to be given
context(A, B) (I, I) -> O
```

Since context lambdas can be passed around, the declaration of contexts must be explicit. This means that lambdas will not
inherent the current context declaration of the current scope they are created in. However, context lambdas may capture
context objects (once introduced) from their creation scope. Determining whether a context object is captured or not will 
be covered further in-depth in [context resolution](#context-resolution). 

> Currently, to create a context lambda literal, the type must be given explicitly to utilize a context declaration. 
> Syntax may be introduced in the future to allow for a shorthand of creating a lambda literal with a context declaration.

### Context Introduction

> The following code examples are not final. The syntax may change as the syntax is highly dependent context resolution.

At definition site, contexts are declared. This is only half of the equation. Recall from [definitions](#definitions), contexts are
a stated set of properties and functions available in a scope. These properties and functions need a concrete 
implementation&mdash;the context objects&mdash;to be present at call-site. This is accomplished with `with`. Take the 
following code block:

```kotlin
// declaration of the A context
interface A {
  fun foo(): Int
}

// declaration of the B context
interface B {
  val bar: Int
}

// creation of a context object that satisfies the A context
object AImpl : A {
    override fun foo(): Int {
        return 30 
    }
}

// creation of a context object that satisfies the B context
object BImpl : B {
    override val bar: Int = 12
}

// declaration that the function calculateMeaningOfLife requires
// both context A and context B
context(A, B)
fun calculateMeaningOfLife(): Int = foo() + bar

// ...

// introducing the context objects required to satisfy the contexts
// needed by `calculateMeaningOfLife`
with(AImpl, BImpl) { // calling scope has context(AImpl, BImpl)
    println(calculateMeaningOfLife())
}
```

In this code block, `calculateMeaningOfLife` requires both context `A` and context `B` to be present in the calling scope.
The `with(AImpl, BImpl)` introduces the `AImpl` and `BImpl` context objects into the succeeding trailing lambda block.
Within this lambda block, the defined context is `context(AImpl, BImpl)` which successfully resolves the `context(A, B)`
required to introduce `calculateMeaningOfLife` into scope. This is through [context resolution](#context-resolution) described below.

> As of writing, `with` is currently planned to be a function with auto generated signatures up to 9 contextual objects.
> The auto generated definition of `with` utilized above code snippet is defined as 
> `fun <T1, T2> with(T1, T2, context(T1, T2) () -> Unit): Unit`
> However, this may be subject to change to either:
> 1. a keyword within the language for context introduction
> 2. a variadic generic function (depending on the viability of adding variadic generics)

### Context Removal

> TODO

### Context Resolution

> TODO

### Definitions of Contexts

> TODO