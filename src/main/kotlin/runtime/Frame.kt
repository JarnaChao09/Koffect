package runtime

public data class CallFrame(val function: ObjectClosure, val locals: MutableList<Value<*>>, val returnIp: Int = -1)