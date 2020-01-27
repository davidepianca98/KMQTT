package mqtt.packets

import mqtt.MQTTException
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

class MQTTPingresp : MQTTPacket {

    override fun toByteArray(): UByteArray {
        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.PINGRESP.value shl 4) and 0xF0
        result.write(fixedHeader.toUByte())
        result.encodeVariableByteInteger(0u)
        return result.toByteArray()
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
