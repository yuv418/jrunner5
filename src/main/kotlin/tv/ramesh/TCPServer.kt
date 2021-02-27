package tv.ramesh

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

class TCPServer(override val handler: JRunnerClientHandler) : LLHandler(handler) {
   override suspend fun listen(host: String, port: Int) {
        val server = ServerSocket(port);
        val handler = JRunnerClientHandler()
        println("DEBUG: Starting TCP socket server on port $port")

        while (true) {
            val client = server.accept()
            println("Found connection $client")
            println("Thread count is ${Thread.getAllStackTraces().keys.size}")

            GlobalScope.launch {
                val writer: OutputStream = client.getOutputStream()
                var req_stream: InputStream = client.getInputStream()
                var data: ByteArray = ByteArray(8192) // Max size 8192 bytes, we will figure this out later.
                var count = req_stream.read(data)
                data = data.slice(0..count-1).toByteArray()

                val ansBytes = handler.handle(data)

                writer.write(ansBytes) // Send back data

                writer.close()
                client.close()

                println("Closed connection.")
            }
        }
    }

}