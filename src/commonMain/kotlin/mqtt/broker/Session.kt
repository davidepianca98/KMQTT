package mqtt.broker

import currentTimeMillis
import mqtt.Subscription
import mqtt.Will
import mqtt.matchesWildcard
import mqtt.packets.mqttv5.MQTT5Packet
import mqtt.packets.mqttv5.MQTTConnect
import mqtt.packets.mqttv5.MQTTPublish
import mqtt.packets.mqttv5.MQTTPubrel

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

    fun sendQosBiggerThanZero(packet: MQTTPublish, block: (packet: MQTTPublish) -> Unit) {
        pendingSendMessages[packet.packetId!!] = packet
        persist()
        if (connected) {
            block(packet)
            pendingSendMessages.remove(packet.packetId)
            persist()
            pendingAcknowledgeMessages[packet.packetId] = packet
            persist()
        }
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
            if (!it.value.messageExpiryIntervalExpired())
                sendPacket(it.value.setDuplicate())
        }
        pendingAcknowledgePubrel.forEach {
            sendPacket(it.value)
        }
        pendingSendMessages.forEach {
            if (!it.value.messageExpiryIntervalExpired())
                sendPacket(it.value)
        }
    }

    // The Clients subscriptions, including any Subscription Identifiers
    private val subscriptions = mutableListOf<Subscription>()

    fun hasSubscriptionsMatching(topicName: String): List<Subscription> {
        return subscriptions.filter { topicName.matchesWildcard(it.matchTopicFilter) }
    }

    fun hasSharedSubscriptionMatching(shareName: String, topicName: String): Subscription? {
        return subscriptions.firstOrNull { it.isShared() && it.shareName == shareName && topicName.matchesWildcard(it.matchTopicFilter) }
    }

    fun addSubscription(subscription: Subscription): Boolean {
        val replaced = subscriptions.removeAll { it.topicFilter == subscription.topicFilter }
        subscriptions += subscription
        persist()
        return replaced
    }

    fun removeSubscription(topicFilter: String): Boolean {
        val result = subscriptions.removeAll { it.topicFilter == topicFilter }
        persist()
        return result
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
            sessionDisconnectedTimestamp = currentTimeMillis()
        }
        persist()
    }

    fun getExpiryTime(): Long? {
        return if (sessionExpiryInterval == 0xFFFFFFFFu || connected) // If connected it doesn't expire
            null
        else
            currentTimeMillis() + (sessionExpiryInterval.toLong() * 1000)
    }

    fun isConnected() = connected
}
