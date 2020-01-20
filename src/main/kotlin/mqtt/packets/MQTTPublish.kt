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
    val qos: Qos = Qos.AT_MOST_ONCE,
    val dup: Boolean = false,
    val topicName: String,
    val packetId: UInt?,
    val properties: MQTTProperties,
    val payload: ByteArray?
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.writeUTF8String(topicName)
        if (qos == Qos.AT_LEAST_ONCE || qos == Qos.EXACTLY_ONCE) {
            outStream.write2BytesInt(packetId!!)
        }
        outStream.writeBytes(properties.serializeProperties(validProperties))
        payload?.let { outStream.writeBytes(it) }

        val result = ByteArrayOutputStream()
        val fixedHeader = ((MQTTControlPacketType.PUBLISH.ordinal shl 4) and 0xF0) or
                (((if (dup) 1 else 0) shl 3) and 0x8) or
                ((qos.ordinal shl 1) and 0x6) or
                ((if (retain) 1 else 0) and 0x1)
        result.write(fixedHeader)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        return result.toByteArray()
    }

    fun validatePayloadFormat(): Boolean {
        properties.payloadFormatIndicator?.let {
            if (it == 1u) {
                return try {
                    Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(payload))
                    true
                } catch (e: CharacterCodingException) {
                    false
                }
            }
        }
        return true
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

            if (qos == Qos.AT_MOST_ONCE && dup)
                throw MQTTException(ReasonCode.MALFORMED_PACKET)

            val inStream = ByteArrayInputStream(data)
            val topicName = inStream.readUTF8String()
            if (topicName.containsWildcard())
                throw MQTTException(ReasonCode.TOPIC_NAME_INVALID)

            val packetIdentifier = if (qos > Qos.AT_MOST_ONCE) inStream.read2BytesInt() else null

            val properties = inStream.deserializeProperties(validProperties)

            val payload = inStream.readAllBytes()

            return MQTTPublish(retain, qos, dup, topicName, packetIdentifier, properties, payload)
        }

        private fun getQos(flags: Int): Qos = Qos.valueOf(flags.flagsBit(1) or (flags.flagsBit(2) shl 1))

        override fun checkFlags(flags: Int) {

        }
    }
}
