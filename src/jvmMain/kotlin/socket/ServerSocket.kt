package socket

import mqtt.Broker
import mqtt.ClientConnection
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel


actual class ServerSocket actual constructor(host: String, port: Int, backlog: Int, private val broker: Broker) {

    private val socket = ServerSocketChannel.open()
    private val selector = Selector.open()
    private val buf = ByteBuffer.allocate(broker.maximumPacketSize.toInt())

    init {
        socket.configureBlocking(false)
        socket.bind(InetSocketAddress(host, port), backlog)
        socket.register(selector, SelectionKey.OP_ACCEPT)
    }

    actual fun run() {
        while (selector.isOpen) {
            val count = selector.select()
            if (count > 0) {
                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    iterator.remove()
                    if (key.isValid)
                        handleKey(key)
                }
            }
        }
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

    private fun SelectionKey.read() {
        val channel = channel() as SocketChannel
        val clientConnection = (attachment() as ClientConnection?)
        buf.clear()
        try {
            val length = channel.read(buf)
            if (length >= 0) {
                buf.flip()
                val array = ByteArray(length)
                buf.get(array, 0, length)
                clientConnection?.dataReceived(array.toUByteArray())
            } else {
                cancel()
                channel.close()
            }
        } catch (e: java.io.IOException) {
            cancel()
            channel.close()
            clientConnection?.closeWithException()
        }
    }

    private fun SelectionKey.write() {
        val clientConnection = (attachment() as ClientConnection?)
        try {
            clientConnection?.client?.sendRemaining()
            interestOps(SelectionKey.OP_READ)
        } catch (e: java.io.IOException) {
            clientConnection?.closeWithException()
        }
    }

    private fun handleKey(key: SelectionKey) {
        if (key.isAcceptable)
            key.accept()
        if (key.isWritable)
            key.write()
        if (key.isReadable)
            key.read()
    }

    actual fun close() {
        selector.close()
        socket.close()
    }

}
