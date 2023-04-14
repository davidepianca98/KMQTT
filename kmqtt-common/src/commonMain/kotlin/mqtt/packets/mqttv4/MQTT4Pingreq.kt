package mqtt.packets.mqttv4

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTPingreq
import socket.streams.ByteArrayOutputStream

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
