import mqtt.packets.ConnectFlags
import mqtt.packets.Qos
import mqtt.packets.mqttv4.MQTT4Connect

class MQTT4Client(
    address: String,
    port: Int
) {

    private val socket = ClientSocket(address, port, 1024)

    init {
        // TODO allow more configuration and TLS
        val connect = MQTT4Connect(
            "MQTT",
            4,
            ConnectFlags(false, false, false, Qos.AT_MOST_ONCE, false, false, false),
            1000,
            generateRandomClientId()
        )
        socket.send(connect.toByteArray())
    }

    fun publish() {
        TODO()
    }

    fun subscribe(topic: String) {
        TODO()
    }

    fun close() {
        TODO()
    }
}