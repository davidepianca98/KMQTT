import mqtt.MQTTInputStream
import mqtt.Session
import mqtt.packets.MQTTConnect
import mqtt.packets.MQTTPacket
import java.net.Socket


class ClientHandler(private val client: Socket) {
    private val reader = MQTTInputStream(client.getInputStream())
    private val writer = client.getOutputStream()
    private var running = false
    private val session = Session()

    @ExperimentalUnsignedTypes
    fun run() {
        running = true

        while (running) {
            try {
                val packet = reader.readPacket()
                handlePacket(packet)
            } catch (ex: Exception) {
                // TODO: Implement exception handling
                close()
            }
        }
    }

    private fun close() {
        running = false
        client.close()
    }

    private fun handlePacket(packet: MQTTPacket) {
        when (packet) {
            is MQTTConnect -> {

            }
        }
    }
}
