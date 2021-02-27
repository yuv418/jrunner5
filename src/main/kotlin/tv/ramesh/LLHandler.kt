package tv.ramesh

import kotlinx.serialization.protobuf.ProtoBuf

abstract class LLHandler(handler: JRunnerClientHandler) {
    abstract val handler: JRunnerClientHandler
    abstract suspend fun listen(host: String, port: Int);
}