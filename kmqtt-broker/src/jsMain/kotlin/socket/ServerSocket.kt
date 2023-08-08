package socket

import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import node.events.Event
import socket.tcp.Socket
import socket.tcp.WebSocket
import web.timers.setTimeout

internal actual open class ServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    protected val clients = mutableMapOf<String, Any?>()
    protected open val mqttSocket: node.net.Server = node.net.createServer { socket: node.net.Socket ->
        val localSocket = createSocket(socket)
        val connection = ClientConnection(localSocket, broker)
        clients[socket.socketId()] = connection
        localSocket.setAttachment(connection)

        onConnect(socket)
    }
    protected open val mqttWebSocket: node.net.Server = node.net.createServer { socket: node.net.Socket ->
        val localSocket = createSocket(socket)
        val connection = ClientConnection(WebSocket(localSocket), broker)
        clients[socket.socketId()] = connection
        localSocket.setAttachment(connection)

        onConnect(socket)
    }

    fun onConnect(socket: node.net.Socket) {
        socket.on(Event.ERROR) { error: Error ->
            println(error.message)
        }

        socket.on(Event.TIMEOUT) {
            socket.end()
        }

        socket.on(Event.END) {
            clients.remove(socket.socketId())
        }

        socket.on(Event.CLOSE) { _: Boolean ->
            clients.remove(socket.socketId())
        }
    }

    private fun node.net.Socket.socketId(): String = "$remoteAddress:$remotePort"

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

    open fun createSocket(socket: node.net.Socket): Socket {
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
