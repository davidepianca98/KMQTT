package socket

import mqtt.Broker

expect class ServerSocket(host: String, port: Int, backlog: Int, broker: Broker) {

    fun run()

    fun close()
}
