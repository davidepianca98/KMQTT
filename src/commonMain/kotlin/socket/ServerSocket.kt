package socket

import mqtt.broker.Broker

expect open class ServerSocket(broker: Broker) : ServerSocketInterface {

    fun isRunning(): Boolean

    fun select(timeout: Long, block: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean)

    fun close()
}
