package socket

import mqtt.broker.Broker

expect open class ServerSocket(
    broker: Broker,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    fun isRunning(): Boolean

    fun select(timeout: Long)

    fun close()
}
