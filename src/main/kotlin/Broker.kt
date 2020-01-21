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

// TODO 5
class Broker(
    local: SocketAddress,
    backlog: Int = 128,
    val authentication: Authentication? = null,
    val maximumSessionExpiryInterval: UInt = 0xFFFFFFFFu,
    val receiveMaximum: Int? = null,
    val maximumQos: Qos? = null,
    val retainedAvailable: Boolean = true,
    val maximumPacketSize: UInt? = null,
    val maximumTopicAlias: Int? = null,
    val wildcardSubscriptionAvailable: Boolean = true,
    val subscriptionIdentifiersAvailable: Boolean = true,
    val sharedSubscriptionsAvailable: Boolean = true,
    val serverKeepAlive: Int? = null
) {

    constructor(port: Int = 1883, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

    // TODO support TLS with custom constructor with default port 8883

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
            thread { ClientConnection(client, this).run() }
        }
    }

    private fun publishShared(
        shareName: String,
        topicName: String,
        qos: Qos,
        properties: MQTTProperties,
        payload: ByteArray?
    ) {
        // Get the sessions which subscribe to this shared session and get the one which hasn't received a message for the longest time
        val session = sessions.minBy {
            it.value.hasSharedSubscriptionMatching(shareName, topicName)?.timestampShareSent ?: Long.MAX_VALUE
        }?.value
        session?.hasSharedSubscriptionMatching(shareName, topicName)?.let { subscription ->
            publish(topicName, qos, properties, payload, session, subscription)
            subscription.timestampShareSent = System.currentTimeMillis()
        }
    }

    fun publish(topicName: String, qos: Qos, properties: MQTTProperties, payload: ByteArray?) {
        val sharedDone = mutableListOf<String>()
        sessions.forEach { session ->
            session.value.hasSubscriptionsMatching(topicName).forEach { subscription ->
                if (subscription.isShared()) {
                    if (subscription.shareName!! !in sharedDone && sharedSubscriptionsAvailable) { // Check we only publish once per shared subscription
                        publishShared(subscription.shareName, topicName, qos, properties, payload)
                        sharedDone += subscription.shareName
                    }
                } else {
                    publish(topicName, qos, properties, payload, session.value, subscription)
                }
            }
        }
    }

    private fun publish(
        topicName: String,
        qos: Qos,
        properties: MQTTProperties,
        payload: ByteArray?,
        session: Session,
        subscription: Subscription
    ) {
        subscription.subscriptionIdentifier?.let {
            properties.subscriptionIdentifier.clear()
            properties.subscriptionIdentifier.add(it)
        }

        val packet = MQTTPublish(
            false,
            Qos.valueOf(min(subscription.options.qos.ordinal, qos.ordinal)),
            false,
            topicName, // TODO maybe use topic aliases
            session.generatePacketId(),
            properties,
            payload
        )
        session.clientConnection.publish(packet)
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
}
