package mqtt.packets.mqttv4

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTPubrel
import mqtt.packets.mqttv5.ReasonCode
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT4Pubrel(
    packetId: UInt
) : MQTTPubrel(packetId), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetId)

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBREL, 2)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Pubrel {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetId = inStream.read2BytesInt()
            return MQTT4Pubrel(packetId)
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
}
