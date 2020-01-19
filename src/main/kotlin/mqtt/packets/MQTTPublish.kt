package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.containsWildcard
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset


class MQTTPublish(
    val retain: Boolean,
    val qos: Int = 0,
    val dup: Boolean = false,
    val topicName: String,
    val packetId: UInt?,
    val properties: MQTTProperties,
    val payload: ByteArray?
) : MQTTPacket {

    init {
        require(qos in 0..2)
    }

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.writeUTF8String(topicName)
        if (qos == 1 || qos == 2) {
            outStream.write2BytesInt(packetId!!)
        }
        outStream.writeBytes(properties.serializeProperties(validProperties))
        payload?.let { outStream.writeBytes(it) }

        val result = ByteArrayOutputStream()
        val fixedHeader = ((MQTTControlPacketType.PUBLISH.ordinal shl 4) and 0xF0) or
                (((if (dup) 1 else 0) shl 3) and 0x8) or
                ((qos shl 1) and 0x6) or
                ((if (retain) 1 else 0) and 0x1)
        result.write(fixedHeader)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        return result.toByteArray()
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

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPublish {
            checkFlags(flags)
            val retain = flags.flagsBit(0) == 1
            val qos = getQos(flags)
            val dup = flags.flagsBit(3) == 1

            if (qos == 0 && dup)
                throw MQTTException(ReasonCode.MALFORMED_PACKET)

            val inStream = ByteArrayInputStream(data)
            val topicName = inStream.readUTF8String()
            if (topicName.containsWildcard())
                throw MQTTException(ReasonCode.TOPIC_NAME_INVALID)

            val packetIdentifier = if (qos > 0) inStream.read2BytesInt() else null

            val properties = inStream.deserializeProperties(validProperties)

            val payload = inStream.readAllBytes()
            properties.payloadFormatIndicator?.let {
                if (it == 1u) {
                    try {
                        Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(payload))
                    } catch (e: CharacterCodingException) {
                        throw MQTTException(ReasonCode.PAYLOAD_FORMAT_INVALID)
                    }
                }
            }

            return MQTTPublish(retain, qos, dup, topicName, packetIdentifier, properties, payload)
        }

        private fun getQos(flags: Int): Int = flags.flagsBit(1) or (flags.flagsBit(2) shl 1)

        override fun checkFlags(flags: Int) {
            if (getQos(flags) !in 0..2)
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }
    }
}
