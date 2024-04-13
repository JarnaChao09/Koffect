# Self-Assessment

## Part A: Individual

For the Koffect programming language, I was a solo project and as the only contributor to the project, my contributions
include:
- Research of all aspects of the underlying programming language theory and implementation details
- Design of all aspects of the Koffect programming language
  - Major focus on designing a cohesive set of features centered around contexts and context oriented programming
  - Design new semantics for additive and subtractive contexts
- Implementation of core compiler subsystems
  - Implementation of Lexer
  - Implementation of Parser
  - Implementation of Code Generator
  - Implementation of Runtime VM
- Creation of #Koffect channel on the r/ProgrammingLanguages Discord for community growth

In the initial assessment for CS 5001, I stated that from classes such as CS 3003 and CS 5170 discussed topics pertaining
to the area of focus for the Koffect project. The current (04/2024) result of the project built upon the understanding given
by these classes. How languages adhere to programming paradigms and compose these paradigms in a cohesive way for the betterment
of the resulting language. Furthermore, a deeper understanding of the essential compiler subsystems: the lexer, parser,
type checker, code generator, and runtime VM. However, due to unforeseen complications (explained below), MATH 5106 did
not have the opportunity to build upon the techniques of numerical analysis for ensuring mathematical correctness. However,
this does not mean that this is not an opportunity for growth in the realm of numerical analysis in the future.

This project has taught me a great deal about programming language design, programming language implementation, and programming
language theory. For design, I learned a great deal about how to make cohesive design decisions and "kill my darlings" for
the sake of cohesive design. Some features that were just not compatible with other features (such as the core feature of 
contexts) had to be removed such that the features won't stick out like a sore thumb in the language's specification. For
implementation, I learned how to create a lexer, parser, type checker, code generator, and a runtime VM. Despite it not 
being in the current (04/2024) result, I also learned how to implement type inference algorithms and the types of type 
systems that make type inference easier. Finally, for theory, I learned many new theoretical concepts pertaining to all
aspects of programming languages. From understanding parse-ability and the chomsky hierarchy for creating context-free
grammars, to understanding of more theoretical computer science literature such as algebraic effects, to connecting the
idea of context oriented programming to other features such as coroutines, algebraic effect handlers, and typeclasses.

My successes with the Koffect project during CS 5001 and CS 5002:
- The ability to complete a prototype of the essential compiler core subsystems: lexer, parser, type checker, code generator,
and runtime VM.
- Creating a cohesive design for the core language semantics and choosing orthogonal features to allow for a concise yet 
expressive language. 
  - With a focus on additive and subtractive contexts, new semantics needed to be created that were not explored by prior arts.
- Learned a great deal about high level system's design for (eventual) industrial stability in a language, low level 
underlying theory of implementing a programming language from scratch (without dependencies), and merging of pragmatic
features with research features that are less battle tested in the real world to further explore their viability in a language.

My failures/struggles with the Koffect project during CS 5001 and CS 5002:
- Cohesive Design: To ensure a cohesive yet flexible and expressive design, research into programming language theory and
development was needed. The balance of cohesion with flexibility and expressive-ness proved tougher than antipated and led
to scope creep.
- Scope creep: initial estimates of the amount of work that would be completed by the senior expo deadline was not accurate
due to the inherent scope creep. A major focus of the language is for cohesive design and due to this, unexpected complications
arose that led to development time being pushed back.