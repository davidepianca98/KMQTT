package mqtt.packets

import mqtt.MQTTException
import socket.streams.ByteArrayOutputStream

class MQTTPingresp : MQTTPacket {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGRESP, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPingresp {
            checkFlags(flags)
            if (data.isNotEmpty())
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return MQTTPingresp()
        }
    }
}
