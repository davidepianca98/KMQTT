package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MQTTAuth(
    val authenticateReasonCode: ReasonCode,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        if (authenticateReasonCode !in validReasonCodes)
            throw IllegalArgumentException("Invalid reason code")
        val outStream = ByteArrayOutputStream()

        outStream.writeByte(authenticateReasonCode.value.toUInt())
        outStream.writeBytes(properties.serializeProperties(validProperties))

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.AUTH.value shl 4) and 0xF0
        result.write(fixedHeader)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        result.writeBytes(outStream.toByteArray())
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.AUTHENTICATION_METHOD,
            Property.AUTHENTICATION_DATA,
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        private val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
            ReasonCode.CONTINUE_AUTHENTICATION,
            ReasonCode.RE_AUTHENTICATE
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTAuth {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val reasonCode =
                ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
            if (reasonCode !in validReasonCodes)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val properties = inStream.deserializeProperties(validProperties)
            if (properties.authenticationMethod == null)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            return MQTTAuth(reasonCode, properties)
        }
    }
}
