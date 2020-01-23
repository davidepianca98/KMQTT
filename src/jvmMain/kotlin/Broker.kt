import mqtt.*
import mqtt.packets.MQTTProperties
import mqtt.packets.MQTTPublish
import mqtt.packets.Qos
import mqtt.packets.ReasonCode
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import kotlin.concurrent.thread
import kotlin.math.min

class Broker(
    local: SocketAddress,
    backlog: Int = 128,
    val authentication: Authentication? = null,
    val authorization: Authorization? = null,
    val maximumSessionExpiryInterval: UInt = 0xFFFFFFFFu,
    val receiveMaximum: Int? = null,
    val maximumQos: Qos? = null,
    val retainedAvailable: Boolean = true,
    val maximumPacketSize: UInt? = null,
    val maximumTopicAlias: Int? = null,
    val wildcardSubscriptionAvailable: Boolean = true,
    val subscriptionIdentifiersAvailable: Boolean = true,
    val sharedSubscriptionsAvailable: Boolean = true,
    val serverKeepAlive: Int? = null,
    val responseInformation: String? = null
) {

    constructor(port: Int = 1883, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

    // TODO support TLS with custom constructor with default port 8883
    // TODO support WebSocket, section 6

    private val server = ServerSocket()
    val sessions = mutableMapOf<String, Session>()
    private val retainedList = mutableMapOf<String, Pair<MQTTPublish, String>>()

    init {
        receiveMaximum?.let {
            require(it in 0..65535)
        }

        server.bind(local, backlog)
    }

    fun listen() {
        while (true) {
            val client = server.accept()
            client.soTimeout = 30000
            thread { ClientConnection(client, this).run() }
        }
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
            subscription.timestampShareSent = System.currentTimeMillis()
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

        session.clientConnection.publish(
            retain,
            topicName,
            Qos.valueOf(min(subscription.options.qos.value, qos.value)),
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
            val timestamp = it.value.destroySessionTimestamp
            timestamp != null && timestamp < System.currentTimeMillis()
        }.forEach {
            sessions.remove(it.key)
        }
    }
}
