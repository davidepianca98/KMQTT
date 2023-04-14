package mqtt.broker

import currentTimeMillis
import generateRandomClientId
import mqtt.*
import mqtt.broker.cluster.RemoteSession
import mqtt.broker.interfaces.EnhancedAuthenticationProvider
import mqtt.packets.ConnectAcknowledgeFlags
import mqtt.MQTTException
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqtt.*
import mqtt.packets.mqttv4.*
import mqtt.packets.mqttv5.*
import socket.IOException
import socket.SocketInterface
import socket.streams.EOFException
import socket.tcp.TCPEventHandler

public class ClientConnection(
    private val client: SocketInterface,
    private val broker: Broker
) : TCPEventHandler {

    public companion object {
        private const val DEFAULT_MAX_SEND_QUOTA = 65535u
    }

    private var clientId: String? = null
    private var username: String? = null
    private var password: UByteArray? = null
    private var session: Session? = null

    // Client connection state
    private val topicAliasesClient = mutableMapOf<UInt, String>()
    private val topicAliasesServer = mutableMapOf<String, UInt>()
    private var maxSendQuota: UInt = DEFAULT_MAX_SEND_QUOTA // Client receive maximum
    internal var sendQuota: UInt = DEFAULT_MAX_SEND_QUOTA
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

    public fun checkKeepAliveExpired() {
        val timeout = ((keepAlive * 1000).toDouble() * 1.5).toInt()
        val expired = currentTimeMillis() > lastReceivedMessageTimestamp + timeout
        if (expired) {
            if (connectHandled) {
                if (keepAlive > 0) {
                    disconnect(ReasonCode.KEEP_ALIVE_TIMEOUT)
                    session?.connected = false
                    clientId?.let { broker.connectionCallbacks?.onDisconnect(it, true) }
                }
            } else {
                disconnect(ReasonCode.MAXIMUM_CONNECT_TIME)
            }
        }
    }

    override fun read(): UByteArray? {
        return client.read()
    }

    override fun sendRemaining() {
        client.sendRemaining()
    }

    override fun dataReceived(data: UByteArray) {
        lastReceivedMessageTimestamp = currentTimeMillis()
        try {
            clientId?.let {
                broker.bytesMetrics?.received(it, data.size.toLong())
            }
            currentReceivedPacket.addData(data).forEach {
                handlePacket(it)
            }
        } catch (e: MQTTException) {
            disconnect(e.reasonCode)
        } catch (e: EOFException) {
            println("EOF")
            close()
        } catch (e: IOException) {
            println("IOException ${e.message}")
            closedWithException()
        } catch (e: Exception) {
            println("Exception ${e.message} ${e.cause?.message}")
            disconnect(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
        }
    }

    internal fun writePacket(packet: MQTTPacket) {
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

    override fun closedWithException() {
        close()
    }

    override fun closedGracefully() {
        close()
    }

    private fun close() {
        client.close()
        (broker.getSession(clientId) as Session?)?.connected = false
        clientId?.let { broker.connectionCallbacks?.onDisconnect(it, false) }
    }

    public fun disconnect(reasonCode: ReasonCode, serverReference: String? = null) {
        if (currentReceivedPacket.mqttVersion == 5) {
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
                    (broker.getSession(clientId) as Session?)?.will = null
                }
            }
        }
        close()
    }

    private fun handlePacket(packet: MQTTPacket) {
        if (packet is MQTTConnect) {
            if (!connectHandled) {
                handleConnect(packet)
                connectHandled = true
            }
        } else {
            if (!connectHandled) // If first packet is not CONNECT, send Protocol Error
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
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
        broker.packetInterceptor?.packetReceived(clientId!!, username, password, packet)
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

    private fun generateClientId(): String {
        var id: String
        do {
            id = generateRandomClientId()
        } while (broker.getSession(id) != null)
        return id
    }

    private fun incrementSendQuota() {
        if (++sendQuota >= maxSendQuota)
            sendQuota = maxSendQuota
        session?.sendPending {
            writePacket(it)
        }
    }

    internal fun decrementSendQuota() {
        if (sendQuota > 0u)
            sendQuota--
    }

    private fun handleAuthentication(packet: MQTTConnect) {
        if (broker.authentication != null) {
            if (packet.userName != null || packet.password != null) {
                if (!broker.authentication.authenticate(clientId!!, packet.userName, packet.password)) {
                    throw MQTTException(ReasonCode.NOT_AUTHORIZED)
                }
            } else {
                throw MQTTException(ReasonCode.NOT_AUTHORIZED)
            }
        }
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

    private fun handleConnect(packet: MQTTConnect) {
        if (connectHandled) {
            return
        }

        connectPacket = packet

        val clientId = packet.clientID.ifEmpty {
            if (packet is MQTT4Connect && !packet.connectFlags.cleanStart) {
                writePacket(MQTT4Connack(ConnectAcknowledgeFlags(false), ConnectReturnCode.IDENTIFIER_REJECTED))
                return
            }
            generateClientId()
        }
        this.clientId = clientId
        this.username = packet.userName
        if (broker.savePassword) {
            this.password = packet.password
        }
        handleAuthentication(packet)

        if (packet is MQTT5Connect && packet.properties.authenticationMethod != null) {
            packet.properties.authenticationMethod?.let { authenticationMethod ->
                handleEnhancedAuthentication(clientId, authenticationMethod, packet.properties.authenticationData)
            }
        } else {
            initSessionAndSendConnack(packet, null)
        }
        connectHandled = true
    }

    internal fun persistSession(clientId: String, session: Session) {
        broker.persistence?.persistSession(clientId, session)
    }

    private fun newSession(packet: MQTTConnect): Session {
        return Session(
            this,
            packet.clientID,
            if (packet is MQTT5Connect) packet.properties.sessionExpiryInterval ?: 0u else 0xFFFFFFFFu,
            if (packet.connectFlags.willFlag) Will(packet) else null,
            this::persistSession,
            broker::propagateSession
        )
    }

    private fun initSessionAndSendConnack(packet: MQTTConnect, authenticationData: UByteArray?) {
        var sessionPresent = false
        val clientId = this.clientId ?: throw MQTTException(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)

        var session = broker.getSession(clientId)
        if (session != null) {
            if (session.connected) {
                // Send disconnect to the old connection and close it
                session.disconnectClientSessionTakenOver()

                if (session is RemoteSession) {
                    session = session.toLocalSession(this, broker)
                }

                // Send old will if present
                if ((session as Session).will?.willDelayInterval == 0u || packet.connectFlags.cleanStart) {
                    broker.sendWill(session)
                }
            } else {
                if (session is RemoteSession) {
                    session = session.toLocalSession(this, broker)
                }
            }
            if (packet.connectFlags.cleanStart) {
                session = newSession(packet)
                broker.addSession(clientId, session)
                this.session = session
            } else {
                // Update the session with the new parameters
                (session as Session).clientConnection = this
                session.will = if (packet.connectFlags.willFlag) Will(packet) else null
                session.sessionExpiryInterval =
                    if (packet is MQTT5Connect) packet.properties.sessionExpiryInterval ?: 0u else 0xFFFFFFFFu
                sessionPresent = true
                this.session = session
            }
        } else {
            session = newSession(packet)
            broker.addSession(clientId, session)
            this.session = session
        }

        session.mqttVersion = currentReceivedPacket.mqttVersion ?: 4
        keepAlive = packet.keepAlive

        val connack = if (packet is MQTT5Connect) {
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
            MQTT5Connack(
                ConnectAcknowledgeFlags(sessionPresent),
                ReasonCode.SUCCESS,
                connackProperties
            )
        } else {
            MQTT4Connack(ConnectAcknowledgeFlags(sessionPresent), ConnectReturnCode.CONNECTION_ACCEPTED)
        }

        writePacket(connack)
        connectCompleted = true
        session.connected = true

        session.resendPending {
            writePacket(it)
        }

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

    private fun checkAuthorization(topicName: String, isSubscription: Boolean, payload: UByteArray?): Boolean {
        return broker.authorization?.authorize(
            clientId!!,
            username,
            password,
            topicName,
            isSubscription,
            payload
        ) != false
    }

    private fun handlePublish(packet: MQTTPublish) {
        if (!connectCompleted) {
            packetsReceivedBeforeConnack.add(packet)
            return
        }

        // Handle topic alias
        val topic = getTopicOrAlias(packet)

        if (!checkAuthorization(topic, false, packet.payload))
            throw MQTTException(ReasonCode.NOT_AUTHORIZED)

        if (packet.qos > (broker.maximumQos ?: Qos.EXACTLY_ONCE)) {
            throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        if (!broker.retainedAvailable && packet.retain)
            throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)

        // Handle receive maximum
        if (packet.qos > Qos.AT_MOST_ONCE && broker.receiveMaximum != null) {
            if (session!!.qos2ListReceived.size + 1 > broker.receiveMaximum.toInt())
                throw MQTTException(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED)
        }

        val dontSend = if (packet.retain) {
            broker.setRetained(packet.topicName, packet, session!!.clientId)
            packet.payload == null || packet.payload!!.isEmpty()
        } else false

        when (packet.qos) {
            Qos.AT_LEAST_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                if (currentReceivedPacket.mqttVersion == 5) {
                    writePacket(MQTT5Puback(packet.packetId!!, reasonCode))
                } else {
                    if (reasonCode == ReasonCode.SUCCESS) {
                        writePacket(MQTT4Puback(packet.packetId!!))
                    }
                }
                if (reasonCode != ReasonCode.SUCCESS)
                    return
            }
            Qos.EXACTLY_ONCE -> {
                val reasonCode = qos12ReasonCode(packet)
                if (currentReceivedPacket.mqttVersion == 5) {
                    writePacket(MQTT5Pubrec(packet.packetId!!, reasonCode))
                } else {
                    if (reasonCode == ReasonCode.SUCCESS) {
                        writePacket(MQTT4Pubrec(packet.packetId!!))
                    }
                }
                if (reasonCode == ReasonCode.SUCCESS)
                    session!!.qos2ListReceived[packet.packetId!!] = packet
                return // Don't send the PUBLISH to other clients until PUBCOMP
            }
            else -> {
            }
        }

        if (!dontSend) {
            broker.publish(
                session!!.clientId,
                packet.retain,
                topic,
                packet.qos,
                false,
                if (packet is MQTT5Publish) packet.properties else null,
                packet.payload
            )
        }
    }

    private fun getTopicOrAlias(packet: MQTTPublish): String {
        var topic = packet.topicName
        if (packet is MQTT5Publish) {
            packet.properties.topicAlias?.let {
                if (it == 0u || it > (broker.maximumTopicAlias?.toUInt() ?: 0u))
                    throw MQTTException(ReasonCode.TOPIC_ALIAS_INVALID)
                if (packet.topicName.isNotEmpty()) {
                    topicAliasesClient[it] = packet.topicName
                }
                topic = topicAliasesClient[it] ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                packet.properties.topicAlias = null
            }
        }
        return topic
    }

    private fun qos12ReasonCode(packet: MQTTPublish): ReasonCode {
        val payloadFormatValid = if (packet is MQTT5Publish) packet.validatePayloadFormat() else true
        return if (!payloadFormatValid)
            ReasonCode.PAYLOAD_FORMAT_INVALID
        else if (session!!.isPacketIdInUse(packet.packetId!!))
            ReasonCode.PACKET_IDENTIFIER_IN_USE
        else
            ReasonCode.SUCCESS
    }

    private fun handlePuback(packet: MQTTPuback) {
        session!!.acknowledgePublish(packet.packetId)
        incrementSendQuota()
    }

    private fun handlePubrec(packet: MQTTPubrec) {
        if (packet is MQTT5Pubrec && packet.reasonCode >= ReasonCode.UNSPECIFIED_ERROR) {
            session!!.acknowledgePublish(packet.packetId)
            incrementSendQuota()
            return
        }
        val reasonCode = if (session!!.hasPendingAcknowledgeMessage(packet.packetId)) {
            ReasonCode.SUCCESS
        } else {
            ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
        }
        val pubrel = if (packet is MQTT5Pubrec) {
            MQTT5Pubrel(packet.packetId, reasonCode)
        } else {
            if (reasonCode == ReasonCode.SUCCESS) {
                MQTT4Pubrel(packet.packetId)
            } else {
                null
            }
        }
        pubrel?.let {
            session!!.addPendingAcknowledgePubrel(pubrel)
            writePacket(pubrel)
        }
    }

    private fun handlePubrel(packet: MQTTPubrel) {
        if (packet is MQTT5Pubrel && packet.reasonCode != ReasonCode.SUCCESS)
            return
        session!!.qos2ListReceived.remove(packet.packetId)?.let {
            val pubcomp = if (packet is MQTT5Pubrel) {
                MQTT5Pubcomp(
                    packet.packetId,
                    ReasonCode.SUCCESS,
                    packet.properties
                )
            } else {
                MQTT4Pubcomp(packet.packetId)
            }
            writePacket(pubcomp)
            if (it.retain && (it.payload == null || it.payload!!.isEmpty())) {
                return
            }
            broker.publish(
                session!!.clientId,
                it.retain,
                getTopicOrAlias(it),
                Qos.EXACTLY_ONCE,
                false,
                if (it is MQTT5Publish) it.properties else null,
                it.payload
            )
        } ?: run {
            if (packet is MQTT5Pubrel) {
                writePacket(
                    MQTT5Pubcomp(
                        packet.packetId,
                        ReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                        packet.properties
                    )
                )
            }
        }
    }

    private fun handlePubcomp(packet: MQTTPubcomp) {
        session!!.acknowledgePubrel(packet.packetId)
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
                if (!(subscription.options.noLocal && session!!.clientId == clientId)) {
                    val qos = Qos.min(retainedMessage.qos, subscription.options.qos)

                    if (currentReceivedPacket.mqttVersion == 5) {
                        retainedMessagesList += MQTT5Publish(
                            if (subscription.options.retainedAsPublished) retainedMessage.retain else false,
                            qos,
                            false,
                            retainedMessage.topicName,
                            if (qos > Qos.AT_MOST_ONCE) session!!.generatePacketId() else null,
                            if (retainedMessage is MQTT5Publish) retainedMessage.properties else MQTT5Properties(),
                            retainedMessage.payload
                        )
                    } else {
                        retainedMessagesList += MQTT4Publish(
                            if (subscription.options.retainedAsPublished) retainedMessage.retain else false,
                            qos,
                            false,
                            retainedMessage.topicName,
                            if (qos > Qos.AT_MOST_ONCE) session!!.generatePacketId() else null,
                            retainedMessage.payload
                        )
                    }
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
            if (!checkAuthorization(subscription.topicFilter, true, null))
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

            if (packet is MQTT5Subscribe) {
                if (packet.properties.subscriptionIdentifier.getOrNull(0) != null && !broker.subscriptionIdentifiersAvailable)
                    return@map ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED
            }

            if (!broker.wildcardSubscriptionAvailable && subscription.matchTopicFilter.containsWildcard())
                return@map ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED

            val replaced = broker.addSubscription(clientId!!, subscription)
            retainedMessagesList += prepareRetainedMessages(subscription, replaced)

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

        val suback = if (packet is MQTT5Subscribe) {
            MQTT5Suback(packet.packetIdentifier, reasonCodes)
        } else {
            MQTT4Suback(packet.packetIdentifier, reasonCodes.toSubackReturnCodes())
        }
        // Send SUBACK
        writePacket(suback)
        // Send retained messages
        retainedMessagesList.forEach {
            session?.publish(it)
        }
    }

    private fun handleUnsubscribe(packet: MQTTUnsubscribe) {
        val reasonCodes = packet.topicFilters.map { topicFilter ->
            if (session!!.isPacketIdInUse(packet.packetIdentifier))
                return@map ReasonCode.PACKET_IDENTIFIER_IN_USE
            if (broker.removeSubscription(clientId!!, topicFilter)) {
                return@map ReasonCode.SUCCESS
            } else
                return@map ReasonCode.NO_SUBSCRIPTION_EXISTED
        }
        val unsuback = if (packet is MQTT5Unsubscribe) {
            MQTT5Unsuback(packet.packetIdentifier, reasonCodes)
        } else {
            MQTT4Unsuback(packet.packetIdentifier)
        }
        writePacket(unsuback)
    }

    private fun handlePingreq() {
        val packet = if (currentReceivedPacket.mqttVersion == 5) {
            MQTT5Pingresp()
        } else {
            MQTT4Pingresp()
        }
        writePacket(packet)
    }

    private fun handleDisconnect(packet: MQTTDisconnect) {
        val session = try {
            session
        } catch (e: Exception) {
            null
        }
        if (packet is MQTT5Disconnect && packet.properties.sessionExpiryInterval != null) {
            if (session?.sessionExpiryInterval == 0u && packet.properties.sessionExpiryInterval != 0u) {
                disconnect(ReasonCode.PROTOCOL_ERROR)
            } else {
                session?.sessionExpiryInterval = packet.properties.sessionExpiryInterval!!
                close()
            }
        } else {
            if ((packet is MQTT5Disconnect && packet.reasonCode == ReasonCode.SUCCESS) || packet is MQTT4Disconnect)
                session?.will = null
            close()
        }
    }

    private fun handleAuth(packet: MQTTAuth) {
        if (packet is MQTT5Auth) {
            val authenticationMethod = packet.properties.authenticationMethod
            val clientId = this.clientId ?: throw MQTTException(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
            if (!connectCompleted && packet.authenticateReasonCode != ReasonCode.CONTINUE_AUTHENTICATION) {
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            } else if (authenticationMethod == null || authenticationMethod != this.authenticationMethod) {
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            } else {
                handleEnhancedAuthentication(clientId, authenticationMethod, packet.properties.authenticationData)
            }
        } else {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
    }
}
