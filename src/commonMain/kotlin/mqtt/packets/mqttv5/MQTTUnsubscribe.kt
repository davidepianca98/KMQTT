package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTTUnsubscribe(
    val packetIdentifier: UInt,
    val topicFilters: List<String>,
    val properties: MQTTProperties = MQTTProperties()
) : MQTT5Packet(properties) {

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTUnsubscribe {
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

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.UNSUBSCRIBE, 2)
    }
}
