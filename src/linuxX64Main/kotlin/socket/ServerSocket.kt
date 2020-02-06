package socket

import mqtt.broker.Broker
import mqtt.broker.ClientConnection

actual open class ServerSocket actual constructor(private val broker: Broker) : ServerSocketInterface {

    actual fun isRunning(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun select(
        timeout: Long,
        block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun accept(socket: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
