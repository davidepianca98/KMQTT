package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTSubscribe
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.mqtt.packets.mqttv5.SubscriptionOptions
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Subscribe(
    packetIdentifier: UInt,
    subscriptions: List<Subscription>
) : MQTTSubscribe(packetIdentifier, subscriptions), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        subscriptions.forEach { subscription ->
            outStream.writeUTF8String(subscription.topicFilter)
            outStream.writeByte(subscription.options.qos.value.toUInt())
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.SUBSCRIBE, 2)
    }

    public companion object : MQTTDeserializer {

        private fun ByteArrayInputStream.deserializeSubscriptionOptions(): SubscriptionOptions {
            val subscriptionOptions = readByte()
            val qos =
                Qos.valueOf((subscriptionOptions and 0x3u).toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)

            val reserved = (subscriptionOptions and 0xFCu) shr 2
            if (reserved != 0u) throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return SubscriptionOptions(qos)
        }

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Subscribe {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = inStream.read2BytesInt()

            val subscriptions = mutableListOf<Subscription>()
            while (inStream.available() > 0) {
                val topicFilter = inStream.readUTF8String()
                val subscriptionOptions = inStream.deserializeSubscriptionOptions()
                val subscription = Subscription(topicFilter, subscriptionOptions)
                subscriptions += subscription
            }
            return MQTT4Subscribe(packetIdentifier, subscriptions)
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
}
