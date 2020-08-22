package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTAuth
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT5Auth(
    val authenticateReasonCode: ReasonCode,
    val properties: MQTT5Properties = MQTT5Properties()
) : MQTTAuth() {
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
        if (authenticateReasonCode !in validReasonCodes)
            throw IllegalArgumentException("Invalid reason code")
        val outStream = ByteArrayOutputStream()

        outStream.writeByte(authenticateReasonCode.value.toUInt())
        outStream.write(properties.serializeProperties(validProperties))

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.AUTH, 0)
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

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Auth {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val reasonCode =
                ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(
                    ReasonCode.MALFORMED_PACKET
                )
            if (reasonCode !in validReasonCodes)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            val properties = inStream.deserializeProperties(validProperties)
            if (properties.authenticationMethod == null)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            return MQTT5Auth(reasonCode, properties)
        }
    }
}
