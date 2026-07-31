package runtime

public data class CallFrame(
    val function: ObjectClosure,
    val locals: MutableList<Value<*>>,
    val captures: MutableMap<Int, UpValue>,
    val returnIp: Int = -1,
)