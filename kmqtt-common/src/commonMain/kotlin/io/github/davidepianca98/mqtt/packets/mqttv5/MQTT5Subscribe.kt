package io.github.davidepianca98.mqtt.packets.mqttv5

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.isSharedTopicFilter
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTSubscribe
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT5Subscribe(
    packetIdentifier: UInt,
    subscriptions: List<Subscription>,
    public val properties: MQTT5Properties = MQTT5Properties()
) : MQTTSubscribe(packetIdentifier, subscriptions), MQTT5Packet {

    public companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SUBSCRIPTION_IDENTIFIER,
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Subscribe {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            if (properties.subscriptionIdentifier.size > 1)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val subscriptionIdentifier = properties.subscriptionIdentifier.getOrNull(0)
            if (subscriptionIdentifier != null && (subscriptionIdentifier == 0u || subscriptionIdentifier > 268435455u))
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)

            val subscriptions = mutableListOf<Subscription>()
            while (inStream.available() > 0) {
                val topicFilter = inStream.readUTF8String()
                val subscriptionOptions = inStream.deserializeSubscriptionOptions()
                val subscription = Subscription(topicFilter, subscriptionOptions, subscriptionIdentifier)
                if (subscription.topicFilter.isSharedTopicFilter() && subscription.options.noLocal)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                subscriptions += subscription
            }
            return MQTT5Subscribe(packetIdentifier, subscriptions, properties)
        }

        private fun ByteArrayInputStream.deserializeSubscriptionOptions(): SubscriptionOptions {
            val subscriptionOptions = readByte()
            val qos =
                Qos.valueOf((subscriptionOptions and 0x3u).toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
            val noLocal =
                ((subscriptionOptions and 0x4u) shr 2) == 1u
            val retainedAsPublished =
                ((subscriptionOptions and 0x8u) shr 3) == 1u
            val retainHandling = (subscriptionOptions and 0x30u) shr 4
            if (retainHandling == 3u) throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val reserved = (subscriptionOptions and 0xC0u) shr 6
            if (reserved != 0u) throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return SubscriptionOptions(
                qos,
                noLocal,
                retainedAsPublished,
                retainHandling
            )
        }

        override fun checkFlags(flags: Int) {
            if (flags.flagsBit(0) != 0 ||
                flags.flagsBit(1) != 1 ||
                flags.flagsBit(2) != 0 ||
                flags.flagsBit(3) != 0
            )
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }
    }

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        if (properties.subscriptionIdentifier.size > 1)
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        val subscriptionIdentifier = properties.subscriptionIdentifier.getOrNull(0)
        if (subscriptionIdentifier != null && (subscriptionIdentifier == 0u || subscriptionIdentifier > 268435455u))
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        outStream.write(properties.serializeProperties(validProperties))

        subscriptions.forEach { subscription ->
            if (subscription.topicFilter.isSharedTopicFilter() && subscription.options.noLocal)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            outStream.writeUTF8String(subscription.topicFilter)
            outStream.writeByte(subscription.options.toByte())
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.SUBSCRIBE, 2)
    }
}
