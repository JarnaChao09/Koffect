package runtime

public data class CallFrame(val function: ObjectFunction, val locals: List<Value<*>>, val returnIp: Int = -1)