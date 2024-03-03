import codegen.CodeGenerator
import lexer.Lexer
import parser.Parser
import runtime.*
import kotlin.system.exitProcess

public fun main(args: Array<String>) {
//    val chunk = Chunk()
//
//    val const1 = chunk.addConstant(1.2.toValue())
//    chunk.write(Opcode.DoubleConstant.toInt(), 123)
//    chunk.write(const1, 123)
//
//    val const2 = chunk.addConstant(3.4.toValue())
//    chunk.write(Opcode.DoubleConstant.toInt(), 123)
//    chunk.write(const2, 123)
//
//    chunk.write(Opcode.DoubleAdd.toInt(), 124)
//
//    val const3 = chunk.addConstant(5.6.toValue())
//    chunk.write(Opcode.DoubleConstant.toInt(), 125)
//    chunk.write(const3, 125)
//
//    chunk.write(Opcode.DoubleDivide.toInt(), 126)
//
//    chunk.write(Opcode.DoubleNegate.toInt(), 127)
//
//    chunk.write(Opcode.Return.toInt(), 127)
//
//    println(chunk.disassemble("test chunk"))
//
//    println(VM().interpret(chunk))

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
            val lexer = Lexer(it)
            val parser = Parser(lexer.tokens)
            val codegen = CodeGenerator()
            val vm = VM()

            val chunk = codegen.generate(parser.parse())

            vm.interpret(chunk)
        } ?: break
    }
}