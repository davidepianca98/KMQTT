package mqtt.packets.mqttv3_1_1

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqttv5.ReasonCode
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTTSuback(
    val packetIdentifier: UInt,
    val reasonCodes: List<SubackReturnCode>
) : MQTT3Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        reasonCodes.forEach {
            outStream.writeByte(it.value.toUInt())
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.SUBACK, 0)
    }

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTTSuback {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val packetIdentifier = inStream.read2BytesInt()
            val reasonCodes = mutableListOf<SubackReturnCode>()
            while (inStream.available() > 0) {
                val reasonCode = SubackReturnCode.valueOf(inStream.readByte().toInt())
                    ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                reasonCodes += reasonCode
            }

            return MQTTSuback(packetIdentifier, reasonCodes)
        }
    }
}
