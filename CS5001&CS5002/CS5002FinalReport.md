# Koffect

## Table of Contents

<!-- TOC -->
* [Koffect](#koffect)
  * [Table of Contents](#table-of-contents)
  * [Project Description](#project-description)
    * [Project Description](#project-description-1)
  * [User Interface Specification](#user-interface-specification)
  * [Test Plan and Result](#test-plan-and-result)
    * [Test Plan](#test-plan)
  * [User Manual](#user-manual)
  * [Spring Final PPT Presentation](#spring-final-ppt-presentation)
    * [Spring Presentation](#spring-presentation)
  * [Final Expo Poster](#final-expo-poster)
    * [Final Expo Poster](#final-expo-poster-1)
  * [CS 5001 (Fall) Assessment](#cs-5001-fall-assessment)
    * [CS 5001 Assessment](#cs-5001-assessment)
  * [CS 5002 (Spring) Assessment](#cs-5002-spring-assessment)
    * [CS 5002 Assessment](#cs-5002-assessment)
  * [Summary of Hours](#summary-of-hours)
    * [Jaran Chao](#jaran-chao)
      * [Fall Semester](#fall-semester)
      * [Spring Semester](#spring-semester)
  * [Summary of Expenses](#summary-of-expenses)
  * [Appendix](#appendix)
    * [Team Contract](#team-contract)
    * [GitHub Repository](#github-repository)
    * [Justification for Hours of Effort](#justification-for-hours-of-effort)
<!-- TOC -->

## Project Description

### [Project Description](../README.md#koffect)

## User Interface Specification

As Koffect is a programming language, the forward facing user input is through the compiler executable (and in the future,
the build system and language server protocol). This occurs either through the command line or the Integrated Development
Environment of the user's choice. Due to these reasons, there are no technical user interface specifications for Koffect.

## Test Plan and Result

### [Test Plan](./CS5002TestPlan.md)

## User Manual

The Koffect user manual can be found at the language [README](../README.md)

## Spring Final PPT Presentation

### [Spring Presentation](./CS5002_Presentation.pptx)

## Final Expo Poster

### [Final Expo Poster](./CS5002_Poster.pdf)

## CS 5001 (Fall) Assessment

### [CS 5001 Assessment](./CS5001CapstoneAssessment.md)

## CS 5002 (Spring) Assessment

### [CS 5002 Assessment](./CS5002SelfAssessment.md)

## Summary of Hours

### Jaran Chao

The following are approximations of the time allocation for all current (04/2024) project work:

#### Fall Semester

- Researching and documenting async paradigms (see [async.md](../design_notes/async.md)): ~15 hours
- Researching and documenting correctness (see [correctness.md](../design_notes/correctness.md)): ~10 hours
- Designing and documenting syntax and semantics around contexts (see [context.md](../design_notes/context.md)): ~10 hours (and counting)
- Following [Type Inference by Example](https://github.com/Ahnfelt/type-inference-by-example) (see [prototype.typeInferenceByExample](../src/main/kotlin/prototype/typeInferenceByExample)): ~10 hours
- Completing all required CS5001 Assignments: ~10 hours

Total time: ~55 hours

#### Spring Semester

- Designing and documenting syntax and semantics around contexts (see [context.md](../design_notes/context.md)): ~10 hours
- Creating the lexer subcomponent (see [lexer](../src/main/kotlin/lexer)): ~3 hours
- Creating the parser subcomponent (see [parser](../src/main/kotlin/parser)): ~5 hours
- Creating the type checker subcomponent (see [analysis](../src/main/kotlin/analysis)): ~10 hours
- Creating the code generator subcomponent (see [codegen](../src/main/kotlin/codegen)): ~10 hours
- Creating the runtime VM subcomponent (see [runtime](../src/main/kotlin/runtime)): ~10 hours
- Completing all required CS5002 Assignments and preparation for the expo: ~7 hours

Total time: ~55 hours

Total project time: ~110 hours

## Summary of Expenses

This project is purely software with no dependencies (as of 04/2024). As such, there are no expenses for hardware. As there
are no dependencies and all development could be completed with community edition/free IDEs. However, Intellij Ultimate 
edition is given free to students. Therefore, there are no expenses for software as well. All software created and utilized
is open-source and free-to-use (following respective licensing).

## Appendix

### [Team Contract](./CS5001TeamContract.md)

### [GitHub Repository](https://github.com/JarnaChao09/Koffect)

### Justification for Hours of Effort

As a solo project and only contributor to the project, all development hours are mine. The beginning of the CS 5001 spent
most of the time allocation on research and design. It was during this period that the first obstacles of scope creep began
to emerge. By the time CS 5002 came around, the design had many holes, particularly in context resolution, so more time
needed to be allocated to that during the first portion of CS 5002. However, during this time, final conclusions on the 
type inference prototype were completed and initial work on the lexer and parser subsystems had begun. Once the senior
expo deadline was coming closer, less time was dedicated towards design and more focus on the remaining subsystems: the
type checker, code generator, and runtime VM. Completing initial prototypes of these subsystems was the new defined goal
and the time allocation was adjusted accordingly.