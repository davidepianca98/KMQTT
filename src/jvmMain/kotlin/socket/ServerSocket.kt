package socket

import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.cluster.ClusterConnection
import mqtt.broker.cluster.ClusterDiscoveryConnection
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*


actual open class ServerSocket actual constructor(private val broker: Broker) : ServerSocketInterface {

    private val mqttSocket = ServerSocketChannel.open()
    private val clusteringSocket = ServerSocketChannel.open()
    private val discoverySocket = DatagramChannel.open()
    private val selector: Selector = Selector.open()

    protected var sendBuffer: ByteBuffer = ByteBuffer.allocate(broker.maximumPacketSize.toInt())
    protected var receiveBuffer: ByteBuffer = ByteBuffer.allocate(broker.maximumPacketSize.toInt())

    init {
        mqttSocket.configureBlocking(false)
        mqttSocket.bind(InetSocketAddress(broker.host, broker.port), broker.backlog)
        mqttSocket.register(selector, SelectionKey.OP_ACCEPT)

        if (broker.cluster != null) {
            clusteringSocket.configureBlocking(false)
            clusteringSocket.bind(InetSocketAddress(broker.host, broker.cluster.tcpPort))
            clusteringSocket.register(selector, SelectionKey.OP_ACCEPT)

            discoverySocket.configureBlocking(false)
            discoverySocket.bind(InetSocketAddress(broker.host, broker.cluster.discoveryPort))
            val datagramKey = discoverySocket.register(selector, SelectionKey.OP_READ)
            val clusterConnection = ClusterDiscoveryConnection(UDPSocket(datagramKey), broker)
            datagramKey.attach(clusterConnection)
            clusterConnection.sendDiscovery(broker.cluster.discoveryPort)
        }
    }

    open fun createSocket(socketKey: SelectionKey): Socket {
        return Socket(socketKey, sendBuffer, receiveBuffer)
    }

    private fun generateDataObject(channel: SocketChannel, socket: Socket): Any? {
        return when (channel.socket().localPort) {
            broker.port -> ClientConnection(socket, broker)
            broker.cluster?.tcpPort -> {
                val clusterConnection = ClusterConnection(socket)
                val remoteAddress = (channel.socket().remoteSocketAddress as InetSocketAddress).address.hostAddress
                broker.addClusterConnection(remoteAddress, clusterConnection)
                clusterConnection
            }
            else -> null
        }
    }

    private fun accept(socket: SelectionKey) {
        try {
            val channel = (socket.channel() as ServerSocketChannel).accept()
            channel.configureBlocking(false)

            val socketKey = channel.register(selector, SelectionKey.OP_READ)
            socketKey.attach(generateDataObject(channel, createSocket(socketKey)))
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
    }

    actual fun close() {
        selector.close()
        mqttSocket.close()
        clusteringSocket.close()
        discoverySocket.close()
    }

    actual fun isRunning(): Boolean = selector.isOpen

    actual fun select(
        timeout: Long,
        block: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
    ) {
        if (isRunning()) {
            val count = selector.select(timeout)
            if (count > 0) {
                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    iterator.remove()
                    if (key.isValid) {
                        val attachment = key.attachment()
                        if (key.isValid && key.isAcceptable)
                            accept(key)
                        if (key.isValid && key.isWritable)
                            block(attachment, ServerSocketLoop.SocketState.WRITE)
                        if (key.isValid && key.isReadable)
                            block(attachment, ServerSocketLoop.SocketState.READ)
                    }
                }
            }
        }
    }

    override fun addClusterConnection(address: String): ClusterConnection? {
        if (broker.cluster != null) {
            val channel = SocketChannel.open(InetSocketAddress(address, broker.cluster.tcpPort))
            channel.configureBlocking(false)
            val socketKey = channel.register(selector, SelectionKey.OP_READ)
            val connection = generateDataObject(channel, createSocket(socketKey)) as ClusterConnection?
            socketKey.attach(connection)
            return connection
        }
        return null
    }

}
