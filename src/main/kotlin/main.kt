import io.ktor.http.cio.websocket.*
import tv.ramesh.JRunnerClientHandler
import tv.ramesh.TCPServer
import tv.ramesh.WSClient
import java.security.AllPermission
import java.security.BasicPermission
import java.security.Permission
import kotlin.system.exitProcess


@ExperimentalWebSocketExtensionApi
suspend fun main(args: Array<String>) {

    System.setSecurityManager(object : SecurityManager() {
        override fun checkPermission(perm: Permission) {
            if (perm is AllPermission || perm is BasicPermission) { // Literally block users from trying anything sneaky
                for (elem in Thread.currentThread().stackTrace) {
                    if (elem.className.startsWith("sandboxed")) {
                        throw SecurityException()
                    }
                }
            }
        }
    })

    var bindHost = System.getenv("JRUNNER5_BINDHOST") ?: printErrorExit("You must provide a valid host for JRunner5 to use/connect to.")
    val bindPort = System.getenv("JRUNNER5_BINDPORT").toIntOrNull() ?: printErrorExit("You must provide a valid port for JRunner5 to use/connect to.")
    var bindMode = System.getenv("JRUNNER5_BINDMODE")

    when (bindMode) {
        "ws" ->  WSClient(JRunnerClientHandler(), false).listen(bindHost, bindPort)
        "wss" -> WSClient(JRunnerClientHandler(), true).listen(bindHost, bindPort)
        "tcp" -> TCPServer(JRunnerClientHandler()).listen(bindHost, bindPort)
        else -> printErrorExit("You must provide a valid mode for JRunner5 to start in.")
    }

}

fun <T> printErrorExit(msg: String): T {
    System.out.println(msg)
    exitProcess(1)

}
