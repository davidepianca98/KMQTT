import mqtt.MQTTException
import mqtt.MQTTInputStream
import mqtt.MQTTOutputStream
import mqtt.Session
import mqtt.packets.*
import java.net.Socket
import java.util.*


class ClientHandler(
    private val client: Socket,
    private val broker: Broker
) {
    private val reader = MQTTInputStream(client.getInputStream())
    private val writer = MQTTOutputStream(client.getOutputStream())
    private var running = false
    private var clientId: String? = null
    private val session: Session =
        broker.sessions[clientId] ?: throw Exception("Session not found") // TODO throw exception correctly

    private val topicAliases = mutableMapOf<UInt, String>()

    fun run() {
        running = true

        while (running) { // TODO if not connect packet after reasonable amount of time from connection, close
            try {
                val packet = reader.readPacket()
                handlePacket(packet)
                // TODO if on qos12list try sending packet
            } catch (e: MQTTException) {
                disconnect(e.reasonCode)
            }
        }
    }

    private fun close() {
        running = false
        client.close()
    }

    private fun disconnect(reasonCode: ReasonCode) {
        writer.writePacket(MQTTDisconnect(reasonCode))
        close()
    }

    private fun handlePacket(packet: MQTTPacket) {
        when (packet) {
            is MQTTConnect -> handleConnect(packet) // TODO only handle connect once as first packet otherwise error
            is MQTTPublish -> handlePublish(packet)
        }
    }

    fun publish(packet: MQTTPublish) {
        when (packet.qos) {
            0 -> writer.writePacket(packet)
            1 -> session.qos1List.add(packet) // TODO for qos 1 and 2 verify that receive maximum isn't exceeded otherwise don't send
            2 -> session.qos2List.add(packet)
        }
    }

    private fun generateClientId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString()
        } while (broker.sessions[id] != null)
        return id
    }

    private fun handleConnect(packet: MQTTConnect) {
        // TODO authentication first with username, password or authentication method/data in properties (section 4.12)
        // TODO if will qos higher than supported, DISCONNECT with code 0x9B
        // TODO if will retain requested but not supported, DISCONNECT with code 0x9a
        var sessionPresent = false

        val clientId = if (packet.clientID.isEmpty()) generateClientId() else packet.clientID

        var session = broker.sessions[clientId]
        if (session != null) {
            if (session.connected) {
                // Send disconnect to the old connection and close it
                session.clientHandler.disconnect(ReasonCode.SESSION_TAKEN_OVER)

                // Send old will if present
                if (session.will?.willDelayInterval == 0u || packet.connectFlags.cleanStart) {
                    // TODO send session's will if present
                }
            }
            if (packet.connectFlags.cleanStart) {
                session = Session(packet, this)
                broker.sessions[clientId] = session
            } else {
                // Update the session with the new parameters
                session.clientHandler = this
                session.update(packet) // TODO maybe must not be done
                sessionPresent = true
            }
        } else {
            session = Session(packet, this)
            broker.sessions[clientId] = session
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
            if (it == 0 || it == 1) // TODO the server must still accept SUBSCRIBE with qos of 0,1,2
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
        this.clientId = clientId
    }

    private fun handlePublish(packet: MQTTPublish) {
        // TODO set DUP to 0 when propagating, but check in session if present packet with this packet id

        // TODO if qos 1 or 2 save in session and handle confirmation
        if (packet.qos > broker.maximumQos ?: 2) {
            throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        // TODO handle section 3.3.1.3 RETAIN

        // TODO last parts of section 3.3.2.1
        // TODO handle section 3.3.2.3.3, must be modified by broker

        // Handle topic alias
        var topic = packet.topicName
        packet.properties.topicAlias?.let {
            if (it == 0u || it > broker.maximumTopicAlias?.toUInt() ?: 0u)
                throw MQTTException(ReasonCode.TOPIC_ALIAS_INVALID)
            if (packet.topicName.isNotEmpty()) {
                topicAliases[it] = packet.topicName
            }
            topic = topicAliases[it] ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            packet.properties.topicAlias = null
        }

        if (packet.qos > 0 && broker.receiveMaximum != null) {
            if (session.qos1List.size + session.qos2List.size + 1 > broker.receiveMaximum)
                throw MQTTException(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED)
        }

        // TODO handle section 3.3.2.3.5
        // TODO handle section 3.3.2.3.6
        // TODO handle section 3.3.2.3.8

        broker.publish()
        // TODO section 3.3.4
    }
}
