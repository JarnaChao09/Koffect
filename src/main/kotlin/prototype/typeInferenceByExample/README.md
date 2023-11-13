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