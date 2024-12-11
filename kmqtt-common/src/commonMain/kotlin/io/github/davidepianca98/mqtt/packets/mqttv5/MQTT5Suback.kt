package io.github.davidepianca98.mqtt.packets.mqttv5

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTSuback
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT5Suback(
    packetIdentifier: UInt,
    public val reasonCodes: List<ReasonCode>,
    public val properties: MQTT5Properties = MQTT5Properties()
) : MQTTSuback(packetIdentifier), MQTT5Packet {

    public companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        private val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
            ReasonCode.GRANTED_QOS1,
            ReasonCode.GRANTED_QOS2,
            ReasonCode.UNSPECIFIED_ERROR,
            ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
            ReasonCode.NOT_AUTHORIZED,
            ReasonCode.TOPIC_FILTER_INVALID,
            ReasonCode.PACKET_IDENTIFIER_IN_USE,
            ReasonCode.QUOTA_EXCEEDED,
            ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED,
            ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
            ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        )

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Suback {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            val reasonCodes = mutableListOf<ReasonCode>()
            while (inStream.available() > 0) {
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(
                        ReasonCode.PROTOCOL_ERROR
                    )
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                reasonCodes += reasonCode
            }

            return MQTT5Suback(packetIdentifier, reasonCodes, properties)
        }
    }

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
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)
        outStream.write(properties.serializeProperties(validProperties))

        reasonCodes.forEach {
            if (it !in validReasonCodes)
                throw IllegalArgumentException("Invalid reason code")
            outStream.writeByte(it.value.toUInt())
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.SUBACK, 0)
    }
}
