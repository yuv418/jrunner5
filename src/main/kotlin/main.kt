import tv.ramesh.JRunnerClientHandler
import tv.ramesh.ReflectionUtil;
import java.security.AllPermission
import java.security.BasicPermission
import java.lang.RuntimePermission
import java.security.Permission
import java.util.*
import java.net.*
import kotlin.concurrent.thread


fun main(args: Array<String>) {

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

    val port = 5791
    val server = ServerSocket(port);
    val handler = JRunnerClientHandler()
    println("DEBUG: Starting TCP socket server on port $port")

    while (true) {
        val client = server.accept()
        println("Found connection $client")
        println("Thread count is ${Thread.getAllStackTraces().keys.size}")

        thread {
            handler.run(client)
        }
    }
}

