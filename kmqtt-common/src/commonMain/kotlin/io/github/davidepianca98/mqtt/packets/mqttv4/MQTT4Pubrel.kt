package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPubrel
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Pubrel(
    packetId: UInt
) : MQTTPubrel(packetId), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetId)

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.PUBREL, 2)
    }

    public companion object : MQTTDeserializer {

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
