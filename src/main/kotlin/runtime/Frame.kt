package runtime

public data class CallFrame(val function: ObjectFunction, val locals: MutableList<Value<*>>, val returnIp: Int = -1)