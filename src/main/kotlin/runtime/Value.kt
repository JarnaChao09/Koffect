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

@Suppress("UNCHECKED_CAST")
public inline fun <T: Any> T?.toValue(): Value<T> = when(this) {
    null -> NullValue
    is Int -> IntValue(this)
    is Double -> DoubleValue(this)
    is Boolean -> BooleanValue(this)
    else -> error("Invalid value type")
} as Value<T>