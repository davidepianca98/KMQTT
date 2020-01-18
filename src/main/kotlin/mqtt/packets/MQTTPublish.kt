package mqtt.packets

import mqtt.MalformedPacketException
import java.io.ByteArrayInputStream

data class MQTTPublish(
    val retain: Boolean,
    val qos: Int,
    val dup: Boolean,
    val packetId: Int?,
    val properties: MQTTProperties,
    val payload: ByteArray?
) : MQTTPacket {

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.PAYLOAD_FORMAT_INDICATOR,
            Property.MESSAGE_EXPIRY_INTERVAL,
            Property.CONTENT_TYPE,
            Property.RESPONSE_TOPIC,
            Property.CORRELATION_DATA,
            Property.SUBSCRIPTION_IDENTIFIER,
            Property.TOPIC_ALIAS,
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPublish {
            checkFlags(flags)
            val retain = flags.flagsBit(0) == 1
            val qos = getQos(flags)
            val dup = flags.flagsBit(3) == 1

            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = if (qos > 0) inStream.getPacketIdentifier() else null

            val properties = inStream.deserializeProperties(validProperties)

            // TODO payload

            return MQTTPublish(retain, qos, dup, packetIdentifier, properties)
        }

        private fun getQos(flags: Int): Int = flags.flagsBit(1) or (flags.flagsBit(2) shl 1)

        override fun checkFlags(flags: Int) {
            if (getQos(flags) !in 0..2)
                throw MalformedPacketException(ReasonCode.MALFORMED_PACKET)
        }
    }
}
