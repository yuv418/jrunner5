package tv.ramesh

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.zip.Deflater

class WSClient(override val handler: JRunnerClientHandler, val wss: Boolean = false) : LLHandler(handler) {


    @ExperimentalWebSocketExtensionApi
    override suspend fun listen(host: String, port: Int) {
        // Host is endpoint

        val client = HttpClient {
            install(WebSockets) {
                extensions {
                    install(WebSocketDeflateExtension) {
                         compressionLevel = Deflater.DEFAULT_COMPRESSION
                         compressIfBiggerThan(bytes = 4 * 1024)
                    }
                }
            }
        }


        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = port,
            path = "/ecws/runner",
            request = {
                url.protocol = if (wss) URLProtocol.WSS else URLProtocol.WS
                url.port = port

                request() {}
            },
        ) {
            while (true) {

                var frame = incoming.receive();

                    GlobalScope.launch {
                        when (frame) {
                            is Frame.Binary -> {
                                val data = frame.readBytes()
                                val ansBytes = handler.handle(data)

                                send(Frame.Binary(true, ByteBuffer.wrap(ansBytes)))

                            }
                            else -> {}
                        }
                    }
            }

            }
    }

}


