import mqtt.MQTTInputStream
import mqtt.MQTTOutputStream
import mqtt.Session
import mqtt.packets.*
import java.net.Socket


class ClientHandler(private val client: Socket, private val sessions: MutableMap<String, Session>) {
    private val reader = MQTTInputStream(client.getInputStream())
    private val writer = MQTTOutputStream(client.getOutputStream())
    private var running = false

    @ExperimentalUnsignedTypes
    fun run() {
        running = true

        while (running) { // TODO if not connect packet after reasonable amount of time from connection, close
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
                // TODO authentication first
                var sessionPresent: Boolean = false
                if (sessions[packet.clientID] != null) {
                    // TODO if already connected to another client the Server sends a DISCONNECT packet to the existing Client with Reason Code of 0x8E (Session taken over) as described in section 4.13 and MUST close the Network Connection of the existing Client . If the existing Client has a Will Message, that Will Message is published
                    //   If the Will Delay Interval of the existing Network Connection is 0 and there is a Will Message, it will be sent because the Network Connection is closed. If the Session Expiry Interval of the existing Network Connection is 0, or the new Network Connection has Clean Start set to 1 then if the existing Network Connection has a Will Message it will be sent because the original Session is ended on the takeover.
                    sessionPresent = true
                } else {
                    // TODO create new session
                    sessions[packet.clientID] = Session()
                }
                val session = sessions[packet.clientID] ?: error("") // TODO

                if (packet.connectFlags.cleanStart) {
                    session.clean()
                }
                if (packet.connectFlags.willFlag) {

                }
                // TODO reason codes and properties
                val connack = MQTTConnack(ConnectAcknowledgeFlags(sessionPresent), ReasonCodes.SUCCESS)
                writer.writePacket(connack)
            }
        }
    }
}
