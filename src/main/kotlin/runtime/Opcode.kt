package runtime

public enum class Opcode {
    IntConstant,
    DoubleConstant,
    ObjectConstant,
    IntIdentity,
    IntNegate,
    DoubleIdentity,
    DoubleNegate,
    Not,
    Null,
    True,
    False,
    IntEquals,
    DoubleEquals,
    IntNotEq,
    DoubleNotEq,
    IntGreaterEq,
    DoubleGreaterEq,
    IntLessEq,
    DoubleLessEq,
    IntGreaterThan,
    DoubleGreaterThan,
    IntLessThan,
    DoubleLessThan,
    IntAdd,
    DoubleAdd,
    IntSubtract,
    DoubleSubtract,
    IntMultiply,
    DoubleMultiply,
    IntDivide,
    DoubleDivide,
    IntMod,
    DoubleMod,
    Jump,
    JumpIfTrue,
    JumpIfFalse,
    DefineGlobal,
    GetGlobal,
    SetGlobal,
    GetLocal,
    SetLocal,
    Call,
    Pop,
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