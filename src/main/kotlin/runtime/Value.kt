package runtime

public sealed interface Value<T> {
    public val value: T
}

public data class IntValue(override val value: Int) : Value<Int> {
    override fun toString(): String = "${this.value}"
}

public data class DoubleValue(override val value: Double) : Value<Double> {
    override fun toString(): String = "${this.value}"
}

public data class BooleanValue(override val value: Boolean) : Value<Boolean> {
    override fun toString(): String = "${this.value}"
}

public data object NullValue : Value<Nothing?> {
    override val value: Nothing?
        get() = null

    override fun toString(): String = "null"
}

public data object UnitValue : Value<Unit> {
    override val value: Unit
        get() = Unit

    override fun toString(): String = "Unit"
}

public sealed interface ObjectValue<T> : Value<T>

public data class Function(val name: String, val arity: Int, val chunk: Chunk)

public data class ObjectFunction(override val value: Function) : ObjectValue<Function> {
    override fun toString(): String = "<function/${value.arity} ${value.name}>"
}

public data class ObjectString(override val value: String) : ObjectValue<String> {
    override fun toString(): String = this.value
}

public typealias NativeFunc = (List<Value<*>>) -> Value<*>

public data class NativeFunction(val function: NativeFunc)

public data class ObjectNativeFunction(override val value: NativeFunction) : ObjectValue<NativeFunction> {
    override fun toString(): String = "<native fn>"
}

//public enum class ObjectType {
//    String
//}
//
//public data class ObjectValue<T>(override val value: T, val type: ObjectType) : Value<T>

@Suppress("UNCHECKED_CAST")
public inline fun <T : Any> T?.toValue(): Value<T> = when(this) {
    null -> NullValue
    is Int -> IntValue(this)
    is Double -> DoubleValue(this)
    is Boolean -> BooleanValue(this)
    is String -> ObjectString(this)
    is Function -> ObjectFunction(this)
    is NativeFunction -> ObjectNativeFunction(this)
    else -> error("Invalid value type")
} as Value<T>
