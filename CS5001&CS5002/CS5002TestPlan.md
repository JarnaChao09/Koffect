# Test Plan

## Overall Test Plan

Testing of Koffect will perform testing in two stages. The first stage will test internal components of the Koffect compiler
individually with sample input and expected output structures. Upon testing each of the internal components of the Koffect
compiler, the entire system will be tested with sample input and expected output. These sets of tests will best mimic a 
common development cycle when using the Koffect compiler. The second stage will test the Koffect language design and semantics.
This will have a exponentially greater surface area than the first stage. Due to this, this stage will focus on testing 
all language features in isolation. Once proven that all features behave correctly in isolation, basic composition of language 
features will be tested to prove that composition of a more complex nature behaves correctly. Lastly, complex programs will
be utilized to test properties of the Koffect language design. Primarily, these tests should show that the language is
Turing Complete.

## Test Cases (Stage 1)

### Tokenization Test 1

- This set of tests will ensure that the tokenizer can correctly tokenize all required token types in isolation
- This set of tests will attempt to feed in source strings corresponding to the correct token type
- Inputs: single token source strings
- Outputs: a correct corresponding token object
- normal
- functional
- unit

### Tokenization Test 2

- This set of tests will ensure that the tokenizer can correctly tokenize a complex source string
- This set of tests will attempt to feed in source strings corresponding to complex but grammatically valid code
- Inputs: multi-token source strings
- Outputs: a stream of correct corresponding token objects
- normal
- functional
- unit

### Tokenization Test 3

- This set of tests will ensure that the tokenizer will correctly handle tokens that are not within the language alphabet
- This set of tests will attempt to feed source strings, both simple and complex, which contain tokens that are not within
the language alphabet
- Inputs: single token and multi-token source strings
- Outputs: a stream of correct corresponding token objects, using default fallback token type for illegal characters
- abnormal
- functional
- unit

### Parser Test 1

- This set of tests will ensure that the parser will correctly parse each production of the language grammar in isolation
- This set of tests will attempt to feed token streams for each production of the language grammar
- Inputs: correctly formed token streams
- Outputs: correct corresponding AST structures
- normal
- functional
- unit

### Parser Test 2

- This set of tests will ensure that the parser will correctly fail to parse token streams that do not follow the language
grammar
- This set of tests will attempt to feed erroneous token streams to the parser
- Inputs: token streams that contain erroneous tokens patterns
- Outputs: correct reports of erroneous productions
- abnormal
- functional
- unit

### Parser Test 3

- This set of tests will ensure that the parser will correctly fail to parse illegal tokens from the token stream
- This set of tests will attempt to feed in token streams with erroneous tokens
- Input: token streams that contain erroneously constructed tokens
- Output: correct reports of erroneous tokens and illegal keywords
- abnormal
- functional
- unit

### Type Checker Test 1

- This set of tests will ensure that the type checker will correctly type check valid statements
- This set of tests will attempt to feed in correctly formed ASTs for a multitude of language constructs
- Inputs: correctly formed ASTs for corresponding language constructs
- Outputs: correctly modify the ASTs to have the correct type data, both inferred and checked
- normal
- functional
- unit

### Type Checker Test 2

- This set of tests will ensure that the type checker will correctly report failed type unification
- This set of tests will attempt to feed in correctly formed ASTs with incorrectly formed type data for a multitude of
language constructs
- Inputs: correctly formed ASTs with incorrectly formed type relationships
- Outputs: correct reports on failure to unify types, either from inference or type checking
- abnormal
- functional
- unit

### Semantic Analysis Tests

> These sets of tests are not well-defined at time of writing. They will generally ensure that the semantic analyzer will
> correctly analyze implicit information about a multitude of language constructs and ensure semantic behavior is correct.

### Optimization Test

> These sets of tests are not well-defined at time of writing. THey will generally ensure that the optimizations passes will
> not alter the behavior of the program. Furthermore, they will not introduce systemic inconsistencies and undefined behavior
> into the resulting program

### Code Generation Test 1

- This set of tests will ensure that the resulting lowered bytecode is correct for the corresponding language construct
- This set of tests will attempt to feed in correctly formed final IR
- Inputs: correctly formed final IR
- Outputs: correctly formed executable bytecode on language VM
- normal
- functional
- unit

### Language Runtime Test 1

- This set of tests will ensure that the language runtime can correctly interpret the lowered bytecode
- This set of tests will attempt to execute bytecode instructions
- Inputs: correctly formed bytecode
- Outputs: correct output from VM execution
- normal
- functional
- unit

### Compiler Test 1

- This set of tests will ensure that all components behave correctly when a correctly formed source program is given
- This set of tests will attempt to feed in correctly formed source programs to the compiler
- Inputs: correctly formed source programs
- Outputs: correctly formed output for said source programs
- normal
- functional
- integration

### Compiler Test 2

- This set of tests will ensure that all components will successfully report relevant errors when a malformed source program
is given
- This set of tests will attempt to feed in incorrectly formed source programs to the compiler
- Inputs: incorrectly formed source programs
- Outputs: correct and relevant error reports for said source programs
- abnormal
- functional
- integration

## Test Cases (Stage 2)

### Isolation Tests

- These sets of tests will ensure that the semantic meaning of language constructs are correct
- These sets of tests will attempt to feed in specific language constructs to the compiler
- Inputs: correctly formed source programs focusing on specific language constructs
- Outputs: correct outputs proving adherence to the semantics defined for said language constructs
- normal
- functional
- unit

### Composition Tests

- These sets of tests will ensure that the semantic meaning of language constructs are correct when composed in simple ways
- These sets of test will attempt to feed in composed language constructs to the compiler
- Inputs: correctly formed source programs focusing on the composition of a couple language constructs
- Outputs: correct outputs proving that semantic definitions are not corrupted or lost during the composition of language constructs
- normal
- functional
- integration

### Turing Completeness Tests

- These sets of tests will test to see if the language is at a maturity both in design and implementation to be considered
Turing Complete
- These sets of tests will attempt to feed in full-fledged programs, some designed to test Turing Completeness and some not,
to the compiler
- Input: correctly formed highly complex source programs
- Output: correct outputs desired by each program
- normal
- functional
- integration

## Test Matrix

| Test Id                   | Normal / Abnormal | Functional / Performance | Unit / Integration |
|---------------------------|-------------------|--------------------------|--------------------|
| Tokenization Test 1       | normal            | functional               | unit               |
| Tokenization Test 2       | normal            | functional               | unit               |
| Tokenization Test 3       | normal            | functional               | unit               |
| Parser Test 1             | normal            | functional               | unit               |
| Parser Test 2             | normal            | functional               | unit               |
| Parser Test 3             | normal            | functional               | unit               |
| Type Checker Test 1       | normal            | functional               | unit               |
| Type Checker Test 2       | normal            | functional               | unit               |
| Semantic Analysis Tests   | N / A             | N / A                    | N / A              |
| Optimization Tests        | N / A             | N / A                    | N / A              |
| Code Generation Test 1    | normal            | functional               | unit               |
| Language Runtime Test 1   | normal            | functional               | unit               | 
| Compiler Test 1           | normal            | functional               | integration        |
| Compiler Test 2           | normal            | functional               | integration        |
| Isolation Tests           | normal            | functional               | unit               |
| Composition Tests         | normal            | functional               | integration        |
| Turing Completeness Tests | normal            | functional               | integration        |