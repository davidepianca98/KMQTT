package mqtt

import currentTimeMillis
import generateRandomClientId
import messageExpiryIntervalExpired
import mqtt.packets.*
import mqtt.packets.mqttv5.*
import socket.IOException
import socket.Socket
import socket.streams.EOFException
import kotlin.math.min


class ClientConnection(
    val client: Socket,
    private val broker: Broker
) {

    companion object {
        private const val DEFAULT_MAX_SEND_QUOTA = 65535u
    }

    private var clientId: String? = null
    private val session: Session
        get() = broker.getSession(clientId) ?: throw Exception("Session not found")

    // Client connection state
    private val topicAliasesClient = mutableMapOf<UInt, String>()
    private val topicAliasesServer = mutableMapOf<String, UInt>()
    private var maxSendQuota: UInt =
        DEFAULT_MAX_SEND_QUOTA // Client receive maximum
    private var sendQuota: UInt = DEFAULT_MAX_SEND_QUOTA
    // TODO don't send packets larger than this, remove certain properties based on the specific packet if possible, if null no limit
    private var maximumPacketSize: UInt? = null
    private var topicAliasMaximum = 0u

    private var keepAlive = 0
    private var connectHandled = false
    private var connectCompleted = false
    private var authenticationMethod: String? = null
    private var connectPacket: MQTTConnect? = null
    private var packetsReceivedBeforeConnack = mutableListOf<MQTTPacket>()

    private val currentReceivedPacket = MQTTCurrentPacket(broker.maximumPacketSize)
    private var lastReceivedMessageTimestamp = currentTimeMillis()

    fun checkKeepAliveExpired() {
        val timeout = ((keepAlive * 1000).toDouble() * 1.5).toInt()
        val expired = currentTimeMillis() > lastReceivedMessageTimestamp + timeout
        if (expired) {
            if (connectHandled) {
                broker.sendWill(broker.sessions[clientId])
                disconnect(ReasonCode.KEEP_ALIVE_TIMEOUT)
            } else {
                disconnect(ReasonCode.MAXIMUM_CONNECT_TIME)
            }
        }
    }

    fun dataReceived(data: UByteArray) {
        lastReceivedMessageTimestamp = currentTimeMillis()
        try {
            currentReceivedPacket.addData(data).forEach {
                handlePacket(it)
            }
        } catch (e: MQTTException) {
            disconnect(e.reasonCode)
        } catch (e: EOFException) {
            close()
        } catch (e: IOException) {
            println(e.message)
            closeWithException()
        } catch (e: Exception) {
            println(e.message)
            disconnect(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
        }
    }

    private fun writePacket(packet: MQTTPacket) {
        try {
            client.send(packet.toByteArray())
        } catch (e: IOException) {
            closeWithException()
        }
    }

    fun closeWithException() {
        close()
        broker.sendWill(broker.sessions[clientId])
    }

    private fun close() {
        broker.sessions[clientId]?.disconnected()
    }

    private fun disconnect(reasonCode: ReasonCode) {
        if (!connectCompleted) {
            writePacket(
                MQTTConnack(
                    ConnectAcknowledgeFlags(
                        false
                    ), reasonCode
                )
            )
        } else {
            writePacket(MQTTDisconnect(reasonCode))
            if (reasonCode != ReasonCode.SUCCESS)
                broker.sendWill(broker.sessions[clientId])
        }
        close()
    }

    private fun handlePacket(packet: MQTTPacket) {
        if (packet is MQTTConnect) {
            if (!connectHandled) {
                handleConnect(packet)
                connectHandled = true
                return
            }
        } else {
            if (!connectHandled) // If first packet is not CONNECT, send Protocol Error
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
        when (packet) {
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
        payload: UByteArray?
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
                it - ((currentTimeMillis() - packet.timestamp) / 1000).toUInt()
        }

        if (packet.qos == Qos.AT_LEAST_ONCE || packet.qos == Qos.EXACTLY_ONCE) {
            if (sendQuota <= 0u)
                return
            session.sendQosBiggerThanZero(packet) {
                writePacket(packet)
                decrementSendQuota()
            }
        } else {
            writePacket(packet)
        }
    }

    private fun generateClientId(): String {
        var id: String
        do {
            id = generateRandomClientId()
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

    private fun handleAuthentication(packet: MQTTConnect) {
        if (packet.userName != null || packet.password != null) {
            if (broker.authentication?.authenticate(packet.userName, packet.password) == false) {
                throw MQTTException(ReasonCode.NOT_AUTHORIZED)
            }
        }
    }

    private fun sendAuth(reasonCode: ReasonCode, authenticationMethod: String, authenticationData: UByteArray?) {
        val properties = MQTTProperties()
        properties.authenticationMethod = authenticationMethod
        properties.authenticationData = authenticationData
        val auth = MQTTAuth(reasonCode, properties)
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
                connectPacket?.let { initSessionAndSendConnack(it) }
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

    private fun handleConnect(packet: MQTTConnect) {
        connectPacket = packet
        handleAuthentication(packet)

        val clientId = if (packet.clientID.isEmpty()) generateClientId() else packet.clientID
        this.clientId = clientId

        packet.properties.authenticationMethod?.let { authenticationMethod ->
            handleEnhancedAuthentication(clientId, authenticationMethod, packet.properties.authenticationData)
        } ?: initSessionAndSendConnack(packet)
    }

    private fun initSessionAndSendConnack(packet: MQTTConnect) {
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
                session = Session(packet, this)
                broker.sessions[clientId] = session
            } else {
                // Update the session with the new parameters
                session.clientConnection = this
                session.will = Will.buildWill(packet)
                session.sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
                session.resendPending {
                    writePacket(it)
                }
                sessionPresent = true
            }
        } else {
            session = Session(packet, this)
            broker.sessions[clientId] = session
        }

        keepAlive = packet.keepAlive

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

        // TODO implement section 3.2.2.3.16 and 4.11 for Server redirection

        val connack = MQTTConnack(
            ConnectAcknowledgeFlags(sessionPresent),
            ReasonCode.SUCCESS,
            connackProperties
        )
        writePacket(connack)
        connectCompleted = true
        session.connected()

        handlePacketsReceivedBeforeConnack()
    }

    private fun handlePacketsReceivedBeforeConnack() {
        packetsReceivedBeforeConnack.forEach { packet ->
            if (packet is MQTTPublish) {
                handlePublish(packet)
            } else if (packet is MQTTSubscribe) {
                handleSubscribe(packet)
            }
        }
        packetsReceivedBeforeConnack.clear()
    }

    private fun checkAuthorization(topicName: String): Boolean {
        return broker.authorization?.authorize(topicName) != false
    }

    private fun handlePublish(packet: MQTTPublish) {
        if (!connectCompleted) {
            packetsReceivedBeforeConnack.add(packet)
            return
        }

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
                writePacket(MQTTPuback(packet.packetId!!, reasonCode))
                if (reasonCode != ReasonCode.SUCCESS)
                    return
            }
            Qos.EXACTLY_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                writePacket(MQTTPubrec(packet.packetId!!, reasonCode))
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
        writePacket(pubrel)
    }

    private fun handlePubrel(packet: MQTTPubrel) {
        if (packet.reasonCode != ReasonCode.SUCCESS)
            return
        session.qos2ListReceived.remove(packet.packetId)?.let {
            writePacket(
                MQTTPubcomp(
                    packet.packetId,
                    ReasonCode.SUCCESS,
                    packet.properties
                )
            )
            broker.publish(it.retain, getTopicOrAlias(it), Qos.EXACTLY_ONCE, false, packet.properties, it.payload)
        } ?: run {
            writePacket(
                MQTTPubcomp(
                    packet.packetId,
                    ReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                    packet.properties
                )
            )
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
                    val qos = Qos.valueOf(min(retainedMessage.qos.value, subscription.options.qos.value))!!
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
        if (!connectCompleted) {
            packetsReceivedBeforeConnack.add(packet)
            return
        }

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
        writePacket(MQTTSuback(packet.packetIdentifier, reasonCodes))
        // Send retained messages
        retainedMessagesList.forEach { writePacket(it) }
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
        writePacket(MQTTUnsuback(packet.packetIdentifier, reasonCodes))
    }

    private fun handlePingreq() {
        writePacket(MQTTPingresp())
    }

    private fun handleDisconnect(packet: MQTTDisconnect) {
        val session = try {
            session
        } catch (e: Exception) {
            null
        }
        if (session?.sessionExpiryInterval == 0u && packet.properties.sessionExpiryInterval != null && packet.properties.sessionExpiryInterval != 0u)
            disconnect(ReasonCode.PROTOCOL_ERROR)
        else {
            if (packet.reasonCode == ReasonCode.SUCCESS)
                session?.will = null
            else
                broker.sendWill(broker.sessions[clientId])
            close()
        }
    }

    private fun handleAuth(packet: MQTTAuth) {
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
