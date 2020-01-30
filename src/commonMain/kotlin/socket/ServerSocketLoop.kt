package socket

import mqtt.Broker

class ServerSocketLoop(host: String, port: Int, backlog: Int, private val broker: Broker) {

    private val serverSocket = ServerSocket(host, port, backlog, broker)

    fun run() {
        while (serverSocket.isRunning()) {
            serverSocket.select(500) { clientConnection, state ->
                try {
                    when (state) {
                        SocketState.READ -> {
                            clientConnection.client.read()?.let {
                                clientConnection.dataReceived(it)
                            }
                        }
                        SocketState.WRITE -> {
                            clientConnection.client.sendRemaining()
                        }
                    }
                    return@select true
                } catch (e: SocketClosedException) {
                    return@select false
                } catch (e: IOException) {
                    clientConnection.closeWithException()
                    return@select false
                }
            }
            broker.cleanUpOperations()
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
