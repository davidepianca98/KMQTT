import mqtt.*
import mqtt.packets.*
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import kotlin.math.min


class ClientConnection(
    private val client: Socket,
    private val broker: Broker
) {

    companion object {
        private const val DEFAULT_MAX_SEND_QUOTA = 65535u
    }

    private val reader = MQTTInputStream(client.getInputStream(), broker.maximumPacketSize)
    private val writer = MQTTOutputStream(client.getOutputStream())

    private var clientId: String? = null
    private val session: Session
        get() = broker.getSession(clientId) ?: throw Exception("Session not found")

    // Client connection state
    private var running = false
    private val topicAliasesClient = mutableMapOf<UInt, String>()
    private val topicAliasesServer = mutableMapOf<String, UInt>()
    private var maxSendQuota: UInt = DEFAULT_MAX_SEND_QUOTA // Client receive maximum
    private var sendQuota: UInt = DEFAULT_MAX_SEND_QUOTA
    // TODO don't send packets larger than this, remove certain properties based on the specific packet if possible, if null no limit
    private var maximumPacketSize: UInt? = null
    private var topicAliasMaximum = 0u

    private var keepAlive = 0
    private var connectHandled = false

    fun run() {
        running = true

        while (running) {
            try {
                val packet = reader.readPacket()
                handlePacket(packet)
                // TODO if on pendingSendMessages try sending packet, checking if expired don't send
            } catch (e: MQTTException) {
                disconnect(e.reasonCode)
            } catch (e: SocketTimeoutException) {
                if (session.isConnected()) {
                    sendWill()
                    disconnect(ReasonCode.KEEP_ALIVE_TIMEOUT)
                } else {
                    disconnect(ReasonCode.MAXIMUM_CONNECT_TIME)
                }
            } catch (e: IOException) {
                sendWill()
            } catch (e: Exception) {
                e.printStackTrace()
                disconnect(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
            }
        }
        session.disconnected()
    }

    private fun close() {
        running = false
        client.close()
    }

    private fun disconnect(reasonCode: ReasonCode) {
        writer.writePacket(MQTTDisconnect(reasonCode))
        close()
        if (reasonCode != ReasonCode.SUCCESS)
            sendWill()
    }

    private fun handlePacket(packet: MQTTPacket) {
        when (packet) {
            is MQTTConnect -> {
                try {
                    if (!connectHandled) {
                        handleConnect(packet)
                        connectHandled = true
                    } else
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                } catch (e: MQTTException) {
                    writer.writePacket(MQTTConnack(ConnectAcknowledgeFlags(false), e.reasonCode))
                    close()
                }
            }
            is MQTTPublish -> handlePublish(packet)
            is MQTTPuback -> handlePuback(packet)
            is MQTTPubrec -> handlePubrec(packet)
            is MQTTPubrel -> handlePubrel(packet)
            is MQTTPubcomp -> handlePubcomp(packet)
            is MQTTSubscribe -> handleSubscribe(packet)
            is MQTTUnsubscribe -> handleUnsubscribe(packet)
            is MQTTPingreq -> handlePingreq()
            is MQTTDisconnect -> handleDisconnect(packet)
            is MQTTAuth -> handleAuth(packet)
            else -> throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
    }

    fun publish(
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTTProperties,
        payload: ByteArray?
    ) {
        val packetId = if (qos >= Qos.AT_MOST_ONCE) session.generatePacketId() else null

        var packetTopicName: String = topicName
        if (topicAliasMaximum > 0u) {
            topicAliasesServer[topicName]?.let {
                packetTopicName = ""
                properties.topicAlias = it
            } ?: run {
                if (topicAliasesServer.size < topicAliasMaximum.toInt() - 1) {
                    topicAliasesServer[topicName] = topicAliasesServer.size.toUInt()
                    packetTopicName = topicName
                    properties.topicAlias = topicAliasesServer[topicName]
                }
            }
        }

        val packet = MQTTPublish(
            retain,
            qos,
            dup,
            packetTopicName,
            packetId,
            properties,
            payload
        )

        if (packet.messageExpiryIntervalExpired())
            return
        // Update the expiry interval if present
        packet.properties.messageExpiryInterval?.let {
            packet.properties.messageExpiryInterval =
                it - ((System.currentTimeMillis() - packet.timestamp) / 1000).toUInt()
        }

        if (packet.qos == Qos.AT_LEAST_ONCE || packet.qos == Qos.EXACTLY_ONCE) {
            if (sendQuota <= 0u)
                return
            session.sendQosBiggerThanZero(packet) {
                writer.writePacket(packet)
                decrementSendQuota()
            }
        } else {
            writer.writePacket(packet)
        }
    }

    private fun generateClientId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString()
        } while (broker.sessions[id] != null)
        return id
    }

    private fun incrementSendQuota() {
        if (++sendQuota >= maxSendQuota)
            sendQuota = maxSendQuota
    }

    private fun decrementSendQuota() {
        if (sendQuota > 0u)
            sendQuota--
    }

    private fun handleAuthentication(packet: MQTTConnect): Boolean {
        if (packet.userName != null || packet.password != null) {
            if (broker.authentication?.authenticate(packet.userName, packet.password) == false) {
                val connack = MQTTConnack(ConnectAcknowledgeFlags(false), ReasonCode.NOT_AUTHORIZED)
                writer.writePacket(connack)
                close()
                return false
            }
        }
        // TODO enhanced authentication method/data in properties (section 4.12)
        return true
    }

    private fun sendWill() {
        val will = broker.sessions[clientId]?.will ?: return
        val properties = MQTTProperties()
        properties.payloadFormatIndicator = will.payloadFormatIndicator
        properties.messageExpiryInterval = will.messageExpiryInterval
        properties.contentType = will.contentType
        properties.responseTopic = will.responseTopic
        properties.correlationData = will.correlationData
        properties.userProperty += will.userProperty
        broker.publish(will.retain, will.topic, will.qos, false, properties, will.payload)
        // The will must be removed after sending
        session.will = null
    }

    private fun handleConnect(packet: MQTTConnect) {
        if (!handleAuthentication(packet))
            return

        var sessionPresent = false

        val clientId = if (packet.clientID.isEmpty()) generateClientId() else packet.clientID

        var session = broker.sessions[clientId]
        if (session != null) {
            if (session.isConnected()) {
                // Send disconnect to the old connection and close it
                session.clientConnection.disconnect(ReasonCode.SESSION_TAKEN_OVER)

                // Send old will if present
                if (session.will?.willDelayInterval == 0u || packet.connectFlags.cleanStart) {
                    sendWill()
                }
            }
            if (packet.connectFlags.cleanStart) {
                session = Session(packet, this)
                broker.sessions[clientId] = session
            } else {
                // Update the session with the new parameters
                session.clientConnection = this
                session.will = Will.buildWill(packet)
                session.sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
                // TODO resend all unacknowledged PUBLISH and PUBREL, with dup = 1
                sessionPresent = true
            }
        } else {
            session = Session(packet, this)
            broker.sessions[clientId] = session
        }

        keepAlive = packet.keepAlive
        client.soTimeout = ((keepAlive * 1000).toDouble() * 1.5).toInt()

        sendQuota = packet.properties.receiveMaximum ?: DEFAULT_MAX_SEND_QUOTA
        maxSendQuota = packet.properties.receiveMaximum ?: DEFAULT_MAX_SEND_QUOTA
        maximumPacketSize = packet.properties.maximumPacketSize
        topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0u

        //
        // CONNACK properties
        //
        val connackProperties = MQTTProperties()
        if (session.sessionExpiryInterval > broker.maximumSessionExpiryInterval) {
            session.sessionExpiryInterval = broker.maximumSessionExpiryInterval
            connackProperties.sessionExpiryInterval = broker.maximumSessionExpiryInterval
        }
        broker.receiveMaximum?.toUInt()?.let {
            if (maxSendQuota > it) {
                maxSendQuota = it
                connackProperties.receiveMaximum = it
            }
        }
        broker.maximumQos?.let { maximumQos ->
            if (maximumQos == Qos.AT_MOST_ONCE || maximumQos == Qos.AT_LEAST_ONCE)
                connackProperties.maximumQos = maximumQos.value.toUInt()
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
            if (broker.serverKeepAlive < keepAlive) {
                keepAlive = it
                connackProperties.serverKeepAlive = it.toUInt()
            }
        }

        packet.properties.requestResponseInformation?.let { requestResponseInformation ->
            if (requestResponseInformation !in 0u..1u)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            if (requestResponseInformation == 1u) {
                broker.responseInformation?.let {
                    connackProperties.responseInformation = it
                }
            }
        }

        packet.properties.requestProblemInformation?.let { requestProblemInformation ->
            if (requestProblemInformation !in 0u..1u)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            // May send reason string here
        }

        // TODO implement section 3.2.2.3.16 and 4.11 for Server redirection

        val connack = MQTTConnack(ConnectAcknowledgeFlags(sessionPresent), ReasonCode.SUCCESS, connackProperties)
        writer.writePacket(connack)
        this.clientId = clientId
        session.connected()
    }

    private fun checkAuthorization(topicName: String): Boolean {
        return broker.authorization?.authorize(topicName) != false
    }

    private fun handlePublish(packet: MQTTPublish) {
        // Handle topic alias
        val topic = getTopicOrAlias(packet)

        if (!checkAuthorization(topic))
            throw MQTTException(ReasonCode.NOT_AUTHORIZED)

        if (packet.qos > broker.maximumQos ?: Qos.EXACTLY_ONCE) {
            throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        if (!broker.retainedAvailable && packet.retain)
            throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)

        // Handle receive maximum
        if (packet.qos > Qos.AT_MOST_ONCE && broker.receiveMaximum != null) {
            if (session.qos2ListReceived.size + 1 > broker.receiveMaximum)
                throw MQTTException(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED)
        }

        if (packet.retain) {
            broker.setRetained(packet.topicName, packet, session.clientId)
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
                    session.qos2ListReceived[packet.packetId] = packet
                return // Don't send the PUBLISH to other clients until PUBCOMP
            }
            else -> {
            }
        }

        broker.publish(packet.retain, topic, packet.qos, false, packet.properties, packet.payload)
    }

    private fun getTopicOrAlias(packet: MQTTPublish): String {
        var topic = packet.topicName
        packet.properties.topicAlias?.let {
            if (it == 0u || it > broker.maximumTopicAlias?.toUInt() ?: 0u)
                throw MQTTException(ReasonCode.TOPIC_ALIAS_INVALID)
            if (packet.topicName.isNotEmpty()) {
                topicAliasesClient[it] = packet.topicName
            }
            topic = topicAliasesClient[it] ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            packet.properties.topicAlias = null
        }
        return topic
    }

    private fun qos12ReasonCode(packet: MQTTPublish): ReasonCode {
        val payloadFormatValid = packet.validatePayloadFormat()
        return if (!payloadFormatValid)
            ReasonCode.PAYLOAD_FORMAT_INVALID
        else if (session.isPacketIdInUse(packet.packetId!!))
            ReasonCode.PACKET_IDENTIFIER_IN_USE
        else
            ReasonCode.SUCCESS
    }

    private fun handlePuback(packet: MQTTPuback) {
        session.acknowledgePublish(packet.packetId)
        incrementSendQuota()
    }

    private fun handlePubrec(packet: MQTTPubrec) {
        if (packet.reasonCode >= ReasonCode.UNSPECIFIED_ERROR) {
            session.acknowledgePublish(packet.packetId)
            incrementSendQuota()
            return
        }
        val reasonCode = if (session.hasPendingAcknowledgeMessage(packet.packetId)) {
            ReasonCode.SUCCESS
        } else {
            ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
        }
        val pubrel = MQTTPubrel(packet.packetId, reasonCode)
        session.addPendingAcknowledgePubrel(pubrel)
        writer.writePacket(pubrel)
    }

    private fun handlePubrel(packet: MQTTPubrel) {
        if (packet.reasonCode != ReasonCode.SUCCESS)
            return
        session.qos2ListReceived.remove(packet.packetId)?.let {
            writer.writePacket(MQTTPubcomp(packet.packetId, ReasonCode.SUCCESS, packet.properties))
            broker.publish(it.retain, getTopicOrAlias(it), Qos.EXACTLY_ONCE, false, packet.properties, it.payload)
        } ?: run {
            writer.writePacket(MQTTPubcomp(packet.packetId, ReasonCode.PACKET_IDENTIFIER_NOT_FOUND, packet.properties))
        }
    }

    private fun handlePubcomp(packet: MQTTPubcomp) {
        session.acknowledgePubrel(packet.packetId)
        incrementSendQuota()
    }

    private fun prepareRetainedMessages(subscription: Subscription, replaced: Boolean): List<MQTTPublish> {
        val retainedMessagesList = mutableListOf<MQTTPublish>()
        if (!subscription.isShared() &&
            ((subscription.options.retainHandling == 0u) ||
                    (subscription.options.retainHandling == 1u && !replaced))
        ) {
            broker.getRetained(subscription.topicFilter).forEach { pair ->
                val retainedMessage = pair.first
                val clientId = pair.second
                if (!(subscription.options.noLocal && session.clientId == clientId)) {
                    val qos = Qos.valueOf(min(retainedMessage.qos.value, subscription.options.qos.value))
                    retainedMessagesList += MQTTPublish(
                        if (subscription.options.retainedAsPublished) retainedMessage.retain else false,
                        qos,
                        false,
                        retainedMessage.topicName,
                        if (qos > Qos.AT_MOST_ONCE) session.generatePacketId() else null,
                        retainedMessage.properties,
                        retainedMessage.payload
                    )
                }
            }
        }
        return retainedMessagesList
    }

    private fun handleSubscribe(packet: MQTTSubscribe) {
        val retainedMessagesList = mutableListOf<MQTTPublish>()
        val reasonCodes = packet.subscriptions.map { subscription ->
            if (!checkAuthorization(subscription.topicFilter))
                return@map ReasonCode.NOT_AUTHORIZED

            if (!subscription.matchTopicFilter.isValidTopic())
                return@map ReasonCode.TOPIC_FILTER_INVALID

            if (session.isPacketIdInUse(packet.packetIdentifier))
                return@map ReasonCode.PACKET_IDENTIFIER_IN_USE

            val isShared = subscription.isShared()
            if (!broker.sharedSubscriptionsAvailable && isShared)
                return@map ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED

            if (isShared && subscription.options.noLocal)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)

            if (packet.properties.subscriptionIdentifier.getOrNull(0) != null && !broker.subscriptionIdentifiersAvailable)
                return@map ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED

            if (!broker.wildcardSubscriptionAvailable && subscription.matchTopicFilter.containsWildcard())
                return@map ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED

            val replaced = session.addSubscription(subscription)
            retainedMessagesList += prepareRetainedMessages(subscription, replaced)

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

        // Send SUBACK
        writer.writePacket(MQTTSuback(packet.packetIdentifier, reasonCodes))
        // Send retained messages
        retainedMessagesList.forEach { writer.writePacket(it) }
    }

    private fun handleUnsubscribe(packet: MQTTUnsubscribe) {
        val reasonCodes = packet.topicFilters.map { topicFilter ->
            if (session.isPacketIdInUse(packet.packetIdentifier))
                return@map ReasonCode.PACKET_IDENTIFIER_IN_USE
            if (session.removeSubscription(topicFilter))
                return@map ReasonCode.SUCCESS
            else
                return@map ReasonCode.NO_SUBSCRIPTION_EXISTED
        }
        writer.writePacket(MQTTUnsuback(packet.packetIdentifier, reasonCodes))
    }

    private fun handlePingreq() {
        writer.writePacket(MQTTPingresp())
    }

    private fun handleDisconnect(packet: MQTTDisconnect) {
        if (session.sessionExpiryInterval == 0u && packet.properties.sessionExpiryInterval != null && packet.properties.sessionExpiryInterval != 0u)
            disconnect(ReasonCode.PROTOCOL_ERROR)
        else {
            if (packet.reasonCode == ReasonCode.SUCCESS)
                session.will = null
            else
                sendWill()
            close()
        }
    }

    private fun handleAuth(packet: MQTTAuth) {
        TODO("handle auth")
    }
}
