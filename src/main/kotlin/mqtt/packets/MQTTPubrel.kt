package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MQTTPubrel(
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
        val fixedHeader = (MQTTControlPacketType.PUBREL.ordinal shl 4) and 0xF2
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
            ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPubrel {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return if (inStream.available() == 0) { // Reason code and properties omitted
                MQTTPubrel(packetId)
            } else {
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                val properties = inStream.deserializeProperties(validProperties)
                MQTTPubrel(packetId, reasonCode, properties)
            }
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
