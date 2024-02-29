import kotlin.system.exitProcess

public fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Flags currently unsupported")
        exitProcess(64)
    } else if (args.size == 1) {
        println("Running file currently unsupported")
        exitProcess(64)
    } else {
        repl()
    }
}

public fun repl() {
    var i = 0
    while (true) {
        i++
        print("[$i]>>> ")
        readlnOrNull()?.takeIf {
            it != ":q"
        }?.let {
            println(it)
        } ?: break
    }
}