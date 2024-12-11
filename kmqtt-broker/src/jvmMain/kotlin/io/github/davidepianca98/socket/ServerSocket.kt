package io.github.davidepianca98.socket

import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.ClientConnection
import io.github.davidepianca98.mqtt.broker.cluster.ClusterConnection
import io.github.davidepianca98.mqtt.broker.cluster.ClusterDiscoveryConnection
import io.github.davidepianca98.mqtt.broker.udp.UDPConnectionsMap
import org.xbill.DNS.ARecord
import org.xbill.DNS.Lookup
import org.xbill.DNS.Type
import io.github.davidepianca98.socket.tcp.Socket
import io.github.davidepianca98.socket.tcp.WebSocket
import io.github.davidepianca98.socket.udp.UDPSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.*


internal actual open class ServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    private val mqttSocket = ServerSocketChannel.open()
    private val mqttUdpSocket = DatagramChannel.open()
    private val clusteringSocket = ServerSocketChannel.open()
    private val discoverySocket = DatagramChannel.open()
    private val mqttWebSocket = ServerSocketChannel.open()
    private val selector: Selector = Selector.open()

    init {
        mqttSocket.configureBlocking(false)
        mqttSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true)
        mqttSocket.bind(InetSocketAddress(broker.host, broker.port), broker.backlog)
        mqttSocket.register(selector, SelectionKey.OP_ACCEPT)

        if (broker.enableUdp) {
            mqttUdpSocket.configureBlocking(false)
            mqttUdpSocket.bind(InetSocketAddress(broker.host, broker.port))
            val datagramKey = mqttUdpSocket.register(selector, SelectionKey.OP_READ)
            datagramKey.attach(UDPConnectionsMap(UDPSocket(datagramKey), broker))
        }

        if (broker.webSocketPort != null) {
            mqttWebSocket.configureBlocking(false)
            mqttWebSocket.bind(InetSocketAddress(broker.host, broker.webSocketPort), broker.backlog)
            mqttWebSocket.register(selector, SelectionKey.OP_ACCEPT)
        }

        if (broker.cluster != null) {
            clusteringSocket.configureBlocking(false)
            clusteringSocket.bind(InetSocketAddress(broker.host, broker.cluster.tcpPort))
            clusteringSocket.register(selector, SelectionKey.OP_ACCEPT)

            if (!broker.cluster.dnsDiscovery) {
                discoverySocket.configureBlocking(false)
                discoverySocket.bind(InetSocketAddress(broker.host, broker.cluster.discoveryPort))
                val datagramKey = discoverySocket.register(selector, SelectionKey.OP_READ)
                val clusterConnection = ClusterDiscoveryConnection(UDPSocket(datagramKey), broker)
                datagramKey.attach(clusterConnection)
                clusterConnection.sendDiscovery(broker.cluster.discoveryPort)
            } else {
                val localAddress = InetAddress.getLocalHost().hostAddress
                Lookup("tasks." + broker.cluster.dnsName, Type.A).run()?.forEach {
                    val aRecord = it as ARecord
                    val address = aRecord.address.hostAddress
                    if (localAddress != address) {
                        addClusterConnection(address)?.let { clusterConnection ->
                            broker.addClusterConnection(address, clusterConnection)
                        }
                    }
                } ?: println("Empty DNS")
            }
        }
    }

    open fun createSocket(channel: SocketChannel, socketKey: SelectionKey): Socket {
        val sendBuffer = ByteBuffer.allocate(broker.maximumPacketSize.toInt())
        val receiveBuffer = ByteBuffer.allocate(broker.maximumPacketSize.toInt())
        return Socket(channel, socketKey, sendBuffer, receiveBuffer)
    }

    private fun generateDataObject(channel: SocketChannel, socket: SocketInterface): Any? {
        return when (channel.socket().localPort) {
            broker.port, broker.webSocketPort -> ClientConnection(socket, broker)
            broker.cluster?.tcpPort -> {
                val clusterConnection = ClusterConnection(socket, broker)
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
            val newSocket = if (channel.socket().localPort == broker.webSocketPort) {
                WebSocket(createSocket(channel, socketKey))
            } else {
                createSocket(channel, socketKey)
            }
            socketKey.attach(generateDataObject(channel, newSocket))
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    actual fun close() {
        selector.close()
        mqttSocket.close()
        mqttWebSocket.close()
        clusteringSocket.close()
        discoverySocket.close()
    }

    actual fun isRunning(): Boolean = selector.isOpen

    actual fun select(timeout: Long) {
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
                            selectCallback(attachment, SocketState.WRITE)
                        if (key.isValid && key.isReadable)
                            selectCallback(attachment, SocketState.READ)
                    }
                }
            }
        }
    }

    actual override fun addClusterConnection(address: String): ClusterConnection? {
        if (broker.cluster != null) {
            val channel = SocketChannel.open(InetSocketAddress(address, broker.cluster.tcpPort))
            channel.configureBlocking(false)
            val socketKey = channel.register(selector, SelectionKey.OP_READ)
            val connection = generateDataObject(channel, createSocket(channel, socketKey)) as ClusterConnection?
            socketKey.attach(connection)
            return connection
        }
        return null
    }

}
