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
            is MQTTConnect -> handleConnect(packet)
        }
    }

    private fun handleConnect(packet: MQTTConnect) {
        // TODO authentication first with username, password or authentication method/data in properties
        var sessionPresent = false
        var session = sessions[packet.clientID]
        if (session != null) {
            if (session.connected) {
                // Send disconnect to the old connection and close it
                session.clientHandler.writer.writePacket(MQTTDisconnect(ReasonCode.SESSION_TAKEN_OVER))
                session.clientHandler.close()

                // Send old will if present
                if (session.will?.willDelayInterval == 0 || packet.connectFlags.cleanStart) {
                    // TODO send session's will if present
                }
            }
            if (packet.connectFlags.cleanStart) {
                session = Session(packet, this)
                sessions[packet.clientID] = session
            } else {
                // Update the session with the new parameters
                session.clientHandler = this
                session.update(packet) // TODO maybe must not be done
                sessionPresent = true
            }
        } else {
            session = Session(packet, this)
            sessions[packet.clientID] = session
        }

        // TODO reason codes and properties
        val connack = MQTTConnack(ConnectAcknowledgeFlags(sessionPresent), ReasonCode.SUCCESS)
        writer.writePacket(connack)
    }
}
