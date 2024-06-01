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

## Variables

Variables can be declared starting with either the `val` or `var` keyword, then the variables name, and finally followed
by the type declaration of the variable. 

The `val` keyword declares a variable that are only assignable to a value once. These variables are immutable, read-only
bindings that cannot be assigned a different value after initialization.

> Currently, `val` declarations are reassign-able, this is a WIP feature

```kotlin
// Declaring the variable `x` with the initial value of 5 and type of Int
val x: Int = 5
```

The `var` keyword declares a variable that are reassign-able. These variables are mutable bindings that can be assigned
a different value after initialization.

```kotlin
// Declaring the variable `x` with the initial value of 5 and type of Int
var x: Int = 5

// Reassigns a new value of 6 to the variable `x`
x += 1
```

Koffect supports type inference on variable declarations, automatically determining the data type of the declared variable.
This means that type declarations can be omitted after the variable name.

> Currently, type inference is not implemented, this is a WIP feature

```kotlin
// Declaring the variable `x` with the initial value of 5 and the type is inferred as Int
val x = 5

// Declaring the variable `y` with the initial value of 4.2 and the type is inferred as Double
var y = 4.2
```

Variables can only be used after initialization. Variable initialization can happen at its declaration or a variable can
be first declared and initialized later. If the latter is the case, the type of the variable must be declared as well.

```kotlin
// Initializes the variable `x` at the moment of declaration and therefore the type is not required and is inferred as Int 
val x = 5

// Declares the variable `y` without initialization and therefore the type is required
val y: Int
// Initializes the variable `y` after declaration 
y = 10
```

Variables can be defined at top level, commonly known as global variables.

```kotlin
val PI = 3.14
var foo = 0

fun incrementFooPlease() {
    foo += 1
}

// x = 0; PI = 3.14
// incrementFooPlease()
// x = 1; PI = 3.14
```

## Conditional Expressions

