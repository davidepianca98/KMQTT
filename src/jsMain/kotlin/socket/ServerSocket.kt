package socket

import await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import net.createServer
import setTimeout
import socket.tcp.Socket
import kotlin.js.Promise

actual open class ServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : ServerSocketInterface {

    private val clients = mutableMapOf<String, Any?>()
    private val mqttSocket = createServer { socket: net.Socket ->
        val localSocket = createSocket(socket)
        clients[socket.socketId()] = ClientConnection(localSocket, broker)
        localSocket.setAttachment(clients[socket.socketId()])

        socket.on("error") { error: Error ->
            println(error.message)
        }

        socket.on("timeout", {
            socket.end()
        } as () -> Unit)

        socket.on("end", {
            clients.remove(socket.socketId())
        } as () -> Unit)

        socket.on("close") { _: Boolean ->
            clients.remove(socket.socketId())
        }
    }

    private fun net.Socket.socketId(): String = "$remoteAddress:$remotePort"

    init {
        mqttSocket.listen(broker.port, broker.host)

        if (broker.enableUdp) {
            TODO("UDP in JS not yet implemented")
        }

        if (broker.webSocketPort != null) {
            TODO("WebSocket in JS not yet implemented")
        }

        if (broker.cluster != null) {
            TODO("Cluster in JS not yet implemented")
        }
    }

    open fun createSocket(socket: net.Socket): Socket {
        return Socket(socket, selectCallback)
    }

    actual fun close() {
        mqttSocket.close()
    }

    actual fun isRunning(): Boolean = mqttSocket.listening

    private fun sleep(ms: Long): Promise<Any> {
        return Promise { resolve, _ ->
            setTimeout(resolve, ms)
        }
    }

    actual fun select(timeout: Long) {
        if (isRunning()) {
            GlobalScope.launch(Dispatchers.Default) {
                sleep(timeout).await()
            }
        }
    }

    final override fun addClusterConnection(address: String): ClusterConnection? {
        TODO("Cluster in JS not yet implemented")
    }
}
