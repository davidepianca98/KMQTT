package mqtt.packets.mqttv4

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTSuback
import mqtt.packets.mqttv5.ReasonCode
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

public class MQTT4Suback(
    packetIdentifier: UInt,
    public val reasonCodes: List<SubackReturnCode>
) : MQTTSuback(packetIdentifier), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        reasonCodes.forEach {
            outStream.writeByte(it.value.toUInt())
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.SUBACK, 0)
    }

    public companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Suback {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val packetIdentifier = inStream.read2BytesInt()
            val reasonCodes = mutableListOf<SubackReturnCode>()
            while (inStream.available() > 0) {
                val reasonCode = SubackReturnCode.valueOf(inStream.readByte().toInt())
                    ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                reasonCodes += reasonCode
            }

            return MQTT4Suback(packetIdentifier, reasonCodes)
        }
    }
}
