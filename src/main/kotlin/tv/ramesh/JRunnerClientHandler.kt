package tv.ramesh

import java.net.Socket
import java.io.OutputStream
import java.io.InputStream
import java.util.Arrays
import kotlinx.serialization.*;
import kotlinx.serialization.protobuf.*;

enum class RunResultType {
    RuntimeError, CompilerError, Success
}

@Serializable
data class Request(val inputMethod: String, val inputMethodName: String, val solutionMethod: String, val inputs: ArrayList<String>) // Reflect the inputs into the input types

@Serializable
data class Response(val solutionOutputs: ArrayList<String>, val methodOutputs: ArrayList<String>, val equalityOutputs: ArrayList<Boolean>, val resultType: RunResultType)

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

        writer.write(ProtoBuf.encodeToByteArray(ans)) // Send back data

        writer.close()
        client.close()

        println("Closed connection.")
    }
}
