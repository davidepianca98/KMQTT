import mqtt.MQTTInputStream
import mqtt.MQTTOutputStream
import mqtt.MalformedPacketException
import mqtt.Session
import mqtt.packets.*
import java.net.Socket
import java.util.*


@ExperimentalUnsignedTypes
class ClientHandler(
    private val client: Socket,
    private val sessions: MutableMap<String, Session>,
    private val broker: Broker
) {
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
            } catch (e: MalformedPacketException) {
                writer.writePacket(MQTTDisconnect(e.reasonCode))
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

    private fun generateClientId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString()
        } while (sessions[id] != null)
        return id
    }

    private fun handleConnect(packet: MQTTConnect) {
        // TODO authentication first with username, password or authentication method/data in properties (section 4.12)
        // TODO if will qos higher than supported, DISCONNECT with code 0x9B
        // TODO if will retain requested but not supported, DISCONNECT with code 0x9a
        var sessionPresent = false

        val clientId = if (packet.clientID.isEmpty()) generateClientId() else packet.clientID

        var session = sessions[clientId]
        if (session != null) {
            if (session.connected) {
                // Send disconnect to the old connection and close it
                session.clientHandler.writer.writePacket(MQTTDisconnect(ReasonCode.SESSION_TAKEN_OVER))
                session.clientHandler.close()

                // Send old will if present
                if (session.will?.willDelayInterval == 0u || packet.connectFlags.cleanStart) {
                    // TODO send session's will if present
                }
            }
            if (packet.connectFlags.cleanStart) {
                session = Session(packet, this)
                sessions[clientId] = session
            } else {
                // Update the session with the new parameters
                session.clientHandler = this
                session.update(packet) // TODO maybe must not be done
                sessionPresent = true
            }
        } else {
            session = Session(packet, this)
            sessions[clientId] = session
        }

        //
        // CONNACK properties
        //
        val connackProperties = MQTTProperties()
        if (session.sessionExpiryInterval > broker.maximumSessionExpiryInterval) {
            session.sessionExpiryInterval = broker.maximumSessionExpiryInterval
            connackProperties.sessionExpiryInterval = broker.maximumSessionExpiryInterval
        }
        broker.receiveMaximum?.toUInt()?.let {
            if (session.receiveMaximum > it) {
                session.receiveMaximum = it
                connackProperties.receiveMaximum = it
            }
        }
        broker.maximumQos?.let {
            if (it == 0 || it == 1) // TODO the server must still accept SUBSCRIBE with qos of 0,1,2 but not PUBLISH (DISCONNECT with code 0x9B)
                connackProperties.maximumQos = it.toUInt()
        }

        if (!broker.retainedAvailable) // TODO disconnect client with code 0x9A if tries to send publish with retain 1 if not available
            connackProperties.retainAvailable = 0u

        broker.maximumPacketSize?.let {
            // TODO disconnect client with code 0x95 if client sends a PUBLISH bigger than limit
            connackProperties.maximumPacketSize = it.toUInt()
        }

        if (packet.clientID.isEmpty())
            connackProperties.assignedClientIdentifier = clientId

        broker.maximumTopicAlias?.let {
            // TODO if topic alias greater than this in PUBLISH close connection with topic_alias_invalid code
            connackProperties.topicAliasMaximum = it.toUInt()
        }

        if (!broker.wildcardSubscriptionAvailable) // TODO if not supported but subscribe with wildcard received disconnect with code 0xA2
            connackProperties.wildcardSubscriptionAvailable = 0u

        if (!broker.subscriptionIdentifiersAvailable) // TODO if not supported but subscribe with sub identifier disconnect with code 0xA1
            connackProperties.subscriptionIdentifierAvailable = 0u

        if (!broker.sharedSubscriptionsAvailable) // TODO if not supported but subscribe with shared sub send disconnect with code 0x9E
            connackProperties.sharedSubscriptionAvailable = 0u

        broker.serverKeepAlive?.let {
            if (broker.serverKeepAlive < session.keepAlive) {
                session.keepAlive = it
                connackProperties.serverKeepAlive = it.toUInt()
            }
        }

        // TODO implement section 3.2.2.3.15, 3.2.2.3.16 maybe

        val connack = MQTTConnack(ConnectAcknowledgeFlags(sessionPresent), ReasonCode.SUCCESS, connackProperties)
        writer.writePacket(connack)
    }
}
