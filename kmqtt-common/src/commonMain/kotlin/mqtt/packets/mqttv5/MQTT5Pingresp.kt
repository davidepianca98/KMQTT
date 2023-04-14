package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTPingresp
import socket.streams.ByteArrayOutputStream

public class MQTT5Pingresp : MQTTPingresp(), MQTT5Packet {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGRESP, 0)
    }

    public companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Pingresp {
            checkFlags(flags)
            if (data.isNotEmpty())
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return MQTT5Pingresp()
        }
    }
}
