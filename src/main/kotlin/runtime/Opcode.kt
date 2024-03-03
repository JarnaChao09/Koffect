package runtime

public enum class Opcode {
    IntConstant,
    DoubleConstant,
    IntIdentity,
    IntNegate,
    DoubleIdentity,
    DoubleNegate,
    Not,
    Null,
    True,
    False,
    IntAdd,
    DoubleAdd,
    IntSubtract,
    DoubleSubtract,
    IntMultiply,
    DoubleMultiply,
    IntDivide,
    DoubleDivide,
    Return;

    override fun toString(): String {
        return super.toString().uppercase()
    }
}

public fun Int.toOpCode(): Opcode {
    return Opcode.entries[this.takeIf { it in Opcode.entries.indices } ?: error("Invalid Int to OpCode conversion $this")]
}

public fun Opcode.toInt(): Int {
    return Opcode.entries.indexOf(this)
}