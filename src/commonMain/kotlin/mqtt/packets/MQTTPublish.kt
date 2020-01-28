package mqtt.packets

import currentTimeMillis
import mqtt.MQTTException
import mqtt.containsWildcard
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import validatePayloadFormat


class MQTTPublish(
    val retain: Boolean,
    val qos: Qos = Qos.AT_MOST_ONCE,
    val dup: Boolean = false,
    val topicName: String,
    val packetId: UInt?,
    val properties: MQTTProperties,
    val payload: UByteArray?,
    val timestamp: Long = currentTimeMillis()
) : MQTTPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.writeUTF8String(topicName)
        if (qos == Qos.AT_LEAST_ONCE || qos == Qos.EXACTLY_ONCE) {
            outStream.write2BytesInt(packetId!!)
        }
        outStream.write(properties.serializeProperties(validProperties))
        payload?.let { outStream.write(it) }

        val flags = (((if (dup) 1 else 0) shl 3) and 0x8) or
                ((qos.value shl 1) and 0x6) or
                ((if (retain) 1 else 0) and 0x1)
        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBLISH, flags)
    }

    fun validatePayloadFormat(): Boolean {
        properties.payloadFormatIndicator?.let {
            return payload?.validatePayloadFormat(it) ?: true
        }
        return true
    }

    fun setDuplicate(): MQTTPublish {
        return MQTTPublish(retain, qos, true, topicName, packetId, properties, payload, timestamp)
    }

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

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPublish {
            checkFlags(flags)
            val retain = flags.flagsBit(0) == 1
            val qos = getQos(flags) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
            val dup = flags.flagsBit(3) == 1

            if (qos == Qos.AT_MOST_ONCE && dup)
                throw MQTTException(ReasonCode.MALFORMED_PACKET)

            val inStream = ByteArrayInputStream(data)
            val topicName = inStream.readUTF8String()
            if (topicName.containsWildcard())
                throw MQTTException(ReasonCode.TOPIC_NAME_INVALID)

            val packetIdentifier = if (qos > Qos.AT_MOST_ONCE) inStream.read2BytesInt() else null

            val properties = inStream.deserializeProperties(validProperties)

            val payload = inStream.readRemaining()

            return MQTTPublish(retain, qos, dup, topicName, packetIdentifier, properties, payload)
        }

        private fun getQos(flags: Int): Qos? = Qos.valueOf(flags.flagsBit(1) or (flags.flagsBit(2) shl 1))

        override fun checkFlags(flags: Int) {

        }
    }
}
