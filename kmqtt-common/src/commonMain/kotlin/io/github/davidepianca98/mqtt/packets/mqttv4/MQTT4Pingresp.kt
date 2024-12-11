package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPingresp
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Pingresp : MQTTPingresp(), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGRESP, 0)
    }

    public companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Pingresp {
            checkFlags(flags)
            return MQTT4Pingresp()
        }
    }
}
