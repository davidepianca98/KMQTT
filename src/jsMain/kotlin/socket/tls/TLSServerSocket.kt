package socket.tls

import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import socket.ServerSocket
import socket.ServerSocketLoop
import socket.tcp.WebSocket
import tls.TlsOptions

actual class TLSServerSocket actual constructor(
    private val broker: Broker,
    private val selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : ServerSocket(broker, selectCallback) {

    private fun TlsOptions(): TlsOptions = js("{}") as TlsOptions

    private val tlsOptions = TlsOptions().apply {
        pfx = fs.readFileSync(broker.tlsSettings!!.keyStoreFilePath, null as String?)
        passphrase = broker.tlsSettings.keyStorePassword
        requestCert = broker.tlsSettings.requireClientCertificate
    }
    override lateinit var mqttSocket: net.Server

    override lateinit var mqttWebSocket: net.Server

    // TODO this duplication is necessary because overriding the properties, they would get initialized too late and get
    //      undefined
    override fun initialize(broker: Broker) {
        mqttSocket = tls.createServer(tlsOptions) { socket: tls.TLSSocket ->
            val localSocket = createSocket(socket)
            val connection = ClientConnection(localSocket, broker)
            clients[socket.socketId()] = connection
            localSocket.setAttachment(connection)

            onConnect(socket)
        }

        mqttWebSocket = tls.createServer(tlsOptions) { socket: tls.TLSSocket ->
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

    private fun tls.TLSSocket.socketId(): String = "$remoteAddress:$remotePort"

    private fun createSocket(socket: tls.TLSSocket): TLSSocket {
        return TLSSocket(socket, selectCallback)
    }
}
