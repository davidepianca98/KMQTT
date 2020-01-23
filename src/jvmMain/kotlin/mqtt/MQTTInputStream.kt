package mqtt

import mqtt.packets.*
import java.io.DataInputStream
import java.io.InputStream

class MQTTInputStream(inputStream: InputStream, private val maximumPacketSize: UInt? = null) :
    DataInputStream(inputStream) {

    fun decodeVariableByteInteger(): UInt { // TODO remove duplicate once the multiplatform port is completed
        var multiplier = 1u
        var value = 0u
        do {
            val encodedByte = read().toUInt()
            value += (encodedByte and 127u) * multiplier
            if (multiplier > 128u * 128u * 128u) {
                throw Exception("Malformed Variable Byte Integer")
            }
            multiplier *= 128u
        } while ((encodedByte and 128u) != 0u)
        return value
    }

    fun readPacket(): MQTTPacket {
        val byte1 = read()
        val mqttControlPacketType = (byte1 shr 4) and 0b1111
        val flags = byte1 and 0b1111

        val type = MQTTControlPacketType.valueOf(mqttControlPacketType)!!

        val remainingLength = decodeVariableByteInteger().toInt()

        maximumPacketSize?.let {
            if (remainingLength + 2 > it.toInt())
                throw MQTTException(ReasonCode.PACKET_TOO_LARGE)
        }

        val packet = ByteArray(remainingLength)
        readFully(packet, 0, remainingLength)
        return parseMQTTPacket(type, flags, packet)
    }

    private fun parseMQTTPacket(type: MQTTControlPacketType, flags: Int, data: ByteArray): MQTTPacket {
        return when (type) {
            MQTTControlPacketType.CONNECT -> MQTTConnect.fromByteArray(flags, data)
            MQTTControlPacketType.Reserved -> throw MQTTException(
                ReasonCode.MALFORMED_PACKET
            )
            MQTTControlPacketType.CONNACK -> MQTTConnack.fromByteArray(flags, data)
            MQTTControlPacketType.PUBLISH -> MQTTPublish.fromByteArray(flags, data)
            MQTTControlPacketType.PUBACK -> MQTTPuback.fromByteArray(flags, data)
            MQTTControlPacketType.PUBREC -> MQTTPubrec.fromByteArray(flags, data)
            MQTTControlPacketType.PUBREL -> MQTTPubrel.fromByteArray(flags, data)
            MQTTControlPacketType.PUBCOMP -> MQTTPubcomp.fromByteArray(flags, data)
            MQTTControlPacketType.SUBSCRIBE -> MQTTSubscribe.fromByteArray(flags, data)
            MQTTControlPacketType.SUBACK -> MQTTSuback.fromByteArray(flags, data)
            MQTTControlPacketType.UNSUBSCRIBE -> MQTTUnsubscribe.fromByteArray(flags, data)
            MQTTControlPacketType.UNSUBACK -> MQTTUnsuback.fromByteArray(flags, data)
            MQTTControlPacketType.PINGREQ -> MQTTPingreq.fromByteArray(flags, data)
            MQTTControlPacketType.PINGRESP -> MQTTPingresp.fromByteArray(flags, data)
            MQTTControlPacketType.DISCONNECT -> MQTTDisconnect.fromByteArray(flags, data)
            MQTTControlPacketType.AUTH -> MQTTAuth.fromByteArray(flags, data)
        }
    }
}
