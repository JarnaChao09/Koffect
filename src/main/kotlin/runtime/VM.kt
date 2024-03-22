package runtime

public class VM(
    private var currentChunk: Chunk? = null,
    private var ip: Int = 0,
    private var stack: ArrayDeque<Value<*>> = ArrayDeque(),
    private val globals: MutableMap<String, Value<*>> = mutableMapOf(),
) {
    private fun push(value: Value<*>) {
        this.stack.addFirst(value)
    }

    private fun peek(distance: Int = 0): Value<*> {
        return this.stack[distance]
    }

    private fun pop(): Value<*> {
        return this.stack.removeFirst()
    }

    public fun interpret(chunk: Chunk): Int {
        this.currentChunk = chunk
        this.ip = 0

        return this.run()
    }

    public fun addNativeFunction(name: String, nativeFunc: NativeFunc) {
        this.globals[name] = NativeFunction(nativeFunc).toValue()
    }

    private fun run(): Int {
        while (true) {
            when (this.currentChunk!!.code[this.ip++].toOpCode()) {
                Opcode.IntConstant, Opcode.DoubleConstant, Opcode.ObjectConstant -> {
                    val constant = this.currentChunk!!.let {
                        val index = it.code[this.ip++]
                        it.constants[index]
                    }
                    push(constant)
                }
                Opcode.IntIdentity -> {
                    val value = this.pop().value as Int
                    this.push((+value).toValue())
                }
                Opcode.IntNegate -> {
                    val value = this.pop().value as Int
                    this.push((-value).toValue())
                }
                Opcode.DoubleIdentity -> {
                    val value = this.pop().value as Int
                    this.push((+value).toValue())
                }
                Opcode.DoubleNegate -> {
                    val value = this.pop().value as Double
                    this.push((-value).toValue())
                }
                Opcode.Not -> {
                    val value = this.pop().value as Boolean
                    this.push((!value).toValue())
                }
                Opcode.Null -> {
                    this.push(null.toValue())
                }
                Opcode.True -> {
                    this.push(true.toValue())
                }
                Opcode.False -> {
                    this.push(false.toValue())
                }
                Opcode.IntEquals -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l == r).toValue())
                }
                Opcode.DoubleEquals -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l == r).toValue())
                }
                Opcode.IntNotEq -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l != r).toValue())
                }
                Opcode.DoubleNotEq -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l != r).toValue())
                }
                Opcode.IntGreaterEq -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l >= r).toValue())
                }
                Opcode.DoubleGreaterEq -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l >= r).toValue())
                }
                Opcode.IntLessEq -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l <= r).toValue())
                }
                Opcode.DoubleLessEq -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l <= r).toValue())
                }
                Opcode.IntGreaterThan -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l > r).toValue())
                }
                Opcode.DoubleGreaterThan -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l > r).toValue())
                }
                Opcode.IntLessThan -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l < r).toValue())
                }
                Opcode.DoubleLessThan -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l < r).toValue())
                }
                Opcode.IntAdd -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l + r).toValue())
                }
                Opcode.DoubleAdd -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l + r).toValue())
                }
                Opcode.IntSubtract -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l - r).toValue())
                }
                Opcode.DoubleSubtract -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l - r).toValue())
                }
                Opcode.IntMultiply -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l * r).toValue())
                }
                Opcode.DoubleMultiply -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l * r).toValue())
                }
                Opcode.IntDivide -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l / r).toValue())
                }
                Opcode.DoubleDivide -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l / r).toValue())
                }
                Opcode.IntMod -> {
                    val r = this.pop().value as Int
                    val l = this.pop().value as Int
                    this.push((l.mod(r)).toValue())
                }
                Opcode.DoubleMod -> {
                    val r = this.pop().value as Double
                    val l = this.pop().value as Double
                    this.push((l.mod(r)).toValue())
                }
                Opcode.Jump -> {
                    val offset = this.currentChunk!!.code[this.ip++]
                    this.ip += offset
                }
                Opcode.JumpIfTrue -> {
                    val offset = this.currentChunk!!.code[this.ip++]
                    if (peek().value as Boolean) {
                        this.ip += offset
                    }
                }
                Opcode.JumpIfFalse -> {
                    val offset = this.currentChunk!!.code[this.ip++]
                    if (!(peek().value as Boolean)) {
                        this.ip += offset
                    }
                }
                Opcode.DefineGlobal -> {
                    val name = this.currentChunk!!.let {
                        val index = it.code[this.ip++]
                        it.constants[index]
                    }.value as String
                    this.globals[name] = this.peek(0)
                    this.pop()
                }
                Opcode.GetGlobal -> {
                    val name = this.currentChunk!!.let {
                        val index = it.code[this.ip++]
                        it.constants[index]
                    }.value as String

                    this.globals[name]?.let {
                        this.push(it)
                    } ?: error("Undefined variable \"$name\"")
                }
                Opcode.SetGlobal -> {
                    val name = this.currentChunk!!.let {
                        val index = it.code[this.ip++]
                        it.constants[index]
                    }.value as String

                    if (name in this.globals) {
                        this.globals[name] = this.peek(0)
                    } else {
                        error("Undefined variable \"$name\"")
                    }
                }
                Opcode.Call -> {
                    val argCount = this.currentChunk!!.code[this.ip++]

                    val callee = this.peek(argCount)
                    val args = Array<Value<*>>(argCount) {
                        NullValue
                    }

                    repeat(argCount) {
                        args[argCount - it - 1] = this.pop()
                    }
                    this.pop()

                    when (callee) {
                        is ObjectNativeFunction -> {
                            val result = callee.value.function(args)

                            this.push(result)
                        }
                        else -> error("Can only call functions and classes")
                    }
                }
                Opcode.Pop -> {
                    this.pop()
                }
                Opcode.Return -> {
                    return 0
                }
            }
        }
    }
}