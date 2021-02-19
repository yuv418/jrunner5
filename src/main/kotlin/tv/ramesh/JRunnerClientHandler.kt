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
    Success, RuntimeError, SecurityError, CompilerError
}

@Serializable
data class Request(val inputMethod: String, val inputMethodName: String, val solutionMethod: String, val inputs: ArrayList<String> = arrayListOf()) // Reflect the inputs into the input types

@Serializable
data class Output(val solutionOutput: String, val solutionOutputType: OutputResultType, val methodOutput: String, val methodOutputType: OutputResultType, val match: Boolean)

@Serializable
data class Response(val resultType: RunResultType, val results: ArrayList<Output>)

class JRunnerClientHandler() {
    private val reflector: ReflectionUtil = ReflectionUtil()

    fun run(client: Socket) {
        val reflectionUtil = ReflectionUtil()
        val writer: OutputStream = client.getOutputStream()

        var req_stream: InputStream = client.getInputStream()

        var data: ByteArray = ByteArray(8192) // Max size 8192 bytes, we will figure this out later.
        var count = req_stream.read(data)
        data = data.slice(0..count-1).toByteArray()

        val req = ProtoBuf.decodeFromByteArray<Request>(data)


        var ans = reflectionUtil.evalProblemSolution(
            req.inputMethod, req.inputMethodName,
            req.solutionMethod, req.inputs
        )

        println("DEBUG: outputted answer is $ans")
        val ansBytes = ProtoBuf.encodeToByteArray(ans)
        println("DEBUG: ans size is ${ansBytes.size}")

        writer.write(ansBytes) // Send back data

        writer.close()
        client.close()

        println("Closed connection.")
    }
}
