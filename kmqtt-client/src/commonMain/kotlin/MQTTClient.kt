import mqtt.MQTTCurrentPacket
import mqtt.MQTTException
import mqtt.Subscription
import mqtt.packets.ConnectFlags
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqtt.*
import mqtt.packets.mqttv4.*
import mqtt.packets.mqttv5.*
import socket.IOException
import socket.streams.EOFException

class MQTTClient(
    private val mqttVersion: Int,
    address: String,
    port: Int,
    tls: Boolean,
    private val keepAlive: Int,
    private val cleanStart: Boolean = true,
    private val clientId: String? = null,
    private val userName: String? = null,
    private val password: UByteArray? = null,
    private val properties: MQTT5Properties = MQTT5Properties(),
    private val willProperties: MQTT5Properties? = null,
    private val willTopic: String? = null,
    private val willPayload: UByteArray? = null,
    private val willRetain: Boolean = false,
    private val willQos: Qos = Qos.AT_MOST_ONCE,
    private val publishReceived: (publish: MQTTPublish) -> Unit
) {

    // TODO check MQTT5 compliance

    private val maximumPacketSize = 1024 * 1024
    private val socket = if (!tls) ClientSocket(address, port, maximumPacketSize, 1000) else TLSClientSocket(address, port, maximumPacketSize, 1000)
    private var running = false

    private val currentReceivedPacket = MQTTCurrentPacket(maximumPacketSize.toUInt(), mqttVersion)
    private var lastActiveTimestamp: Long = currentTimeMillis()

    // Session
    private var packetIdentifier: UInt = 1u
    // QoS 1 and QoS 2 messages which have been sent to the Server, but have not been completely acknowledged
    private val pendingAcknowledgeMessages = mutableMapOf<UInt, MQTTPublish>()
    private val pendingAcknowledgePubrel = mutableMapOf<UInt, MQTTPubrel>()
    // QoS 2 messages which have been received from the Server, but have not been completely acknowledged
    private val qos2ListReceived = mutableListOf<UInt>()

    // TODO upon disconnection, if cleanStart == 0, reconnect and resend pending publish and pubrel messages

    init {
        // TODO allow more configuration and TLS
        if (mqttVersion != 4 && mqttVersion != 5) {
            throw IllegalArgumentException("Unknown MQTT version")
        }

        if (keepAlive > 18 * 60 * 60 + 12 * 60 + 15) {
            throw IllegalArgumentException("Keep alive exceeding the maximum value")
        }

        if (willTopic == null && (willQos != Qos.AT_MOST_ONCE || willPayload != null || willRetain)) {
            throw IllegalArgumentException("Will topic null, but other will options have been set")
        }

        if (userName == null && password != null) {
            throw IllegalArgumentException("Cannot set password without username")
        }

        sendConnect()
    }

    private fun sendConnect() {
        val connect = if (mqttVersion == 4) {
            MQTT4Connect(
                "MQTT",
                mqttVersion,
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
                mqttVersion,
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
        socket.send(connect.toByteArray())
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
        return false
    }

    fun publish(retain: Boolean, qos: Qos, topic: String, payload: UByteArray?, properties: MQTT5Properties = MQTT5Properties()) {
        val packetId = if (qos != Qos.AT_MOST_ONCE) {
            generatePacketId()
        } else {
            null
        }
        val publish = if (mqttVersion == 4) {
            MQTT4Publish(retain, qos, false, topic, packetId, payload)
        } else {
            MQTT5Publish(retain, qos, false, topic, packetId, properties, payload)
        }
        if (qos != Qos.AT_MOST_ONCE) {
            pendingAcknowledgeMessages[packetId!!] = publish
        }
        socket.send(publish.toByteArray())
    }

    fun subscribe(subscriptions: List<Subscription>, properties: MQTT5Properties = MQTT5Properties()) {
        val subscribe = if (mqttVersion == 4) {
            MQTT4Subscribe(generatePacketId(), subscriptions)
        } else {
            MQTT5Subscribe(generatePacketId(), subscriptions, properties)
        }
        socket.send(subscribe.toByteArray())
    }

    fun unsubscribe(topics: List<String>, properties: MQTT5Properties = MQTT5Properties()) {
        val unsubscribe = if (mqttVersion == 4) {
            MQTT4Unsubscribe(generatePacketId(), topics)
        } else {
            MQTT5Unsubscribe(generatePacketId(), topics, properties)
        }
        socket.send(unsubscribe.toByteArray())
    }

    fun disconnect(reasonCode: ReasonCode) {
        val disconnect = if (mqttVersion == 4) {
            MQTT4Disconnect()
        } else {
            MQTT5Disconnect(reasonCode)
        }
        socket.send(disconnect.toByteArray())
        close()
    }

    fun run() {
        running = true
        while (running) {
            val data = socket.read()
            if (data != null) {
                lastActiveTimestamp = currentTimeMillis()

                try {
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
                    disconnect(ReasonCode.UNSPECIFIED_ERROR)
                } catch (e: Exception) {
                    println("Exception ${e.message} ${e.cause?.message}")
                    disconnect(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
                }
            } else {
                // TODO if connack not received in a reasonable amount of time, then disconnect
                if (keepAlive != 0) {
                    val currentTime = currentTimeMillis()
                    if (currentTime > lastActiveTimestamp + (keepAlive * 1000)) {
                        // Timeout
                        close()
                        throw MQTTException(ReasonCode.KEEP_ALIVE_TIMEOUT)
                    } else if (currentTime > lastActiveTimestamp + (keepAlive * 1000 * 0.9)) {
                        val pingreq = if (mqttVersion == 4) {
                            MQTT4Pingreq()
                        } else {
                            MQTT5Pingreq()
                        }
                        socket.send(pingreq.toByteArray())
                        // TODO if not receiving pingresp after a reasonable amount of time, close connection
                    }
                }
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
            is MQTTUnsuback -> handleUnsuback()
            is MQTTPingresp -> handlePingresp()
            is MQTTDisconnect -> handleDisconnect(packet)
            //is MQTTAuth -> handleAuth(packet) TODO
            else -> throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
    }

    private fun handleConnack(packet: MQTTConnack) {
        if (cleanStart && packet.connectAcknowledgeFlags.sessionPresentFlag) {
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        } else if (!cleanStart && !packet.connectAcknowledgeFlags.sessionPresentFlag) {
            // TODO either proceed with the session or disconnect
        }
    }

    private fun handlePublish(packet: MQTTPublish) {
        when (packet.qos) {
            Qos.AT_MOST_ONCE -> {
                publishReceived(packet)
            }
            Qos.AT_LEAST_ONCE -> {
                val puback = if (packet is MQTT4Publish) {
                    MQTT4Puback(packet.packetId!!)
                } else {
                    MQTT5Puback(packet.packetId!!)
                }
                socket.send(puback.toByteArray())
                publishReceived(packet)
            }
            Qos.EXACTLY_ONCE -> {
                val pubrec = if (packet is MQTT4Publish) {
                    MQTT4Pubrec(packet.packetId!!)
                } else {
                    MQTT5Pubrec(packet.packetId!!)
                }
                socket.send(pubrec.toByteArray())
                if (!qos2ListReceived.contains(packet.packetId!!)) {
                    qos2ListReceived.add(packet.packetId!!)
                    publishReceived(packet)
                }
            }
        }

    }

    private fun handlePuback(packet: MQTTPuback) {
        pendingAcknowledgeMessages.remove(packet.packetId)
    }

    private fun handlePubrec(packet: MQTTPubrec) {
        pendingAcknowledgeMessages.remove(packet.packetId)
        val pubrel = if (packet is MQTT4Pubrec) {
            MQTT4Pubrel(packet.packetId)
        } else {
            MQTT5Pubrel(packet.packetId)
        }
        pendingAcknowledgePubrel[packet.packetId] = pubrel
        socket.send(pubrel.toByteArray())
    }

    private fun handlePubrel(packet: MQTTPubrel) {
        qos2ListReceived.remove(packet.packetId)
        val pubcomp = if (packet is MQTT4Pubrel) {
            MQTT4Pubcomp(packet.packetId)
        } else {
            MQTT5Pubcomp(packet.packetId)
        }
        socket.send(pubcomp.toByteArray())
    }

    private fun handlePubcomp(packet: MQTTPubcomp) {
        pendingAcknowledgePubrel.remove(packet.packetId)
    }

    private fun handleSuback(packet: MQTTSuback) {
        if (packet is MQTT4Suback) {
            for (reasonCode in packet.reasonCodes) {
                if (reasonCode == SubackReturnCode.FAILURE) {
                    throw MQTTException(ReasonCode.UNSPECIFIED_ERROR)
                }
            }
        } else if (packet is MQTT5Suback) {
            for (reasonCode in packet.reasonCodes) {
                if (reasonCode != ReasonCode.SUCCESS || reasonCode != ReasonCode.GRANTED_QOS1 || reasonCode != ReasonCode.GRANTED_QOS2) {
                    throw MQTTException(reasonCode)
                }
            }
        }
    }

    private fun handleUnsuback() {

    }

    private fun handlePingresp() {
        lastActiveTimestamp = currentTimeMillis()
    }

    private fun handleDisconnect(disconnect: MQTTDisconnect) {
        if (disconnect is MQTT5Disconnect) {
            close()
            throw MQTTException(disconnect.reasonCode)
        }
    }

    private fun close() {
        running = false
        socket.close()
    }
}