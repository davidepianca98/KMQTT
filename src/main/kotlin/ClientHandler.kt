import mqtt.*
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
            } catch (e: Exception) {
                // TODO handle generic exception
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
            is MQTTPubrel -> handlePubrel(packet)
            is MQTTSubscribe -> handleSubscribe(packet)
            is MQTTUnsubscribe -> handleUnsubscribe(packet)
            is MQTTPingreq -> handlePingreq(packet)
        }
    }

    fun publish(packet: MQTTPublish) {
        when (packet.qos) {
            Qos.AT_MOST_ONCE -> writer.writePacket(packet)
            Qos.AT_LEAST_ONCE -> session.qos1List.add(packet) // TODO for qos 1 and 2 verify that client's receive maximum isn't exceeded otherwise don't send
            Qos.EXACTLY_ONCE -> session.qos2List.add(packet)
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
        broker.maximumQos?.let { maximumQos ->
            if (maximumQos == Qos.AT_MOST_ONCE || maximumQos == Qos.AT_LEAST_ONCE) // TODO the server must still accept SUBSCRIBE with qos of 0,1,2
                connackProperties.maximumQos = maximumQos.ordinal.toUInt()
            session.will?.qos?.let {
                if (it > maximumQos)
                    throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
            }
        }

        if (!broker.retainedAvailable) {
            connackProperties.retainAvailable = 0u
            if (session.will?.retain == true)
                throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)
        }

        broker.maximumPacketSize?.let {
            connackProperties.maximumPacketSize = it.toUInt()
        }

        if (packet.clientID.isEmpty())
            connackProperties.assignedClientIdentifier = clientId

        broker.maximumTopicAlias?.let {
            connackProperties.topicAliasMaximum = it.toUInt()
        }

        if (!broker.wildcardSubscriptionAvailable)
            connackProperties.wildcardSubscriptionAvailable = 0u

        if (!broker.subscriptionIdentifiersAvailable)
            connackProperties.subscriptionIdentifierAvailable = 0u

        if (!broker.sharedSubscriptionsAvailable)
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

        if (packet.qos > broker.maximumQos ?: Qos.EXACTLY_ONCE) {
            throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        broker.maximumPacketSize?.let {
            if (packet.toByteArray().size.toUInt() > it)
                throw MQTTException(ReasonCode.PACKET_TOO_LARGE)
        }

        if (!broker.retainedAvailable && packet.retain)
            throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)

        // TODO handle section 3.3.1.3 RETAIN

        // TODO last parts of section 3.3.2.1
        // TODO handle section 3.3.2.3.3, must be modified by broker

        // Handle topic alias
        val topic = getTopicOrAlias(packet)

        // Handle receive maximum
        if (packet.qos > Qos.AT_MOST_ONCE && broker.receiveMaximum != null) {
            if (session.qos1List.size + session.qos2List.size + 1 > broker.receiveMaximum)
                throw MQTTException(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED)
        }


        when (packet.qos) {
            Qos.AT_LEAST_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                writer.writePacket(MQTTPuback(packet.packetId!!, reasonCode))
                if (reasonCode != ReasonCode.SUCCESS)
                    return
            }
            Qos.EXACTLY_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                writer.writePacket(MQTTPubrec(packet.packetId!!, reasonCode))
                if (reasonCode == ReasonCode.SUCCESS)
                    session.qos2ListReceived.add(packet)
                return // Don't send the PUBLISH to other clients until PUBCOMP
            }
        }

        broker.publish(topic, packet.qos, packet.properties, packet.payload)
    }

    private fun getTopicOrAlias(packet: MQTTPublish): String {
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
        return topic
    }

    private fun qos12ReasonCode(packet: MQTTPublish): ReasonCode {
        // TODO check quota exceeded if necessary, topic name invalid
        val payloadFormatValid = packet.validatePayloadFormat()
        return if (!payloadFormatValid)
            ReasonCode.PAYLOAD_FORMAT_INVALID
        else if (session.isPacketIdInUse(packet.packetId!!))
            ReasonCode.PACKET_IDENTIFIER_IN_USE
        else
            ReasonCode.SUCCESS
    }

    private fun handlePubrel(packet: MQTTPubrel) {
        if (packet.reasonCode != ReasonCode.SUCCESS)
            return
        session.qos2ListReceived.firstOrNull { it.packetId == packet.packetId }?.let {
            writer.writePacket(MQTTPubcomp(packet.packetId, ReasonCode.SUCCESS, packet.properties))
            broker.publish(getTopicOrAlias(it), Qos.EXACTLY_ONCE, packet.properties, it.payload)
            // TODO remove packet from list
        } ?: run {
            writer.writePacket(MQTTPubcomp(packet.packetId, ReasonCode.PACKET_IDENTIFIER_NOT_FOUND, packet.properties))
        }
    }

    private fun handleSubscribe(packet: MQTTSubscribe) {
        val reasonCodes = packet.subscriptions.map { subscription ->
            if (!subscription.topicFilter.isValidTopic())
                return@map ReasonCode.TOPIC_FILTER_INVALID

            if (session.isPacketIdInUse(packet.packetIdentifier))
                return@map ReasonCode.PACKET_IDENTIFIER_IN_USE

            // TODO return quota exceeded if it happens

            val isShared = subscription.isShared()
            if (!broker.sharedSubscriptionsAvailable && isShared)
                return@map ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED

            if (packet.properties.subscriptionIdentifier.getOrNull(0) != null && !broker.subscriptionIdentifiersAvailable)
                return@map ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED

            if (!broker.wildcardSubscriptionAvailable && subscription.topicFilter.containsWildcard())
                return@map ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED

            session.subscriptions.removeIf { it.topicFilter == subscription.topicFilter }
            session.subscriptions += subscription
            if (!isShared) {
                // TODO send retained messages based on options and broker retained enabled
            }
            when (subscription.options.qos) {
                Qos.AT_MOST_ONCE -> ReasonCode.GRANTED_QOS0
                Qos.AT_LEAST_ONCE -> ReasonCode.GRANTED_QOS1
                Qos.EXACTLY_ONCE -> ReasonCode.GRANTED_QOS2
            }
        }
        if (ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED in reasonCodes) {
            disconnect(ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED)
            return
        }
        if (ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED in reasonCodes) {
            disconnect(ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED)
            return
        }
        if (ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED in reasonCodes) {
            disconnect(ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED)
            return
        }

        writer.writePacket(MQTTSuback(packet.packetIdentifier, reasonCodes))
    }

    private fun handleUnsubscribe(packet: MQTTUnsubscribe) {
        val reasonCodes = packet.topicFilters.map { topicFilter ->
            if (session.isPacketIdInUse(packet.packetIdentifier))
                return@map ReasonCode.PACKET_IDENTIFIER_IN_USE
            if (session.subscriptions.removeIf { it.topicFilter == topicFilter })
                return@map ReasonCode.SUCCESS
            else
                return@map ReasonCode.NO_SUBSCRIPTION_EXISTED
        }
        writer.writePacket(MQTTUnsuback(packet.packetIdentifier, reasonCodes))
    }

    private fun handlePingreq(packet: MQTTPingreq) {
        writer.writePacket(MQTTPingresp())
    }
}
