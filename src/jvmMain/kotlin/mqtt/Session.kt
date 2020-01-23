package mqtt

import ClientConnection
import mqtt.packets.MQTTConnect
import mqtt.packets.MQTTPublish
import mqtt.packets.MQTTPubrel

class Session(packet: MQTTConnect, var clientConnection: ClientConnection) {

    private var connected = false // true only after sending CONNACK
    var destroySessionTimestamp: Long? = null

    var clientId = packet.clientID

    private var packetIdentifier = 1u

    // QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely acknowledged
    private val pendingAcknowledgeMessages = mutableMapOf<UInt, MQTTPublish>()
    private val pendingAcknowledgePubrel = mutableMapOf<UInt, MQTTPubrel>()
    // QoS 1 and QoS 2 messages pending transmission to the Client
    private val pendingSendMessages = mutableMapOf<UInt, MQTTPublish>()
    // QoS 2 messages which have been received from the Client that have not been completely acknowledged
    val qos2ListReceived = mutableMapOf<UInt, MQTTPublish>()

    fun sendQosBiggerThanZero(packet: MQTTPublish, block: (packet: MQTTPublish) -> Unit) {
        pendingSendMessages[packet.packetId!!] = packet
        block(packet)
        pendingSendMessages.remove(packet.packetId)
        pendingAcknowledgeMessages[packet.packetId] = packet
    }

    fun hasPendingAcknowledgeMessage(packetId: UInt): Boolean {
        return pendingAcknowledgeMessages[packetId] != null
    }

    fun acknowledgePublish(packetId: UInt) {
        pendingAcknowledgeMessages.remove(packetId)
    }

    fun addPendingAcknowledgePubrel(packet: MQTTPubrel) {
        pendingAcknowledgePubrel[packet.packetId] = packet
    }

    fun hasPendingAcknowledgePubrel(packetId: UInt): Boolean {
        return pendingAcknowledgePubrel[packetId] != null
    }

    fun acknowledgePubrel(packetId: UInt) {
        pendingAcknowledgePubrel.remove(packetId)
    }

    // The Clients subscriptions, including any Subscription Identifiers
    private val subscriptions = mutableListOf<Subscription>()

    fun hasSubscriptionsMatching(topicName: String): List<Subscription> {
        if (!connected)
            return listOf()
        return subscriptions.filter { topicName.matchesWildcard(it.matchTopicFilter) }
    }

    fun hasSharedSubscriptionMatching(
        shareName: String,
        topicName: String
    ): Subscription? {
        if (!connected)
            return null
        return subscriptions.firstOrNull { it.isShared() && it.shareName == shareName && topicName.matchesWildcard(it.matchTopicFilter) }
    }

    fun addSubscription(subscription: Subscription): Boolean {
        val replaced = subscriptions.removeIf { it.topicFilter == subscription.topicFilter }
        subscriptions += subscription
        return replaced
    }

    fun removeSubscription(topicFilter: String): Boolean {
        return subscriptions.removeIf { it.topicFilter == topicFilter }
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
        destroySessionTimestamp = null
    }

    fun disconnected() {
        connected = false
        destroySessionTimestamp = if (sessionExpiryInterval == 0xFFFFFFFFu)
            null
        else
            System.currentTimeMillis() + (sessionExpiryInterval.toLong() * 1000)
    }

    fun isConnected() = connected
}
