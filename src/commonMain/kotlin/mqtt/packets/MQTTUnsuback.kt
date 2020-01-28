package mqtt.packets

import mqtt.MQTTException
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTTUnsuback(
    val packetIdentifier: UInt,
    val reasonCodes: List<ReasonCode>,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        val validReasonCodes = listOf(
            ReasonCode.SUCCESS,
            ReasonCode.NO_SUBSCRIPTION_EXISTED,
            ReasonCode.UNSPECIFIED_ERROR,
            ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
            ReasonCode.NOT_AUTHORIZED,
            ReasonCode.TOPIC_FILTER_INVALID,
            ReasonCode.PACKET_IDENTIFIER_IN_USE
        )

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTUnsuback {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            val reasonCodes = mutableListOf<ReasonCode>()
            while (inStream.available() > 0) {
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                reasonCodes += reasonCode
            }

            return MQTTUnsuback(packetIdentifier, reasonCodes, properties)
        }
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

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.UNSUBACK, 0)
    }
}
