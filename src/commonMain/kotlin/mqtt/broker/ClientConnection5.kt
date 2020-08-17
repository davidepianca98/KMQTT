package mqtt.broker

import mqtt.*
import mqtt.broker.interfaces.EnhancedAuthenticationProvider
import mqtt.packets.ConnectAcknowledgeFlags
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqttv5.*
import socket.SocketInterface
import socket.tcp.IOException


class ClientConnection5(
    private val client: SocketInterface,
    private val broker: Broker
) : ClientConnection(client, broker) {

    companion object {
        private const val DEFAULT_MAX_SEND_QUOTA = 65535u
    }

    // Client connection state
    private val topicAliasesClient = mutableMapOf<UInt, String>()
    private val topicAliasesServer = mutableMapOf<String, UInt>()
    private var maxSendQuota: UInt =
        DEFAULT_MAX_SEND_QUOTA // Client receive maximum
    internal var sendQuota: UInt = DEFAULT_MAX_SEND_QUOTA
    private var maximumPacketSize: UInt? = null
    private var topicAliasMaximum = 0u

    private var connectCompleted = false
    private var authenticationMethod: String? = null
    private var connectPacket: MQTT5Connect? = null
    private var packetsReceivedBeforeConnack = mutableListOf<MQTT5Packet>()


    internal fun writePacket(packet: MQTT5Packet) {
        try {
            if (maximumPacketSize?.let { packet.resizeIfTooBig(it) } != false) {
                val packetBytes = packet.toByteArray()
                client.send(packetBytes)
                clientId?.let {
                    broker.bytesMetrics?.sent(it, packetBytes.size.toLong())
                }
            }
        } catch (e: IOException) {
            closedWithException()
        }
    }

    override fun disconnect(reasonCode: ReasonCode, serverReference: String?) {
        if (!connectCompleted) {
            val connack = MQTT5Connack(
                ConnectAcknowledgeFlags(false),
                reasonCode,
                MQTT5Properties().apply { this.serverReference = serverReference })
            writePacket(connack)
        } else {
            val disconnect =
                MQTT5Disconnect(reasonCode, MQTT5Properties().apply { this.serverReference = serverReference })
            writePacket(disconnect)
            if (reasonCode in listOf(
                    ReasonCode.SUCCESS,
                    ReasonCode.SERVER_SHUTTING_DOWN,
                    ReasonCode.USE_ANOTHER_SERVER,
                    ReasonCode.SERVER_MOVED
                )
            ) {
                broker.sessions[clientId]?.will = null
            }
        }
        close()
    }

    /**
     * Either generates a new topic alias or uses an existing one, if they are enabled in the connection
     * @param topicName to send the message to
     * @param properties the publish properties, will get modified if topic aliases enabled
     */
    internal fun getPublishTopicAlias(topicName: String, properties: MQTT5Properties): String {
        var packetTopicName: String = topicName
        if (topicAliasMaximum > 0u) {
            topicAliasesServer[topicName]?.let {
                packetTopicName = ""
                properties.topicAlias = it
            } ?: run {
                if (topicAliasesServer.size < topicAliasMaximum.toInt()) {
                    topicAliasesServer[topicName] = topicAliasesServer.size.toUInt()
                    packetTopicName = topicName
                    properties.topicAlias = topicAliasesServer[topicName]
                }
            }
        }
        return packetTopicName
    }

    private fun incrementSendQuota() {
        if (++sendQuota >= maxSendQuota)
            sendQuota = maxSendQuota
    }

    internal fun decrementSendQuota() {
        if (sendQuota > 0u)
            sendQuota--
    }

    private fun sendAuth(reasonCode: ReasonCode, authenticationMethod: String, authenticationData: UByteArray?) {
        val properties = MQTT5Properties()
        properties.authenticationMethod = authenticationMethod
        properties.authenticationData = authenticationData
        val auth = MQTT5Auth(reasonCode, properties)
        writePacket(auth)
    }

    private fun enhancedAuthenticationResult(
        result: EnhancedAuthenticationProvider.Result,
        authenticationMethod: String,
        authenticationData: UByteArray?
    ) {
        if (result == EnhancedAuthenticationProvider.Result.NEEDS_MORE) {
            sendAuth(ReasonCode.CONTINUE_AUTHENTICATION, authenticationMethod, authenticationData)
        } else if (result == EnhancedAuthenticationProvider.Result.SUCCESS) {
            if (!connectCompleted) {
                connectPacket?.let { initSessionAndSendConnack(it, authenticationData) }
                    ?: throw MQTTException(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
            } else {
                sendAuth(ReasonCode.SUCCESS, authenticationMethod, authenticationData)
            }
        } else if (result == EnhancedAuthenticationProvider.Result.ERROR) {
            throw MQTTException(ReasonCode.NOT_AUTHORIZED)
        }
    }

    private fun handleEnhancedAuthentication(
        clientId: String,
        authenticationMethod: String,
        authenticationData: UByteArray?
    ) {
        val provider = broker.enhancedAuthenticationProviders[authenticationMethod]
        if (provider == null) {
            throw MQTTException(ReasonCode.BAD_AUTHENTICATION_METHOD)
        } else {
            this.authenticationMethod = authenticationMethod
            provider.authReceived(clientId, authenticationData) { result, data ->
                enhancedAuthenticationResult(result, authenticationMethod, data)
            }
        }
    }

    override fun handleConnect(packet: MQTTPacket) {
        packet as MQTT5Connect
        connectPacket = packet
        handleAuthentication(packet)

        val clientId = if (packet.clientID.isEmpty()) generateClientId() else packet.clientID
        this.clientId = clientId

        packet.properties.authenticationMethod?.let { authenticationMethod ->
            handleEnhancedAuthentication(clientId, authenticationMethod, packet.properties.authenticationData)
        } ?: initSessionAndSendConnack(packet, null)
    }

    private fun initSessionAndSendConnack(packet: MQTT5Connect, authenticationData: UByteArray?) {
        var sessionPresent = false
        val clientId = this.clientId ?: throw MQTTException(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)

        var session = broker.sessions[clientId]
        if (session != null) {
            if (session.isConnected()) {
                // Send disconnect to the old connection and close it
                session.clientConnection?.disconnect(ReasonCode.SESSION_TAKEN_OVER)

                // Send old will if present
                if (session.will?.willDelayInterval == 0u || packet.connectFlags.cleanStart) {
                    broker.sendWill(session)
                }
            }
            if (packet.connectFlags.cleanStart) {
                session = Session(packet, this) { id, sess ->
                    broker.persistence?.persistSession(id, sess)
                }
                broker.sessions[clientId] = session
                this.session = session
            } else {
                // Update the session with the new parameters
                session.clientConnection = this
                session.will = Will.buildWill(packet)
                session.sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
                sessionPresent = true
                this.session = session
            }
        } else {
            session = Session(packet, this) { id, sess ->
                broker.persistence?.persistSession(id, sess)
            }
            broker.sessions[clientId] = session
            this.session = session
        }

        keepAlive = packet.keepAlive

        sendQuota = packet.properties.receiveMaximum ?: DEFAULT_MAX_SEND_QUOTA
        maxSendQuota = packet.properties.receiveMaximum ?: DEFAULT_MAX_SEND_QUOTA
        maximumPacketSize = packet.properties.maximumPacketSize
        topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0u

        //
        // CONNACK properties
        //
        val connackProperties = MQTT5Properties()
        if (session.sessionExpiryInterval > broker.maximumSessionExpiryInterval) {
            session.sessionExpiryInterval = broker.maximumSessionExpiryInterval
            connackProperties.sessionExpiryInterval = broker.maximumSessionExpiryInterval
        }
        broker.receiveMaximum?.toUInt()?.let {
            connackProperties.receiveMaximum = it
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

        connackProperties.maximumPacketSize = broker.maximumPacketSize

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

        this.authenticationMethod?.let {
            connackProperties.authenticationMethod = it
        }

        authenticationData?.let {
            connackProperties.authenticationData = it
        }

        val connack = MQTT5Connack(
            ConnectAcknowledgeFlags(sessionPresent),
            ReasonCode.SUCCESS,
            connackProperties
        )

        writePacket(connack)
        connectCompleted = true
        session.connected()

        session.resendPending {
            writePacket(it)
        }

        handlePacketsReceivedBeforeConnack()
    }

    private fun handlePacketsReceivedBeforeConnack() {
        packetsReceivedBeforeConnack.forEach { packet ->
            if (packet is MQTT5Publish) {
                handlePublish(packet)
            } else if (packet is MQTT5Subscribe) {
                handleSubscribe(packet)
            }
        }
        packetsReceivedBeforeConnack.clear()
    }

    private fun checkAuthorization(topicName: String, isSubscription: Boolean): Boolean {
        return broker.authorization?.authorize(clientId!!, topicName, isSubscription) != false
    }

    override fun handlePublish(packet: MQTTPacket) {
        packet as MQTT5Publish
        if (!connectCompleted) {
            packetsReceivedBeforeConnack.add(packet)
            return
        }

        // Handle topic alias
        val topic = getTopicOrAlias(packet)

        if (!checkAuthorization(topic, false))
            throw MQTTException(ReasonCode.NOT_AUTHORIZED)

        if (packet.qos > broker.maximumQos ?: Qos.EXACTLY_ONCE) {
            throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        if (!broker.retainedAvailable && packet.retain)
            throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)

        // Handle receive maximum
        if (packet.qos > Qos.AT_MOST_ONCE && broker.receiveMaximum != null) {
            if (session!!.qos2ListReceived.size + 1 > broker.receiveMaximum.toInt())
                throw MQTTException(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED)
        }

        if (packet.retain) {
            broker.setRetained(packet.topicName, packet, session!!.clientId)
        }

        when (packet.qos) {
            Qos.AT_LEAST_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                writePacket(MQTT5Puback(packet.packetId!!, reasonCode))
                if (reasonCode != ReasonCode.SUCCESS)
                    return
            }
            Qos.EXACTLY_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                writePacket(MQTT5Pubrec(packet.packetId!!, reasonCode))
                if (reasonCode == ReasonCode.SUCCESS)
                    session!!.qos2ListReceived[packet.packetId] = packet
                return // Don't send the PUBLISH to other clients until PUBCOMP
            }
            else -> {
            }
        }

        broker.publish(session!!.clientId, packet.retain, topic, packet.qos, false, packet.properties, packet.payload)
    }

    private fun getTopicOrAlias(packet: MQTT5Publish): String {
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

    private fun qos12ReasonCode(packet: MQTT5Publish): ReasonCode {
        val payloadFormatValid = packet.validatePayloadFormat()
        return if (!payloadFormatValid)
            ReasonCode.PAYLOAD_FORMAT_INVALID
        else if (session!!.isPacketIdInUse(packet.packetId!!))
            ReasonCode.PACKET_IDENTIFIER_IN_USE
        else
            ReasonCode.SUCCESS
    }

    override fun handlePuback(packet: MQTTPacket) {
        packet as MQTT5Puback
        session!!.acknowledgePublish(packet.packetId)
        incrementSendQuota()
    }

    override fun handlePubrec(packet: MQTTPacket) {
        packet as MQTT5Pubrec
        if (packet.reasonCode >= ReasonCode.UNSPECIFIED_ERROR) {
            session!!.acknowledgePublish(packet.packetId)
            incrementSendQuota()
            return
        }
        val reasonCode = if (session!!.hasPendingAcknowledgeMessage(packet.packetId)) {
            ReasonCode.SUCCESS
        } else {
            ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
        }
        val pubrel = MQTT5Pubrel(packet.packetId, reasonCode)
        session!!.addPendingAcknowledgePubrel(pubrel)
        writePacket(pubrel)
    }

    override fun handlePubrel(packet: MQTTPacket) {
        packet as MQTT5Pubrel
        if (packet.reasonCode != ReasonCode.SUCCESS)
            return
        session!!.qos2ListReceived.remove(packet.packetId)?.let {
            writePacket(
                MQTT5Pubcomp(
                    packet.packetId,
                    ReasonCode.SUCCESS,
                    packet.properties
                )
            )
            broker.publish(
                session!!.clientId,
                it.retain,
                getTopicOrAlias(it),
                Qos.EXACTLY_ONCE,
                false,
                it.properties,
                it.payload
            )
        } ?: run {
            writePacket(
                MQTT5Pubcomp(
                    packet.packetId,
                    ReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                    packet.properties
                )
            )
        }
    }

    override fun handlePubcomp(packet: MQTTPacket) {
        packet as MQTT5Pubcomp
        session!!.acknowledgePubrel(packet.packetId)
        incrementSendQuota()
    }

    private fun prepareRetainedMessages(subscription: Subscription, replaced: Boolean): List<MQTT5Publish> {
        val retainedMessagesList = mutableListOf<MQTT5Publish>()
        if (!subscription.isShared() &&
            ((subscription.options.retainHandling == 0u) ||
                    (subscription.options.retainHandling == 1u && !replaced))
        ) {
            broker.getRetained(subscription.topicFilter).forEach { pair ->
                val retainedMessage = pair.first
                val clientId = pair.second
                if (!(subscription.options.noLocal && session!!.clientId == clientId)) {
                    val qos = Qos.min(retainedMessage.qos, subscription.options.qos)
                    retainedMessagesList += MQTT5Publish(
                        if (subscription.options.retainedAsPublished) retainedMessage.retain else false,
                        qos,
                        false,
                        retainedMessage.topicName,
                        if (qos > Qos.AT_MOST_ONCE) session!!.generatePacketId() else null,
                        retainedMessage.properties,
                        retainedMessage.payload
                    )
                }
            }
        }
        return retainedMessagesList
    }

    override fun handleSubscribe(packet: MQTTPacket) {
        packet as MQTT5Subscribe
        if (!connectCompleted) {
            packetsReceivedBeforeConnack.add(packet)
            return
        }

        val retainedMessagesList = mutableListOf<MQTT5Publish>()
        val reasonCodes = packet.subscriptions.map { subscription ->
            if (!checkAuthorization(subscription.topicFilter, true))
                return@map ReasonCode.NOT_AUTHORIZED

            if (!subscription.matchTopicFilter.isValidTopic())
                return@map ReasonCode.TOPIC_FILTER_INVALID

            if (session!!.isPacketIdInUse(packet.packetIdentifier))
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

            val replaced = broker.subscriptions.insert(subscription, clientId!!)
            retainedMessagesList += prepareRetainedMessages(subscription, replaced)
            broker.persistence?.persistSubscription(clientId!!, subscription)

            when (subscription.options.qos) {
                Qos.AT_MOST_ONCE -> ReasonCode.SUCCESS
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
        writePacket(MQTT5Suback(packet.packetIdentifier, reasonCodes))
        // Send retained messages
        retainedMessagesList.forEach { writePacket(it) }
    }

    override fun handleUnsubscribe(packet: MQTTPacket) {
        packet as MQTT5Unsubscribe
        val reasonCodes = packet.topicFilters.map { topicFilter ->
            if (session!!.isPacketIdInUse(packet.packetIdentifier))
                return@map ReasonCode.PACKET_IDENTIFIER_IN_USE
            if (broker.subscriptions.delete(topicFilter, clientId!!)) {
                broker.persistence?.removeSubscription(clientId!!, topicFilter)
                return@map ReasonCode.SUCCESS
            } else
                return@map ReasonCode.NO_SUBSCRIPTION_EXISTED
        }
        writePacket(MQTT5Unsuback(packet.packetIdentifier, reasonCodes))
    }

    override fun handlePingreq(packet: MQTTPacket) {
        writePacket(MQTT5Pingresp())
    }

    override fun handleDisconnect(packet: MQTTPacket) {
        packet as MQTT5Disconnect
        val session = try {
            session
        } catch (e: Exception) {
            null
        }
        if (packet.properties.sessionExpiryInterval != null) {
            if (session?.sessionExpiryInterval == 0u && packet.properties.sessionExpiryInterval != 0u) {
                disconnect(ReasonCode.PROTOCOL_ERROR)
            } else {
                session?.sessionExpiryInterval = packet.properties.sessionExpiryInterval!!
                close()
            }
        } else {
            if (packet.reasonCode == ReasonCode.SUCCESS)
                session?.will = null
            close()
        }
    }

    override fun handleAuth(packet: MQTTPacket) {
        packet as MQTT5Auth
        val authenticationMethod = packet.properties.authenticationMethod
        val clientId = this.clientId ?: throw MQTTException(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
        if (!connectCompleted && packet.authenticateReasonCode != ReasonCode.CONTINUE_AUTHENTICATION) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        } else if (authenticationMethod == null || authenticationMethod != this.authenticationMethod) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        } else {
            handleEnhancedAuthentication(clientId, authenticationMethod, packet.properties.authenticationData)
        }
    }
}
