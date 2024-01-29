# Type Inference by Example

This directory is dedicated to following [Type Inference by Example](
https://github.com/Ahnfelt/type-inference-by-example) by [Ahnfelt](https://github.com/Ahnfelt).

## Notes

### Initial Thoughts

The code is originally written in Scala. It will be translated to Kotlin. This may lead to unforeseen consequences as
pattern matching is utilized heavily in the original source code.

### Part 5

Translation of Scala to Kotlin took longer than expected. Having to manually translate every pattern matching branch into
`when` statements was very mind-bending. The code gave the same result first try, however, it may not be a sufficient 
test to determine if all patterns were correctly translated.

### Part 6

Found another error in a translated pattern matching branch. However, so far, adding new types to the AST has been pretty
straight forward. It should not take much retrofitting of the code to make the inference code work on a imperative syntax
tree (i.e. moving from a single expression to a list of expression)

### Part 6 Modified

Retrofitting the algorithm to work on a list of statements was straight forward. However, better understanding of the
underlying algorithm is needed to determine how to handle function overloading in the type system. Initial thoughts on the
solution are to just introduce a backtracking step in the unification process.