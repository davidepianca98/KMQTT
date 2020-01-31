package socket

import mqtt.Broker
import mqtt.ClientConnection

expect open class ServerSocket(broker: Broker) {

    fun isRunning(): Boolean

    fun select(timeout: Long, block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean)

    fun close()
}
