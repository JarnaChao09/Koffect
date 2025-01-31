# Async

<!-- TOC -->
* [Async](#async)
  * [Inspirations](#inspirations)
  * [Solution in Koffect](#solution-in-koffect)
    * [Initial thoughts](#initial-thoughts)
    * [1/8/2024](#182024)
    * [6/29/2024](#6292024)
    * [1/31/2025](#1312025)
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
  - [Swift](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/#Actors)
  - [Akka](https://akka.io/)
- [Swift Tasks](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/)

## Solution in Koffect

### Initial thoughts

While the actor model looks promising, having a strong mathematical foundation and theory around concurrent computation, 
it looks like the model dictates and promotes a certain code architecture for actors to be the foundational atomic unit 
of computation. This means that the asynchronous/concurrent code must permeate throughout the entire program, even when 
not utilized. This leads the belief that the actor model will be ill-suited for the other planned features in Koffect, 
particularly in composability with contexts and their corresponding context objects. A possible solution (with 
current limited understanding of the actor model) is that context objects would be passed as additional data in the 
messages sent between actors, however, this may lead to an exponential explosion of problems with semantic analysis, 
especially when a single actor receives multiple messages with conflicting contexts tied to them.
> Updated thoughts on how the actor model fits into Koffect can be seen [here](#actors-in-koffect)

Stackfull coroutines (fibers) allow for complete call stack retention, which by proxy means that the contexts and 
context objects are retained uniquely for each call stack. This would allow for the composability of each fiber with 
the context-oriented features of the language. Since each fiber would hold a copy of their own stack, the context 
information and context objects will be uniquely retained for each separate fiber of execution. This would allow for 
cooperative multitasking to also cooperatively switch between contexts and context objects. However, this has the 
downside of forcing the user to deal with the overhead of the asynchronous/concurrency runtime/engine to perform the 
stack switches when a fiber yields. Furthermore, the runtime is mandatory and unable to opt-out (usually), meaning that 
the user must deal with the performance overhead of the runtime/engine even if the code written does not utilize the 
asynchronous/concurrency API given by the language (This is also a problem with the actor model) (see [here](#flexibility1)
and [here](#flexibility2) for updated thoughts on flexibility of the asynchronous engine). Finally, this means that the
asynchronous/concurrency API is unable to be utilized for synchronous applications. The API may not be as flexible (at 
least current implementations such as goroutines and crystal fibers do not allow for utilization of the underlying API 
facilities for the implementation of synchronous coroutines) since the runtime/engine handles many of the intrinsic details
such as scheduling and stack management, it may be purposely designed for asynchrony/concurrency and may have challenges
in creating an API such that allows for injection of user designed control schemes.

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

### 1/8/2024

Further thoughts on async (stackless coroutines):

A more restrictive definition of coroutines, found in [Why async Rust?](https://without.boats/blog/why-async-rust/). To
quote: 
> The big ambiguity is that some people use the term “coroutine” to mean a function which has explicit syntax for 
pausing and resuming it (this would correspond to a cooperatively scheduled task) and some people use it to mean any 
function that can pause, even if the pause is performed implicitly by a language runtime (this would also include a 
preemptively scheduled task).

For Koffect, just like the author of the blog post, the former definition will be used and in Koffect (as seen in Kotlin)
the `suspend` keyword will be utilized to annotate that a function is inherently a coroutine and can be paused and resumed.
The notion of a "perfectly-sized stack" may also carry over to Koffect, as the compilation of coroutines down to state 
machines (what the blog post refers to as the future type) will most likely be the avenue of implementation. However, as
stated above, the end goal is for the coroutines to be a low level primitive in the language for the basic capability of 
pausing and resuming the execution of code cooperatively through explicit and loud(?) syntax and can be used to create 
state machines that work in both the synchronous and asynchronous contexts.

> It is currently unknown whether loud syntax is needed. Kotlin gets around this need of loud syntax by using the LSP to
> denote suspend points and have the IDE annotate them in the source file. Other languages will use `await` or `go`
> keywords to denote a "pause/resume" boundary. This problem is inherently a design choice of whether "pause/resume"
> boundaries should be annotated at language level or at tool level.

Further thoughts on the actor vs channels:

From an [X post](https://twitter.com/thegingerbill/status/1742222366539727142?s=46&t=Kw4On19m0_D8yj5Y7MpZ0w) by 
[GingerBill](https://twitter.com/TheGingerBill) (the creator of the [Odin](https://odin-lang.org/) language) on the 
relationships between actors and channels:
- Actor Model:
  - Nodes in the graph (actors) are explicitly named 
  - Edges in the graph (channels) are implicit and anonymous 
  - You send messages to named nodes/actors 
  - Each actor can accept an infinite number of messages 
  - Each actor is non-blocking (by default)
- CSP Model 
  - Nodes in the graph (threads) are implicit and anonymous 
  - Edges in the graph (channels) are explicit and "named"
  - You send messages to named edges/channels 
  - Each channel can accept a FINITE number of messages 
  - Each channel is blocking (by default)

As GingerBill states: these both inherently describe the same theoretical mathematics of concurrent programming but in 
differing aspects. 

However, there is one model that may also fit into this analogy and that is sender/receivers in C++. They are an explicit
naming of nodes (where senders are the root and non-sink nodes of the DAG and receivers are the sink nodes of the
DAG) and explicit statement of edges (composition of senders through the given combinators), but not official naming of 
edges (as the combinators will implicitly build the call graph, but the user can never explicitly access an edge. Only 
follow the control flow of the DAG). This would mean that senders/receivers sit somewhere in the middle of this 
mathematical spectrum leaning more towards the actor side.

Koffect (as of writing) will adopt the methodologies of the CSP with the combination of dispatchers, asynchronous 
coroutines and channels. This methodology seems to be the most scalable and each part of the combination can be, 
at least in terms of implementation, completely dependent on the primitives of Koffect (with coroutines themselves being
one of those primitives), while not sacrificing the flexibility in design.

> NOTE: An assumption is made that senders/receivers will build a DAG call graph but cycles may be possible through the 
> combinators. However, by the nature of the design, this seems to be defined as undefined behavior (or may be entirely 
> impossible, more research required)

<a id="flexibility1"> </a>
Further thoughts on flexibility of the asynchronous engine:

From a brief look on [concurrency in Hylo](https://docs.hylo-lang.org/language-tour/concurrency), it seems that this
design, at its core, is a fibers/stackful coroutines implementation. Where the runtime environment dispatcher is a global
and the suspend boundaries are defined by `spawn`. However, one interesting design choice is the ability for the global
dispatcher to be modified. This would seem to follow another core design principle of Koffect, context-oriented programming.
Take for instance this analogy, the current scheduler is some implicit context in the current scope, and when a new 
scheduler is activated, that implicit context changes. Therefore, the user must know of the current scheduler activated 
in the current context of program execution. What if every time activate was invoked, the previous "context scope" would
die and a new "context scope" starts. This is exactly the end goal for contexts in Koffect with the small exception that
Koffect aims to be explicit about the changing of context scopes. Furthermore, this explicit changing of context scopes
would allow for both synchronous and asynchronous coroutines to exist without modification of any other code, only the 
modification of the current coroutine context. This gives confidence that following the dispatchers as context objects,
coroutines as primitives, and channels for communication is the best design for the async solution in Koffect (as of writing).

> It does help that this design was proven to be powerful following structured concurrency in Kotlin. However, the 
> proposed concurrency model in Hylo does address the concern stated above of current implementations of fibers/stackful 
> coroutine APIs not being able to modify the runtime scheduler and environment in any way. The introduction of scheduler
> activation would allow for synchronous fiber/stackful coroutine creation to be possible, though as stated before this 
> would now fall under the need to remember an implicit context (which Koffect seeks to remedy such mental bookkeeping).

> As of writing, Kotlin's implementation and design of coroutines has more confidence and will be the most likely candidate 
> for the async solution in Koffect.

### 6/29/2024

Further thoughts on the actor model:

> Relevant Swift Evolution Proposals:
> - [SE-304: Structured Concurrency](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0304-structured-concurrency.md)
> - [SE-306: Actors](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0306-actors.md)
> - [SE-313: Improved control over actor isolation](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0313-actor-isolation-control.md)
> - [SE-316: Global Actors](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0316-global-actors.md)
> - [SE-338: Clarify the Execution of Non-Actor-Isolated Async Functions](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0338-clarify-execution-non-actor-async.md)
> - [SE-392: Custom Actor Executors](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0392-custom-actor-executors.md)
> - [SE-417: Task Executor Preference](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0417-task-executor-preference.md)

Swift 5.5 added [actors](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/#Actors)
to the Swift programming language which work on top of [tasks](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/#Tasks-and-Task-Groups).
This is an interesting departure from how actors have been implemented prior in the async space. Both Erlang/Elixir and 
Pony utilize actors are the lowest concurrency primitive for asynchrony in the language. Despite this, many of the core 
aspects of actors are present in Swift's implementation such as: isolation, message queues (mailboxes), and reentrancy.

> NOTE: The definition of actors is assumed to be following the definition dictated by those of the Erlang BEAM VM actors.
> While it has been shown that the characteristics of Erlang's actors do not exactly follow that of the original proposed
> theory by Carl Hewitt (which leaves aspects such as mailboxes and process isolation as implementation details), it is
> the more commonly used (arguably accepted) definition of actors which in a pragmatic setting must discuss particular
> implementation details.

Actor isolation in Swift is achieved through how the Swift runtime handles scheduling of access to an actor's internal state.
Firstly, actors are reference types meaning they act like classes and are as such always passed by reference (no implicit
copies of actors occur when passed to functions or when assigned to variables). However, unlike classes, when accessing
a property or method on an actor, an `await` (suspension point) is needed. This is needed to ensure that only one task 
is accessing the actors internal mutable state (if the state is immutable, then an `await` is not needed). Furthermore, 
to ensure actor isolation and immediate state updates, every actor holds its own executor and all access to an actor will
immediately switch the execution context to that of the actor's executor (functions not tied to an actor, known as 
non-isolated functions, will immediately context switch off of an actor executor to the general global concurrent executor,
however, this can be changed with the proposed Task Executor Preference).

A core aspect of actors is the ability to pass messages between each other. These messages are sent to an actor's mailbox.
A mailbox is where all messages to an actor are stored (usually a FIFO queue) before being processed by the
actor. Swift defines an actor's mailbox implicitly through the use of executors. Upon accessing the state of an actor (a.k.a.
send a message to it), the current task will yield* and context switch over to the actor's executor. All "messages" to an 
actor are therefore submitted to the actor's executor instead of the global executor or the task's preferred executor. 
Do note that the current task will only yield if the actor's executor is running another actor isolated function (it is
unclear from documentation if the context switch occurs before or after this "is running check"), and if the actor's
executor is available then execution will immediately continue (not yielding the current task to allow other scheduled 
tasks a chance to be run on the underlying thread). As all "messages" are submitted to an actor's executor ("mailbox"),
it is up to implementation of the actor's executor to determine the order in which "messages" are operated on, and the
default implementation of an actor's executor is the `SerialExecutor` which gives FIFO-like mailbox semantics similar to
that seen in Erlang (without the use of selective receive) and Pony (tasks awaiting an actor are **not** guaranteed to be
run in the same order they originally awaited that actor due to techniques such as priority escalation to avoid priority
inversions during scheduling). Custom executors can be implemented by implementing the `SerialExecutor` protocol and 
exposed through the `unownedExecutor` property inherent to all actors (this property is of the type `UnownedSerialExecutor`).
As an executor is the actor's "mailbox", each actor instance gets a unique executor instance. This is to ensure that a 
message sent to an actor `A` of type `ActorType1` is not intercepted by actor `B` of type `ActorType1`. If the desired
behavior is for individual actor instances to work with the same executor, this can be achieved through implementing the
`unownedExecutor` property to point to the same shared executor instance. Furthermore, if only one instance of an actor
should exist as a singleton instance, then the `@globalActor` annotation can be given to ensure that only a singleton
instance can exist (this is most commonly used for encapsulating global and static variables inside an actor to ensure 
data race safety). As actors hold an executor, the `@MainActor` is a dedicated global actor to the "main"/"ui" thread.

Actors in Swift are reentrant. Contrast that with Erlang/Elixir and Pony* actors which are non-reentrant. If an actor
is reentrant it means that if the current execution on the actor suspends, another message sent to the actor can begin 
execution. If an actor is non-reentrant it means that if the current execution on the actor suspends, that actor will wait 
for the current execution to un-suspend, not taking in new work (responding to queued up messages). Each type has its own
tradeoffs. Reentrant actors eliminate a source of deadlocks, however, it now means that race conditions are reintroduced
over actor state as the actor state is now susceptible to mutation over a suspension boundary. Non-reentrant actors eliminate
a source of race conditions, however, it now means that deadlocks are reintroduced as cyclic call structures will cause
program execution to hang as the actor is currently under execution but said execution relies on said actor responding to
another message. Pony actors are non-reentrant, however, do not suffer from deadlocks as unlike Erlang/Elixir and Swift
actors, actor behaviors (what actor methods are called in Pony) are not able to return values. Therefore, an actor's execution
cannot depend on the value returned by said actor's response to a different message and as such all messages are sequentially 
(FIFO) ordered (an example of this can be seen [here](https://tutorial.ponylang.io/types/actors#sequential)). Other examples
of avoiding deadlocks in non-reentrant actors can be seen [here](https://eduardbme.medium.com/erlang-gen-server-never-call-your-public-interface-functions-internally-c17c8f28a1ee). 
Swift actors, on the other hand, are reentrant on recursive actor calls and suspension points (`await`). Thanks to actor
isolation, any direct call to another method inside the same actor (a *recursive actor* call) is immediately executed 
(since actor isolation is maintained, the method call does not need to go through the mailbox). Furthermore, thanks to 
actor methods being implicitly `async`, if another `await` is present within the actor method, the actor method will 
suspend allowing for other jobs (messages) on the current executor (mailbox) to be scheduled, which allows for cyclic call
structures to avoid deadlocking (as long as the original call into the cycle is done through an `await`). While Swift actors
are currently reentrant, this may be subject to change as seen in the [future directions](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0306-actors.md#future-directions)
section of the SE-306 proposal.

<a id="actors-in-koffect"> </a>

> As of writing, the most likely candidate for the async solution in Koffect will still be the design of Kotlinx coroutines.
> However, this does give some food for thought for a first party library built on top of the coroutines candidate that 
> could implement actor-like semantics. Swift actors are not the foundational atomic unit of computation like they are
> in Erlang/Elixir and Pony. Instead, they are part of the larger structured concurrency model built on top of Tasks, Jobs,
> and Executors. Actors, in a way, could be seen as a higher level of abstraction around process communication through
> channels (just as GingerBill said, Actors and CSP are mathematical duals of each other). And since tasks and async functions
> in Swift are stackless, it gives precedence and confidence that a user-land actor model (as opposed to a language-land
> model) built on top of coroutines, channels, and dispatchers could work. An actor's mailbox is just a channel which 
> messages are submitted to. Reentrancy can be given more fine-tune control (similar in light to Swift's proposed 
> `@reentrant`/`@reentrant(never)`) through the use of `runBlocking` (for non-reentrant actors) and `coroutineScope` (for
> reentrant actors). Furthermore, this reentrancy behavior could be defined per actor method. Executors would be Dispatchers
> and actors could each own their own dispatcher or delegate to a shared dispatcher (Some of this functionality may depend
> on the [metaprogramming story](./meta.md) such as: how to mark an entire actor as non/reentrant without having to specify
> for every method; how to ensure at compile time that recursive actor calls is an error depending on reentrancy and whether
> it is through a suspending call or not; how to mark methods as non-isolated to the current actor).

<a id="flexibility2"> </a>
Further thoughts on flexibility of the asynchronous engine:

From a brief look at [Akka](https://akka.io/), it is an implementation of the actor model for the JVM which introduces
the asynchronous engine with the dependency. This means for code that does not use actors, the runtime does not need to
be utilized. This is another example of user-land actors versus language-land actors. As such, the runtime engine is greatly
configurable, allowing for different mailbox strategies and actor dispatchers. This gives confidence that a first party
library implementing actors on top of a more primitive system of coroutines, channels, and dispatchers is a possible avenue
for bringing the actor model to the Koffect async ecosystem.

> The design of Akka does address the concern stated about of current implementations of actor APIs not being flexible 
> enough to be able to modify the runtime scheduler and environment in any way. However, with the actor model, it is quite
> restricted in its other use cases as modifying the underlying scheduler only provides benefits if it maintains an asynchronous
> context, unlike being able to modify coroutines for use in synchronous situations. This gives further confidence that
> following Kotlin's implementation and design of coroutines for the foundational async solution in Koffect is the best
> candidate as it is the most flexible and the most primitive (seeing as other models can be built on top of coroutines).
 
> From a deep dive into Swift structured concurrency, it also gives confidence that the flexibility of the async API should
> be given outright on release. As Swift slowly added more and more flexibility with each release: 5.5 released structured
> concurrency, 5.7 released clarifications on where tasks ran, 5.9 released custom actor executors, and 6.0 released (not
> yet as of writing, 6.0 is due to release in 09/2024) task executor preference. This gives importance to fully bake an
> async API with a focus on flexibility when designing, or else Koffect will fall into the same cycle of retrofitting in
> flexibility into the design that Swift fell into (see kotlinx-coroutines releasing 1.0 will the ability specify on which
> dispatcher a coroutine should be launched and the ability to make custom dispatchers. The confusion around where code 
> was running was never an issue).

> As of writing, Kotlin's implementation and design of coroutines has greatly more confidence and will be the most likely
> candidate for the async solution in Koffect.

### 1/31/2025

Thoughts on "colorblind" async/await in Zig:

> Initial notes here: [link](https://iridescent-measure-268.notion.site/What-is-Zig-s-Colorblind-Async-Await-58a2e2782c3d4fe48dd9f24b21969ce2)
> 
> TODO: revisit post and notes and paraphrase with focus on Koffect's design