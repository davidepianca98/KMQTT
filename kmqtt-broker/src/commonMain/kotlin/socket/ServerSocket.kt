package socket

import mqtt.broker.Broker

expect open class ServerSocket(
    broker: Broker,
    selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : ServerSocketInterface {

    fun isRunning(): Boolean

    fun select(timeout: Long)

    fun close()
}
