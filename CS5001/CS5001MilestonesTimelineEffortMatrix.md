# Milestones, Timeline, and Effort Matrix

# Milestones

1. Finalization of Koffect specification. This includes a finalization of syntax, of semantics, and of feature set. This will allow for the language to have a sense of direction and has the biggest influence over all other aspects of the language implementation (lexer, parser, code generation). This milestone is one of the many that are needed for the fabled 1.0 release. This milestone may not happen before initial completion of many aspects of the language and is closely tied to the 1.0 release milestone. However, this milestone may occur before or after the 1.0 release.
2. 1.0 release. The fabled 1.0 release as it shows that a language has matured to a stable state. The 1.0 release also implies that the language is set in stone and will not have major breaking changes that break backwards compatibility (these are often saved for major version bumps, see Python 2 to 3 and Scala 2 to 3). As stated above, new features and new syntax may be added to the language after the 1.0 release, but no major breaking changes will be added.
3. Development of lexer, parser, code generation, and runtime. Completion of these stages of the compiler will allow for basic usage of the language and testing of language semantics and syntax. This milestone is the catalyst for the rest.
4. Development of standard library, unit testing framework, and documentation generator. Completion of a standard library, unit test framework, and documentation generator signifies that a language has some level of batteries included and is nearing maturity and 1.0.
5. Answering and developing the async, multiplatform, and build tooling solutions. Completion of the async story is very important in the modern programming language world as more and more applications are utilizing good asynchronous code solutions. With the prevalence of technology, multiplatform support is also heavily needed as it allows for developers to utilize a language for the many common targets that exist. Build tooling and good dependency management allow for a better batteries included solution and allow for the fostering of a community for the language that constantly build libraries and frameworks to complete more and more tasks, making the language more and more general purpose.

# Deliverables

1. Koffect Compiler and Runtime: This is the main product of this project. It will be the program(s) to compile and run Koffect programs
2. Koffect Specification and Documentation: This is the secondary product of this project. It will be the main source of information about the Koffect program.

# Timeline

Due to the nature of how programming languages are created and evolve over time, many tasks are not set in stone on specific timelines but will evolve when more structure and maturity are achieved. 

| Task | Timeline | Comments | Effort |
| --- | --- | --- | --- |
| Finalize Specification | Weeks 1-3 | Can be done concurrently with the other tasks but would like to be completed before code generation has started | Jaran (100%) |
| Develop Lexer | Weeks 1-2 |  | Jaran (100%) |
| Develop Parser | Weeks 2-3 | Depends on Lexer | Jaran (100%) |
| Develop Code Generation | Weeks 3-5 | Depends on Parser | Jaran (100%) |
| Develop Code Optimizer | Weeks 3-7 | Depends on Parser and can be done concurrently with Code Generation | Jaran (100%) |
| Research Async Story | Concurrent | Can be completed concurrently with all other tasks | Jaran (100%) |
| Research Multiplatform Story | Concurrent | Can be completed concurrently with all other tasks | Jaran (100%) |
| Research Build Tooling Story | Concurrent | Can be completed concurrently with all other tasks | Jaran (100%) |
| Develop Standard Library | Week 6+ | Timeline is fuzzy, can be completed concurrently with other tasks once basic language execution facilities are completed | Jaran (100%) |
| Develop Unit Testing Framework | Week 6+ | Timeline is fuzzy, can be completed concurrently with other tasks once basic language execution facilities are completed | Jaran (100%) |
| Develop Documentation Generator | Week 6+ | Timeline is fuzzy, can be completed concurrently with other tasks once basic language parsing facilities are completed | Jaran (100%) |
| Create Language Documentation | Concurrent | Can be completed concurrently with all other tasks | Jaran (100%) |