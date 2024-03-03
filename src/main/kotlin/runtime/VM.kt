package runtime

public class VM(
    private var currentChunk: Chunk? = null,
    private var ip: Int = 0,
    private var stack: ArrayDeque<Value<*>> = ArrayDeque()
) {
    private fun push(value: Value<*>) {
        this.stack.addFirst(value)
    }

    private fun pop(): Value<*> {
        return this.stack.removeFirst()
    }

    public fun interpret(chunk: Chunk): Int {
        this.currentChunk = chunk
        this.ip = 0

        return this.run()
    }

    private fun run(): Int {
        while (true) {
            when (this.currentChunk!!.code[this.ip++].toOpCode()) {
                Opcode.IntConstant, Opcode.DoubleConstant -> {
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
                Opcode.Return -> {
                    println(pop())
                    return 0
                }
            }
        }
    }
}