# Syntax

> The syntax in Koffect is not finalized. Initial design on syntax and features are listed below.
> Do not take this as official documentation as it is subject to change as the language matures.
 
## Variables

Variables can be defined one of two ways, using the `val` or `var` keywords followed by a name and optionally a type.

`val` is used for defining variables which are immutable and can only be assigned once. Once initialized, these variables
are immutable, read-only variables that cannot be reassigned a value.

```kotlin
// declaring a variable of type Int with a value of 5
val x: Int = 5

// declaring a variable with a value of 10, type Int is inferred
val y = 10

// declaring a variable of type Int, but is left uninitialized 
val z: Int
// variable is initialized to 15 
z = 15
```

`var` is used for defining variables which are mutable and can be reassigned after initialization. These variables can be
assigned a new value as long as they are of the same type as the declared type or the inferred type from the initial value.

```kotlin
// declaring a variable of type Int with a value of 4
var x: Int = 4

// reassigning the variable to the value of 5
x = 5

// declaring a variable with the value of 10, type Int is inferred
var y = 10

// reassigning the variable to 11 by "mutating the variable"
y += 1
```

## Standard I/O

`print` and its newline counterpart `println` prints their arguments to the standard output.
```kotlin
print("Hello")
print("World")

println("The meaning of life is")
println(42)
```

## Functions

Functions are declared with the `fun` keyword. Parameter types must be annotated. Return types must be annotated for functions
with a function body. If the function body is an expression, the return type can be inferred. Functions that have no return
value, or in other words, do not return any value of importance are labeled as returning `Unit`. If a function returns
`Unit`, the return type can be omitted.

```kotlin
fun sum(a: Int, b: Int, c: Int): Int {
    return a + b + c
}

fun sum(a: Int, b: Int, c: Int): Int = a + b + c

fun printSum(a: Int, b: Int, c: Int): Unit {
    println(a + b + c)
}

fun printSum(a: Int, b: Int, c: Int) {
    println(a + b + c)
}
```

## Conditional Expressions

