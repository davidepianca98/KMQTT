package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTUnsuback
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Unsuback(
    packetIdentifier: UInt
) : MQTTUnsuback(packetIdentifier), MQTT4Packet {

    public companion object : MQTTDeserializer {

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
