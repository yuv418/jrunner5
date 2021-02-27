package tv.ramesh

import java.net.Socket
import java.io.OutputStream
import java.io.InputStream
import java.util.Arrays
import kotlinx.serialization.*;
import kotlinx.serialization.protobuf.*;

enum class RunResultType {
    Success, CompilerError
}

enum class OutputResultType {
    Success, RuntimeError, SecurityError, CompilerError, TimeoutError
}

@Serializable
data class Request(val id: String, val inputMethod: String, val inputMethodName: String, val solutionMethod: String, val inputs: ArrayList<String> = arrayListOf(), val timeout: Long = 15) // Reflect the inputs into the input types

@Serializable
data class Output(val solutionOutput: String, val solutionOutputType: OutputResultType, val methodOutput: String, val methodOutputType: OutputResultType, val match: Boolean)

@Serializable
data class Response(val id: String, val resultType: RunResultType, val results: ArrayList<Output>)

class JRunnerClientHandler() {
    private val reflector: ReflectionUtil = ReflectionUtil()

    fun handle(data: ByteArray): ByteArray {
        val req = ProtoBuf.decodeFromByteArray<Request>(data)
        val reflectionUtil = ReflectionUtil()


        var ans = reflectionUtil.evalProblemSolution(
            req.id, req.inputMethod, req.inputMethodName,
            req.solutionMethod, req.inputs, req.timeout
        )

        println("DEBUG: outputted answer is $ans")
        val ansBytes = ProtoBuf.encodeToByteArray(ans)
        println("DEBUG: ans size is ${ansBytes.size}")

        return ansBytes



    }
}
