package io.github.davidepianca98

import io.github.davidepianca98.socket.SocketInterface
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class WebSocket(host: String, port: Int, path: String = "/mqtt", ready: () -> Unit) : SocketInterface {

    //TODO: handle exception properly inside of scope
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default).apply {
    }
    private var session: WebSocketSession? = null

    init {
        val client = HttpClient() {
            install(WebSockets)
        }
        scope.launch {
            session = client.webSocketSession(method = HttpMethod.Get, host = host, port = port, path = path) {
                header(HttpHeaders.SecWebSocketProtocol, "mqtt")
            }

            println("Connected to WebSocket")
            ready()
        }
    }

    override fun send(data: UByteArray) {
        println("Sending data to WebSocket - outside launch")
        scope.launch {
            println("Sending data to WebSocket")
            session?.send(Frame.Binary(true, data.toByteArray()))
        }
    }

    override fun sendRemaining() {
        //NO-OP, we let send do the work for us
    }

    override fun read(): UByteArray? {
        val frame = session?.incoming?.tryReceive()?.getOrNull() ?: return null

        return when (frame) {
            is Frame.Binary -> return frame.data.asUByteArray()
            else -> null
        }
    }

    override fun close() {
        scope.launch {
            session?.close()
        }
    }
}
