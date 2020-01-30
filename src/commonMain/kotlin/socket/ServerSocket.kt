package socket

import mqtt.Broker
import mqtt.ClientConnection

expect class ServerSocket(host: String, port: Int, backlog: Int, broker: Broker) {

    fun isRunning(): Boolean

    fun select(timeout: Long, block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean)

    fun close()
}
