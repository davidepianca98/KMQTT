package mqtt.broker

import currentTimeMillis
import mqtt.MQTTException
import mqtt.Subscription
import mqtt.matchesWildcard
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTTProperties
import mqtt.packets.mqttv5.MQTTPublish
import mqtt.packets.mqttv5.ReasonCode
import socket.ServerSocketLoop
import socket.tls.TLSSettings
import kotlin.math.min

class Broker(
    val port: Int = 1883,
    val host: String = "127.0.0.1",
    val backlog: Int = 128,
    val tlsSettings: TLSSettings? = null,
    val authentication: Authentication? = null,
    val enhancedAuthenticationProviders: Map<String, EnhancedAuthenticationProvider> = mapOf(),
    val authorization: Authorization? = null,
    val maximumSessionExpiryInterval: UInt = 0xFFFFFFFFu,
    val receiveMaximum: UShort? = null,
    val maximumQos: Qos? = null,
    val retainedAvailable: Boolean = true,
    val maximumPacketSize: UInt = 32768u,
    val maximumTopicAlias: Int? = null,
    val wildcardSubscriptionAvailable: Boolean = true,
    val subscriptionIdentifiersAvailable: Boolean = true,
    val sharedSubscriptionsAvailable: Boolean = true,
    val serverKeepAlive: Int? = null,
    val responseInformation: String? = null,
    val packetInterceptor: PacketInterceptor? = null
) {
    // TODO support WebSocket, section 6

    private val server = ServerSocketLoop(this)
    val sessions = mutableMapOf<String, Session>()
    private val retainedList = mutableMapOf<String, Pair<MQTTPublish, String>>()

    fun listen() {
        server.run()
    }

    private fun publishShared(
        publisherClientId: String,
        shareName: String,
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTTProperties,
        payload: UByteArray?
    ) {
        // Get the sessions which subscribe to this shared session and get the one which hasn't received a message for the longest time
        val session = sessions.minBy {
            it.value.hasSharedSubscriptionMatching(shareName, topicName)?.timestampShareSent ?: Long.MAX_VALUE
        }?.value
        session?.hasSharedSubscriptionMatching(shareName, topicName)?.let { subscription ->
            publish(publisherClientId, retain, topicName, qos, dup, properties, payload, session, subscription)
            subscription.timestampShareSent = currentTimeMillis()
        }
    }

    fun sendWill(session: Session?) {
        val will = session?.will ?: return
        val properties = MQTTProperties()
        properties.payloadFormatIndicator = will.payloadFormatIndicator
        properties.messageExpiryInterval = will.messageExpiryInterval
        properties.contentType = will.contentType
        properties.responseTopic = will.responseTopic
        properties.correlationData = will.correlationData
        properties.userProperty += will.userProperty
        publish(session.clientId, will.retain, will.topic, will.qos, false, properties, will.payload)
        // The will must be removed after sending
        session.will = null
    }

    fun publish(
        publisherClientId: String,
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTTProperties,
        payload: UByteArray?
    ) {
        if (!retainedAvailable && retain)
            throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)
        maximumQos?.let {
            if (qos > it)
                throw MQTTException(ReasonCode.QOS_NOT_SUPPORTED)
        }

        val sharedDone = mutableListOf<String>()
        sessions.forEach { session ->
            session.value.hasSubscriptionsMatching(topicName).forEach { subscription ->
                if (subscription.isShared()) {
                    if (subscription.shareName!! !in sharedDone && sharedSubscriptionsAvailable) { // Check we only publish once per shared subscription
                        publishShared(
                            publisherClientId,
                            subscription.shareName,
                            retain,
                            topicName,
                            qos,
                            dup,
                            properties,
                            payload
                        )
                        sharedDone += subscription.shareName
                    }
                } else {
                    publish(
                        publisherClientId,
                        retain,
                        topicName,
                        qos,
                        dup,
                        properties,
                        payload,
                        session.value,
                        subscription
                    )
                }
            }
        }
    }

    private fun publish(
        publisherClientId: String,
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTTProperties,
        payload: UByteArray?,
        session: Session,
        subscription: Subscription
    ) {
        if (subscription.options.noLocal && publisherClientId == session.clientId) {
            return
        }

        subscription.subscriptionIdentifier?.let {
            properties.subscriptionIdentifier.clear()
            properties.subscriptionIdentifier.add(it)
        }

        session.clientConnection?.publish(
            retain,
            topicName,
            Qos.valueOf(min(subscription.options.qos.value, qos.value))!!,
            dup,
            properties,
            payload
        )
    }

    fun setRetained(topicName: String, message: MQTTPublish, clientId: String) {
        if (retainedAvailable) {
            if (message.payload?.isNotEmpty() == true)
                retainedList[topicName] = Pair(message, clientId)
            else
                retainedList.remove(topicName)
        } else {
            throw MQTTException(ReasonCode.RETAIN_NOT_SUPPORTED)
        }
    }

    private fun removeExpiredRetainedMessages() {
        retainedList.filter {
            val message = it.value.first
            message.messageExpiryIntervalExpired()
        }.forEach {
            retainedList.remove(it.key)
        }
    }

    fun getRetained(topicFilter: String): List<Pair<MQTTPublish, String>> {
        removeExpiredRetainedMessages()
        return retainedList.filter { it.key.matchesWildcard(topicFilter) }.map { it.value }
    }

    fun getSession(clientId: String?): Session? {
        deleteExpiredSessions()
        return sessions[clientId]
    }

    private fun deleteExpiredSessions() {
        sessions.filter {
            val timestamp = it.value.getExpiryTime()
            timestamp != null && timestamp < currentTimeMillis()
        }.forEach { session ->
            // Expired sessions
            session.value.will?.let {
                sendWill(session.value)
            }
            sessions.remove(session.key)
        }
    }

    fun cleanUpOperations() {
        sessions.forEach { session ->
            if (session.value.isConnected()) { // Check the keep alive timer is being respected
                session.value.clientConnection!!.checkKeepAliveExpired()
            } else {
                session.value.will?.let {
                    // Check if the will delay interval has expired, if yes send the will
                    if (session.value.sessionDisconnectedTimestamp!! + (it.willDelayInterval.toLong() * 1000L) > currentTimeMillis())
                        sendWill(session.value)
                }
            }
        }
    }

    fun stop() {
        server.stop()
    }
}
