package io.github.davidepianca98.mqtt.packets.mqttv5

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPubcomp
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT5Pubcomp(
    packetId: UInt,
    public val reasonCode: ReasonCode = ReasonCode.SUCCESS,
    public val properties: MQTT5Properties = MQTT5Properties()
) : MQTTPubcomp(packetId), MQTT5Packet {
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

        outStream.write2BytesInt(packetId)
        outStream.writeByte(reasonCode.value.toUInt())
        outStream.write(properties.serializeProperties(validProperties))

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBCOMP, 0)
    }

    public companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        private val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
            ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
        )

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Pubcomp {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return if (inStream.available() == 0) { // Reason code and properties omitted
                MQTT5Pubcomp(packetId)
            } else {
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(
                        ReasonCode.MALFORMED_PACKET
                    )
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                if (inStream.available() == 0) {
                    MQTT5Pubcomp(packetId, reasonCode)
                } else {
                    val properties = inStream.deserializeProperties(validProperties)
                    MQTT5Pubcomp(packetId, reasonCode, properties)
                }
            }
        }
    }
}
