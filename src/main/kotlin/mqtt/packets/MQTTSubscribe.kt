package mqtt.packets

import mqtt.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MQTTSubscribe(
    val packetIdentifier: UInt,
    val properties: MQTTProperties = MQTTProperties(),
    val subscriptions: List<Subscription>
) : MQTTPacket {

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.SUBSCRIPTION_IDENTIFIER,
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTSubscribe {
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
            return MQTTSubscribe(packetIdentifier, properties, subscriptions)
        }

        class SubscriptionOptions(
            val qos: Qos,
            val noLocal: Boolean,
            val retainedAsPublished: Boolean,
            val retainHandling: UInt
        ) {
            fun toByte(): UInt {
                if (retainHandling !in 0u..2u)
                    throw MQTTException(ReasonCode.MALFORMED_PACKET)
                val optionsByte = 0 or
                        ((retainHandling.toInt() shl 4) and 0x30) or
                        (((if (retainedAsPublished) 1 else 0) shl 3) and 0x8) or
                        (((if (noLocal) 1 else 0) shl 2) and 0x4) or
                        (qos.ordinal and 0x3)
                return optionsByte.toUInt()
            }
        }

        private fun ByteArrayInputStream.deserializeSubscriptionOptions(): SubscriptionOptions {
            val subscriptionOptions = readByte()
            val qos = Qos.valueOf((subscriptionOptions and 0x3u).toInt())
            val noLocal =
                ((subscriptionOptions and 0x4u) shr 2) == 1u
            val retainedAsPublished =
                ((subscriptionOptions and 0x8u) shr 3) == 1u
            val retainHandling = (subscriptionOptions and 0x30u) shr 4
            if (retainHandling == 3u) throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val reserved = (subscriptionOptions and 0xC0u) shr 6
            if (reserved != 0u) throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return SubscriptionOptions(qos, noLocal, retainedAsPublished, retainHandling)
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

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        if (properties.subscriptionIdentifier.size > 1)
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        val subscriptionIdentifier = properties.subscriptionIdentifier.getOrNull(0)
        if (subscriptionIdentifier != null && (subscriptionIdentifier == 0u || subscriptionIdentifier > 268435455u))
            throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        outStream.writeBytes(properties.serializeProperties(validProperties))

        subscriptions.forEach { subscription ->
            if (subscription.topicFilter.isSharedTopicFilter() && subscription.options.noLocal)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            outStream.writeUTF8String(subscription.topicFilter)
            outStream.writeByte(subscription.options.toByte())
        }

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.SUBSCRIBE.ordinal shl 4) and 0xF2
        result.write(fixedHeader)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        return result.toByteArray()
    }
}
