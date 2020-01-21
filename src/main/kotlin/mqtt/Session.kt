package mqtt

import ClientConnection
import mqtt.packets.MQTTConnect
import mqtt.packets.MQTTPublish
import mqtt.packets.MQTTPubrel

class Session(packet: MQTTConnect, var clientConnection: ClientConnection) {

    var connected = false
    var destroySessionTimestamp: Long? =
        null // TODO set it on disconnection from now + sessionExpiryInterval, then periodically check if passed time and delete after

    var clientId = packet.clientID
    var keepAlive = packet.keepAlive

    // TODO handle packet identifier counter section 2.2.1
    var packetIdentifier = 1u

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
        return subscriptions.filter { topicName.matchesWildcard(it.matchTopicFilter) }
    }

    fun hasSharedSubscriptionMatching(
        shareName: String,
        topicName: String
    ): Subscription? { // TODO check if connected first
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
    //  If the Server is in the process of sending a QoS 1 message to its chosen subscribing Client and the connection to that Client breaks before the Server has received an acknowledgement from the Client, the Server MAY wait for the Client to reconnect and retransmit the message to that Client. If the Client'sSession terminates before the Client reconnects, the Server SHOULD send the Application Message to another Client that is subscribed to the same Shared Subscription. It MAY attempt to send the message to another Client as soon as it loses its connection to the first Client.

    // TODO publish will when:
    //  An I/O error or network failure detected by the Server.
    //  The Client fails to communicate within the Keep Alive time.
    //  The Client closes the Network Connection without first sending a DISCONNECT packet with a Reason Code 0x00 (Normal disconnection).
    //  The Server closes the Network Connection without first receiving a DISCONNECT packet with a Reason Code 0x00 (Normal disconnection).

    // TODO must be removed from the session also if will message published
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
        return if (packet.connectFlags.willFlag)
            Will(
                packet.connectFlags.willRetain,
                packet.connectFlags.willQos,
                packet.willTopic!!,
                packet.willPayload!!,
                packet.willProperties!!.willDelayInterval
                    ?: 0u, // TODO publish will after this interval or when the session ends, first to come
                packet.willProperties.payloadFormatIndicator ?: 0u, // TODO if 1 validate willpayload is utf-8
                packet.willProperties.messageExpiryInterval, // TODO lifetime of will message as publication expiry interval when sending
                packet.willProperties.contentType, // TODO send as content type if present
                packet.willProperties.responseTopic, // TODO send as response topic if present
                packet.willProperties.correlationData, // TODO send as correlation data if present
                packet.willProperties.userProperty // TODO send as user properties maintaining order
            )
        else
            null
    }

    fun update(packet: MQTTConnect) { // TODO probably many of these settings must be in client connection and not in session
        clientId = packet.clientID
        keepAlive = packet.keepAlive
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
        if (qos2ListReceived[packetId] != null)
            return true
        return false
    }
}
