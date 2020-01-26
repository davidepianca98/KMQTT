package mqtt.packets

import mqtt.MQTTException
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

class MQTTPingreq : MQTTPacket {

    override suspend fun toByteArray(): UByteArray {
        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.PINGREQ.value shl 4) and 0xF0
        result.write(fixedHeader.toUByte())
        result.encodeVariableByteInteger(0u)
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        override suspend fun fromByteArray(flags: Int, data: UByteArray): MQTTPingreq {
            checkFlags(flags)
            if (data.isNotEmpty())
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return MQTTPingreq()
        }
    }
}
