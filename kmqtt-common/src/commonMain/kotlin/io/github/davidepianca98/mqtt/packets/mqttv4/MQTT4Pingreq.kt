package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPingreq
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Pingreq : MQTTPingreq(), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGREQ, 0)
    }
    public
    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Pingreq {
            checkFlags(flags)
            return MQTT4Pingreq()
        }
    }
}
