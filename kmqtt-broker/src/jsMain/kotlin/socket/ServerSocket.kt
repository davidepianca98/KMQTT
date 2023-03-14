package socket

import await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import setTimeout
import socket.tcp.Socket
import socket.tcp.WebSocket
import kotlin.js.Promise

actual open class ServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    protected val clients = mutableMapOf<String, Any?>()
    protected open lateinit var mqttSocket: net.Server
    protected open lateinit var mqttWebSocket: net.Server

    fun onConnect(socket: net.Socket) {
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
        initialize(broker)
    }

    open fun initialize(broker: Broker) {
        mqttSocket = net.createServer { socket: net.Socket ->
            val localSocket = createSocket(socket)
            val connection = ClientConnection(localSocket, broker)
            clients[socket.socketId()] = connection
            localSocket.setAttachment(connection)

            onConnect(socket)
        }
        mqttWebSocket = net.createServer { socket: net.Socket ->
            val localSocket = createSocket(socket)
            val connection = ClientConnection(WebSocket(localSocket), broker)
            clients[socket.socketId()] = connection
            localSocket.setAttachment(connection)

            onConnect(socket)
        }

        mqttSocket.listen(broker.port, broker.host)

        if (broker.enableUdp) {
            TODO("UDP in JS not yet implemented")
        }

        if (broker.webSocketPort != null) {
            mqttWebSocket.listen(broker.webSocketPort, broker.host)
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
        mqttWebSocket.close()
    }

    actual fun isRunning(): Boolean = mqttSocket.listening || mqttWebSocket.listening

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
