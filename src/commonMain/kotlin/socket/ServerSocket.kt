package socket

import mqtt.broker.Broker
import mqtt.broker.ClientConnection

expect open class ServerSocket(broker: Broker) {

    fun isRunning(): Boolean

    fun select(timeout: Long, block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean)

    fun close()
}
