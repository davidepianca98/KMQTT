package socket

import mqtt.Broker
import mqtt.ClientConnection

actual open class ServerSocket actual constructor(broker: Broker) {
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

}
