# Async

<!-- TOC -->
* [Async](#async)
  * [Inspirations](#inspirations)
  * [Solution in Koffect](#solution-in-koffect)
    * [Initial thoughts](#initial-thoughts)
<!-- TOC -->

> The async story/solution in Koffect is not finalized. Thoughts and notes are listed below.
> Do not take this as official documentation as it is subject to change as the language matures.

Asynchronous code is often times very context-oriented, but pollutive. Often times, introducing asynchronous code will 
cause other parts of the code to contain asynchronous markers (see [What Color is Your Function](
https://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/)). Other times, it can lead to seemingly 
non-linear/non-sequential control flow (see [Go Statement Considered Harmful](
https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/)). The asynchronous story 
for Koffect will be very tedious and filled with tradeoffs, but hopefully it will be comprehensive and composable with 
the rest of the language.

## Inspirations

- [Kotlin(x) Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Project Loom](https://openjdk.org/projects/loom/)
- [C++ Coroutines](https://en.cppreference.com/w/cpp/language/coroutines)
  - [C++ Unified Executors Model](https://www.open-std.org/jtc1/sc22/wg21/docs/papers/2020/p0443r14.html)
- [Crystal Fibers](https://crystal-lang.org/reference/1.9/guides/concurrency.html)
- [Go Goroutines](https://go.dev/tour/concurrency/1)
- [Ocaml Effects](https://v2.ocaml.org/manual/effects.html)
  - OCaml 5.0 introduced unchecked effect handlers for concurrency, 
  dubbed [OCaml Multicore](https://github.com/ocaml-multicore/ocaml-multicore)
- Actor Model
  - [Pony](https://www.ponylang.io/)
  - [Erlang](https://www.erlang.org/)
  - [Elixir](https://elixir-lang.org/)

## Solution in Koffect

### Initial thoughts

While the actor model looks promising, having a strong mathematical foundation and theory around concurrent computation.
It looks like the model dictates and promotes a certain code architecture for actors to be the foundational atomic unit 
of computation. This means that the asynchronous/concurrent code must permeate throughout the entire program, even when 
not utilized. This leads me to believe that the actor model will be ill-suited for the other planned features in the 
language, particularly in composability with contexts and their corresponding context objects. A possible solution (with 
current limited understanding of the actor model) is that context objects would be passed as additional data in the 
messages sent between actors, however, this may lead to an exponential explosion of problems with semantic analysis, 
especially when a single actor receives multiple messages with conflicting contexts tied to them.

Stackfull coroutines (fibers) allow for complete call stack retention, which by proxy means that the contexts and 
context objects are retained uniquely for each call stack. This would allow for the composability of each fiber with 
the context-oriented features of the language. Since each fiber would hold a copy of their own stack, the context 
information and context objects will be uniquely retained for each separate fiber of execution. This would allow for 
cooperative multitasking to also cooperatively switch between contexts and context objects. However, this has the 
downside of forcing the user to deal with the overhead of the asynchronous/concurrency runtime/engine to perform the 
stack switches when a fiber yields. Furthermore, the runtime is mandatory and unable to opt-out (usually), meaning that 
the user must deal with the performance overhead of the runtime/engine even if the code written does not utilize the 
asynchronous/concurrency API given by the language (This is also a problem with the actor model). Finally, this means 
that the asynchronous/concurrency API is unable to be utilized for synchronous applications. The API may not be as 
flexible (at least current implementations such as goroutines and crystal fibers do not allow for utilization of the 
underlying API facilities for the implementation of synchronous coroutines) since the runtime/engine handles many of the
intrinsic details such as scheduling and stack management, it may be purposely designed for asynchrony/concurrency and 
may have challenges in creating an API such that allows for injection of user designed control schemes.

> Research into Project Loom and the executors system to see how flexible and "hackable" the API is to use stackfull 
> coroutines for synchronous purposes.

Stackless coroutines look like the best of both worlds, allowing for powerful structure concurrency while still 
providing a good low level API to create custom coroutines for both synchronous and asynchronous use. This is best 
exemplified by Kotlin coroutines. The asynchronous implementation of coroutines are in a first-party library, 
`kotlinx.coroutines`, while the foundational API for building the asynchronous coroutines are in the standard library 
under `kotlin.coroutines.intrinsics` and `kotlin.coroutines.cancellation`. As the foundational building blocks are in 
the standard library, and through the use of the `suspend` keyword (performing CPS translation of functions at compile
time), the standard library and users are able to implement synchronous coroutines without the need to suffer the 
consequences of the asynchronous engine from `kotlinx.coroutines`. Examples of these synchronous coroutines in the 
Kotlin standard library are 
[DeepRecursiveFunction](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deep-recursive-function/) and 
[Sequences](https://kotlinlang.org/docs/sequences.html). This allows for users and library developers to fully take 
advantage of the possibilities that come with CPS 
([Continuation-Passing Style](https://en.wikipedia.org/wiki/Continuation-passing_style)) without needing to suffer the 
baggage of the asynchronous/concurrency runtime/engine. Furthermore, the design of `kotlinx.coroutines`, with 
`CoroutineScope` and `Dispatcher`s maps very nicely to the planned context-oriented features in Koffect (Many of the
context-oriented features in Koffect took inspiration from multiple receivers in Kotlin and `kotlinx.coroutines` just so
happens to be a few of the features that greatly benefit from multiple receivers: see 
[multiple receivers KEEP use cases for structured concurrency](
https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md#use-cases)).

> Kotlin's implementation of coroutines will be the most likely candidate for the async story/solution in Koffect as 
> of writing.

> In a similar vain as Kotlin coroutines, C++ implementation of coroutines also allow for low level control of the 
> control flow when using coroutines. However, more research into the API style and design consequences need to be 
> conducted to make a more informed decision on how to design the low level coroutines API.