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

### Part 7

Part 7 was relatively straight forward. However, some code changes were not documented in the [article.md](https://github.com/Ahnfelt/type-inference-by-example/blob/master/part7/article.md) and instead 
had to resort to reading the [inference.scala](https://github.com/Ahnfelt/type-inference-by-example/blob/master/part7/Inference.scala) to see the necessary changes to be made to achieve a working program.
This is the official stopping point of the type inference by example articles as part 8 for overloading with typeclasses
has not be written (this should be unneeded at this point to start moving forward as typelcasses will be expressed with
contextual objects and therefore the reliance is now on the resolution algorithm which will be a modification of type checking
most likely). This code submitted will be a good starting point to better retrofit the algorithm to the needs of Koffect. 