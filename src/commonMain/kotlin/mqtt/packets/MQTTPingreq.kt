package mqtt.packets

import mqtt.MQTTException
import socket.streams.ByteArrayOutputStream

class MQTTPingreq : MQTTPacket {

    override fun toByteArray(): UByteArray {
        return ByteArrayOutputStream().wrapWithFixedHeader(MQTTControlPacketType.PINGREQ, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTPingreq {
            checkFlags(flags)
            if (data.isNotEmpty())
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return MQTTPingreq()
        }
    }
}
