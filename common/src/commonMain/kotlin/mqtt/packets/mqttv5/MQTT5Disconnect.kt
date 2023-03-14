package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTDisconnect
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT5Disconnect(
    val reasonCode: ReasonCode,
    val properties: MQTT5Properties = MQTT5Properties()
) : mqtt.packets.mqtt.MQTTDisconnect(), MQTT5Packet {
    override fun resizeIfTooBig(maximumPacketSize: UInt): Boolean {
        if (size() > maximumPacketSize) {
            properties.reasonString = null
        }
        if (size() > maximumPacketSize) {
            properties.userProperty.clear()
        }
        return size() <= maximumPacketSize
    }

    override fun toByteArray(): UByteArray {
        if (reasonCode !in validReasonCodes)
            throw IllegalArgumentException("Invalid reason code")
        val outStream = ByteArrayOutputStream()

        outStream.writeByte(reasonCode.value.toUInt())
        outStream.write(properties.serializeProperties(validProperties))

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.DISCONNECT, 0)
    }

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.SERVER_REFERENCE,
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
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

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Disconnect {
            checkFlags(flags)
            return if (data.isEmpty()) {
                MQTT5Disconnect(ReasonCode.SUCCESS)
            } else {
                val inStream = ByteArrayInputStream(data)
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.MALFORMED_PACKET)
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                if (inStream.available() == 0) {
                    MQTT5Disconnect(reasonCode)
                } else {
                    val properties = inStream.deserializeProperties(validProperties)
                    MQTT5Disconnect(reasonCode, properties)
                }
            }
        }
    }
}
