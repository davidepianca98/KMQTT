import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mqtt.MQTTCurrentPacket
import mqtt.MQTTException
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.ConnectFlags
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqtt.*
import mqtt.packets.mqttv4.*
import mqtt.packets.mqttv5.*
import socket.IOException
import socket.SocketClosedException
import socket.SocketInterface
import socket.streams.EOFException
import socket.tls.TLSClientSettings

/**
 * MQTT 3.1.1 and 5 client
 *
 * @param mqttVersion sets the version of MQTT for this client (4 -> 3.1.1, 5 -> 5)
 * @param address the URL of the server
 * @param port the port of the server
 * @param tls TLS settings, null if no TLS
 * @param keepAlive the MQTT keep alive of the connection in seconds
 * @param webSocket whether to use a WebSocket for the underlying connection, null if no WebSocket, otherwise the HTTP path, usually /mqtt
 * @param userName the username field of the CONNECT packet
 * @param password the password field of the CONNECT packet
 * @param properties the properties to be included in the CONNECT message (used only in MQTT5)
 * @param willProperties the properties to be included in the will PUBLISH message (used only in MQTT5)
 * @param willTopic the topic of the will PUBLISH message
 * @param willPayload the content of the will PUBLISH message
 * @param willQos the QoS of the will PUBLISH message
 * @param enhancedAuthCallback the callback called when authenticationData is received, it should return the data necessary to continue authentication or null if completed (used only in MQTT5 if authenticationMethod has been set in the CONNECT properties)
 * @param publishReceived the callback called when a PUBLISH message is received by this client
 */
public class MQTTClient(
    private val mqttVersion: MQTTVersion,
    private val address: String,
    private val port: Int,
    private val tls: TLSClientSettings?,
    private var keepAlive: Int = 60,
    private val webSocket: String? = null,
    private val cleanStart: Boolean = true,
    private var clientId: String? = null,
    private val userName: String? = null,
    private val password: UByteArray? = null,
    private val properties: MQTT5Properties = MQTT5Properties(),
    private val willProperties: MQTT5Properties? = null,
    private val willTopic: String? = null,
    private val willPayload: UByteArray? = null,
    private val willRetain: Boolean = false,
    private val willQos: Qos = Qos.AT_MOST_ONCE,
    private val enhancedAuthCallback: (authenticationData: UByteArray?) -> UByteArray? = { null },
    private val onConnected: (connack: MQTTConnack) -> Unit = {},
    private val onDisconnected: (disconnect: MQTTDisconnect?) -> Unit = {},
    private val onError: (error: Throwable) -> Unit = { throw it },
    private val publishReceived: (publish: MQTTPublish) -> Unit
) {

    private val maximumPacketSize = properties.maximumPacketSize?.toInt() ?: (1024 * 1024)
    private var socket: SocketInterface? = null
    public var running: Boolean = false
        private set
    private val lock = reentrantLock()

    private val currentReceivedPacket = MQTTCurrentPacket(maximumPacketSize.toUInt(), mqttVersion)
    private var lastActiveTimestamp: Long = currentTimeMillis()

    // Session
    private var packetIdentifier: UInt = 1u
    // QoS 1 and QoS 2 messages which have been sent to the Server, but have not been completely acknowledged
    private val pendingAcknowledgeMessages = mutableMapOf<UInt, MQTTPublish>()
    private val pendingAcknowledgePubrel = mutableMapOf<UInt, MQTTPubrel>()
    // QoS 2 messages which have been received from the Server, but have not been completely acknowledged
    private val qos2ListReceived = mutableListOf<UInt>()

    // Connection
    private val topicAliasesClient = mutableMapOf<UInt, String>() // TODO reset all these on reconnection
    private var maximumQos = Qos.EXACTLY_ONCE
    private var retainedSupported = true
    private var maximumServerPacketSize = 128 * 1024 * 1024
    private var topicAliasMaximum = 0u
    private var wildcardSubscriptionAvailable = true
    private var subscriptionIdentifiersAvailable = true
    private var sharedSubscriptionAvailable = true
    private var receiveMax = 65535u
    public var connackReceived: Boolean = false
        private set

    init {
        if (keepAlive > 65535) {
            throw IllegalArgumentException("Keep alive exceeding the maximum value")
        }

        if (willTopic == null && (willQos != Qos.AT_MOST_ONCE || willPayload != null || willRetain)) {
            throw IllegalArgumentException("Will topic null, but other will options have been set")
        }

        if (userName == null && password != null) {
            throw IllegalArgumentException("Cannot set password without username")
        }

        running = true
    }

    private fun connectSocket() {
        if (socket == null) {
            connackReceived = false
            socket = if (tls == null)
                ClientSocket(address, port, maximumPacketSize, 250, ::check)
            else
                TLSClientSocket(address, port, maximumPacketSize, 250, tls, ::check)
            if (webSocket != null) {
                socket = WebSocket(socket!!, address, webSocket)
            }

            sendConnect()
        }
    }

    private fun send(data: UByteArray) {
        connectSocket()
        lastActiveTimestamp = currentTimeMillis()
        socket!!.send(data)
    }

    private fun sendConnect() {
        val connect = if (mqttVersion == MQTTVersion.MQTT3_1_1) {
            MQTT4Connect(
                "MQTT",
                ConnectFlags(userName != null, password != null, willRetain, willQos, willTopic != null, cleanStart, false),
                keepAlive,
                clientId ?: generateRandomClientId(),
                willTopic,
                willPayload,
                userName,
                password
            )
        } else {
            MQTT5Connect(
                "MQTT",
                ConnectFlags(userName != null, password != null, willRetain, willQos, willTopic != null, cleanStart, false),
                keepAlive,
                clientId ?: generateRandomClientId(),
                properties,
                willProperties,
                willTopic,
                willPayload,
                userName,
                password
            )
        }
        send(connect.toByteArray())
        lastActiveTimestamp = currentTimeMillis()
    }

    private fun generatePacketId(): UInt {
        do {
            packetIdentifier++
            if (packetIdentifier > 65535u)
                packetIdentifier = 1u
        } while (isPacketIdInUse(packetIdentifier))

        return packetIdentifier
    }

    private fun isPacketIdInUse(packetId: UInt): Boolean {
        if (qos2ListReceived.contains(packetId))
            return true
        if (pendingAcknowledgeMessages[packetId] != null)
            return true
        if (pendingAcknowledgePubrel[packetId] != null)
            return true
        return false
    }

    /**
     * Send a PUBLISH message
     *
     * @param retain whether the message should be retained by the server
     * @param qos the QoS value
     * @param topic the topic of the message
     * @param payload the content of the message
     * @param properties the properties to be included in the message (used only in MQTT5)
     */
    public fun publish(retain: Boolean, qos: Qos, topic: String, payload: UByteArray?, properties: MQTT5Properties = MQTT5Properties()) {
        lock.withLock {
            if (!connackReceived && properties.authenticationData != null) {
                throw Exception("Not sending until connection complete")
            }
            if (qos > maximumQos) {
                throw Exception("QoS exceeding maximum server supported QoS")
            }
            if (retain && !retainedSupported) {
                throw Exception("Retained not supported by the server")
            }

            val packetId = if (qos != Qos.AT_MOST_ONCE) {
                generatePacketId()
            } else {
                null
            }
            val publish = if (mqttVersion == MQTTVersion.MQTT3_1_1) {
                MQTT4Publish(retain, qos, false, topic, packetId, payload)
            } else {
                // TODO support client topic aliases
                MQTT5Publish(retain, qos, false, topic, packetId, properties, payload)
            }
            if (qos != Qos.AT_MOST_ONCE) {
                if (pendingAcknowledgeMessages.size + pendingAcknowledgePubrel.size >= receiveMax.toInt()) {
                    throw Exception("Sending more PUBLISH with QoS > 0 than indicated by the server in receiveMax")
                }
                pendingAcknowledgeMessages[packetId!!] = publish
            }
            val data = publish.toByteArray()
            if (data.size > maximumServerPacketSize) {
                throw Exception("Packet size too big for the server to handle")
            }
            send(data)
        }
    }

    /**
     * Subscribe to the specified topics
     *
     * @param subscriptions the list of topic filters and relative settings (many settings are used only in MQTT5)
     * @param properties the properties to be included in the message (used only in MQTT5)
     */
    public fun subscribe(subscriptions: List<Subscription>, properties: MQTT5Properties = MQTT5Properties()) {
        lock.withLock {
            if (!connackReceived && properties.authenticationData != null) {
                throw Exception("Not sending until connection complete")
            }
            val subscribe = if (mqttVersion == MQTTVersion.MQTT3_1_1) {
                MQTT4Subscribe(generatePacketId(), subscriptions)
            } else {
                MQTT5Subscribe(generatePacketId(), subscriptions, properties)
            }
            send(subscribe.toByteArray())
        }
    }

    /**
     * Unsubscribe from the specified topics
     *
     * @param topics the list of topic filters
     * @param properties the properties to be included in the message (used only in MQTT5)
     */
    public fun unsubscribe(topics: List<String>, properties: MQTT5Properties = MQTT5Properties()) {
        lock.withLock {
            if (!connackReceived && properties.authenticationData != null) {
                throw Exception("Not sending until connection complete")
            }
            val unsubscribe = if (mqttVersion == MQTTVersion.MQTT3_1_1) {
                MQTT4Unsubscribe(generatePacketId(), topics)
            } else {
                MQTT5Unsubscribe(generatePacketId(), topics, properties)
            }
            send(unsubscribe.toByteArray())
        }
    }

    /**
     * Disconnect the client
     *
     * @param reasonCode the specific reason code (only used in MQTT5)
     */
    public fun disconnect(reasonCode: ReasonCode) {
        lock.withLock {
            val disconnect = if (mqttVersion == MQTTVersion.MQTT3_1_1) {
                MQTT4Disconnect()
            } else {
                MQTT5Disconnect(reasonCode)
            }
            send(disconnect.toByteArray())
            close()
        }
    }

    private var lastException: Exception? = null

    private fun check() {
        lock.withLock {
            if (!running) {
                return
            }
            if (socket == null) {
                close()
                // Needed because of JS callbacks, otherwise the exception gets swallowed and tests don't complete correctly
                throw lastException ?: SocketClosedException("")
            }
            val data = socket?.read()

            if (data != null) {
                lastActiveTimestamp = currentTimeMillis()

                try {
                    currentReceivedPacket.addData(data).forEach {
                        handlePacket(it)
                    }
                } catch (e: MQTTException) {
                    lastException = e
                    e.printStackTrace()
                    disconnect(e.reasonCode)
                    close()
                    onDisconnected(null)
                    throw e
                } catch (e: EOFException) {
                    lastException = e
                    println("EOF")
                    close()
                    onDisconnected(null)
                    throw e
                } catch (e: IOException) {
                    lastException = e
                    println("IOException ${e.message}")
                    disconnect(ReasonCode.UNSPECIFIED_ERROR)
                    close()
                    onDisconnected(null)
                    throw e
                } catch (e: Exception) {
                    lastException = e
                    println("Exception ${e.message} ${e.cause?.message}")
                    disconnect(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
                    close()
                    onDisconnected(null)
                    throw e
                }
            } else {
                // If connack not received in a reasonable amount of time, then disconnect
                val currentTime = currentTimeMillis()
                if (!connackReceived && currentTime > lastActiveTimestamp + 30000) {
                    close()
                    lastException = Exception("CONNACK not received in 30 seconds")
                    throw lastException!!
                }

                if (keepAlive != 0 && connackReceived) {
                    if (currentTime > lastActiveTimestamp + (keepAlive * 1000)) {
                        // Timeout
                        close()
                        lastException = MQTTException(ReasonCode.KEEP_ALIVE_TIMEOUT)
                        throw lastException!!
                    } else if (currentTime > lastActiveTimestamp + (keepAlive * 1000 * 0.9)) {
                        val pingreq = if (mqttVersion == MQTTVersion.MQTT3_1_1) {
                            MQTT4Pingreq()
                        } else {
                            MQTT5Pingreq()
                        }
                        send(pingreq.toByteArray())
                        // TODO if not receiving pingresp after a reasonable amount of time, close connection
                    }
                }
            }
        }
    }

    /**
     * Run a single iteration of the client (non-blocking)
     */
    public fun step() {
        lock.withLock {
            if (running) {
                connectSocket()

                check()
            }
        }
    }

    /**
     * Run the client (blocking)
     */
    public fun run() {
        while (running) {
            step()
        }
    }

    /**
     * Run the client in a coroutine using [dispatcher] with a [delayTimeMs] delay between each
     * iteration to workaround abusing thread lock
     */
    public fun runAsync(
        dispatcher: CoroutineDispatcher,
        delayTimeMs : Long = 1L
    ) {
        CoroutineScope(dispatcher).launch {
            while (running) {
                try {
                    step()
                } catch (t: Throwable) {
                    close()
                    onError(t)
                    return@launch
                }
                delay(delayTimeMs)
            }
        }
    }

    private fun handlePacket(packet: MQTTPacket) {
        when (packet) {
            is MQTTConnack -> handleConnack(packet)
            is MQTTPublish -> handlePublish(packet)
            is MQTTPuback -> handlePuback(packet)
            is MQTTPubrec -> handlePubrec(packet)
            is MQTTPubrel -> handlePubrel(packet)
            is MQTTPubcomp -> handlePubcomp(packet)
            is MQTTSuback -> handleSuback(packet)
            is MQTTUnsuback -> handleUnsuback(packet)
            is MQTTPingresp -> handlePingresp()
            is MQTTDisconnect -> handleDisconnect(packet)
            is MQTT5Auth -> handleAuth(packet)
            else -> throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
    }

    private fun handleConnack(packet: MQTTConnack) {
        if (packet is MQTT5Connack) {
            if (packet.connectReasonCode != ReasonCode.SUCCESS) {
                if ((packet.connectReasonCode == ReasonCode.USE_ANOTHER_SERVER || packet.connectReasonCode == ReasonCode.SERVER_MOVED) && packet.properties.serverReference != null) {
                    // TODO if reason code 0x9C try to connect to the given server (4.11 format)
                } else {
                    throw MQTTException(packet.connectReasonCode)
                }
            }

            receiveMax = packet.properties.receiveMaximum ?: 65535u
            maximumQos = packet.properties.maximumQos?.let { Qos.valueOf(it.toInt()) } ?: Qos.EXACTLY_ONCE
            retainedSupported = packet.properties.retainAvailable != 0u
            maximumServerPacketSize = packet.properties.maximumPacketSize?.toInt() ?: maximumServerPacketSize
            clientId = packet.properties.assignedClientIdentifier ?: clientId
            topicAliasMaximum = packet.properties.topicAliasMaximum ?: topicAliasMaximum
            wildcardSubscriptionAvailable = packet.properties.wildcardSubscriptionAvailable != 0u
            subscriptionIdentifiersAvailable = packet.properties.subscriptionIdentifierAvailable != 0u
            sharedSubscriptionAvailable = packet.properties.sharedSubscriptionAvailable != 0u
            keepAlive = packet.properties.serverKeepAlive?.toInt() ?: keepAlive

            enhancedAuthCallback(packet.properties.authenticationData)
        } else if (packet is MQTT4Connack) {
            if (packet.connectReturnCode != ConnectReturnCode.CONNECTION_ACCEPTED) {
                throw IOException("Connection failed with code: ${packet.connectReturnCode}")
            }
        }

        connackReceived = true
        if (cleanStart && packet.connectAcknowledgeFlags.sessionPresentFlag) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        } else if (!cleanStart && !packet.connectAcknowledgeFlags.sessionPresentFlag) {
            // Session expired on the server, so clean the local session
            packetIdentifier = 1u
            pendingAcknowledgeMessages.clear()
            pendingAcknowledgePubrel.clear()
            qos2ListReceived.clear()
        } else if (!cleanStart && packet.connectAcknowledgeFlags.sessionPresentFlag) {
            // Resend pending publish and pubrel messages (with dup=1)
            pendingAcknowledgeMessages.forEach {
                send(it.value.setDuplicate().toByteArray())
            }
            pendingAcknowledgePubrel.forEach {
                send(it.value.toByteArray())
            }
        }
        onConnected(packet)
    }

    private fun handlePublish(packet: MQTTPublish) {
        var correctPacket = packet

        if (correctPacket.qos > maximumQos) {
            throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        if (correctPacket.qos > Qos.AT_LEAST_ONCE) {
            if (qos2ListReceived.size > (properties.receiveMaximum?.toInt() ?: 65535)) {
                // Received too many messages
                throw MQTTException(ReasonCode.RECEIVE_MAXIMUM_EXCEEDED)
            }
        }

        if (correctPacket is MQTT5Publish) {
            if (correctPacket.properties.topicAlias != null) {
                if (correctPacket.properties.topicAlias == 0u || correctPacket.properties.topicAlias!! > (properties.topicAliasMaximum
                        ?: 65535u)
                ) {
                    throw MQTTException(ReasonCode.TOPIC_ALIAS_INVALID)
                }
                if (correctPacket.topicName.isNotEmpty()) {
                    // Map alias
                    topicAliasesClient[correctPacket.properties.topicAlias!!] = correctPacket.topicName
                } else if (correctPacket.topicName.isEmpty()) {
                    // Use alias
                    val topicName = topicAliasesClient[correctPacket.properties.topicAlias!!] ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    correctPacket = correctPacket.setTopicFromAlias(topicName)
                }
            }
        }

        when (correctPacket.qos) {
            Qos.AT_MOST_ONCE -> {
                publishReceived(correctPacket)
            }
            Qos.AT_LEAST_ONCE -> {
                val puback = if (correctPacket is MQTT4Publish) {
                    MQTT4Puback(correctPacket.packetId!!)
                } else {
                    MQTT5Puback(correctPacket.packetId!!)
                }
                send(puback.toByteArray())
                publishReceived(correctPacket)
            }
            Qos.EXACTLY_ONCE -> {
                val pubrec = if (correctPacket is MQTT4Publish) {
                    MQTT4Pubrec(correctPacket.packetId!!)
                } else {
                    MQTT5Pubrec(correctPacket.packetId!!)
                }
                send(pubrec.toByteArray())
                if (!qos2ListReceived.contains(correctPacket.packetId!!)) {
                    qos2ListReceived.add(correctPacket.packetId!!)
                    publishReceived(correctPacket)
                }
            }
        }
    }

    private fun handlePuback(packet: MQTTPuback) {
        if (packet is MQTT5Puback && properties.requestProblemInformation == 0u && (packet.properties.reasonString != null || packet.properties.userProperty.isNotEmpty())) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
        pendingAcknowledgeMessages.remove(packet.packetId)
    }

    private fun handlePubrec(packet: MQTTPubrec) {
        if (packet is MQTT5Pubrec && properties.requestProblemInformation == 0u && (packet.properties.reasonString != null || packet.properties.userProperty.isNotEmpty())) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
        pendingAcknowledgeMessages.remove(packet.packetId)
        val pubrel = if (packet is MQTT4Pubrec) {
            MQTT4Pubrel(packet.packetId)
        } else {
            MQTT5Pubrel(packet.packetId)
        }
        pendingAcknowledgePubrel[packet.packetId] = pubrel
        send(pubrel.toByteArray())
    }

    private fun handlePubrel(packet: MQTTPubrel) {
        if (packet is MQTT5Pubrel && properties.requestProblemInformation == 0u && (packet.properties.reasonString != null || packet.properties.userProperty.isNotEmpty())) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
        val pubcomp = if (packet is MQTT4Pubrel) {
            MQTT4Pubcomp(packet.packetId)
        } else {
            MQTT5Pubcomp(packet.packetId)
        }
        send(pubcomp.toByteArray())
        if (!qos2ListReceived.remove(packet.packetId)) {
            throw MQTTException(ReasonCode.PACKET_IDENTIFIER_NOT_FOUND)
        }
    }

    private fun handlePubcomp(packet: MQTTPubcomp) {
        if (packet is MQTT5Pubcomp && properties.requestProblemInformation == 0u && (packet.properties.reasonString != null || packet.properties.userProperty.isNotEmpty())) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
        if (pendingAcknowledgePubrel.remove(packet.packetId) == null) {
            throw MQTTException(ReasonCode.PACKET_IDENTIFIER_NOT_FOUND)
        }
    }

    private fun handleSuback(packet: MQTTSuback) {
        if (packet is MQTT4Suback) {
            for (reasonCode in packet.reasonCodes) {
                if (reasonCode == SubackReturnCode.FAILURE) {
                    throw MQTTException(ReasonCode.UNSPECIFIED_ERROR)
                }
            }
        } else if (packet is MQTT5Suback) {
            if (properties.requestProblemInformation == 0u && (packet.properties.reasonString != null || packet.properties.userProperty.isNotEmpty())) {
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            }
            for (reasonCode in packet.reasonCodes) {
                if (reasonCode != ReasonCode.SUCCESS && reasonCode != ReasonCode.GRANTED_QOS1 && reasonCode != ReasonCode.GRANTED_QOS2) {
                    throw MQTTException(reasonCode)
                }
            }
        }
    }

    private fun handleUnsuback(packet: MQTTUnsuback) {
        if (packet is MQTT5Unsuback && properties.requestProblemInformation == 0u && (packet.properties.reasonString != null || packet.properties.userProperty.isNotEmpty())) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
    }

    private fun handlePingresp() {
        lastActiveTimestamp = currentTimeMillis()
    }

    private fun handleAuth(packet: MQTT5Auth) {
        if (packet.authenticateReasonCode == ReasonCode.CONTINUE_AUTHENTICATION) {
            val data = enhancedAuthCallback(packet.properties.authenticationData)
            val auth = MQTT5Auth(ReasonCode.CONTINUE_AUTHENTICATION, MQTT5Properties(authenticationMethod = packet.properties.authenticationMethod, authenticationData = data))
            send(auth.toByteArray())
        }
    }

    /**
     * Start the re-authentication process, to be used only when authenticationMethod has been set in the CONNECT packet
     *
     * @param data the authenticationData if necessary
     */
    public fun reAuthenticate(data: UByteArray?) {
        lock.withLock {
            val auth = MQTT5Auth(
                ReasonCode.RE_AUTHENTICATE,
                MQTT5Properties(authenticationMethod = properties.authenticationMethod, authenticationData = data)
            )
            send(auth.toByteArray())
        }
    }

    private fun handleDisconnect(disconnect: MQTTDisconnect) {
        if (disconnect is MQTT5Disconnect) {
            close()
            if ((disconnect.reasonCode == ReasonCode.USE_ANOTHER_SERVER || disconnect.reasonCode == ReasonCode.SERVER_MOVED) && disconnect.properties.serverReference != null) {
                // TODO connect to the new server
            } else {
                throw MQTTException(disconnect.reasonCode)
            }
        }
        onDisconnected(disconnect)
    }

    private fun close() {
        running = false
        socket?.close()
        connackReceived = false
        socket = null
    }
}