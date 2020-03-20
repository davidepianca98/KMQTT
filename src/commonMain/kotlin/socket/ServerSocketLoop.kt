package socket

import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import socket.tls.TLSServerSocket

open class ServerSocketLoop(private val broker: Broker) {

    private val serverSocket = if (broker.tlsSettings == null) ServerSocket(broker) else TLSServerSocket(broker)

    fun run() {
        while (serverSocket.isRunning()) {
            serverSocket.select(250) { clientConnection, state ->
                try {
                    handleEvent(clientConnection, state)
                    return@select true
                } catch (e: SocketClosedException) {
                    clientConnection.closedGracefully()
                    return@select false
                } catch (e: IOException) {
                    clientConnection.closedWithException()
                    return@select false
                }
            }
            broker.cleanUpOperations()
        }
    }

    private fun handleEvent(clientConnection: ClientConnection, state: SocketState) {
        when (state) {
            SocketState.READ -> {
                do {
                    val data = clientConnection.client.read()
                    data?.let {
                        clientConnection.dataReceived(it)
                    }
                } while (data != null)
            }
            SocketState.WRITE -> {
                clientConnection.client.sendRemaining()
            }
        }
    }

    fun stop() {
        serverSocket.close()
    }

    enum class SocketState {
        READ,
        WRITE
    }
}
