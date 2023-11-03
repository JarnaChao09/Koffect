# Correctness

<!-- TOC -->
* [Correctness](#correctness)
  * [Inspirations](#inspirations)
  * [Correctness in Koffect](#correctness-in-koffect)
    * [Initial Thoughts](#initial-thoughts)
<!-- TOC -->

> This document is thoughts and notes about ensuring correctness in code.
> Do not take this as official documentation as it is subject to change as the language matures.

While Koffect would like to be able to ensure code correctness, certain aspects of correctness may have adverse effects
on other portions of the language: introduction of complexity into other aspects such as the type system and/or object
model, introduction of complexity into implementation of the compiler leading to performance hits during compilation
(see Swift failing to compile simple array of integer cases *citation needed*), or introduction of complexity into
syntax and semantics that may not be composable with other language features and cause permeation of non-composable
boilerplate throughout user code.

This leads the opinion in Koffect for correctness to be a secondary focus of the language, and moreso a consequence of
the other features given in the language, instead of a primary focus of the language and allowing correctness to dictate
other aspects of the language.

## Inspirations

- [Agda](https://wiki.portal.chalmers.se/agda/pmwiki.php)
    - Code correctness through Dependent Types
- Refinement Types:
    - [Liquid Haskell](https://ucsd-progsys.github.io/liquidhaskell/)
    - [Refined Typescript](https://goto.ucsd.edu/~pvekris/docs/pldi16.pdf)
    - Libraries such as:
        - [Refined for Haskell](http://nikita-volkov.github.io/refined/)
        - [Refined for Scala](https://github.com/fthomas/refined)
- [Rhovas](https://rhovas.dev/)
    - Code correctness through builtin runtime evaluation of pre-/post-conditions
- [Rust](https://www.rust-lang.org/)
    - Code correctness through ownership model and borrow checker
- [Hylo](https://www.hylo-lang.org/)
    - Code correctness through mutable value semantics
- Abstractions as shown in other languages used as the primitives to build up systems that ensure correctness:
    - Scala:
        - [Dependent Function Types](https://docs.scala-lang.org/scala3/reference/new-types/dependent-function-types.html)
        - [Contextual Abstractions](https://docs.scala-lang.org/scala3/reference/contextual/index.html)
        - [Compile-time operations](https://docs.scala-lang.org/scala3/reference/metaprogramming/compiletime-ops.html)
        - [Runtime Multi-stage programming](https://docs.scala-lang.org/scala3/reference/metaprogramming/staging.html)
    - Kotlin:
        - [Contracts](https://github.com/Kotlin/KEEP/blob/master/proposals/kotlin-contracts.md)
        - [Context Receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
- ML family style languages such as:
    - [Haskell](https://www.haskell.org/)
    - [F#](https://fsharp.org/)
    - [OCaml](https://ocaml.org/)

## Correctness in Koffect

### Initial Thoughts

> The following code examples are not final. The syntax is due to change, as well as the semantics of the context objects.
> These context objects are just theoretical behavior at this current moment with the current mental model of how context
> resolution will work.

Correctness in Koffect should not be hard-coded into the language through syntax and semantics (ala Rust borrow checker)
or through inherent properties of the underlying system (ala dependent types and constraint solvers). Not to say that 
these solutions are inherently bad, quite the opposite, they are great solutions to the problem that is code correctness.
However, these solutions have inherent consequences on the rest of their respective language's design, of which Koffect 
does not wish to incur. Instead, Koffect wishes to give the foundational building blocks to allow for developers to 
encode correctness systems into their programs.

The main focus around Koffect is context-oriented/context-aware programming. This may lead to forms of correctness that 
inherently rely on contextual elements and therefore reliant on scope and context management. Some examples of this form
of code correctness can be seen with resource connections and resource management. An example of this can be best 
demonstrated by ORMs. Database query code should only be accessible if a database connection exists. While runtime errors
such as failure to connect to the database may (and probably will) occur, writing SQL query code and performing
transactions before an attempt to make a database connection will be caught at compile time as it would be impossible to
utilize the SQL query and transaction APIs without the introduction of a database connection context object into the 
scope. For example:

```kotlin
val databaseConnection = SomeDatabaseConnection()

val data = transaction(databaseConnection) { // context(DatabaseConnection, SQLOperations) () -> R
    // DatabaseConnection context API is accessed through `connection` for demonstration purposes
    connection.logging { log, level ->
        println("[$level]: $log")
    }
    
    // insert is now available as the context `SQLOperations` is in scope 
    val dataId = SomeTable.insert {
        // some data
    } get SomeTable.id
    
    // ...
    
    // select is now available as the context `SQLOperations` is in scope
    SomeTable.select { SomeTable.id == dataId || somethingElse() }.toList() // ^ returns from the transaction block
}

// ...
```
In this code, all operations working on the SQL table `SomeTable` will only compile if and only if a database connection
exists inside the current transaction block. In this case, the transaction block introduces the `DatabaseConnection` and
`SQLOperations` context APIs into scope. The `DatabaseConnection` introduces all code relating to the connection, such as
logging. The `SQLOperations` introduces all code relating to SQL queries, such as inserting into a table and selecting 
from a table. This allows for all database related code to compile if and only if a connection *should* exist at this 
point in time. This is determine lexically, as determining this dynamically would lead to holes in correctness.

Another example, though less applicable to real world applications, is the use of context oriented programming to model
mathematics. Contexts can be used to model [algebraic structures](https://en.wikipedia.org/wiki/Algebraic_structure)
(such as groups, rings, algebras, spaces, etc). For example, the following code can be used to describe some linear 
algebra in such a fashion that it adheres to the axioms of algebraic structures:

```kotlin
val e = with(R3, DoubleField, Symbolic) { // context(R3, DoubleField, Symbolic) () -> R
    // Var constructor is available as the `Symbolic` context is in scope
    val x = Var("lambda") // type: Symbolic
    
    val a = someMatrix() // type: Matrix3x3<Double>
    
    // identity is available as the `R3` context is in scope
    // since we have the contextual information that we are in a R3 scope
    // identity will specifically be a type of Matrix3x3 and return a type Matrix3x3<Double>
    // the generic type of Double is known as we are in a DoubleField, this allows for other
    // operations to also be in scope (such as + and *) as doubles are closed under addition
    // and multiplication due to the properties of the DoubleField
    // since we have the contextual information that we are in a Symbolic context
    // characteristic will return a Matrix3x3<Symbolic> as identity is multiplied by a
    // symbolic variable
    val characteristic = a - x * identity
    
    // the determinant will return the characteristic polynomial as a Symbolic type
    // this will allow for us to symbolically solve for the roots of the characteristic 
    // polynomial
    val characteristicPolynomial = characteristic.det
    
    // as a Symbolic type, we can symbolically solve for all roots of the characteristic 
    // polynomial
    // this will be returned up through the with function block to the `e` variable as 
    // type List<Symbolic>
    characteristicPolynomial.roots(terms=x) // ^ returns from the with block 
}
```

> This code example is created with a rudimentary knowledge of algebraic structures. The behavior is what I, as the creator,
> wishes to be possible in Koffect. It may not hold up to scrutiny of a true mathematician, however, it is sugar that I
> have wanted when implementing and working with mathematical abstractions. Similar use cases of mathematical abstractions
> are also called out in the Kotlin KEEP for possible use cases of context receivers (of which Koffect took major inspiration
> from)

As seen in the code sample above, mathematical correctness is guaranteed thanks to the introduction of contextual objects
describing the problem domain. As described, it allows for the seamless merging of numerical and symbolic code aspects. 
Furthermore, with the contextual object describing the current vector space `R3`, this removes potential developer error
when describing matrix and vector sizes (such as `identity`) leading to runtime shape conflicts. Finally, with the 
introduction of the `DoubleField`, we are guaranteed that specific operations over doubles (and by proxy, over vectors 
and matrices of doubles) are well-defined mathematically. This does not take into consideration hardware differences and
other real world factors such as floating point arithmetic not being commutative. This could be remedied by introducing 
a mathematically correct `Real` type instead of relying on IEEE-754 64 bit floating point representations of numbers.

The astute may see this code as a less functionally rigorous implementation of typeclasses. Or a more apt comparison is 
to Scala 3's implementation of implicit parameters through the use of `using` and `given` (which have been shown to be 
as expressive as typeclasses). However, unlike Scala 3's `given`s and to a similar extent implementation of typeclasses 
in languages such as Haskell, the scope in which context objects are present are lexically and explicitly defined. A 
block of code which introduces a context object into scope will also remove the context object from scope once that block
dies. Contrast with Scala 3's method of introducing `given`s into scope with the `given` keyword or an import statement.
This methodology allows for the code to reduce the need for nesting (staying linear) when introducing new context objects
into scope. Koffect chose the former methodology as it allows for the explicit introduction of a context near the affected
code. In addition, it allows for the reader to understand all necessary context objects required to read the following 
code block to ensure both the correctness in the code and correctness and disambiguate the code when reading as well as
writing.

> More reasoning for Koffect's choice of context introduction methodologies can be found at [context.md](./context.md)