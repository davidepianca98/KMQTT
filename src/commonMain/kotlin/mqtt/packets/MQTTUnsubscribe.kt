package mqtt.packets

import encodeVariableByteInteger
import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.streams.ByteArrayInputStream
import mqtt.streams.ByteArrayOutputStream

class MQTTUnsubscribe(
    val packetIdentifier: UInt,
    val topicFilters: List<String>,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTUnsubscribe {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            val topicFilters = mutableListOf<String>()
            while (inStream.available() > 0) {
                topicFilters += inStream.readUTF8String()
            }
            return MQTTUnsubscribe(packetIdentifier, topicFilters, properties)
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

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)
        outStream.write(properties.serializeProperties(validProperties))

        topicFilters.forEach {
            outStream.writeUTF8String(it)
        }

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.UNSUBSCRIBE.value shl 4) and 0xF2
        result.write(fixedHeader.toUInt())
        result.encodeVariableByteInteger(outStream.size().toUInt())
        result.write(outStream.toByteArray())
        return result.toByteArray()
    }
}
