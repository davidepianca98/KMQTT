package socket

import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import setTimeout
import socket.tcp.Socket
import socket.tcp.WebSocket

actual open class ServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    protected val clients = mutableMapOf<String, Any?>()
    protected open val mqttSocket: net.Server = net.createServer { socket: net.Socket ->
        val localSocket = createSocket(socket)
        val connection = ClientConnection(localSocket, broker)
        clients[socket.socketId()] = connection
        localSocket.setAttachment(connection)

        onConnect(socket)
    }
    protected open val mqttWebSocket: net.Server = net.createServer { socket: net.Socket ->
        val localSocket = createSocket(socket)
        val connection = ClientConnection(WebSocket(localSocket), broker)
        clients[socket.socketId()] = connection
        localSocket.setAttachment(connection)

        onConnect(socket)
    }

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
        mqttSocket.listen(broker.port, broker.host) {
            doLater()
        }

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

    protected fun doLater() {
        setTimeout({
            broker.cleanUpOperations()
            doLater()
        }, 1000)
    }

    open fun createSocket(socket: net.Socket): Socket {
        return Socket(socket, selectCallback)
    }

    actual fun close() {
        mqttSocket.close()
        mqttWebSocket.close()
    }

    actual fun isRunning(): Boolean = false

    actual fun select(timeout: Long) {
        // Not doing anything as NodeJs is callback based
    }

    final override fun addClusterConnection(address: String): ClusterConnection? {
        TODO("Cluster in JS not yet implemented")
    }
}
