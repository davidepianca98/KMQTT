package io.github.davidepianca98.socket.tls

import io.github.davidepianca98.close
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.ClientConnection
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.ServerSocket
import io.github.davidepianca98.socket.SocketState
import io.github.davidepianca98.socket.tcp.WebSocket

internal actual class TLSServerSocket actual constructor(
    private val broker: Broker,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocket(broker, selectCallback) {

    private val tlsServerContext = TLSServerContext(broker.tlsSettings!!)

    override fun accept(socket: Int, type: TCPSocketType) {
        try {
            val engine = TLSServerEngine(tlsServerContext)
            val sock = TLSSocket(socket, engine, writeRequest, buffer)
            clients[socket] = when (type) {
                TCPSocketType.MQTT -> ClientConnection(sock, broker)
                TCPSocketType.MQTTWS -> ClientConnection(WebSocket(sock), broker)
                TCPSocketType.CLUSTER -> {
                    TODO("Cluster not yet complete in Native")
                    //val clusterConnection = ClusterConnection(sock, broker)
                    //val remoteAddress = (channel.socket().remoteSocketAddress as InetSocketAddress).address.hostAddress
                    //broker.addClusterConnection(remoteAddress, clusterConnection)
                    //clusterConnection
                }
            }
        } catch (e: IOException) {
            close(socket)
        }
    }

    override fun close() {
        super.close()
        tlsServerContext.close()
    }
}
