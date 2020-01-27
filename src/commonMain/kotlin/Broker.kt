import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.*
import mqtt.packets.MQTTProperties
import mqtt.packets.MQTTPublish
import mqtt.packets.Qos
import mqtt.packets.ReasonCode
import socket.ServerSocket
import kotlin.math.min

class Broker(
    port: Int = 1883,
    host: String = "127.0.0.1",
    backlog: Int = 128,
    val authentication: Authentication? = null,
    val authorization: Authorization? = null,
    val maximumSessionExpiryInterval: UInt = 0xFFFFFFFFu,
    val receiveMaximum: Int? = null,
    val maximumQos: Qos? = null,
    val retainedAvailable: Boolean = true,
    val maximumPacketSize: UInt = 32768u,
    val maximumTopicAlias: Int? = null,
    val wildcardSubscriptionAvailable: Boolean = true,
    val subscriptionIdentifiersAvailable: Boolean = true,
    val sharedSubscriptionsAvailable: Boolean = true,
    val serverKeepAlive: Int? = null,
    val responseInformation: String? = null
) {

    // TODO support TLS with custom constructor with default port 8883
    // TODO support WebSocket, section 6

    private val server = ServerSocket(host, port, backlog, this)
    val sessions = mutableMapOf<String, Session>()
    private val retainedList = mutableMapOf<String, Pair<MQTTPublish, String>>()

    init {
        receiveMaximum?.let {
            require(it in 0..65535)
        }
    }

    fun listen() {
        GlobalScope.launch { cleanUpOperations() }
        server.run()
    }

    private fun publishShared(
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
            publish(retain, topicName, qos, dup, properties, payload, session, subscription)
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
        publish(will.retain, will.topic, will.qos, false, properties, will.payload)
        // The will must be removed after sending
        session.will = null
    }

    fun publish(
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTTProperties,
        payload: UByteArray?
    ) { // TODO something here not working in mingw
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
                        publishShared(subscription.shareName, retain, topicName, qos, dup, properties, payload)
                        sharedDone += subscription.shareName
                    }
                } else {
                    publish(retain, topicName, qos, dup, properties, payload, session.value, subscription)
                }
            }
        }
    }

    private fun publish(
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTTProperties,
        payload: UByteArray?,
        session: Session,
        subscription: Subscription
    ) {
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
        val expired = retainedList.filter {
            val message = it.value.first
            message.messageExpiryIntervalExpired()
        }
        expired.forEach {
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
            session.value.will?.let {
                sendWill(session.value)
            }
            sessions.remove(session.key)
        }
    }

    private fun cleanUpOperations() {
        sessions.forEach { session ->
            if (session.value.isConnected()) {
                session.value.clientConnection!!.checkKeepAliveExpired()
            } else {
                session.value.will?.let {
                    if (session.value.sessionDisconnectedTimestamp!! + (it.willDelayInterval.toLong() * 1000L) > currentTimeMillis())
                        sendWill(session.value)
                }
            }
        }
    }

    fun stop() {
        server.close()
    }
}
