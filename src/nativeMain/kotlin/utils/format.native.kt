package utils

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.snprintf

@OptIn(ExperimentalForeignApi::class)
public actual fun String.format(vararg args: Any?): String = memScoped {
    val length = this@format.length * 5
    val buffer = allocArray<ByteVar>(length)

    when (args.size) {
        1 -> snprintf(buffer, length.toULong(), this@format, args[0])
        2 -> snprintf(buffer, length.toULong(), this@format, args[0], args[1])
        3 -> snprintf(buffer, length.toULong(), this@format, args[0], args[1], args[2])
        4 -> snprintf(buffer, length.toULong(), this@format, args[0], args[1], args[2], args[3])
        else -> error("Too many arguments to format ${this@format}")
    }

    buffer.toKString()
}