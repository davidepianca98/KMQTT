package mqtt.packets.mqttv4

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTUnsuback
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT4Unsuback(
    packetIdentifier: UInt
) : MQTTUnsuback(packetIdentifier) {

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Unsuback {
            MQTT4Unsuback.checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val packetIdentifier = inStream.read2BytesInt()

            return MQTT4Unsuback(packetIdentifier)
        }

    }

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.UNSUBACK, 0)
    }
}
