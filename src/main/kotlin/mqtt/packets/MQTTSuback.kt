package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayOutputStream

class MQTTSuback(
    val packetIdentifier: UInt,
    val reasonCodes: List<ReasonCode>,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)
        outStream.writeBytes(properties.serializeProperties(validProperties))

        reasonCodes.forEach {
            if (it !in validReasonCodes)
                throw IllegalArgumentException("Invalid reason code")
            outStream.writeByte(it.ordinal.toUInt())
        }

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.SUBACK.ordinal shl 4) and 0xF0
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
            ReasonCode.GRANTED_QOS0,
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

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
