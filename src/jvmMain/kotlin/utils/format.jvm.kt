package utils

public actual fun String.format(vararg args: Any?): String = String.format(this, *args)