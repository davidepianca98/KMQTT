package mqtt.packets

import mqtt.MQTTException
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

class MQTTDisconnect(
    val reasonCode: ReasonCode,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket, MQTTSerializer {

    override suspend fun toByteArray(): UByteArray {
        if (reasonCode !in validReasonCodes)
            throw IllegalArgumentException("Invalid reason code")
        val outStream = ByteArrayOutputStream()

        outStream.writeByte(reasonCode.value.toUInt())
        outStream.write(properties.serializeProperties(validProperties))

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.DISCONNECT.value shl 4) and 0xF0
        result.write(fixedHeader.toUByte())
        result.encodeVariableByteInteger(outStream.size().toUInt())
        result.write(outStream.toByteArray())
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.SERVER_REFERENCE,
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        val validReasonCodes = listOf(
            ReasonCode.NORMAL_DISCONNECTION,
            ReasonCode.DISCONNECT_WITH_WILL_MESSAGE,
            ReasonCode.UNSPECIFIED_ERROR,
            ReasonCode.MALFORMED_PACKET,
            ReasonCode.PROTOCOL_ERROR,
            ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
            ReasonCode.NOT_AUTHORIZED,
            ReasonCode.SERVER_BUSY,
            ReasonCode.SERVER_SHUTTING_DOWN,
            ReasonCode.KEEP_ALIVE_TIMEOUT,
            ReasonCode.SESSION_TAKEN_OVER,
            ReasonCode.TOPIC_FILTER_INVALID,
            ReasonCode.TOPIC_NAME_INVALID,
            ReasonCode.RECEIVE_MAXIMUM_EXCEEDED,
            ReasonCode.TOPIC_ALIAS_INVALID,
            ReasonCode.PACKET_TOO_LARGE,
            ReasonCode.MESSAGE_RATE_TOO_HIGH,
            ReasonCode.QUOTA_EXCEEDED,
            ReasonCode.ADMINISTRATIVE_ACTION,
            ReasonCode.PAYLOAD_FORMAT_INVALID,
            ReasonCode.RETAIN_NOT_SUPPORTED,
            ReasonCode.QOS_NOT_SUPPORTED,
            ReasonCode.USE_ANOTHER_SERVER,
            ReasonCode.SERVER_MOVED,
            ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED,
            ReasonCode.CONNECTION_RATE_EXCEEDED,
            ReasonCode.MAXIMUM_CONNECT_TIME,
            ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
            ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        )

        override suspend fun fromByteArray(flags: Int, data: UByteArray): MQTTDisconnect {
            checkFlags(flags)
            return if (data.isEmpty()) {
                MQTTDisconnect(ReasonCode.SUCCESS)
            } else {
                val inStream = ByteArrayInputStream(data)
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                val properties = inStream.deserializeProperties(validProperties)
                MQTTDisconnect(reasonCode, properties)
            }
        }
    }
}
