package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MQTTPuback(
    val packetId: UInt,
    val reasonCode: ReasonCode = ReasonCode.SUCCESS,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        if (reasonCode !in validReasonCodes)
            throw IllegalArgumentException("Invalid reason code")
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetId)
        outStream.writeByte(reasonCode.ordinal.toUInt())
        outStream.writeBytes(properties.serializeProperties(validProperties))

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.PUBACK.ordinal shl 4) and 0xF0
        result.write(fixedHeader)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
            ReasonCode.NO_MATCHING_SUBSCRIBERS,
            ReasonCode.UNSPECIFIED_ERROR,
            ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
            ReasonCode.NOT_AUTHORIZED,
            ReasonCode.TOPIC_NAME_INVALID,
            ReasonCode.PACKET_IDENTIFIER_IN_USE,
            ReasonCode.QUOTA_EXCEEDED,
            ReasonCode.PAYLOAD_FORMAT_INVALID
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPuback {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return if (inStream.available() == 0) { // Reason code and properties omitted
                MQTTPuback(packetId)
            } else {
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                val properties = inStream.deserializeProperties(validProperties)
                MQTTPuback(packetId, reasonCode, properties)
            }
        }
    }
}
