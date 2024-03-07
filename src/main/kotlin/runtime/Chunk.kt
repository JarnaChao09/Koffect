package runtime

public class Chunk(
    public val code: MutableList<Int> = mutableListOf(),
    public val constants: MutableList<Value<*>> = mutableListOf(),
    public val lineInfo: MutableList<Int> = mutableListOf(),
) {
    public fun write(value: Int, line: Int) {
        this.code.add(value)
        this.lineInfo.add(line)
    }

    public fun addConstant(value: Value<*>): Int {
        this.constants.add(value)
        return this.constants.size - 1
    }

    public fun disassemble(name: String): String {
        return buildString {
            appendLine("=== $name ===")

            var offset = 0
            while (offset < this@Chunk.code.size) {
                offset = disassembleInstruction(offset)
            }
        }
    }

    private fun StringBuilder.disassembleInstruction(offset: Int): Int {
        append("%04d".format(offset))

        if (offset > 0 && this@Chunk.lineInfo[offset] == this@Chunk.lineInfo[offset - 1]) {
            append("   | ")
        } else {
            append("%4d ".format(this@Chunk.lineInfo[offset]))
        }

        return when (val instruction = this@Chunk.code[offset].toOpCode()) {
            Opcode.IntConstant, Opcode.DoubleConstant -> {
                val constant = this@Chunk.code[offset + 1]
                appendLine("%-16s %4d ${this@Chunk.constants[constant]}".format(instruction, constant))
                offset + 2
            }
            Opcode.Jump,
            Opcode.JumpIfTrue,
            Opcode.JumpIfFalse -> {
                val jump = this@Chunk.code[offset + 1]
                appendLine("%-16s %4d -> %4d".format(instruction, offset, offset + 2 + jump))
                offset + 2
            }
            Opcode.IntIdentity,
            Opcode.IntNegate,
            Opcode.DoubleIdentity,
            Opcode.DoubleNegate,
            Opcode.Not,
            Opcode.Null,
            Opcode.True,
            Opcode.False,
            Opcode.IntEquals,
            Opcode.DoubleEquals,
            Opcode.IntNotEq,
            Opcode.DoubleNotEq,
            Opcode.IntGreaterEq,
            Opcode.DoubleGreaterEq,
            Opcode.IntLessEq,
            Opcode.DoubleLessEq,
            Opcode.IntGreaterThan,
            Opcode.DoubleGreaterThan,
            Opcode.IntLessThan,
            Opcode.DoubleLessThan,
            Opcode.IntAdd,
            Opcode.DoubleAdd,
            Opcode.IntSubtract,
            Opcode.DoubleSubtract,
            Opcode.IntMultiply,
            Opcode.DoubleMultiply,
            Opcode.IntDivide,
            Opcode.DoubleDivide,
            Opcode.Pop,
            Opcode.Return -> {
                appendLine(instruction)
                offset + 1
            }
        }
    }
}