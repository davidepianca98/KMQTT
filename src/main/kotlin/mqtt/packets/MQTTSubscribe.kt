package mqtt.packets

import mqtt.MQTTException
import mqtt.Subscription
import java.io.ByteArrayInputStream

class MQTTSubscribe(
    val packetIdentifier: UInt,
    val properties: MQTTProperties = MQTTProperties(),
    val subscriptions: List<Subscription>
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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
                subscriptions += Subscription(topicFilter, subscriptionOptions, subscriptionIdentifier)
            }
            return MQTTSubscribe(packetIdentifier, properties, subscriptions)
        }

        class SubscriptionOptions(
            val qos: Qos,
            val noLocal: Boolean,
            val retainedAsPublished: Boolean,
            val retainHandling: UInt
        )

        private fun ByteArrayInputStream.deserializeSubscriptionOptions(): SubscriptionOptions {
            val subscriptionOptions = readByte()
            val qos = Qos.valueOf((subscriptionOptions and 0x3u).toInt())
            val noLocal =
                ((subscriptionOptions and 0x4u) shr 2) == 1u // TODO protocol error if shared subscription and true
            val retainedAsPublished =
                ((subscriptionOptions and 0x8u) shr 3) == 1u // TODO if true maintain RETAIN flag as received by broker, else 0
            val retainHandling = (subscriptionOptions and 0x30u) shr 4
            // TODO This option specifies whether retained messages are sent when the subscription is established. This does not affect the sending of retained messages at any point after the subscribe. If there are no retained messages matching the Topic Filter, all of these values act the same. The values are:
            //    0 = Send retained messages at the time of the subscribe
            //    1 = Send retained messages at subscribe only if the subscription does not currently exist
            //    2 = Do not send retained messages at the time of the subscribe
            if (retainHandling == 3u) throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val reserved = (subscriptionOptions and 0xC0u) shr 6
            if (reserved != 0u) throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return SubscriptionOptions(qos, noLocal, retainedAsPublished, retainHandling)
        }

        override fun checkFlags(flags: Int) {
            require(flags.flagsBit(0) == 0)
            require(flags.flagsBit(1) == 1)
            require(flags.flagsBit(2) == 0)
            require(flags.flagsBit(3) == 0)
        }
    }
}
