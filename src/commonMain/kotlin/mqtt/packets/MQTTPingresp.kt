package mqtt.packets

import encodeVariableByteInteger
import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.streams.ByteArrayOutputStream

class MQTTPingresp : MQTTPacket {

    override fun toByteArray(): UByteArray {
        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.PINGRESP.value shl 4) and 0xF0
        result.write(fixedHeader.toUInt())
        result.encodeVariableByteInteger(0u)
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPingresp {
            checkFlags(flags)
            if (data.isNotEmpty())
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return MQTTPingresp()
        }
    }
}
