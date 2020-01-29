package socket

import mqtt.Broker

actual class ServerSocket actual constructor(host: String, port: Int, backlog: Int, private val broker: Broker) {

    actual fun run() {
        TODO()
    }

    actual fun close() {
        TODO()
    }

}
