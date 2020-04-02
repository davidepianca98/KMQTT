package mqtt.broker

import currentTimeMillis
import mqtt.Will
import mqtt.packets.Qos
import mqtt.packets.mqttv5.*

class Session(
    packet: MQTTConnect,
    var clientConnection: ClientConnection?,
    private val persist: (clientId: String, session: Session) -> Unit
) {

    private var connected = false // true only after sending CONNACK
    var sessionDisconnectedTimestamp: Long? = null

    var clientId = packet.clientID

    private var packetIdentifier = 1u

    // QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely acknowledged
    private val pendingAcknowledgeMessages = mutableMapOf<UInt, MQTTPublish>()
    private val pendingAcknowledgePubrel = mutableMapOf<UInt, MQTTPubrel>()
    // QoS 1 and QoS 2 messages pending transmission to the Client
    private val pendingSendMessages = mutableMapOf<UInt, MQTTPublish>()
    // QoS 2 messages which have been received from the Client that have not been completely acknowledged
    val qos2ListReceived = mutableMapOf<UInt, MQTTPublish>()

    init {
        persist()
    }

    private fun persist() {
        this.persist(clientId, this)
    }

    fun hasPendingAcknowledgeMessage(packetId: UInt): Boolean {
        return pendingAcknowledgeMessages[packetId] != null
    }

    fun acknowledgePublish(packetId: UInt) {
        pendingAcknowledgeMessages.remove(packetId)
        persist()
    }

    fun addPendingAcknowledgePubrel(packet: MQTTPubrel) {
        pendingAcknowledgePubrel[packet.packetId] = packet
        persist()
    }

    fun acknowledgePubrel(packetId: UInt) {
        pendingAcknowledgePubrel.remove(packetId)
        persist()
    }

    fun resendPending(sendPacket: (packet: MQTT5Packet) -> Unit) {
        pendingAcknowledgeMessages.forEach {
            if (!it.value.messageExpiryIntervalExpired()) {
                it.value.updateMessageExpiryInterval()
                sendPacket(it.value.setDuplicate())
            }
        }
        pendingAcknowledgePubrel.forEach {
            sendPacket(it.value)
        }
        pendingSendMessages.forEach {
            if (!it.value.messageExpiryIntervalExpired()) {
                it.value.updateMessageExpiryInterval()
                sendPacket(it.value)
            }
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
        val packetId = if (qos >= Qos.AT_MOST_ONCE) generatePacketId() else null

        val packetTopicName = clientConnection?.getPublishTopicAlias(topicName, properties) ?: topicName

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
        packet.updateMessageExpiryInterval()

        if (packet.qos == Qos.AT_LEAST_ONCE || packet.qos == Qos.EXACTLY_ONCE) {
            if (clientConnection?.sendQuota ?: 1u <= 0u)
                return

            pendingSendMessages[packet.packetId!!] = packet
            persist()
            if (isConnected()) {
                clientConnection!!.writePacket(packet)
                clientConnection!!.decrementSendQuota()
                pendingSendMessages.remove(packet.packetId)
                persist()
                pendingAcknowledgeMessages[packet.packetId] = packet
                persist()
            }
        } else {
            if (isConnected()) {
                clientConnection!!.writePacket(packet)
            }
        }
    }

    // TODO shared subscription note:
    //  If the Server is in the process of sending a QoS 1 message to its chosen subscribing Client and the connection
    //  to that Client breaks before the Server has received an acknowledgement from the Client, the Server MAY wait for
    //  the Client to reconnect and retransmit the message to that Client. If the Client'sSession terminates before the
    //  Client reconnects, the Server SHOULD send the Application Message to another Client that is subscribed to the
    //  same Shared Subscription. It MAY attempt to send the message to another Client as soon as it loses its
    //  connection to the first Client.

    var will = Will.buildWill(packet)

    var sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u

    fun generatePacketId(): UInt {
        do {
            packetIdentifier++
            if (packetIdentifier > 65535u)
                packetIdentifier = 1u
        } while (isPacketIdInUse(packetIdentifier))
        persist()

        return packetIdentifier
    }

    fun isPacketIdInUse(packetId: UInt): Boolean {
        if (pendingSendMessages[packetId] != null)
            return true
        if (pendingAcknowledgeMessages[packetId] != null)
            return true
        return false
    }

    fun connected() {
        connected = true
        sessionDisconnectedTimestamp = null
        persist()
    }

    fun disconnected() {
        if (connected) {
            connected = false
            clientConnection = null
            if (sessionDisconnectedTimestamp == null) {
                sessionDisconnectedTimestamp = currentTimeMillis()
            }
        }
        persist()
    }

    fun getExpiryTime(): Long? {
        return if (sessionExpiryInterval == 0xFFFFFFFFu || connected) // If connected it doesn't expire
            null
        else
            sessionDisconnectedTimestamp?.plus((sessionExpiryInterval.toLong() * 1000))
    }

    fun isConnected() = connected
}
