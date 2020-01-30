package socket

import mqtt.Broker
import mqtt.ClientConnection
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel


actual class ServerSocket actual constructor(host: String, port: Int, backlog: Int, private val broker: Broker) {

    private val socket = ServerSocketChannel.open()
    private val selector = Selector.open()

    init {
        socket.configureBlocking(false)
        socket.bind(InetSocketAddress(host, port), backlog)
        socket.register(selector, SelectionKey.OP_ACCEPT)
    }

    private fun SelectionKey.accept() {
        try {
            val channel = (channel() as ServerSocketChannel).accept()
            channel.configureBlocking(false)
            val socket = Socket()
            val clientConnection = ClientConnection(socket, broker)
            val key = channel.register(selector, SelectionKey.OP_READ, clientConnection)
            socket.key = key
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
    }

    actual fun close() {
        selector.close()
        socket.close()
    }

    actual fun isRunning(): Boolean = selector.isOpen

    actual fun select(
        timeout: Long,
        block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean
    ) {
        if (isRunning()) {
            val count = selector.select(timeout)
            if (count > 0) {
                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    iterator.remove()
                    if (key.isValid) {
                        val clientConnection = (key.attachment() as ClientConnection?)
                        when {
                            key.isAcceptable -> key.accept()
                            key.isReadable -> block(clientConnection!!, ServerSocketLoop.SocketState.READ)
                            key.isWritable -> block(clientConnection!!, ServerSocketLoop.SocketState.WRITE)
                        }
                    }
                }
            }
        }
    }

}
