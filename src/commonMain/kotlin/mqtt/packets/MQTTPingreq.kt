package mqtt.packets

import encodeVariableByteInteger
import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.streams.ByteArrayOutputStream

class MQTTPingreq : MQTTPacket {

    override fun toByteArray(): UByteArray {
        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.PINGREQ.value shl 4) and 0xF0
        result.write(fixedHeader.toUInt())
        result.encodeVariableByteInteger(0u)
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPingreq {
            checkFlags(flags)
            if (data.isNotEmpty())
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            return MQTTPingreq()
        }
    }
}
