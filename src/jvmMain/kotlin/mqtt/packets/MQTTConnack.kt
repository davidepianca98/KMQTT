package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

data class ConnectAcknowledgeFlags(val sessionPresentFlag: Boolean)

class MQTTConnack(
    val connectAcknowledgeFlags: ConnectAcknowledgeFlags,
    val connectReasonCode: ReasonCode,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.ASSIGNED_CLIENT_IDENTIFIER,
            Property.SERVER_KEEP_ALIVE,
            Property.AUTHENTICATION_METHOD,
            Property.AUTHENTICATION_DATA,
            Property.RESPONSE_INFORMATION,
            Property.SERVER_REFERENCE,
            Property.REASON_STRING,
            Property.RECEIVE_MAXIMUM,
            Property.TOPIC_ALIAS_MAXIMUM,
            Property.MAXIMUM_QOS,
            Property.RETAIN_AVAILABLE,
            Property.USER_PROPERTY,
            Property.MAXIMUM_PACKET_SIZE,
            Property.WILDCARD_SUBSCRIPTION_AVAILABLE,
            Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE,
            Property.SHARED_SUBSCRIPTION_AVAILABLE
        )

        private val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
            ReasonCode.UNSPECIFIED_ERROR,
            ReasonCode.MALFORMED_PACKET,
            ReasonCode.PROTOCOL_ERROR,
            ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
            ReasonCode.UNSUPPORTED_PROTOCOL_VERSION,
            ReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
            ReasonCode.BAD_USER_NAME_OR_PASSWORD,
            ReasonCode.NOT_AUTHORIZED,
            ReasonCode.SERVER_UNAVAILABLE,
            ReasonCode.SERVER_BUSY,
            ReasonCode.BANNED,
            ReasonCode.BAD_AUTHENTICATION_METHOD,
            ReasonCode.TOPIC_NAME_INVALID,
            ReasonCode.PACKET_TOO_LARGE,
            ReasonCode.QUOTA_EXCEEDED,
            ReasonCode.PAYLOAD_FORMAT_INVALID,
            ReasonCode.RETAIN_NOT_SUPPORTED,
            ReasonCode.QOS_NOT_SUPPORTED,
            ReasonCode.USE_ANOTHER_SERVER,
            ReasonCode.SERVER_MOVED,
            ReasonCode.CONNECTION_RATE_EXCEEDED
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTConnack {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val connectAcknowledgeFlags = when (inStream.readByte()) {
                0u -> ConnectAcknowledgeFlags(false)
                1u -> ConnectAcknowledgeFlags(true)
                else -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            }
            val connectReasonCode =
                ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val properties = inStream.deserializeProperties(validProperties)

            return MQTTConnack(connectAcknowledgeFlags, connectReasonCode, properties)
        }
    }

    override fun toByteArray(): ByteArray {
        if (connectReasonCode !in validReasonCodes)
            throw IllegalArgumentException("Invalid reason code")

        val outStream = ByteArrayOutputStream()

        outStream.write(if (connectAcknowledgeFlags.sessionPresentFlag && connectReasonCode == ReasonCode.SUCCESS) 1 else 0)
        outStream.write(connectReasonCode.value)
        outStream.writeBytes(properties.serializeProperties(validProperties))

        val result = ByteArrayOutputStream()
        result.write((MQTTControlPacketType.CONNACK.value shl 4) and 0xF0)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        result.writeBytes(outStream.toByteArray())
        return result.toByteArray()
    }
}
