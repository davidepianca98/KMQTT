package mqtt

import ClientConnection
import mqtt.packets.MQTTConnect
import mqtt.packets.MQTTPublish
import mqtt.packets.MQTTPubrel
import mqtt.packets.ReasonCode

class Session(packet: MQTTConnect, var clientConnection: ClientConnection) {

    var connected = false // true only after sending CONNACK
    var destroySessionTimestamp: Long? =
        null // TODO set it on disconnection from now + sessionExpiryInterval, then periodically check if passed time and delete after

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
        // TODO maybe must be added to pending and sent later, check specification
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

    var will = buildWill(packet)

    // TODO if 0 delete session on disconnection else delete after timeout, if 0xFFFFFFFF never delete
    var sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
    // TODO don't send packets larger than this, if null no limit
    var maximumPacketSize = packet.properties.maximumPacketSize
    // TODO if 0 don't send topic alias, otherwise maximum number of aliases
    var topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0u
    // TODO if different from 0 or 1 protocol error, if 1 may return response information in connack
    var requestResponseInformation = packet.properties.requestResponseInformation ?: 0u
    // TODO if different from 0 or 1 protocol error, if 0 may send a reson string or user properties in connack or disconnect, but no reason string or user properties in any other packet than publish, connack, disconnect
    var requestProblemInformation = packet.properties.requestProblemInformation ?: 1u
    var userProperties = packet.properties.userProperty

    private fun buildWill(packet: MQTTConnect): Will? {
        val formatIndicator = packet.willProperties!!.payloadFormatIndicator ?: 0u
        if (packet.willPayload?.validatePayloadFormat(formatIndicator) == false)
            throw MQTTException(ReasonCode.PAYLOAD_FORMAT_INVALID)
        return if (packet.connectFlags.willFlag)
            Will(
                packet.connectFlags.willRetain,
                packet.connectFlags.willQos,
                packet.willTopic!!,
                packet.willPayload!!,
                packet.willProperties.willDelayInterval
                    ?: 0u, // TODO publish will after this interval or when the session ends, first to come, if client reconnects to session don't send
                formatIndicator,
                packet.willProperties.messageExpiryInterval,
                packet.willProperties.contentType,
                packet.willProperties.responseTopic,
                packet.willProperties.correlationData,
                packet.willProperties.userProperty
            )
        else
            null
    }

    fun update(packet: MQTTConnect) { // TODO probably many of these settings must be in client connection and not in session
        clientId = packet.clientID
        will = buildWill(packet)

        sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
        maximumPacketSize = packet.properties.maximumPacketSize
        topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0u
        requestResponseInformation = packet.properties.requestResponseInformation ?: 0u
        requestProblemInformation = packet.properties.requestProblemInformation ?: 1u
        userProperties = packet.properties.userProperty
    }

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
}
